package com.example.imrecognition.communicators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.aylien.textapi.parameters.ConceptsParams;
import com.aylien.textapi.parameters.EntitiesParams;
import com.aylien.textapi.parameters.HashTagsParams;
import com.aylien.textapi.parameters.LanguageParams;
import com.aylien.textapi.responses.Concept;
import com.aylien.textapi.responses.Concepts;
import com.aylien.textapi.responses.Entities;
import com.aylien.textapi.responses.Entity;
import com.aylien.textapi.responses.HashTags;
import com.aylien.textapi.responses.Language;
import com.aylien.textapi.responses.SurfaceForm;
import com.example.imrecognition.TextRecognitionManagement;
import com.example.imrecognition.transformers.RecognitionModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.aylien.textapi.TextAPIClient;
import com.aylien.textapi.parameters.SentimentParams;
import com.aylien.textapi.responses.Sentiment;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;

public class AylienCommunicator implements TagsCommunicator
{
	private static final Logger logger = LogManager.getLogger(AylienCommunicator.class);

	@Override
	public RecognitionModel recognizeTags(String description, Map<String, String> configAttributes)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		String clientApikey = configAttributes.get(TextRecognitionManagement.AYLIEN_PROVIDER + CLIENT_API_KEY);
		String clientClientId = configAttributes.get(TextRecognitionManagement.AYLIEN_PROVIDER + "CLIENT_ID");
		try
		{
			TextAPIClient client = new TextAPIClient(clientClientId, clientApikey);
			SentimentParams.Builder builder = SentimentParams.newBuilder();
			builder.setText(description);
			Sentiment sentiment = client.sentiment(builder.build());
			StringBuilder sentimentBuilder = new StringBuilder();
			if (!"unknown".equalsIgnoreCase(sentiment.getPolarity())){
				sentimentBuilder.append(sentiment.getPolarity()).append(":").append(String.format("%.2f", 100 * sentiment.getPolarityConfidence())).append("\n");
			}
			if (!"unknown".equalsIgnoreCase(sentiment.getSubjectivity())){
				sentimentBuilder.append(sentiment.getSubjectivity()).append(":").append(String.format("%.2f", 100 * sentiment.getSubjectivityConfidence()));
			}
			recognitionModel.setSentiment(sentimentBuilder.toString());

			LanguageParams.Builder langBuilder = LanguageParams.newBuilder();
			langBuilder.setText(description);
			Language detectedLanguage = client.language(langBuilder.build());
			recognitionModel.setLanguage(detectedLanguage.getLanguage()+":"+String.format("%.2f", 100 *detectedLanguage.getConfidence()));
			EntitiesParams.Builder entitiesBuilder = EntitiesParams.newBuilder();
			entitiesBuilder.setText(description);
			entitiesBuilder.setLanguage("auto");
			Entities detectedEntities = client.entities(entitiesBuilder.build());
			List<Entity> entities = detectedEntities.getEntities();
			List<String> linkedEntities = new ArrayList<>();
			for (int i=0 ; i<entities.size();i++){
				StringBuilder answer = new StringBuilder();
				Entity entity = entities.get(i);
				answer.append(entity.getType()).append(":\n").append(buildInfo(entity.getSurfaceForms()));
				linkedEntities.add(answer.toString());
			}
			recognitionModel.setLinkedEntities(linkedEntities);
			recognitionModel.setKeyPhrases(new ArrayList<>());
			ConceptsParams.Builder conceptsBuilder = ConceptsParams.newBuilder();
			conceptsBuilder.setText(description);
			conceptsBuilder.setLanguage("auto");
			Concepts conceptsDetected = client.concepts(conceptsBuilder.build());
			List<Concept> concepts = conceptsDetected.getConcepts();
			List<String> keys = new ArrayList<>();
			for (int i=0 ; i<concepts.size();i++){
				Concept concept = concepts.get(i);
				StringBuilder formBuilder = new StringBuilder();
				SurfaceForm[] surfaceForms = concept.getSurfaceForms();
				for (int j=0; j<surfaceForms.length; j++){
					formBuilder.append(surfaceForms[j].getString()).append(":").append(100*surfaceForms[j].getScore());
				}
				keys.add(formBuilder.toString());
			}
			recognitionModel.setKeyPhrases(keys);

			HashTagsParams.Builder hashTagsBuilder = HashTagsParams.newBuilder();
			hashTagsBuilder.setText(description);
			hashTagsBuilder.setLanguage("auto");
			HashTags hashTagsDetected = client.hashtags(hashTagsBuilder.build());
			String[] hashTags = hashTagsDetected.getHashtags();
			String tags="";
			for (int i =0; i < hashTags.length; i++ ){
				tags=tags+hashTags[i]+"\n";
			}
			recognitionModel.setIntent(tags);
			recognitionModel.setClassification(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);
			recognitionModel.setEmotions(TextRecognitionManagement.SERVICE_NOT_SUPPORTED);

		}
		catch (Exception e)
		{
			logger.error("Error getting tags", e);
			recognitionModel.setKeyPhrases(Collections.singletonList("Error getting info from Aylien"));
		}
		return recognitionModel;
	}



	private String buildInfo(List<String> forms){
		StringBuilder response = new StringBuilder();
		for (int i =0 ; i <forms.size(); i++){
			response.append(forms.get(i)).append("\n");
		}
		return response.toString();
	}
}
