package com.github.hcsp;

import java.sql.Connection;
import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLink(Connection connection, String sql) throws SQLException;

    String getNextLinkThenDelete() throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void storeNews(String link, String title, String content) throws SQLException;

    void updateDatabase(String link, String sql) throws SQLException;
}
