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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class TextEngineCommunicator implements TextCommunicator
{
	private static final Logger logger = LogManager.getLogger(TextEngineCommunicator.class);
	private static final String CONTENT = "content-Type";
	private static final String JSON = "application/json";

	@Override
	public RecognitionModel recognize(Long productId, Map<String, String> productAttributes, Map<String, String> configAttributes)
	{
		String apiKey = configAttributes.get(TextRecognitionManagement.TEXTENGINE_PROVIDER + CLIENT_API_KEY);
		String uriBase = configAttributes.get(TextRecognitionManagement.TEXTENGINE_PROVIDER + CLIENT_API_URL);
		String collectionId = configAttributes.get(TextRecognitionManagement.TEXTENGINE_PROVIDER + TextRecognitionManagement.COLLECTION_ID);
		RecognitionModel recognitionModel = new RecognitionModel();
		try
		{
			recognitionModel.setGeneratedText(sendDocument(uriBase, apiKey, collectionId, productAttributes));
		}
		catch (Exception e)
		{
			logger.error("Error processing provider", e);
		}
		return recognitionModel;
	}

	private String sendDocument(String uriBase, String apiKey, String collectionId, Map<String, String> productAttributes)
		throws IOException
	{
		Map<String, Object> bodyMap = new HashMap<>();
		bodyMap.put("token", apiKey);
		bodyMap.put("data", productAttributes);
		bodyMap.put("strict_validation", true);
		bodyMap.put("channel", "some_string");
		bodyMap.put("refresh", true);
		bodyMap.put("output_format", "PLAIN_TEXT");
		Gson gson = new Gson();
		String json = gson.toJson(bodyMap);
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse(JSON);
		RequestBody body = RequestBody.create(mediaType, json);
		Request request =
			new Request.Builder().url(uriBase + collectionId + "/generate").post(body).addHeader(CONTENT, JSON).build();
		Response response = client.newCall(request).execute();
		if (response.body() != null)
		{
			return getTransformedData(response.body().string(), "text");
		}
		else
		{
			return null;
		}
	}

	private String getTransformedData(String rawObject, String key)
	{
		if (rawObject != null)
			try
			{
				HashMap mapValue = new ObjectMapper().readValue(rawObject, HashMap.class);
				return String.valueOf(mapValue.get(key));
			}
			catch (IOException e)
			{
				logger.error("Error parse response String into Map with values");
			}
		return null;
	}
}
