package ru.job4j.grabber;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import ru.job4j.grabber.utils.DateTimeParser;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private String retrieveDescription(String link) throws IOException {
        var document = Jsoup.connect(link).get();
        var descriptionElement = document.selectFirst(".style-ugc");
        return Objects.requireNonNull(descriptionElement).text();
    }

    private Post createPost(Element element) throws IOException {
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
            for (var i = 1; i <= 5; i++) {
                var document = Jsoup.connect(link).get();
                var cards = document.select(".vacancy-card__inner");
                cards.forEach(card -> {
                    try {
                        postList.add(createPost(card));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return postList;
    }
}
