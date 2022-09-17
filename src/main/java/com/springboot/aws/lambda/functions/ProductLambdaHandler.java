package com.springboot.aws.lambda.functions;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.util.Base64;
import com.google.gson.Gson;
import com.springboot.aws.lambda.domain.OrderDTO;
import com.springboot.aws.lambda.domain.Product;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ProductLambdaHandler implements RequestStreamHandler {

	private String DYNAMO_TABLE = "Products";
	private static final AWSKMS AWSKMS_CLIENT = AWSKMSClientBuilder
			.standard()
			.withRegion(Regions.US_EAST_1)
			.build();


	@Override
	public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {

		OutputStreamWriter writer = new OutputStreamWriter(output);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		JSONParser parser = new JSONParser();
		JSONObject responseObject = new JSONObject();
		JSONObject responseBody = new JSONObject();

		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);

		int id;
		Item resItem = null;

		try {
			JSONObject reqObject = (JSONObject) parser.parse(reader);
			//pathParameters
			if (reqObject.get("pathParameters")!=null) {
				JSONObject pps = (JSONObject)reqObject.get("pathParameters");
				if (pps.get("id")!=null) {
					id = Integer.parseInt((String)pps.get("id"));
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id",id);
				}
			}
			//queryStringParameters
			else if (reqObject.get("queryStringParameters")!=null) {
				JSONObject qps =(JSONObject) reqObject.get("queryStringParameters");
				if (qps.get("id")!=null) {
					id= Integer.parseInt((String)qps.get("id"));
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id",id);
				}
			}

			if (resItem!=null) {
				Product product = new Product(resItem.toJSON());
				responseBody.put("product", product);
				responseObject.put("statusCode", 200);
			}else {
				responseBody.put("message", "Not found");
				responseObject.put("statusCode", 404);
			}

			responseObject.put("body", responseBody.toString());
		} catch (Exception e) {
			context.getLogger().log("ERROR : "+e.getMessage());
		}
		writer.write(responseObject.toString());
		reader.close();
		writer.close();
	}

	public void handleGetEncryptedResponse(InputStream input, OutputStream output, Context context) throws IOException {

		OutputStreamWriter writer = new OutputStreamWriter(output);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		JSONParser parser = new JSONParser();
		JSONObject responseObject = new JSONObject();
		JSONObject responseBody = new JSONObject();

		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);

		int id;
		Item resItem = null;

		try {
			JSONObject reqObject = (JSONObject) parser.parse(reader);
			if (reqObject.get("pathParameters")!=null) {
				JSONObject pps = (JSONObject)reqObject.get("pathParameters");
				if (pps.get("id")!=null) {
					id = Integer.parseInt((String)pps.get("id"));
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id",id);
				}
			}
			//queryStringParameters
			else if (reqObject.get("queryStringParameters")!=null) {
				JSONObject qps =(JSONObject) reqObject.get("queryStringParameters");
				if (qps.get("id")!=null) {
					id= Integer.parseInt((String)qps.get("id"));
					resItem = dynamoDB.getTable(DYNAMO_TABLE).getItem("id",id);
				}
			}

			if (resItem!=null) {
				Product product = new Product(resItem.toJSON());
				responseBody.put("product", product);
				responseObject.put("statusCode", 200);
			}else {
				responseBody.put("message", "Not found");
				responseObject.put("statusCode", 404);
			}
			ByteBuffer plaintext = ByteBuffer.wrap(responseBody.toString().getBytes());
			EncryptRequest req = new EncryptRequest().withKeyId("arn:aws:kms:us-east-1:071572282510:key/bfb8b376-bd0c-4cb0-95b9-d0b36fd56ec3").withPlaintext(plaintext);
			ByteBuffer ciphertext = AWSKMS_CLIENT.encrypt(req).getCiphertextBlob();

			byte[] base64EncodedValue = Base64.encode(ciphertext.array());
			String value = new String(base64EncodedValue, Charset.forName("UTF-8"));

			responseObject.put("body", value);
		} catch (Exception e) {
			context.getLogger().log("ERROR : "+e.getMessage());
		}
		writer.write(responseObject.toString());
		reader.close();
		writer.close();
	}

	public void handlePutRequest(InputStream input, OutputStream output, Context context) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(output);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		JSONParser parser = new JSONParser();
		JSONObject responseObject = new JSONObject();
		JSONObject responseBody = new JSONObject();
		
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);
		
		try {
			JSONObject reqObject =(JSONObject) parser.parse(reader);
			System.out.println("reqObject: "+reqObject.get("body")!=null);
			if (reqObject.get("body")!=null) {
				Product product = new Product(reqObject.get("body").toString());
				
				dynamoDB.getTable(DYNAMO_TABLE)
				.putItem(new PutItemSpec().withItem(new Item()
						.withNumber("id", product.getId())
						.withString("name", product.getName())
						.withNumber("price", product.getPrice())));
				responseBody.put("message", "New Item created/updated");
				responseObject.put("statusCode", 200);
				responseObject.put("body", responseBody.toString());
			}

		} catch (Exception e) {
			responseObject.put("statusCode", 400);
			responseObject.put("error", e);
		}
		
		writer.write(responseObject.toString());
		reader.close();
		writer.close();
	}

	public void handleBatchPutRequest(InputStream input, OutputStream output, Context context) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(output);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		JSONParser parser = new JSONParser();
		JSONObject responseObject = new JSONObject();
		JSONObject responseBody = new JSONObject();

		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);
		DynamoDB dynamoDB = new DynamoDB(client);

		try {
			JSONObject reqObject =(JSONObject) parser.parse(reader);
			System.out.println("reqObject: "+reqObject.get("body")!=null);
			if (reqObject.get("body")!=null) {
				Gson gson = new Gson();
				OrderDTO orderDTO = gson.fromJson(gson.toJson(reqObject.get("body")), OrderDTO.class);
				dynamoDBMapper.batchSave(orderDTO.getOrderList());
				responseBody.put("message", "Orders created");
				responseObject.put("statusCode", 200);
				responseObject.put("body", responseBody.toString());
			}
		} catch (Exception e) {
			responseObject.put("statusCode", 400);
			responseObject.put("error", e);
		}

		writer.write(responseObject.toString());
		reader.close();
		writer.close();
	}

