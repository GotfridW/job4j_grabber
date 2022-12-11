package ru.job4j.grabber;

import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {
    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            cnn = DriverManager.getConnection(
                    cfg.getProperty("url"),
                    cfg.getProperty("login"),
                    cfg.getProperty("password"));
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(Post post) {
        var created = Timestamp.valueOf(post.getCreated());
        try (var statement = cnn.prepareStatement(
                "insert into post (name, text, link, created) values (?, ?, ?, ?)"
                        + "on conflict (link) do nothing", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, created);
            statement.execute();
            try (var generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> postList = new ArrayList<>();
        try (var statement = cnn.prepareStatement(
                "select * from post")) {
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    postList.add(createPost(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return postList;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (var statement = cnn.prepareStatement(
                "select * from post where id = ?")) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    post = createPost(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return post;
    }

    private Post createPost(ResultSet set) throws SQLException {
        var id = set.getInt("id");
        var title = set.getString("name");
        var link = set.getString("link");
        var description = set.getString("text");
        var created = set.getTimestamp("created").toLocalDateTime();
        return new Post(id, title, link, description, created);
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public static void main(String[] args) {
        try (var in = PsqlStore.class.getClassLoader()
                .getResourceAsStream("grabber.properties")) {
            var cfg = new Properties();
            cfg.load(in);
            try (Store store = new PsqlStore(cfg)) {
                DateTimeParser parser = new HabrCareerDateTimeParser();
                Parse parse = new HabrCareerParse(parser);
                var link = "https://career.habr.com/vacancies/java_developer?page=";
                List<Post> jobs = parse.list(link);
                jobs.forEach(store::save);
                store.getAll().forEach(System.out::println);
                System.out.printf("%nPost with id #10:%n%s", store.findById(35));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
