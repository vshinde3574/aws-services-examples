package com.springboot.aws.lambda.functions;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Map;
import java.util.function.Function;

@Component("SpringCloudGetProducts")
public class ProductLambdaHandleSpringCloudFunction implements Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
private String DYNAMO_TABLE = "Products";

	@Override
	public APIGatewayProxyResponseEvent apply(APIGatewayProxyRequestEvent input) {
		Map<String, String> pathParams = input.getPathParameters();
		Map<String, String> queryParams = input.getQueryStringParameters();
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);

		int id;
		Item resItem = null;
		APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
		try {
			if (pathParams.size() != 0) {
				if (pathParams.get("id") != null) {
					id = Integer.parseInt(pathParams.get("id"));
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id", id);
				}
			}
			//queryStringParameters
			else if (queryParams.size() != 0) {
				if (pathParams.get("id") != null) {
					id = Integer.parseInt(pathParams.get("id"));
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id", id);
				}
			}

			responseEvent.setBody(resItem.toJSON());
			responseEvent.setStatusCode(200);
		}catch (Exception e){
			responseEvent.setBody("Internal server error");
			responseEvent.setStatusCode(500);
			e.printStackTrace();
		}
		return responseEvent;
	}
}