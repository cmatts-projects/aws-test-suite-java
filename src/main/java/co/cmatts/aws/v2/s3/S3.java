package co.cmatts.aws.v2.s3;

import org.apache.http.HttpStatus;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.nio.file.Path;

import static co.cmatts.aws.v2.client.Configuration.configureEndPoint;

public class S3 {

    private static S3Client client;

    public static S3Client getS3Client() {
        if (client != null) {
            return client;
        }

        S3ClientBuilder builder = S3Client.builder();
        configureEndPoint(builder);

        client = builder.build();
        return client;
    }

    public static void resetS3Client() {
        client = null;
    }

    public static boolean bucketExists(String bucket) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucket)
                .build();
        HeadBucketResponse headBucketResponse = getS3Client().headBucket(headBucketRequest);
        return HttpStatus.SC_OK == headBucketResponse.sdkHttpResponse().statusCode();
    }

    public static void createBucket(String bucket) {
        CreateBucketRequest createBuckerRequest = CreateBucketRequest.builder()
                .bucket(bucket)
                .build();
        getS3Client().createBucket(createBuckerRequest);

        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucket)
                .build();
        getS3Client().waiter().waitUntilBucketExists(headBucketRequest);
    }

    public static void writeToBucket(String bucket, String key, Path path) throws IllegalArgumentException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        getS3Client().putObject(putObjectRequest, path);
    }

    public static void writeToBucket(String bucket, String key, String content) throws IllegalArgumentException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        RequestBody requestBody = RequestBody.fromString(content);
        getS3Client().putObject(putObjectRequest, requestBody);
    }

    public static boolean fileExists(String bucket, String key) {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        HeadObjectResponse headObjectResponse = getS3Client().headObject(headObjectRequest);

        return HttpStatus.SC_OK == headObjectResponse.sdkHttpResponse().statusCode();
    }

    public static InputStream readFromBucket(String bucket, String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return getS3Client().getObject(getObjectRequest);
    }
}
