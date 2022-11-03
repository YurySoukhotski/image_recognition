package com.example.imrecognition.communicators;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.example.imrecognition.ImageRecognitionManagement;
import com.example.imrecognition.transformers.AWSRawDataTransformer;
import com.google.gson.Gson;
import com.example.imrecognition.transformers.RawDataTransformer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;

public class AWSCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(ImageRecognitionCommunicator.class);
	@Autowired
	private AWSRawDataTransformer awsRawDataTransformer;

	/**
	 * Send byte to service
	 * @param imageData
	 *            image
	 * @param parameters
	 *            all parameters for service
	 * @return json map answer or message with error
	 */
	@Override
	public String recognize(byte[] imageData, Map<String, String> parameters, String url)
	{
		String clientApikey = parameters.get(ImageRecognitionManagement.AWS_PROVIDER + CLIENT_API_KEY);
		String[] split = clientApikey.split(":");
		String answer = "";
		HashMap<String, Object> answerMap = new HashMap<>();
		try
		{
			AmazonRekognition rekognitionClient =
				AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_2)
					.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(split[0], split[1]))).build();
			ByteBuffer bytes = ByteBuffer.wrap(imageData);
			DetectTextRequest request = new DetectTextRequest().withImage(new Image().withBytes(bytes));
			DetectTextResult detectTextResult = rekognitionClient.detectText(request);
			DetectLabelsRequest requestLabel = new DetectLabelsRequest().withImage(new Image().withBytes(bytes));
			DetectLabelsResult detectLabelsResult = rekognitionClient.detectLabels(requestLabel);
			DetectFacesRequest requestFace =
				new DetectFacesRequest().withImage(new Image().withBytes(bytes)).withAttributes(Attribute.ALL);
			DetectFacesResult resultFace = rekognitionClient.detectFaces(requestFace);
			RecognizeCelebritiesRequest requestCelebritiesRequest =
				new RecognizeCelebritiesRequest().withImage(new Image().withBytes(bytes));
			RecognizeCelebritiesResult celebritiesResult = rekognitionClient.recognizeCelebrities(requestCelebritiesRequest);
			rekognitionClient.shutdown();
			if (!detectLabelsResult.getLabels().isEmpty())
			{
				answerMap.put("label", detectLabelsResult.getLabels());
			}
			if (!detectTextResult.getTextDetections().isEmpty())
			{
				answerMap.put("text", detectTextResult.getTextDetections());
			}
			if (!resultFace.getFaceDetails().isEmpty())
			{
				answerMap.put("face", resultFace.getFaceDetails());
			}
			if (!celebritiesResult.getCelebrityFaces().isEmpty())
			{
				answerMap.put("celebrity", celebritiesResult.getCelebrityFaces());
			}
			answer = new Gson().toJson(answerMap);
		}
		catch (Exception e)
		{
			answer = "Unexpected error during work AWS service \n" + e.getMessage();
			logger.error("Error while get answer", e);
		}
		return answer;
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return awsRawDataTransformer;
	}
}
