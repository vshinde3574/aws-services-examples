package com.springboot.aws.lambda.functions;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TriggerLambdaOnS3Event implements RequestHandler<S3Event, Boolean> {
    private static final AmazonS3 s3Client = AmazonS3Client.builder()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .build();

    private static String queue = "https://sqs.us-east-1.amazonaws.com/071572282510/queue-for-S3-lambda";
    private static final AmazonSQS sqsClient = AmazonSQSClientBuilder
            .standard()
            .withRegion(Regions.US_EAST_1)
            .build();


    @Override
    public Boolean handleRequest(S3Event input, Context context) {
        if(input.getRecords().isEmpty()){
            return false;
        }

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);

        //process the records
        for(S3EventNotification.S3EventNotificationRecord record: input.getRecords()){
            String bucketName = record.getS3().getBucket().getName();
            String objectKey = record.getS3().getObject().getKey();

            S3Object s3Object = s3Client.getObject(bucketName, objectKey);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();

            try(final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))){
                br.lines().skip(1)
                        .map(line ->{
                            String[] row = line.split(",");
                            com.springboot.aws.lambda.domain.Order order = new com.springboot.aws.lambda.domain.Order();
                            order.setId(Integer.parseInt(row[0]));
                            order.setName(row[1]);
                            order.setPrice(Double.parseDouble(row[2]));
                            return order;
                        }).forEach(order -> dynamoDBMapper.save(order));
            } catch (IOException e){
                e.printStackTrace();
                return false;
            }
        }
        sendSingleMessage();
        return true;
    }

    public static void sendSingleMessage() {
        try {
            sqsClient.sendMessage(new SendMessageRequest()
                    .withQueueUrl(queue)
                    .withMessageBody("Orders are created from S3 object to dynamo db")
                    .withDelaySeconds(10));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
