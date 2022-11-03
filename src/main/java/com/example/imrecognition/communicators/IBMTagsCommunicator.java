package com.example.imrecognition.communicators;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.example.imrecognition.TextRecognitionManagement;
import com.example.imrecognition.helper.ClassificationHelper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.Classification;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifiedClass;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifyOptions;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.GetClassifierOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.ConceptsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.ConceptsResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EmotionOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EmotionResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.SentimentOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.SentimentResult;
import com.ibm.watson.developer_cloud.service.security.IamOptions;

import com.example.imrecognition.transformers.RecognitionModel;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class IBMTagsCommunicator implements TagsCommunicator
{
	private static final Logger logger = LogManager.getLogger(IBMTagsCommunicator.class);
	private Integer countAttempt = 3;
	private static final String VERSION = "2018-11-16";

	@Override
	public RecognitionModel recognizeTags(String description, Map<String, String> configAttributes)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		boolean success = false;
		if (countAttempt != 3)
		{
			countAttempt = 3;
		}
		while (!success && countAttempt > 0)
		{
			try
			{
				String subscriptionKey = configAttributes.get(TextRecognitionManagement.IBMTAGS_PROVIDER + CLIENT_API_KEY);
				String uriBase = configAttributes.get(TextRecognitionManagement.IBMTAGS_PROVIDER + CLIENT_API_URL);
				String subscriptionKeyClassifier = configAttributes.get(TextRecognitionManagement.IBMTAGS_PROVIDER + CLIENT_API_KEY + ClassificationHelper.LANGUAGE);
				String uriBaseClassifier = configAttributes.get(TextRecognitionManagement.IBMTAGS_PROVIDER + CLIENT_API_URL + ClassificationHelper.LANGUAGE);
				String classifierId = configAttributes.get(TextRecognitionManagement.IBMTAGS_PROVIDER + "MODEL_ID");
				if (subscriptionKey != null)
				{
					detectTagsLanguageEmotions(uriBase, subscriptionKey, description, recognitionModel);
					detectCategories(uriBase, subscriptionKey, description, recognitionModel);
					detectEntities(uriBase, subscriptionKey, description, recognitionModel);
					detectConcepts(uriBase, subscriptionKey, description, recognitionModel);
					classifierPhrase(uriBaseClassifier, subscriptionKeyClassifier, description, classifierId, recognitionModel);
				}
				else
				{
					classifierPhrase(uriBaseClassifier, subscriptionKeyClassifier, description, classifierId, recognitionModel);
				}
				success = true;
			}
			catch (Exception e)
			{
				logger.error("Error getting tags", e);
				countAttempt--;
				recognitionModel.setKeyPhrases(Collections.singletonList("Error getting info from IBM"));
			}
		}
		return recognitionModel;
	}

	private void classifierPhrase(String uriBase, String subscriptionKey, String description, String classifierId,
		RecognitionModel recognitionModel) throws FileNotFoundException
	{
		// /****** train ******/
		// IamOptions options = new IamOptions.Builder().apiKey(subscriptionKey).build();
		// NaturalLanguageClassifier naturalLanguageClassifier = new NaturalLanguageClassifier(options);
		// naturalLanguageClassifier.setEndPoint(uriBase);
		// CreateClassifierOptions createOptions =
		// new CreateClassifierOptions.Builder()
		// .metadata(new File("/home/INTEXSOFT/yury.soukhotski/work/dev/OMN-Day/metadata.json"))
		// .trainingData(new File("/home/INTEXSOFT/yury.soukhotski/work/dev/OMN-Day/dataEbikes1.csv")).build();
		// String classifierId1 = naturalLanguageClassifier.createClassifier(createOptions).execute().getClassifierId();
		// System.out.println(classifierId1);
		/*****************************/
		IamOptions options = new IamOptions.Builder().apiKey(subscriptionKey).build();
		NaturalLanguageClassifier naturalLanguageClassifier = new NaturalLanguageClassifier(options);
		naturalLanguageClassifier.setEndPoint(uriBase);
		GetClassifierOptions getOptions = new GetClassifierOptions.Builder().classifierId(classifierId).build();
		String classifierStatus = naturalLanguageClassifier.getClassifier(getOptions).execute().getStatus();
		if (classifierStatus.equalsIgnoreCase("Available"))
		{
			ClassifyOptions classifyOptions = new ClassifyOptions.Builder().classifierId(classifierId).text(description).build();
			Classification classification = naturalLanguageClassifier.classify(classifyOptions).execute();
			StringBuilder answer = new StringBuilder();
			List<ClassifiedClass> classesResponse = classification.getClasses();
			if (classesResponse != null && !classesResponse.isEmpty())
			{
				recognitionModel.setNode(classesResponse.get(0).getClassName());
				logger.info("Set node: " + classesResponse.get(0));
				for (ClassifiedClass classifiedClass : classification.getClasses())
				{
					answer.append(classifiedClass.getClassName()).append(":")
						.append(String.format("%.2f", 100 * classifiedClass.getConfidence())).append("\n");
				}
				recognitionModel.setClassification(answer.toString());
			}
		}
		else
		{
			recognitionModel.setClassification("Model is not ready");
		}
	}

	private void detectTagsLanguageEmotions(String uriBase, String subscriptionKey, String description,
		RecognitionModel recognitionModel)
	{
		IamOptions options = new IamOptions.Builder().apiKey(subscriptionKey).build();
		NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding(VERSION, options);
		naturalLanguageUnderstanding.setEndPoint(uriBase);
		KeywordsOptions keywords = new KeywordsOptions.Builder().sentiment(false).emotion(false).limit(10).build();
		Features features = new Features.Builder().keywords(keywords).build();
		AnalyzeOptions parameters = new AnalyzeOptions.Builder().text(description).features(features).build();
		AnalysisResults response = naturalLanguageUnderstanding.analyze(parameters).execute();
		List<KeywordsResult> keywordsResponse = response.getKeywords();
		List<String> answer = new ArrayList<>();
		StringBuilder answerEmotions = new StringBuilder();
		StringBuilder answerSentiments = new StringBuilder();
		for (KeywordsResult keywordsResult : keywordsResponse)
		{
			answer.add(keywordsResult.getText() + ":" + String.format("%.2f", 100 * keywordsResult.getRelevance()));
		}
		SentimentOptions sentiment = new SentimentOptions.Builder().document(true).build();
		features = new Features.Builder().sentiment(sentiment).build();
		parameters = new AnalyzeOptions.Builder().text(description).features(features).build();
		SentimentResult sentimentResult = naturalLanguageUnderstanding.analyze(parameters).execute().getSentiment();
		answerSentiments.append(sentimentResult.getDocument().getLabel()).append(":")
			.append(String.format("%.2f", 100 * sentimentResult.getDocument().getScore()));
		EmotionOptions emotion = new EmotionOptions.Builder().document(true).build();
		features = new Features.Builder().emotion(emotion).build();
		parameters = new AnalyzeOptions.Builder().text(description).features(features).build();
		try
		{
			EmotionResult emotionResult = naturalLanguageUnderstanding.analyze(parameters).execute().getEmotion();
			answerEmotions.append("anger:")
				.append(String.format("%.2f", 100 * emotionResult.getDocument().getEmotion().getAnger())).append("\n")
				.append("disgust:").append(String.format("%.2f", 100 * emotionResult.getDocument().getEmotion().getDisgust()))
				.append("\n").append("fear:")
				.append(String.format("%.2f", 100 * emotionResult.getDocument().getEmotion().getFear())).append("\n")
				.append("joy:").append(String.format("%.2f", 100 * emotionResult.getDocument().getEmotion().getJoy()))
				.append("\n").append("sadness:")
				.append(String.format("%.2f", 100 * emotionResult.getDocument().getEmotion().getSadness()));
		}
		catch (Exception e)
		{
			logger.error("Emotions detection error for text : " + description, e);
			answerEmotions.append("DE language is not supported for emotions");
		}
		recognitionModel.setKeyPhrases(answer);
		recognitionModel.setEmotions(answerEmotions.toString());
		recognitionModel.setLanguage(response.getLanguage());
		recognitionModel.setSentiment(answerSentiments.toString());
	}

	private void detectCategories(String uriBase, String subscriptionKey, String description, RecognitionModel recognitionModel)
	{
		IamOptions options = new IamOptions.Builder().apiKey(subscriptionKey).build();
		NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding(VERSION, options);
		naturalLanguageUnderstanding.setEndPoint(uriBase);
		CategoriesOptions categories = new CategoriesOptions();
		Features features = new Features.Builder().categories(categories).build();
		AnalyzeOptions parameters = new AnalyzeOptions.Builder().text(description).features(features).build();
		AnalysisResults response = naturalLanguageUnderstanding.analyze(parameters).execute();
		List<String> result = new ArrayList<>();
		for (CategoriesResult category : response.getCategories())
		{
			result.add(category.getLabel() + ":" + String.format("%.2f", 100 * category.getScore()));
		}
		recognitionModel.setCategories(result);
	}

	private void detectEntities(String uriBase, String subscriptionKey, String description, RecognitionModel recognitionModel)
	{
		IamOptions options = new IamOptions.Builder().apiKey(subscriptionKey).build();
		NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding(VERSION, options);
		naturalLanguageUnderstanding.setEndPoint(uriBase);
		EntitiesOptions entities = new EntitiesOptions.Builder().sentiment(false).limit(10).build();
		Features features = new Features.Builder().entities(entities).build();
		AnalyzeOptions parameters = new AnalyzeOptions.Builder().text(description).features(features).build();
		AnalysisResults response = naturalLanguageUnderstanding.analyze(parameters).execute();
		List<EntitiesResult> rawResult = response.getEntities();
		List<String> answer = new ArrayList<>();
		recognitionModel.setEntities("Entities not found");
		for (EntitiesResult entitiesResult : rawResult)
		{
			StringBuilder workEntity = new StringBuilder();
			workEntity.append(entitiesResult.getText()).append(" - ").append(entitiesResult.getType()).append(" - ")
				.append(String.format("%.2f", 100 * entitiesResult.getRelevance()));
			answer.add(workEntity.toString());
		}
		recognitionModel.setLinkedEntities(answer);
	}

	private void detectConcepts(String uriBase, String subscriptionKey, String description, RecognitionModel recognitionModel)
	{
		IamOptions options = new IamOptions.Builder().apiKey(subscriptionKey).build();
		NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding(VERSION, options);
		naturalLanguageUnderstanding.setEndPoint(uriBase);
		ConceptsOptions concepts = new ConceptsOptions.Builder().limit(10).build();
		Features features = new Features.Builder().concepts(concepts).build();
		AnalyzeOptions parameters = new AnalyzeOptions.Builder().text(description).features(features).build();
		AnalysisResults response = naturalLanguageUnderstanding.analyze(parameters).execute();
		List<String> result = new ArrayList<>();
		List<ConceptsResult> conceptsRaw = response.getConcepts();
		for (ConceptsResult conceptsResult : conceptsRaw)
		{
			result.add(conceptsResult.getText() + ":" + String.format("%.2f", 100 * conceptsResult.getRelevance()));
		}
		recognitionModel.setConcepts(result);
	}
}
