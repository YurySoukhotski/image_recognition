package com.example.imrecognition.communicators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.imrecognition.TextRecognitionManagement;
import com.example.imrecognition.transformers.MapHelper;
import com.example.imrecognition.transformers.RecognitionModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder;
import com.amazonaws.services.comprehend.model.DetectDominantLanguageRequest;
import com.amazonaws.services.comprehend.model.DetectDominantLanguageResult;
import com.amazonaws.services.comprehend.model.DetectEntitiesRequest;
import com.amazonaws.services.comprehend.model.DetectEntitiesResult;
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesRequest;
import com.amazonaws.services.comprehend.model.DetectKeyPhrasesResult;
import com.amazonaws.services.comprehend.model.DetectSentimentRequest;
import com.amazonaws.services.comprehend.model.DetectSentimentResult;
import com.amazonaws.services.comprehend.model.DominantLanguage;
import com.amazonaws.services.comprehend.model.Entity;
import com.amazonaws.services.comprehend.model.KeyPhrase;

public class AWSTagsCommunicatorImpl implements TagsCommunicator
{
	private static final Logger logger = LogManager.getLogger(AWSTagsCommunicatorImpl.class);

	@Override
	public RecognitionModel recognizeTags(String description, Map<String, String> configAttributes)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		String clientApikey = configAttributes.get(TextRecognitionManagement.AWSTAGS_PROVIDER + ConstantHelper.CLIENT_API_KEY);
		String[] split = clientApikey.split(":");
		try
		{
			String language = "de";
			StringBuilder rawResult = new StringBuilder();
			AWSCredentialsProvider awsCreds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(split[0], split[1]));
			AmazonComprehend comprehendClient =
				AmazonComprehendClientBuilder.standard().withCredentials(awsCreds).withRegion(Regions.US_EAST_2).build();

			DetectDominantLanguageRequest detectDominantLanguageRequest =
					new DetectDominantLanguageRequest().withText(description);
			DetectDominantLanguageResult detectDominantLanguageResult =
					comprehendClient.detectDominantLanguage(detectDominantLanguageRequest);
			for (DominantLanguage dominantLanguage : detectDominantLanguageResult.getLanguages())
			{
				language=dominantLanguage.getLanguageCode();
				rawResult.append(dominantLanguage.getLanguageCode()).append(":")
						.append(String.format("%.2f", 100 * dominantLanguage.getScore())).append("\n");
				recognitionModel.setLanguage(rawResult.toString());
			}

			DetectSentimentRequest detectSentimentRequest =
				new DetectSentimentRequest().withText(description).withLanguageCode(language);
			DetectSentimentResult detectSentimentResult = comprehendClient.detectSentiment(detectSentimentRequest);
			rawResult = new StringBuilder();
			rawResult.append("general: ").append(detectSentimentResult.getSentiment().toLowerCase()).append("\n").append("mixed:")
				.append(String.format("%.2f", 100 * Double.valueOf(detectSentimentResult.getSentimentScore().getMixed())))
				.append("\n").append("negative:")
				.append(String.format("%.2f", 100 * Double.valueOf(detectSentimentResult.getSentimentScore().getNegative())))
				.append("\n").append("neutral:")
				.append(String.format("%.2f", 100 * Double.valueOf(detectSentimentResult.getSentimentScore().getNeutral())))
				.append("\n").append("positive:")
				.append(String.format("%.2f", 100 * Double.valueOf(detectSentimentResult.getSentimentScore().getPositive())))
				.append("\n");
			recognitionModel.setSentiment(rawResult.toString());
			DetectEntitiesRequest detectEntitiesRequest =
				new DetectEntitiesRequest().withText(description).withLanguageCode(language);
			DetectEntitiesResult detectEntitiesResult = comprehendClient.detectEntities(detectEntitiesRequest);
			List<String> resultList = new ArrayList<>();
			for (Entity entity : detectEntitiesResult.getEntities())
			{
				rawResult = new StringBuilder();
				rawResult.append(entity.getText().toLowerCase()).append(" - ").append(entity.getType().toLowerCase()).append(":")
					.append(String.format("%.2f", 100 * entity.getScore()));
				resultList.add(rawResult.toString());
			}
			if (resultList.isEmpty()){
				resultList.add("Entities not found");
			}
			recognitionModel.setLinkedEntities(resultList);
			DetectKeyPhrasesRequest detectKeyPhrasesRequest =
				new DetectKeyPhrasesRequest().withText(description).withLanguageCode(language);
			DetectKeyPhrasesResult detectKeyPhrasesResult = comprehendClient.detectKeyPhrases(detectKeyPhrasesRequest);
			Map<String, Double> answerMap= new HashMap<>();
			for (KeyPhrase keyPhrase : detectKeyPhrasesResult.getKeyPhrases())
			{
				answerMap.put(keyPhrase.getText().toLowerCase(),100 * keyPhrase.getScore().doubleValue());
			}
			recognitionModel.setKeyPhrases(MapHelper.mapToList(MapHelper.sortMap(answerMap)));
			recognitionModel.setEmotions(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
			recognitionModel.setClassification(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
		}
		catch (Exception e)
		{
			logger.error("Error while get answer", e);
			recognitionModel.setKeyPhrases(Collections.singletonList("Error getting info from AWS"));
		}
		return recognitionModel;
	}
}
