package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.TriggerBuilder.*;

public class AlertRabbit {
    private static Properties getProperties() {
        var properties = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static Connection getConnection(Properties properties) throws
            ClassNotFoundException, SQLException {
        Class.forName(properties.getProperty("driver"));
        String url = properties.getProperty("url");
        String login = properties.getProperty("login");
        String password = properties.getProperty("password");
        return DriverManager.getConnection(url, login, password);
    }

    public static void main(String[] args) {
        var properties = getProperties();
        try (var connection = getConnection(properties)) {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            var data = new JobDataMap();
            data.put("connection", connection);
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            int interval = Integer.parseInt(getProperties()
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
                ResultSet set = statement.executeQuery("select * from rabbit");
                while (set.next()) {
                    System.out.println(set.getString("created_date"));
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
        public void execute(JobExecutionContext context) throws JobExecutionException {
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
