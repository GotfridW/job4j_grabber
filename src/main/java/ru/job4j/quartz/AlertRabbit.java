package ru.job4j.quartz;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class AlertRabbit {
    private static Properties getProperties() throws IOException {
        try (var in = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            var properties = new Properties();
            properties.load(in);
            return properties;
        }
    }

    private static Connection getConnection(Properties properties) throws
            ClassNotFoundException, SQLException {
        Class.forName(properties.getProperty("driver"));
        var url = properties.getProperty("url");
        var login = properties.getProperty("login");
        var password = properties.getProperty("password");
        return DriverManager.getConnection(url, login, password);
    }

    public static void main(String[] args) throws IOException {
        var properties = getProperties();
        try (var connection = getConnection(properties)) {
            var scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            var data = new JobDataMap();
            data.put("connection", connection);
            var job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            var interval = Integer.parseInt(properties
                    .getProperty("rabbit.interval"));
            var times = simpleSchedule()
                    .withIntervalInSeconds(interval)
                    .repeatForever();
            var trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
            try (var statement = connection.createStatement()) {
                var resultSet = statement.executeQuery("select * from rabbit");
                while (resultSet.next()) {
                    System.out.println(resultSet.getString("created_date"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Rabbit implements Job {

        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            var cn = (Connection) context.getJobDetail().getJobDataMap().get("connection");
            try (var ps = cn.prepareStatement("insert into rabbit(created_date) values (?)")) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().withNano(0)));
                ps.executeUpdate();
            } catch (SQLException sq) {
                sq.printStackTrace();
            }
        }
    }
}
