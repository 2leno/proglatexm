package api.poja.app.endpoint.event;

import api.poja.app.PojaGenerated;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@PojaGenerated
@Configuration
public class EventConf {
  private final Region region;
  private final String sqsEndpoint;

  public EventConf(
      @Value("${aws.region:eu-west-3}") Region region,
      @Value("${aws.sqs.endpoint:}") String sqsEndpoint) {
    this.region = region;
    this.sqsEndpoint = sqsEndpoint;
  }

  @Bean
  public SqsClient getSqsClient() {
    SqsClientBuilder builder = SqsClient.builder().region(region);
    if (sqsEndpoint != null && !sqsEndpoint.isEmpty()) {
      builder
          .endpointOverride(URI.create(sqsEndpoint))
          .credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
    }
    return builder.build();
  }
}
