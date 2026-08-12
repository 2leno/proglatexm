package api.poja.app.conf;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

public class LocalStackConf {

  private static final String BUCKET_NAME = "exam-bucket";
  private static final String QUEUE_NAME = "exam-queue";
  private static String queueUrl;

  static final LocalStackContainer LOCALSTACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0.0"))
          .withServices("s3", "sqs", "events")
          .withEnv("DEBUG", "0");

  static {
    LOCALSTACK.start();
    ensureBucket();
    ensureQueue();
  }

  private static void ensureBucket() {
    s3Client().createBucket(bucket -> bucket.bucket(BUCKET_NAME));
  }

  private static void ensureQueue() {
    queueUrl = sqsClient().createQueue(queue -> queue.queueName(QUEUE_NAME)).queueUrl();
  }

  private static S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(LOCALSTACK.getEndpoint())
        .region(Region.of(LOCALSTACK.getRegion()))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .forcePathStyle(true)
        .build();
  }

  private static SqsClient sqsClient() {
    return SqsClient.builder()
        .endpointOverride(LOCALSTACK.getEndpoint())
        .region(Region.of(LOCALSTACK.getRegion()))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .build();
  }

  void configureProperties(DynamicPropertyRegistry registry) {
    var endpoint = LOCALSTACK.getEndpoint().toString();
    registry.add("aws.region", LOCALSTACK::getRegion);
    registry.add("aws.s3.bucket", () -> BUCKET_NAME);
    registry.add("aws.s3.endpoint", () -> endpoint);
    registry.add("aws.sqs.endpoint", () -> endpoint);
    registry.add("aws.sqs.queue.url", () -> queueUrl);
    registry.add("aws.eventBridge.bus", () -> "default");
    registry.add("aws.eventBridge.endpoint", () -> endpoint);
  }
}
