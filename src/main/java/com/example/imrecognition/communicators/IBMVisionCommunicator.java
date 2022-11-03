package com.example.imrecognition.communicators;

import com.example.imrecognition.ImageRecognitionManagement;
import com.google.gson.Gson;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.*;
import com.example.imrecognition.transformers.IBMVisionRawDataTransformer;
import com.example.imrecognition.transformers.RawDataTransformer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import static com.example.imrecognition.communicators.ConstantHelper.*;

public class IBMVisionCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(IBMVisionCommunicator.class);
	private static final String APPVERSION="2018-03-19";
	@Autowired
	private IBMVisionRawDataTransformer ibmVisionRawDataTransformer;
	private Integer countAttempt = 3;

	@Override
	public String recognize(byte[] imageData, Map<String, String> parameters, String url)
	{
		String answer;
		Map<String, Object> answerMap = new HashMap<>();
		String apiKey = parameters.get(ImageRecognitionManagement.IBMVISION_PROVIDER + CLIENT_API_KEY);
		String uriBase = parameters.get(ImageRecognitionManagement.IBMVISION_PROVIDER + CLIENT_API_URL);
		String language = parameters.get(ImageRecognitionManagement.IBMVISION_PROVIDER + CLIENT_API_LANGUAGE);
		if (apiKey != null && uriBase != null)
		{
			boolean success = false;
			if (countAttempt != 3)
			{
				countAttempt = 3;
			}
			while (!success && countAttempt > 0)
			{
				try
				{
					List<ClassifiedImage> classifiedImages = analiseImage(uriBase, apiKey, language, imageData);
					if (classifiedImages != null && !classifiedImages.isEmpty())
					{
						answerMap.put("label", classifiedImages.get(0));
					}
					List<ImageWithFaces> imageWithFaces = analiseFace(uriBase, apiKey, imageData);
					if (imageWithFaces != null && !imageWithFaces.isEmpty())
					{
						answerMap.put("face", imageWithFaces.get(0).getFaces());
					}
					success = true;
				}
				catch (Exception e)
				{
					logger.error("Error while get response from IBM server. Attempt :" + countAttempt, e);
					countAttempt--;
				}
			}
			if (!answerMap.isEmpty())
			{
				answer = new Gson().toJson(answerMap);
			}
			else
			{
				answer = "Unexpected error during work IBMVision service";
			}
		}
		else
		{
			answer = EMPTY_SECRET_KEY + " or " + EMPTY_URL;
		}
		return answer;
	}

	private List<ClassifiedImage> analiseImage(String uriBase, String apiKey, String language, byte[] imageData)
	{
		VisualRecognition service = new VisualRecognition(APPVERSION);
		service.setEndPoint(uriBase);
		IamOptions options = new IamOptions.Builder().apiKey(apiKey).build();
		service.setIamCredentials(options);
		ClassifyOptions classifyOptions;
		List<String> listOptions = new ArrayList<>();
		listOptions.add("food");
		listOptions.add("explicit");
		listOptions.add("default");
		InputStream imagesStream = new ByteArrayInputStream(imageData);
		classifyOptions =
			new ClassifyOptions.Builder().imagesFile(imagesStream).imagesFilename("test.jpg").threshold((float) 0.2)
				.owners(Arrays.asList("IBM")).acceptLanguage(language).classifierIds(listOptions).build();
		ClassifiedImages execute = service.classify(classifyOptions).execute();
		return execute.getImages();
	}

	private List<ImageWithFaces> analiseFace(String uriBase, String apiKey, byte[] imageData)
	{
		VisualRecognition service = new VisualRecognition(APPVERSION);
		service.setEndPoint(uriBase);
		service = new VisualRecognition(APPVERSION);
		service.setEndPoint(uriBase);
		IamOptions options = new IamOptions.Builder().apiKey(apiKey).build();
		service.setIamCredentials(options);
		InputStream imagesStream = new ByteArrayInputStream(imageData);
		DetectFacesOptions detectFacesOptions =
			new DetectFacesOptions.Builder().imagesFile(imagesStream).imagesFilename("test.jpg").build();
		DetectedFaces detectedFaces = service.detectFaces(detectFacesOptions).execute();
		return detectedFaces.getImages();
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return ibmVisionRawDataTransformer;
	}
}
