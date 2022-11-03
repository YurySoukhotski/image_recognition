package com.example.imrecognition.communicators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.imrecognition.TextRecognitionManagement;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.paralleldots.paralleldots.App;

import com.example.imrecognition.transformers.MapHelper;
import com.example.imrecognition.transformers.RecognitionModel;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class DotsCommunicatorImpl implements TagsCommunicator
{
	private static final String CLIENT_API_THRESHOLD_VALUE = "CLIENT_API_THRESHOLD_VALUE";
	private static final String PROBABILITIES = "probabilities";
	private static final String NODES_LIST = "NODES_LIST";
	private static final Logger logger = LogManager.getLogger(DotsCommunicatorImpl.class);

	@Override
	public RecognitionModel recognizeTags(String description, Map<String, String> configAttributes)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		try
		{
			String clientApikey = configAttributes.get(TextRecognitionManagement.DOTS_PROVIDER + CLIENT_API_KEY);
			String threshold = configAttributes.get(TextRecognitionManagement.DOTS_PROVIDER + CLIENT_API_THRESHOLD_VALUE);
			String nodeValues = configAttributes.get(TextRecognitionManagement.DOTS_PROVIDER + NODES_LIST);
			String url = configAttributes.get(TextRecognitionManagement.DOTS_PROVIDER + CLIENT_API_URL);
			String language = configAttributes.get(TextRecognitionManagement.DOTS_PROVIDER + "LANGUAGE");
			if (nodeValues != null && !nodeValues.isEmpty())
			{
				processTextAnalyze(recognitionModel, clientApikey, threshold, nodeValues, description);
			}
			else
			{
				processAllInformation(recognitionModel, clientApikey, threshold, description, language, url);
			}
		}
		catch (Exception e)
		{
			logger.error("Unexpected error during Dots service work", e);
			recognitionModel.setKeyPhrases(Collections.singletonList("Error getting info from Dots"));
		}
		return recognitionModel;
	}

	private void processAllInformation(RecognitionModel recognitionModel, String clientApikey, String threshold,
		String description, String language, String url)
	{
		App pd = new App(clientApikey);
		try
		{
			String sentiment;
			List<String> keywords;
			String emotion;
			if (language != null && !language.equalsIgnoreCase("en"))
			{
				sentiment = parseSentiment(pd.sentiment(description, language));
				keywords = parseKeywords(pd.multilang_keywords(description, language));
				emotion = parseEmotions(pd.emotion(description, language));
			}
			else
			{
				sentiment = parseSentiment(pd.sentiment(description));
				keywords = parseKeywords(pd.keywords(description));
				emotion = parseEmotions(pd.emotion(description));
			}
			String abuse = parseAbuse(pd.abuse(description));
			List<String> ner = parseEntities(pd.ner(description));
			String intent = parseIntent(pd.intent(description));
			String classification = parseClassification(description, url, clientApikey);
			recognitionModel.setClassification(classification);
			recognitionModel.setSentiment(sentiment);
			recognitionModel.setLinkedEntities(ner);
			recognitionModel.setKeyPhrases(keywords);
			recognitionModel.setIntent(intent);
			recognitionModel.setAbuse(abuse);
			recognitionModel.setEmotions(emotion);
			recognitionModel.setLanguage(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
		}
		catch (Exception e)
		{
			logger.error("Error getting info", e);
		}
	}

	private String parseClassification(String description, String url, String key)
	{
		StringBuilder classififcation = new StringBuilder();
		try
		{
			OkHttpClient client = new OkHttpClient();
			RequestBody requestBody =
				new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("api_key", key)
					.addFormDataPart("text", description).build();
			Request request = new Request.Builder().url(url).post(requestBody).build();
			Response response = client.newCall(request).execute();
			if (response.body() != null)
			{
				JsonArray jsonElements = new JsonParser().parse(response.body().string()).getAsJsonArray();
				for (int i = 0; i < jsonElements.size(); i++)
				{
					JsonObject element = jsonElements.get(i).getAsJsonObject();
					classififcation.append(element.get("label").getAsString().replace("\"", "")).append(":")
						.append(String.format("%.2f", 100 * element.get("score").getAsDouble())).append("\n");
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Error get response from Dots classification URL", e);
			return "Error getting classififcation";
		}
		return classififcation.toString();
	}

	private String parseIntent(String raw)
	{
		StringBuilder intent = new StringBuilder();
		JsonElement jsonElement = new JsonParser().parse(raw).getAsJsonObject();
		intent.append(jsonElement.getAsJsonObject().get("intent").getAsString().toLowerCase()).append("\n")
			.append("probabilities: ");
		JsonObject probabilities = jsonElement.getAsJsonObject().get(PROBABILITIES).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : probabilities.entrySet())
		{
			intent.append(entry.getKey().toLowerCase()).append(":")
				.append(String.format("%.2f", 100 * Double.valueOf(entry.getValue().getAsString()))).append("\n");
		}
		return intent.toString();
	}

	private String parseEmotions(String raw)
	{
		StringBuilder emotions = new StringBuilder();
		JsonElement jsonElement = new JsonParser().parse(raw).getAsJsonObject().get("emotion").getAsJsonObject();
		JsonObject probabilities = jsonElement.getAsJsonObject().get(PROBABILITIES).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : probabilities.entrySet())
		{
			emotions.append(entry.getKey().toLowerCase()).append(":")
				.append(String.format("%.2f", 100 * Double.valueOf(entry.getValue().getAsString()))).append("\n");
		}
		return emotions.toString();
	}

	private String parseAbuse(String raw)
	{
		StringBuilder abuse = new StringBuilder();
		JsonElement jsonElement = new JsonParser().parse(raw).getAsJsonObject();
		abuse
			.append(jsonElement.getAsJsonObject().get("sentence_type").getAsString().toLowerCase())
			.append(":")
			.append(
				String.format("%.2f", 100 * Double.valueOf(jsonElement.getAsJsonObject().get("confidence_score").getAsString())));
		return abuse.toString();
	}

	private List<String> parseKeywords(String keywords)
	{
		try
		{
			Map<String, Double> responseMap = new HashMap();
			JsonElement jsonElement = new JsonParser().parse(keywords).getAsJsonObject().get("keywords");
			if (jsonElement != null && jsonElement.getAsJsonArray().size() > 0)
			{
				for (JsonElement jsonElementInner : jsonElement.getAsJsonArray())
				{
					responseMap.put(jsonElementInner.getAsJsonObject().get("keyword").getAsString().toLowerCase(),
						Double.valueOf(jsonElementInner.getAsJsonObject().get("confidence_score").getAsString()));
				}
				return MapHelper.mapToList(MapHelper.sortMap(responseMap));
			}
		}
		catch (Exception e)
		{
			logger.error("Error getting keywords ", e);
		}
		return Collections.singletonList("Empty response");
	}

	private List<String> parseEntities(String ner)
	{
		List<String> answer = new ArrayList<>();
		JsonElement jsonElement = new JsonParser().parse(ner).getAsJsonObject().get("entities");
		if (jsonElement != null && jsonElement.getAsJsonArray().size() > 0)
		{
			for (JsonElement jsonElementInner : jsonElement.getAsJsonArray())
			{
				StringBuilder entity = new StringBuilder();
				entity
					.append(jsonElementInner.getAsJsonObject().get("name").getAsString().toLowerCase())
					.append(" category:")
					.append(jsonElementInner.getAsJsonObject().get("category").getAsString().toLowerCase())
					.append(" - ")
					.append(
						String.format("%.2f",
							100 * Double.valueOf(jsonElementInner.getAsJsonObject().get("confidence_score").getAsString())));
				answer.add(entity.toString());
			}
			return answer;
		}
		else
		{
			return Collections.singletonList("Empty response");
		}
	}

	private String parseSentiment(String raw)
	{
		StringBuilder sentiment = new StringBuilder();
		JsonObject jsonObject = new JsonParser().parse(raw).getAsJsonObject();
		sentiment.append("general:").append(jsonObject.get("sentiment").getAsString().toLowerCase()).append("\n");
		JsonObject probabilities = jsonObject.get(PROBABILITIES).getAsJsonObject();
		sentiment.append("negative:")
			.append(String.format("%.2f", 100 * Double.valueOf(probabilities.get("negative").getAsString().toLowerCase())))
			.append("\n");
		sentiment.append("neutral:")
			.append(String.format("%.2f", 100 * Double.valueOf(probabilities.get("neutral").getAsString().toLowerCase())))
			.append("\n");
		sentiment.append("positive:")
			.append(String.format("%.2f", 100 * Double.valueOf(probabilities.get("positive").getAsString().toLowerCase())))
			.append("\n");
		return sentiment.toString();
	}

	private void processTextAnalyze(RecognitionModel recognitionModel, String clientApikey, String threshold, String nodeValues,
		String description)
	{
		String[] nodes = nodeValues.split(",");
		Double thresholdValue = Double.valueOf(threshold);
		String rawAnswer;
		App pd = new App(clientApikey);
		Map<String, Double> answerMap = new HashMap<>();
		try
		{
			for (String node : nodes)
			{
				String nodeName = node.trim();
				rawAnswer = pd.similarity(description, nodeName + " " + nodeName);
				double score = getScore(rawAnswer);
				logger.info(node + " with score: " + score);
				if (score >= thresholdValue)
				{
					logger.info("Add into answer node:" + node + " with score: " + score);
					answerMap.put(node, score);
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Error get info", e);
		}
		recognitionModel.setNodeAnaliseStatus(answerMap);
	}

	private double getScore(String rawAnswer) throws IOException
	{
		HashMap<String, Object> result = new ObjectMapper().readValue(rawAnswer, HashMap.class);
		String score = String.valueOf(result.get("actual_score"));
		if (!"null".equalsIgnoreCase(score))
			return Double.valueOf(score);
		else
			return 0d;
	}
}
