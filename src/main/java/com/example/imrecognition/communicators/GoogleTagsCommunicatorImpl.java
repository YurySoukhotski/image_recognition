package com.example.imrecognition.communicators;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.imrecognition.TextRecognitionManagement;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.example.imrecognition.transformers.RecognitionModel;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class GoogleTagsCommunicatorImpl implements TagsCommunicator
{
	private static final Logger logger = LogManager.getLogger(GoogleTagsCommunicatorImpl.class);
	private static final String CONTENT = "content-Type";
	private static final String JSON = "application/json";
	private static final String PLAIN_TEXT = "PLAIN_TEXT";

	@Override
	public RecognitionModel recognizeTags(String description, Map<String, String> configAttributes)
	{
		String key = configAttributes.get(TextRecognitionManagement.GOOGLETAGS_PROVIDER + CLIENT_API_KEY);
		String url = configAttributes.get(TextRecognitionManagement.GOOGLETAGS_PROVIDER + CLIENT_API_URL);
		String urlMl = configAttributes.get(TextRecognitionManagement.GOOGLETAGS_PROVIDER + "URL_ML");
		String keyFile = configAttributes.get(TextRecognitionManagement.GOOGLETAGS_PROVIDER + "KEY_FILE");
		RecognitionModel recognitionModel = new RecognitionModel();
		try
		{
			recognitionModel.setSentiment(analyzeSentiment(description, key, url));
			recognitionModel.setLinkedEntities(analyzeEntities(description, key, url));
			recognitionModel.setClassification(analyzeCategories(description, key, url));
			recognitionModel.setIntent(analyzeML(description, urlMl, keyFile));
		}
		catch (Exception e)
		{
			logger.error("Error getting info ", e);
			recognitionModel.setKeyPhrases(Collections.singletonList("Error getting info from Google api"));
		}
		recognitionModel.setKeyPhrases(Collections.singletonList(TextRecognitionManagement.SERVICE_NOT_SUPPORTED));
		recognitionModel.setEmotions(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
		recognitionModel.setLanguage(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
		return recognitionModel;
	}

	private String analyzeCategories(String description, String key, String url) throws IOException
	{
		StringBuilder answer = new StringBuilder();
		Map<String, Object> bodyMap = new HashMap<>();
		Map<String, Object> documentMap = new HashMap<>();
		documentMap.put("type", PLAIN_TEXT);
		documentMap.put("content", description);
		bodyMap.put("document", documentMap);
		Gson gson = new Gson();
		String json = gson.toJson(bodyMap);
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse(JSON);
		RequestBody body = RequestBody.create(mediaType, json);
		Request request = new Request.Builder().url(url + "classifyText?" + key).post(body).addHeader(CONTENT, JSON).build();
		Response response = client.newCall(request).execute();
		if (response.body() != null)
		{
			JsonElement jsonElement = new JsonParser().parse(response.body().string()).getAsJsonObject().get("categories");
			if (jsonElement != null)
			{
				for (JsonElement jsonElementInner : jsonElement.getAsJsonArray())
				{
					answer
						.append(jsonElementInner.getAsJsonObject().get("name").getAsString().toLowerCase())
						.append(":")
						.append(
							String.format("%.2f",
								100 * Double.valueOf(jsonElementInner.getAsJsonObject().get("confidence").getAsString())))
						.append("\n");
				}
			}
			else
			{
				answer.append("Categories not found");
			}
		}
		return answer.toString();
	}

	private String analyzeML(String description, String url, String keyFileName) throws IOException
	{
		GoogleCredentials credentials =
			GoogleCredentials.fromStream(new FileInputStream(keyFileName)).createScoped(
				Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
		StorageOptions.newBuilder().setCredentials(credentials).build().getService();
		credentials.refreshIfExpired();
		AccessToken accesstoken = credentials.getAccessToken();
		logger.info("Token " + accesstoken.getTokenValue());
		StringBuilder answer = new StringBuilder();
		Map<String, Object> payLoad = new HashMap<>();
		Map<String, Object> textSnippet = new HashMap<>();
		Map<String, Object> content = new HashMap<>();
		content.put("content", description);
		content.put("mime_type", "text/plain");
		textSnippet.put("textSnippet", content);
		payLoad.put("payload", textSnippet);
		Gson gson = new Gson();
		String json = gson.toJson(payLoad);
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse(JSON);
		RequestBody body = RequestBody.create(mediaType, json);
		Request request =
			new Request.Builder().url(url).post(body).addHeader(CONTENT, JSON)
				.addHeader("Authorization", "Bearer " + accesstoken.getTokenValue()).build();
		Response response = client.newCall(request).execute();
		if (response.body() != null)
		{
			JsonElement jsonElement = new JsonParser().parse(response.body().string()).getAsJsonObject().get("payload");
			if (jsonElement != null)
			{
				for (JsonElement jsonElementInner : jsonElement.getAsJsonArray())
				{
					answer.append(jsonElementInner.getAsJsonObject().get("displayName").getAsString().toLowerCase()).append(":");
					JsonElement textExtraction = jsonElementInner.getAsJsonObject().get("textExtraction");
					answer.append(
						String.format("%.2f", 100 * Double.valueOf(textExtraction.getAsJsonObject().get("score").getAsString())))
						.append("\n");
				}
			}
			else
			{
				answer.append("Categories not found");
			}
		}
		return answer.toString();
	}

	private String analyzeSentiment(String description, String key, String url) throws IOException
	{
		StringBuilder sentiment = new StringBuilder();
		Map<String, Object> bodyMap = new HashMap<>();
		Map<String, Object> documentMap = new HashMap<>();
		documentMap.put("type", PLAIN_TEXT);
		documentMap.put("content", description);
		bodyMap.put("encodingType", "UTF8");
		bodyMap.put("document", documentMap);
		Gson gson = new Gson();
		String json = gson.toJson(bodyMap);
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse(JSON);
		RequestBody body = RequestBody.create(mediaType, json);
		Request request = new Request.Builder().url(url + "analyzeSentiment?" + key).post(body).addHeader(CONTENT, JSON).build();
		Response response = client.newCall(request).execute();
		if (response.body() != null)
		{
			JsonObject jsonObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
			if (jsonObject.get("documentSentiment") != null)
			{
				JsonObject documentSentiment = jsonObject.get("documentSentiment").getAsJsonObject();// todo npe
				sentiment.append("magnitude:").append(documentSentiment.get("magnitude").getAsString().toLowerCase())
					.append("\n");
				sentiment
					.append("score:")
					.append(
						String.format("%.2f", 100 * Double.valueOf(documentSentiment.get("score").getAsString().toLowerCase())))
					.append("\n");
			}
			return sentiment.toString();
		}
		return sentiment.toString();
	}

	private List<String> analyzeEntities(String description, String key, String url) throws IOException
	{
		List<String> answer = new ArrayList<>();
		Map<String, Object> bodyMap = new HashMap<>();
		Map<String, Object> documentMap = new HashMap<>();
		documentMap.put("type", PLAIN_TEXT);
		documentMap.put("content", description);
		bodyMap.put("encodingType", "UTF8");
		bodyMap.put("document", documentMap);
		Gson gson = new Gson();
		String json = gson.toJson(bodyMap);
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse(JSON);
		RequestBody body = RequestBody.create(mediaType, json);
		Request request = new Request.Builder().url(url + "analyzeEntities?" + key).post(body).addHeader(CONTENT, JSON).build();
		Response response = client.newCall(request).execute();
		if (response.body() != null)
		{
			JsonElement jsonElement = new JsonParser().parse(response.body().string()).getAsJsonObject().get("entities");
			for (JsonElement jsonElementInner : jsonElement.getAsJsonArray())
			{
				StringBuilder entity = new StringBuilder();
				entity
					.append(jsonElementInner.getAsJsonObject().get("name").getAsString())
					.append(" - ")
					.append(jsonElementInner.getAsJsonObject().get("type").getAsString().toLowerCase())
					.append(" : ")
					.append(
						String.format("%.2f",
							100 * Double.valueOf(jsonElementInner.getAsJsonObject().get("salience").getAsString())));
				answer.add(entity.toString());
			}
		}
		return answer;
	}
}
