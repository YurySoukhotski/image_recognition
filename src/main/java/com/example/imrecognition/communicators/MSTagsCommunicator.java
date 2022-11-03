package com.example.imrecognition.communicators;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.imrecognition.TextRecognitionManagement;
import com.example.imrecognition.transformers.RecognitionModel;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public class MSTagsCommunicator implements TagsCommunicator
{
	private static final Logger logger = LogManager.getLogger(MSTagsCommunicator.class);

	@Override
	public RecognitionModel recognizeTags(String description, Map<String, String> configAttributes)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		try
		{
			String subscriptionKey = configAttributes.get(TextRecognitionManagement.MSTAGS_PROVIDER + ConstantHelper.CLIENT_API_KEY);
			String uriBase = configAttributes.get(TextRecognitionManagement.MSTAGS_PROVIDER + ConstantHelper.CLIENT_API_URL);
			String language = configAttributes.get(TextRecognitionManagement.MSTAGS_PROVIDER + "LANGUAGE");
			recognitionModel.setKeyPhrases(detectTags(uriBase, subscriptionKey, language, description));
			TimeUnit.MILLISECONDS.sleep(1000);
			recognitionModel.setLanguage(detectLanguage(uriBase, subscriptionKey, description));
			TimeUnit.MILLISECONDS.sleep(1000);
			recognitionModel.setLinkedEntities(detectEntities(uriBase, subscriptionKey, description));
			TimeUnit.MILLISECONDS.sleep(1000);
			recognitionModel.setSentiment(detectSentiment(uriBase, subscriptionKey, description));
            recognitionModel.setClassification(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
            recognitionModel.setEmotions(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
		}
		catch (Exception e)
		{
			logger.error("Error getting tags", e);
			recognitionModel.setKeyPhrases(Collections.singletonList("Error getting info from MS"));
		}

		return recognitionModel;
	}

	private List<String> detectTags(String uriBase, String subscriptionKey, String language, String description)
		throws URISyntaxException, IOException
	{
		HttpClient httpclient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder(uriBase + "/keyPhrases");
		URI uri = builder.build();
		HttpPost request = new HttpPost(uri);
		request.setHeader("Content-Type", "application/json");
		request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
		Documents documents = new Documents();
		documents.add("1", language, description);
		Gson gson = new Gson();
		String s = gson.toJson(documents);
		StringEntity reqEntity = new StringEntity(s, Charset.forName("UTF-8"));
		request.setEntity(reqEntity);
		HttpResponse response = httpclient.execute(request);
		HttpEntity entity = response.getEntity();
		if (entity != null)
		{
			String serverResponse = EntityUtils.toString(entity);
			List<String> tagsResponse = (List<String>) getTransformedTags(serverResponse, "keyPhrases");
			List<String> sortedList = new ArrayList<>();
			for (String key: tagsResponse){
				sortedList.add(key.toLowerCase());
			}
			Collections.sort(sortedList);
			return sortedList;
		}
		return Collections.emptyList();
	}

	private String detectLanguage(String uriBase, String subscriptionKey, String description) throws URISyntaxException,
		IOException
	{
		StringBuilder responseAnswer = new StringBuilder("");
		HttpClient httpclient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder(uriBase + "/languages");
		URI uri = builder.build();
		HttpPost request = new HttpPost(uri);
		request.setHeader("Content-Type", "application/json");
		request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
		// Request body
		Documents documents = new Documents();
		documents.add("1", "de", description);
		Gson gson = new Gson();
		String s = gson.toJson(documents);
		StringEntity reqEntity = new StringEntity(s, Charset.forName("UTF-8"));
		request.setEntity(reqEntity);
		HttpResponse response = httpclient.execute(request);
		HttpEntity entity = response.getEntity();
		if (entity != null)
		{
			String serverResponse = EntityUtils.toString(entity);
			List<Object> rawLanguages = (List<Object>) getTransformedTags(serverResponse, "detectedLanguages");
			if (rawLanguages != null)
			{
				for (Object rawLanguage : rawLanguages)
				{
					Map<String, Object> lang = (Map<String, Object>) rawLanguage;
					responseAnswer.append(lang.get("name")).append(" score: ")
						.append(String.format("%.2f", 100 * Double.valueOf(lang.get("score").toString()))).append("\n");
					logger.info("lang" + lang);
				}
			}
		}
		return responseAnswer.toString();
	}

	private List<String> detectEntities(String uriBase, String subscriptionKey, String description) throws URISyntaxException,
		IOException
	{
		List<String> responseAnswer = new ArrayList<>();
		HttpClient httpclient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder(uriBase + "/entities");
		URI uri = builder.build();
		HttpPost request = new HttpPost(uri);
		request.setHeader("Content-Type", "application/json");
		request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
		// Request body
		Documents documents = new Documents();
		documents.add("1", "en", description);
		Gson gson = new Gson();
		String s = gson.toJson(documents);
		StringEntity reqEntity = new StringEntity(s, Charset.forName("UTF-8"));
		request.setEntity(reqEntity);
		HttpResponse response = httpclient.execute(request);
		HttpEntity entity = response.getEntity();
		if (entity != null)
		{
			String serverResponse = EntityUtils.toString(entity);
			List<Object> entities = (List<Object>) getTransformedTags(serverResponse, "entities");
			if (entities != null)
			{
				for (Object rawEntity : entities)
				{
					Map<String, Object> detectedEntity = (Map<String, Object>) rawEntity;
					StringBuilder answer = new StringBuilder();
					answer.append(detectedEntity.get("name")).append("\n").append(detectedEntity.get("wikipediaUrl"));
					responseAnswer.add(answer.toString());
				}
			}
		}
		Collections.sort(responseAnswer);
		return responseAnswer;
	}

	private String detectSentiment(String uriBase, String subscriptionKey, String description) throws URISyntaxException,
		IOException
	{
		HttpClient httpclient = HttpClients.createDefault();
		URIBuilder builder = new URIBuilder(uriBase + "/sentiment");
		URI uri = builder.build();
		HttpPost request = new HttpPost(uri);
		request.setHeader("Content-Type", "application/json");
		request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
		// Request body
		Documents documents = new Documents();
		documents.add("1", "de", description);
		Gson gson = new Gson();
		String s = gson.toJson(documents);
		StringEntity reqEntity = new StringEntity(s, Charset.forName("UTF-8"));
		request.setEntity(reqEntity);
		HttpResponse response = httpclient.execute(request);
		HttpEntity entity = response.getEntity();
		if (entity != null)
		{
			String serverResponse = EntityUtils.toString(entity);
			Double answerScore = 100 * (Double) getTransformedTags(serverResponse, "score");
			return String.format("%.2f", answerScore);
		}
		return "0";
	}

	private Object getTransformedTags(String rawObject, String tags)
	{
		if (rawObject != null)
			try
			{
				HashMap<String, Object> result = new ObjectMapper().readValue(rawObject, HashMap.class);
				List<Object> docs = (List<Object>) result.get("documents");
				if (docs != null)
				{
					HashMap<String, Object> responseText = (HashMap<String, Object>) docs.get(0);
					return responseText.get(tags);
				}
			}
			catch (Exception e)
			{
				logger.error("Error parse response", e);
			}
		return 0d;
	}

	class Document
	{
		public String language, id, text;

		Document(String id, String language, String text)
		{
			this.language = language;
			this.id = id;
			this.text = text;
		}
	}

	public class Documents
	{
		List<Document> documents;

		Documents()
		{
			this.documents = new ArrayList<>();
		}

		public void add(String id, String language, String text)
		{
			this.documents.add(new Document(id, language, text));
		}
	}
}
