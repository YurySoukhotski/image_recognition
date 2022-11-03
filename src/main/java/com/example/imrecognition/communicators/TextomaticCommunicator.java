package com.example.imrecognition.communicators;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.example.imrecognition.TextRecognitionManagement;
import com.example.imrecognition.transformers.RecognitionModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class TextomaticCommunicator implements TextCommunicator
{
	private static final Logger logger = LogManager.getLogger(TextomaticCommunicator.class);
	private static final String CONTENT = "content-Type";
	private static final String JSON = "application/json";
	private static final String API_USER = "API_USER";

	@Override
	public RecognitionModel recognize(Long productId, Map<String, String> productAttributes, Map<String, String> configAttributes)
	{
		String apiKey = configAttributes.get(TextRecognitionManagement.TEXTOMATIC_PROVIDER + CLIENT_API_KEY);
		String uriBase = configAttributes.get(TextRecognitionManagement.TEXTOMATIC_PROVIDER + CLIENT_API_URL);
		String user = configAttributes.get(TextRecognitionManagement.TEXTOMATIC_PROVIDER + API_USER);
		String collectionId = configAttributes.get(TextRecognitionManagement.TEXTOMATIC_PROVIDER + TextRecognitionManagement.COLLECTION_ID);
		RecognitionModel recognitionModel = new RecognitionModel();
		try
		{
			recognitionModel.setGeneratedText(sendDocument(uriBase, apiKey, user, collectionId, productAttributes));
		}
		catch (Exception e)
		{
			logger.error("Error processing provider", e);
		}
		return recognitionModel;
	}

	private String sendDocument(String uriBase, String apiKey, String user, String collectionId,
		Map<String, String> productAttributes) throws IOException
	{
		Map<String, Object> bodyMap = new HashMap<>();
		bodyMap.put("user", user);
		bodyMap.put("apikey", apiKey);
		productAttributes.put("kblanguage", "de");
		Object[] dataArray = new Object[1];
		dataArray[0] = productAttributes;
		bodyMap.put("data", dataArray);
		Gson gson = new Gson();
		String json = gson.toJson(bodyMap);
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse(JSON);
		RequestBody body = RequestBody.create(mediaType, json);
		Request request = new Request.Builder().url(uriBase + "import").post(body).addHeader(CONTENT, JSON).build();
		Response response = client.newCall(request).execute();
		if (response.body() != null)
		{
			String id = productAttributes.get("id");
			return getGeneratedText(uriBase, apiKey, user, id);
		}
		return null;
	}

	private String getGeneratedText(String uriBase, String apiKey, String user, String id) throws IOException
	{
		Map<String, Object> bodyMap = new HashMap<>();
		bodyMap.put("user", user);
		bodyMap.put("id", id);
		bodyMap.put("apikey", apiKey);
		Map<String, String> template = new HashMap<>();
		Object[] templateArray = new Object[1];
		template.put("name", "BikeDetail");
		template.put("language", "de");
		templateArray[0] = template;
		bodyMap.put("templates", templateArray);
		Gson gson = new Gson();
		String json = gson.toJson(bodyMap);
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse(JSON);
		RequestBody body = RequestBody.create(mediaType, json);
		Request request = new Request.Builder().url(uriBase + "generate").post(body).addHeader(CONTENT, JSON).build();
		Response response = client.newCall(request).execute();
		if (response.body() != null)
		{
			JsonObject jsonObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
			if (jsonObject.get("result") != null && !jsonObject.get("result").getAsJsonArray().isJsonNull())
			{
				return jsonObject.get("result").getAsJsonArray().get(0).getAsJsonObject().get("text").getAsString();
			}
		}
		return "No generated text ";
	}
}
