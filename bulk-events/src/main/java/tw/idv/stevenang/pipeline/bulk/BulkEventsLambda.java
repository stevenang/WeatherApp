package tw.idv.stevenang.pipeline.bulk;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import tw.idv.stevenang.pipeline.common.WeatherEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BulkEventsLambda {

    private final ObjectMapper objectMapper = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
    );

    private AmazonSQS amazonSQS;
    private AmazonSNS amazonSNS;
    private AmazonS3 amazonS3;
    private String snsTopic;
    private String sqsURL;

    public BulkEventsLambda() {
        this(AmazonSNSClientBuilder.defaultClient(),
                AmazonS3ClientBuilder.defaultClient(),
                AmazonSQSClientBuilder.defaultClient());
    }

    public BulkEventsLambda(AmazonSNS sns, AmazonS3 s3, AmazonSQS sqs) {
        this.amazonSNS = sns;
        this.amazonS3 = s3;
        this.snsTopic = System.getenv("FAN_OUT_TOPIC");

        final String queue_name = System.getenv("QUEUE_NAME");
        System.out.println("Queue Name: " + queue_name);

        this.amazonSQS = sqs;
        this.sqsURL = sqs.getQueueUrl(queue_name).getQueueUrl();
        System.out.println(this.sqsURL);

        if (this.snsTopic == null || this.snsTopic.isEmpty()) {
            throw new RuntimeException(String.format("%s must be set.",
                    "FAN_OUT_TOPIC_ENV"));
        }
    }

    public void handlerRequest(S3Event event) {
        // Read the events and get all records in datafile and convert it
        // into WeatherEvent then put it into a List
        List<WeatherEvent> events = event.getRecords()
                .stream()
                .map(this::getObjectFromS3)
                .map(this::readWeatherEvents)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Convert each event in the event list to a string message and send
        // it to Amazon SNS Service specified
        events.stream()
                .map(this::weatherToMessage)
                .forEach(this::publicToSNS);

        System.out.println("Published " + events.size() + " weather events to" +
                " SNS");
    }

    private void publicToSNS(String message) {
        this.amazonSNS.publish(snsTopic, message);
        this.amazonSQS.sendMessage(this.sqsURL, message);

    }

    private void addToSQS(String message) {
        this.amazonSQS.sendMessage(this.sqsURL, message);
    }

    private S3Object getObjectFromS3(
            S3EventNotification.S3EventNotificationRecord record) {

        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();

        return amazonS3.getObject(bucket, key);
    }

    private List<WeatherEvent> readWeatherEvents(S3Object object) {

        try (InputStream input = object.getObjectContent()) {

            final WeatherEvent [] events = objectMapper.readValue(input,
                    WeatherEvent[].class);

            // Delete Uploaded object from the bucket
            this.amazonS3.deleteObject(
                    object.getBucketName(),
                    object.getKey()
            );

            return Arrays.asList(events);

        } catch (IOException exception) {
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }

    private String weatherToMessage(WeatherEvent event) {
        try {
            return this.objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }
}
