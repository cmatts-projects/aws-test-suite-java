package awsv2.repackaged.software.amazon.payloadoffloading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * This class is used for carrying pointer to Amazon S3 objects which contain payloads.
 */
public class PayloadS3Pointer {
    private static final Logger LOG = LoggerFactory.getLogger(PayloadS3Pointer.class);
    private String s3BucketName;
    private String s3Key;

    // Needed for Jackson
    private PayloadS3Pointer() {
    }

    public PayloadS3Pointer(String s3BucketName, String s3Key) {
        this.s3BucketName = s3BucketName;
        this.s3Key = s3Key;
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String toJson() {
        String s3PointerStr = null;
        try {
            JsonDataConverter jsonDataConverter = new JsonDataConverter();
            s3PointerStr = jsonDataConverter.serializeToJson(this);

        } catch (Exception e) {
            String errorMessage = "Failed to convert S3 object pointer to text.";
            LOG.error(errorMessage, e);
            throw SdkClientException.create(errorMessage, e);
        }
        return s3PointerStr;
    }

    public static PayloadS3Pointer fromJson(String s3PointerJson) {
        PayloadS3Pointer s3Pointer = null;
        try {
            JsonDataConverter jsonDataConverter = new JsonDataConverter();
            s3Pointer = jsonDataConverter.deserializeFromJson(s3PointerJson, PayloadS3Pointer.class);

        } catch (Exception e) {
            String errorMessage = "Failed to read the S3 object pointer from given string.";
            LOG.error(errorMessage, e);
            throw SdkClientException.create(errorMessage, e);
        }
        return s3Pointer;
    }
}
