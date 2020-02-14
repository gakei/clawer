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

public class Crawler {

    private CrawlerDao dao = new JdbcCrawlerDao();

    public void run() throws SQLException, IOException {
        String link;

        while ((link = dao.getNextLinkThenDelete()) != null) {
            if (dao.isLinkProcessed(link)) {
                continue;
            }

            if (isInteresting(link)) {
                System.out.println(link);

                Document doc = httpGetAndParseHtml(link);

                if (doc == null) {
                    continue;
                }

                parseUrlsFromPageAndStoreIntoDatabase(doc);

                storeIntoDatabaseIfItIsNewsPage(doc, link);

                dao.updateDatabase(link, "INSERT INTO LINKS_ALREADY_PROCESSED (link) values (?)");
            }
        }
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https" + href;
            }

            if (!href.toLowerCase().startsWith("javascript")) {
                dao.updateDatabase(href, "INSERT INTO LINKS_TO_BE_PROCESSED(link) values (?)");
            }
        }
    }

    private void storeIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.storeNews(link, title, content);
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
    private boolean isInteresting(String link) {
        return (isNewsPage(link) || isIndexPage(link) && isNotLoginPage(link));
    }

    private boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}
