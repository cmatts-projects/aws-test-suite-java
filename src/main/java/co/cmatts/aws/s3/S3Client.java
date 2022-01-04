package co.cmatts.aws.s3;

import co.cmatts.aws.client.Configuration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.servicequotas.model.IllegalArgumentException;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static co.cmatts.aws.client.Configuration.configureEndPoint;

public class S3Client {

    private static AmazonS3 client;

    public static AmazonS3 getS3Client() {
        if (client != null) {
            return client;
        }

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public static void resetS3Client() {
        client = null;
    }

    public static boolean bucketExists(String bucket) {
        return getS3Client().doesBucketExistV2(bucket);
    }

    public static void createBucket(String bucket) {
        getS3Client().createBucket(bucket);
    }

    public static void writeToBucket(String s3Url, File file) throws IllegalArgumentException {
        try {
            URI uri = new URI(s3Url);
            assert uri.getScheme().equalsIgnoreCase("s3");

            getS3Client().putObject(uri.getHost(), uri.getPath(), file);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid s3 url");
        }
    }

    public static void writeToBucket(String s3Url, String content) throws IllegalArgumentException {
        try {
            URI uri = new URI(s3Url);
            assert uri.getScheme().equalsIgnoreCase("s3");

            getS3Client().putObject(uri.getHost(), uri.getPath(), content);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid s3 url");
        }
    }

    public static boolean fileExists(String s3Url) {
        try {
            URI uri = new URI(s3Url);
            assert uri.getScheme().equalsIgnoreCase("s3");
            return getS3Client().doesObjectExist(uri.getHost(), uri.getPath());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid s3 url");
        }
    }

    public static InputStream readFromBucket(String s3Url) {
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
