package ru.job4j.grabber;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.job4j.grabber.utils.DateTimeParser;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);
    private static final int PAGES = 5;
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private String retrieveDescription(String link) {
        Document document;
        try {
            document = Jsoup.connect(link).get();
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format(
                    "Error getting description from %s", link));
        }
        return Objects.requireNonNull(document.selectFirst(".style-ugc")).text();
    }

    private Post createPost(Element element) {
        var linkElement = element.select(".vacancy-card__title").first();
        var dateElement = element.select(".basic-date");
        var title = Objects.requireNonNull(linkElement).child(0).text();
        var link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        var description = retrieveDescription(link);
        var created = dateTimeParser.parse(dateElement.attr("datetime"));
        return new Post(title, link, description, created);
    }

    @Override
    public List<Post> list(String link) {
        List<Post> postList = new ArrayList<>();
        try {
            for (var i = 1; i <= PAGES; i++) {
                var document = Jsoup.connect(link + i).get();
                var cards = document.select(".vacancy-card__inner");
                cards.stream()
                        .map(this::createPost)
                        .forEach(postList::add);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error getting document");
        }
        return postList;
    }
}
