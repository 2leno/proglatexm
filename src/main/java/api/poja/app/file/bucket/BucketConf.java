package api.poja.app.file.bucket;

import api.poja.app.PojaGenerated;
import java.net.URI;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@PojaGenerated
@Configuration
public class BucketConf {

  @Getter private final String bucketName;
  @Getter private final S3TransferManager s3TransferManager;
  @Getter private final S3Presigner s3Presigner;
  @Getter private final S3Client s3Client;

  @SneakyThrows
  public BucketConf(
      @Value("${aws.region:eu-west-3}") String regionString,
      @Value("${aws.s3.bucket}") String bucketName,
      @Value("${aws.s3.endpoint:}") String endpointString) {
    this.bucketName = bucketName;
    var region = Region.of(regionString);
    var endpoint = endpointString.isEmpty() ? null : URI.create(endpointString);
    var credentialsProvider =
        endpoint == null
            ? null
            : StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    var s3AsyncClientBuilder = S3AsyncClient.crtBuilder().region(region);
    var s3ClientBuilder = S3Client.builder().region(region);
    var s3PresignerBuilder = S3Presigner.builder().region(region);
    if (endpoint != null) {
      s3AsyncClientBuilder
          .endpointOverride(endpoint)
          .credentialsProvider(credentialsProvider)
          .forcePathStyle(true);
      s3ClientBuilder
          .endpointOverride(endpoint)
          .credentialsProvider(credentialsProvider)
          .forcePathStyle(true);
      s3PresignerBuilder.endpointOverride(endpoint).credentialsProvider(credentialsProvider);
    }
    this.s3TransferManager =
        S3TransferManager.builder().s3Client(s3AsyncClientBuilder.build()).build();
    this.s3Presigner = s3PresignerBuilder.build();
    this.s3Client = s3ClientBuilder.build();
  }
}
