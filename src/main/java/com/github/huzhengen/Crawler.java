package com.github.huzhengen;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Crawler {
    private CrawlerDao dao = new MyBatisCrawlerDao();

    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }

    public void run() throws IOException, SQLException {
        String link;
        // 先从数据库拿出来一个link，并删除他，然后开始处理这个link。默认最开始里面有一个链接， 是"https://sina.cn"
        while ((link = dao.getNextLinkThenDelete()) != null) { // 如果link不是null的话，才进入这个while
            // 从数据库中 已处理的链接表 里查询，看当前链接是否被处理过了
            if (dao.isLinkProcessed(link)) {
                continue;
            }
            // 如果这个链接是我们感兴趣的，就开始处理
            if (isInterestingLink(link)) {
                System.out.println(link);
                // 解析HTML
                Document doc = httpGetAndParseHtml(link);

                parseUrlsFromPageAndStoreIntoDatabase(doc);

                // 如果是新闻详情页，就存入数据库
                storeIntoDatabaseIfItIsNewsPage(doc, link);

                // 这个链接处理过了，就把这个链接放入数据库的 已处理链接表 中
                dao.insertProcessedLink(link);
            }
        }
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            // 找出a标签链接
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            if(!href.contains("sohu.com") && !href.toLowerCase().startsWith("javascript")){
                href = "https://m.sohu.com" + href;
            }
            if (!href.toLowerCase().startsWith("javascript")) {
                // 把链接放到待处理的表中
                dao.insertLinkToBeProcessed(href);
            }

        }
    }

    private void storeIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select(".article-content-wrapper");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
//                String title = articleTags.get(0).child(0).text();
                String title = articleTags.get(0).child(1).text();
                System.out.println(title);
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.insertNewsIntoDatabase(link, title, content);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        // 只处理新浪站内的链接
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://m.sohu.com".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("m.sohu.com/a");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}


