package searchengine.utils.parse;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.UserAgentsCfg;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.lang.Thread.sleep;

public class LinkParser {

    public static Connection getConnection(String url, UserAgentsCfg userAgents) {
        //sleep(150);
        return Jsoup.connect(url)
                .userAgent(userAgents.getRandomUser())
                .referrer(userAgents.getRandomUser())
                .ignoreHttpErrors(true)
                //.timeout(15_000)
                .followRedirects(true);
    }

    public static int getStatusCode(Connection connection) throws IOException  {
        return connection.execute().statusCode();
    }

    public static String getContent(Connection connection) throws IOException {
        if (getStatusCode(connection) != 200) {
            return "";
        }
        return connection.get().html();
    }

    public static ConcurrentSkipListSet<String> getLinks(Connection connection) throws IOException {
        ConcurrentSkipListSet<String> links = new ConcurrentSkipListSet<>();
        Document document = connection.get();
        Elements elements = document.select("body").select("a");

        for (Element element : elements) {
            String link = element.absUrl("href");
            if (isLink(link) && !isFile(link) && !link.isEmpty()) {
                link = link.split("#")[0];
                links.add(link);
            }
        }
        return links;
    }

    private static boolean isLink(String link) {
        String regex = "^(https?|ftp)://[-\\w+&@#/%?=~_|!:,.;]*[-\\w+&@#/%=~_|]";
        return link.matches(regex);

    }

    private static boolean isFile(String link) {
        link = link.toLowerCase();
        return link.contains(".jpg")
                || link.contains(".jpeg")
                || link.contains(".png")
                || link.contains(".gif")
                || link.contains(".webp")
                | link.contains(".pdf")
                || link.contains(".eps")
                || link.contains(".xlsx")
                || link.contains(".doc")
                || link.contains(".pptx")
                || link.contains(".docx")
                || link.contains("?_ga");
    }


}
