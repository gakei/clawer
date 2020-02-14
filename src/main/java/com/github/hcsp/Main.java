package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

public class Main {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";

    private static String getNextLink(Connection connection, String sql) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return null;
    }

    private static String getNextLinkThenDelete(Connection connection) throws SQLException {
        String link = getNextLink(connection, "select link from LINKS_TO_BE_PROCESSED LIMIT 1");
        if (link != null) {
            updateDatabase(connection, link, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?");
        }
        return link;
    }


    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:C:/Users/WHJ/Desktop/clawer/news", USER_NAME, PASSWORD);

        String link;

        while ((link = getNextLinkThenDelete(connection)) != null) {
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInteresting(link)) {
                System.out.println(link);

                Document doc = httpGetAndParseHtml(link);

                if (doc == null) {
                    continue;
                }

                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);

                storeIntoDatabaseIfItIsNewsPage(connection, doc, link);

                updateDatabase(connection, link, "INSERT INTO LINKS_ALREADY_PROCESSED (link) values (?)");
            }
        }
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https" + href;
            }

            if (!href.toLowerCase().startsWith("javascript")) {
                updateDatabase(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED(link) values (?)");
            }
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT LINK from LINKS_ALREADY_PROCESSED where LINK = ?")) {
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

    private static void updateDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Connection connection, Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));

                try (PreparedStatement statement = connection.prepareStatement("insert into NEWS (url, title, CONTENT, created_at, modified_at)Values(?,?,?,now(),now())")) {
                    statement.setString(1, link);
                    statement.setString(2, title);
                    statement.setString(3, content);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        System.out.println(link);
        HttpGet httpGet = null;
        try {
            httpGet = new HttpGet(link);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }

        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();

            String html = EntityUtils.toString(entity1);
            return Jsoup.parse(html);
        }
    }

    //排除登陆页面
    private static boolean isInteresting(String link) {
        return (isNewsPage(link) || isIndexPage(link) && isNotLoginPage(link));
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}
