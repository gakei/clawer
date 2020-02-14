package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {

    String getNextLinkThenDelete() throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;

    void insertProcessedLink(String link);

    void insertLinkToBeProcessed(String href);
}