//	public static void main(String args[]) throws ParseException {
//		String json = "{\n" +
//				"    \"Orders\": [\n" +
//				"      {\n" +
//				"        \"id\": 1,\n" +
//				"        \"name\": \"apple\",\n" +
//				"        \"price\": 435\n" +
//				"      },\n" +
//				"      {\n" +
//				"        \"id\": 2,\n" +
//				"        \"name\": \"MI\",\n" +
//				"        \"price\": 430\n" +
//				"      }\n" +
//				"    ]\n" +
//				"}";
//		Gson gson = new Gson();
//		JSONParser parser = new JSONParser();
//		JSONObject reqObject =(JSONObject) parser.parse(json);
//		OrderDTO orderDTO = gson.fromJson(json, OrderDTO.class);
//		orderDTO.getOrderList();
//		orderDTO.toString();
//
//	}

	@SuppressWarnings("unchecked")
	public void handleDeleteRequest(InputStream input, OutputStream output, Context context) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(output);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		JSONParser parser = new JSONParser();
		JSONObject responseObject = new JSONObject();
		JSONObject responseBody = new JSONObject();
		
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);
		
		try {
			JSONObject reqObject =(JSONObject) parser.parse(reader);
			
			if (reqObject.get("pathParameters")!=null) {
				JSONObject pps =(JSONObject) reqObject.get("pathParameters");
				
				if (pps.get("id")!=null) {
					int id = Integer.parseInt((String)pps.get("id"));
					dynamoDB.getTable(DYNAMO_TABLE).deleteItem("id",id);
				}
				
			}
			
			responseBody.put("message", "Item deleted");
			responseObject.put("statusCode", 200);
			responseObject.put("body", responseBody.toString());
		} catch (Exception e) {
			responseObject.put("statusCode", 400);
			responseObject.put("error", e);
		}
		
		writer.write(responseObject.toString());
		reader.close();
		writer.close();
	}
}