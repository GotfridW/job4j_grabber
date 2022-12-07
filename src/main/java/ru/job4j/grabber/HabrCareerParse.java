package ru.job4j.grabber;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class HabrCareerParse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public static void main(String[] args) throws IOException {
        var connection = Jsoup.connect(PAGE_LINK);
        var document = connection.get();
        var rows = document.select(".vacancy-card__inner");
        rows.forEach(row -> {
            var titleElement = row.select(".vacancy-card__title").first();
            var linkElement = titleElement.child(0);
            var dateElement = row.select(".basic-date");
            var vacancyName = titleElement.text();
            var link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            var dateAttr = dateElement.attr("datetime");
            var dateTime = LocalDateTime.parse(dateAttr, DateTimeFormatter.ISO_DATE_TIME);
            System.out.printf("%s %s %s %n", dateTime, vacancyName, link);
        });
    }
}
