package com.example.imrecognition.communicators;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.example.imrecognition.TextRecognitionManagement;
import com.example.imrecognition.transformers.RecognitionModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import static com.example.imrecognition.TextRecognitionManagement.GETSENTIMENT_PROVIDER;
import static com.example.imrecognition.TextRecognitionManagement.SYSTRAN_PROVIDER;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class SystranCommunicator implements TagsCommunicator
{
	private static final Logger logger = LogManager.getLogger(SystranCommunicator.class);

	@Override
	public RecognitionModel recognizeTags(String description, Map<String, String> configAttributes)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		String key = configAttributes.get(SYSTRAN_PROVIDER + CLIENT_API_KEY);
		String url = configAttributes.get(SYSTRAN_PROVIDER + CLIENT_API_URL);
		try
		{
			String params ="input=" + description;
			String response = makeCall(url+"/nlp/lid/detectLanguage/document?"+params, key);
			JsonElement jsonElement = new JsonParser().parse(response).getAsJsonObject().get("detectedLanguages");
			StringBuilder answer = new StringBuilder();
			String detectedLang= "en";
			if (jsonElement != null) {
				JsonElement languageElement = jsonElement.getAsJsonArray().get(0);
				detectedLang = languageElement.getAsJsonObject().get("lang").getAsString().toLowerCase();

				for (JsonElement jsonElementInner : jsonElement.getAsJsonArray()) {
					answer.append(jsonElementInner.getAsJsonObject().get("lang").getAsString().toLowerCase()).append(":")
							.append(String.format("%.2f", 100 * Double.valueOf(jsonElementInner.getAsJsonObject().get("confidence").getAsString()))).append("\n");
				}

				recognitionModel.setLanguage(answer.toString());
			}
				params = "input=" + description + "&lang=" + detectedLang;
				response = makeCall(url + "/nlp/ner/extract/entities?" + params, key);
				jsonElement = new JsonParser().parse(response).getAsJsonObject().get("entities");
				answer = new StringBuilder();
				if (jsonElement != null)
					for (JsonElement jsonElementInner : jsonElement.getAsJsonArray()) {
						answer.append(jsonElementInner.getAsJsonObject().get("type").getAsString().toLowerCase()).append(":")
								.append(jsonElementInner.getAsJsonObject().get("value").getAsString().toLowerCase()).append("\n");
					}

			recognitionModel.setEntities(answer.toString());
			recognitionModel.setClassification(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
			recognitionModel.setEmotions(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
			recognitionModel.setKeyPhrases(Collections.singletonList(TextRecognitionManagement.SERVICE_NOT_SUPPORTED));
			recognitionModel.setSentiment(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);

		}
		catch (Exception e)
		{
			logger.error("Error getting tags", e);
			recognitionModel.setKeyPhrases(Collections.singletonList("Error getting info from Systran"));
		}
		return recognitionModel;
	}

	private String makeCall(String targetURL, String key) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().addHeader("X-RapidAPI-Key",key).url(targetURL).get().build();
		Response response = client.newCall(request).execute();
		if (response.body() != null)
		{
			return response.body().string();

		}
		return null;
	}
}

