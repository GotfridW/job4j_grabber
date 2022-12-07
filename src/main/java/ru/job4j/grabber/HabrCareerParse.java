package ru.job4j.grabber;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class HabrCareerParse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    private String retrieveDescription(String link) throws IOException {
        var document = Jsoup.connect(link).get();
        var descriptionElement = document.selectFirst(".style-ugc");
        return Objects.requireNonNull(descriptionElement).text();
    }

    public static void main(String[] args) throws IOException {
        for (var i = 1; i <= 5; i++) {
            var connection = Jsoup.connect(PAGE_LINK + i);
            var document = connection.get();
            var rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                var titleElement = row.select(".vacancy-card__title").first();
                var linkElement = Objects.requireNonNull(titleElement).child(0);
                var dateElement = row.select(".basic-date");
                var vacancyName = titleElement.text();
                var link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                var dateAttr = dateElement.attr("datetime");
                var dateTime = LocalDateTime.parse(dateAttr, DateTimeFormatter.ISO_DATE_TIME);
                System.out.printf("%s %s %s %n", dateTime, vacancyName, link);
            });
        }
    }
}
