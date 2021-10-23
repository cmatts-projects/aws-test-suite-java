# AWS Test Suite

The AWS Test Suite is a testing repository for AWS services.
This repo contains example implementations of AWS features and services along with a Localstack test implementation of those services.

# Pre-requisites

Docker must be installed and configured so that the current user can invoke containers. On Linux, this means adding docker and the user to a docker group.
Java 11+ must be installed.
Maven 3.8+ must be installed.

# Build
To build and test:
```bash
mvn clean verify
```

# Services
## DynamoDB
The dynamoDB example demonstrates how to create dynamo tables using Cloudformation and read and write to those tables using the DynamoDB mapper feature.

Features:
* Cloudformation definition of tables
* Bulk loading data
* Searching by Partition key
* Searching by GSI
* Use of DynamoDBMapper and annotations
* Table name prefix override configuration
* Optimistic locking
* Transactions
* Localstack test container for DynamoDB and Cloudformation
* Lombok based pojo's

## Cloudwatch
The cloudwatch example demonstrates some basic logging of cloudwatch metrics and extracting statistics.

Features:
* Bulk logging of metrics
* Getting average statistics from metrics
* Localstack test container for Cloudwatch

## Kinesis Streams
The Kinesis example demonstrates how to send and retrieve messages using a Kinesis data stream.

Features:
* Creation of a stream
* Waiting for the stream to be active
* Use of a Kinesis Producer to batch message send requests
* Listening to a Kinesis stream and collecting messages

## S3
The S3 example demonstrates how to store and retrieve content in an S3 bucket.

Features:
* Creation of an S3 bucket
* Verifying that a bucket exists
* Writing content to S3
* Reading content from S3
* Verifying that an S3 object exists

## Secrets Manager
The secrets manager examples demonstrate how to create, read and update secrets.

Features:
* Creation of a secret
* Updating a secret
* Reading a secret

