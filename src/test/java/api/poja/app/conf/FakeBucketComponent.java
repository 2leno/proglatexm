package api.poja.app.conf;

import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.file.bucket.BucketConf;
import api.poja.app.file.hash.FileHash;
import api.poja.app.file.hash.FileHashAlgorithm;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import org.mockito.Mockito;

public class FakeBucketComponent extends BucketComponent {

  private final ConcurrentHashMap<String, File> filesByKey = new ConcurrentHashMap<>();

  public FakeBucketComponent() {
    super(Mockito.mock(BucketConf.class));
  }

  @Override
  public FileHash upload(File file, String bucketKey) {
    filesByKey.put(bucketKey, file);
    return new FileHash(FileHashAlgorithm.NONE, null);
  }

  @Override
  public File download(String bucketKey) {
    return filesByKey.get(bucketKey);
  }

  @Override
  @SneakyThrows
  public URL presign(String bucketKey, Duration expiration) {
    if (bucketKey.startsWith("http")) {
      return URI.create(bucketKey).toURL();
    }
    return URI.create("http://localhost/" + bucketKey).toURL();
  }
}
