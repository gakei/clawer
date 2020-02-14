package com.github.hcsp;

import java.sql.Connection;
import java.sql.SQLException;

public class MybatisDao implements CrawlerDao {
    @Override
    public String getNextLink(Connection connection, String sql) throws SQLException {
        return null;
    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        return null;
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        return false;
    }

    @Override
    public void storeNews(String link, String title, String content) throws SQLException {

    }

    @Override
    public void updateDatabase(String link, String sql) throws SQLException {

    }
}
