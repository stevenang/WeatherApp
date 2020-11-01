package tw.idv.stevenang.pipeline.single;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import tw.idv.stevenang.pipeline.common.WeatherEvent;

import java.io.IOException;

public class SingleEventLambda {

    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final DynamoDB dynamoDB = new DynamoDB(
            AmazonDynamoDBAsyncClientBuilder.defaultClient()
    );

    private final String tableName = System.getenv("LOCATIONS_TABLE");

    public void handlerRequest(SNSEvent event) {
        event.getRecords()
                .stream()
                .map(record -> record.getSNS().getMessage())
                .map(this::readWeatherEvent)
                .forEach(this::writeWeatherEventToDynamoDB);
    }

    /**
     * This method will convert a String weather message to a WeatherEvent
     * class object
     * @param message   Weather event String
     * @return          WeatherEvent
     */
    private WeatherEvent readWeatherEvent(String message) {
        System.out.println("Message receieved: " + message);
        try {
            return objectMapper.readValue(message, WeatherEvent.class);
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }

    /**
     * This method will write the given WeatherEvent object into DynomoDB
     * @param weatherEvent  Weather Event Object to be written into DynamoDB
     */
    private void writeWeatherEventToDynamoDB(WeatherEvent weatherEvent) {

        System.out.println("Received Weather Event: ");
        System.out.println(weatherEvent);

        final Table table = dynamoDB.getTable(tableName);
        final Item item = new Item().withPrimaryKey(
                "locationName", weatherEvent.locationName)
                .withDouble("temperature", weatherEvent.temperature)
                .withLong("timestamp", weatherEvent.timestamp)
                .withDouble("longitute", weatherEvent.longitute)
                .withDouble("latitute", weatherEvent.latitute);
        table.putItem(item);

    }

    /**
     * This method will write the given weather event into the RDS
     * @param weatherEvent
     */
    private void writeWeatherEventTRDS(WeatherEvent weatherEvent) {
        System.out.println("Received Weather Event: ");
        System.out.println(weatherEvent);
    }
}
