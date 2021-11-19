package co.cmatts.aws.s3;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.servicequotas.model.IllegalArgumentException;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class S3Client {

    private AmazonS3 client;

    public AmazonS3 getS3Client() {
        if (client != null) {
            return client;
        }

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        String localS3Endpoint = System.getenv("LOCAL_S3_ENDPOINT");
        String awsRegion = System.getenv("AWS_REGION");

        if (localS3Endpoint != null && awsRegion != null) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(localS3Endpoint, awsRegion));
        }

        client = builder.build();
        return client;
    }

    public boolean bucketExists(String bucket) {
        return getS3Client().doesBucketExistV2(bucket);
    }

    public void createBucket(String bucket) {
        getS3Client().createBucket(bucket);
    }

    public void writeToBucket(String s3Url, File file) throws IllegalArgumentException {
        try {
            URI uri = new URI(s3Url);
            assert uri.getScheme().equalsIgnoreCase("s3");

            getS3Client().putObject(uri.getHost(), uri.getPath(), file);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid s3 url");
        }
    }

    public void writeToBucket(String s3Url, String content) throws IllegalArgumentException {
        try {
            URI uri = new URI(s3Url);
            assert uri.getScheme().equalsIgnoreCase("s3");

            getS3Client().putObject(uri.getHost(), uri.getPath(), content);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid s3 url");
        }
    }

    public boolean fileExists(String s3Url) {
        try {
            URI uri = new URI(s3Url);
            assert uri.getScheme().equalsIgnoreCase("s3");
            return getS3Client().doesObjectExist(uri.getHost(), uri.getPath());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid s3 url");
        }
    }

    public InputStream readFromBucket(String s3Url) {
        try {
            URI uri = new URI(s3Url);
            assert uri.getScheme().equalsIgnoreCase("s3");
            return getS3Client().getObject(uri.getHost(), uri.getPath())
                    .getObjectContent();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid s3 url");
        }
    }
}
