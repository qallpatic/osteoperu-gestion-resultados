package pe.com.qallpatic.osteoperu.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Beans de AWS SDK v2 (S3Client + S3Presigner) cuando el storage es S3.
 * Habilitado solo si {@code storage.type=s3}.
 */
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3Config {

	@Bean(destroyMethod = "close")
	public S3Client s3Client(StorageProperties props) {
		StorageProperties.S3 cfg = props.getS3();
		return S3Client.builder()
				.endpointOverride(URI.create(cfg.getEndpoint()))
				.region(Region.of(cfg.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())))
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(cfg.isForcePathStyle())
						.build())
				.build();
	}

	@Bean(destroyMethod = "close")
	public S3Presigner s3Presigner(StorageProperties props) {
		StorageProperties.S3 cfg = props.getS3();
		return S3Presigner.builder()
				.endpointOverride(URI.create(cfg.getEndpoint()))
				.region(Region.of(cfg.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())))
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(cfg.isForcePathStyle())
						.build())
				.build();
	}
}
