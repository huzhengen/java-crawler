package com.github.huzhengen;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;

public class JdbcCrawlerDao implements CrawlerDao {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";
    private Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file:E:\\hcsp\\java-crawler\\crawler", USER_NAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getNextLink(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink("select link from links_to_be_processed limit 1");
        if (link != null) {
            updateDatabase(link, "delete from links_to_be_processed where link = ?");
        }
        return link;
    }

    private void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    public void insertNewsIntoDatabase(String url, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into news (url, title, content, created_at, modified_at)values(?,?,?,now(),now())")) {
            statement.setString(1, url);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.executeUpdate();
        }
    }

    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select link from links_already_processed where link = ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    @Override
    public void insertProcessedLink(String link) throws SQLException {
        updateDatabase(link, "insert into links_already_processed link (values ) (?)");
    }

    @Override
    public void insertLinkToBeProcessed(String href) throws SQLException {
        updateDatabase(href, "insert into links_to_be_processed link (values ) (?)");
    }
}
