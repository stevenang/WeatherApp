package tw.idv.stevenang.pipeline.single;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import tw.idv.stevenang.pipeline.common.WeatherEvent;
import tw.idv.stevenang.pipeline.common.dao.RecordDao;

import javax.sql.DataSource;
import java.io.IOException;

public class SQSEventLambda {

    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public SQSEventLambda() {
        this.dbUrl = System.getenv("DB_URL");
        this.dbUser = System.getenv("DB_USER");
        this.dbPassword = System.getenv("DB_Password");
    }

    public void handlerRequest(SQSEvent event) {

        event.getRecords()
                .stream()
                .map(record -> record.getBody())
                .map(this::readWeatherEvent)
                .forEach(this::writeWeatherEventToRDS);

    }

    /**
     * This method will convert a String weather message to a WeatherEvent
     * class object
     * @param message   Weather event String
     * @return          WeatherEvent
     */
    private WeatherEvent readWeatherEvent(String message) {
        try {
            return objectMapper.readValue(message, WeatherEvent.class);
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }

    private void writeWeatherEventToRDS(WeatherEvent event) {
        RecordDao.addRecord(
                this.dbUrl, this.dbUser, this.dbPassword, event
        );
    }
}
