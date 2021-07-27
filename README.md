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
* Localstack test container for DynamoDB and Cloudformation
* Lombok based pojo's

