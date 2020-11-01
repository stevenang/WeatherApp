package tw.idv.stevenang.pipeline.common.dao;

import org.sql2o.Connection;
import org.sql2o.Sql2o;
import tw.idv.stevenang.pipeline.common.WeatherEvent;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class RecordDao {

    private static final String INSERT_SQL = "INSERT INTO WeatherRecord " +
            "(location_name, temperature, timestamp, longitute, latitute)" +
            "VALUES(:location_name, :temperature, :timestamp, :longitute, " +
            ":latitute)";

    public static void addRecord(String dbUrl, String dbUser,
                                 String dbPassword, WeatherEvent weather) {

        Sql2o sql2o = new Sql2o(dbUrl, dbUser, dbPassword);

        try (Connection connection = sql2o.beginTransaction()) {
            connection.createQuery(INSERT_SQL)
                    .addParameter("location_name", weather.locationName)
                    .addParameter("temperature", weather.temperature)
                    .addParameter("timestamp", weather.timestamp)
                    .addParameter("longitute", weather.longitute)
                    .addParameter("latitute", weather.latitute)
                    .executeUpdate();
            connection.commit();
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }
}
