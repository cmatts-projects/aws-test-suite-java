# AWS Test Suite for Java

The AWS Test Suite is a testing repository for AWS services in Java.
This repo contains sample implementations of AWS features and services along with a Localstack test implementation of
those services.

For completeness this suite includes sample implementations of services for the AWS Java SDK V1 and SDK V2.

_Note:_ There is a large overlap between the v1 and v2 test cases and to a lesser extent the sample implementations
resulting in a lot of duplication of code. This is intentional to help isolate the implementations and aid with the
readability of the samples and the tests.

# Pre-requisites

Docker must be installed and configured so that the current user can invoke containers. On Linux, this means adding
docker and the user to a docker group.
Java 11+ must be installed.
Maven 3.8+ must be installed.

# Build
To build and test:
```bash
mvn clean verify
```

# Services
## DynamoDB
The dynamoDB samples demonstrate how to create dynamo tables using Cloudformation and read and write to those tables.

Features:
* Cloudformation definition of tables
* Bulk loading data
* Searching by Partition keys
* Searching by Secondary Partition keys
* Use of DynamoDBMapper and annotations (SDK V1)
* Use of DynamoDbEnhancedClient and annotations (SDK V2)
* Table name prefix override configuration
* Optimistic locking
* Transactions
* Localstack test container for DynamoDB and Cloudformation
* Lombok based pojo's

### Implementation Notes
There are some significant differences between SDK V1 and V2 implementations. Most notably:
* SDK V1 uses a DynamoDBMapper to map data to POJO's whereas SDK V2 uses a DynamoDbEnhancedClient to map data to POJO's
* SDK V1 ignores versioning when bulk loading whereas SDK V2 requires the enhanced client to have the versioning
extension disabled
* SDK V1 uses annotations and client configuration to override table name mapping whereas SDK V2 requires table name
overriding and prefixing to be specified when the accessing the table.

All of these differences have been overcome and encapsulated in the v1 and v2 sample implementations.

The SDK V2 samples implemented here make use of the new asynchronous Dynamo DB client to illustrate how to handle
asynchronous responses.

## Cloudwatch
The cloudwatch samples demonstrate some basic logging of cloudwatch metrics and extracting statistics.

Features:
* Bulk logging of metrics
* Getting average statistics from metrics
* Localstack test container for Cloudwatch

## Kinesis Streams
The Kinesis samples demonstrate how to send and retrieve messages using a Kinesis data stream.

Features:
* Creation of a stream
* Waiting for the stream to be active
* Use of a Kinesis Producer to batch message send requests (SDK V1)
* Use of a Kinesis Client to batch message send requests (SDK V2)
* Listening to a Kinesis stream and collecting messages

### Implementation Notes
As the time of writing, there is no AWS Kinesis Producer available for SDK V2. Therefore batch sending must be
implemented using the standard Kinesis Client.

## S3
The S3 samples demonstrate how to store and retrieve content in an S3 bucket.

Features:
* Creation of an S3 bucket
* Verifying that a bucket exists
* Writing content to S3
* Reading content from S3
* Verifying that an S3 object exists

## Secrets Manager
The secrets manager samples demonstrate how to create, read and update secrets.

Features:
* Creation of a secret
* Updating a secret
* Reading a secret

## Parameter Store
The parameter store samples demonstrate how to create and read parameters.

Features:
* Creation of a parameter
* Reading a parameter

## SQS
The Sqs samples demonstrate how to create queues and send and receive messages.

Features:
* Creation of a queue
* Sending messages to a queue
* Receiving messages from a queue
* S3 support for large messages
* Queue purging

### Implementation Notes
In order to be able to use SDK V1 and SDK V2 alongside one and other. I have included repackaged versions of:

* amazon-sqs-java-extended-client-lib v2.0.3
  * Available at https://github.com/awslabs/amazon-sqs-java-extended-client-lib
* payload-offloading-java-common-lib-for-aws v2.1.3
  * Available at https://github.com/awslabs/payload-offloading-java-common-lib-for-aws

These third party packages have been repackaged with the prefix `awsv2.repackaged` to prevent class conflicts and enable
both versions to be loaded simultaneously.

Both repos are available under an Apache 2.0 license.

## Lambda
The Lambda samples demonstrate how to handle events from a variety of sources.

Features:
* Simple object event handling
* Stream event object handling
* Sqs event handling
* Extended Sqs event handling

### Implementation Notes
The AWS Lambda Services sit outside of any AWS SDK versions and are therefore not version specific.
That said, the test cases and implementation of the Sqs based Lambdas currently use the v1 Sqs client samples to send
and receive messages. This can easily be switched to use the v2 client sample as the interfaces are the same.
