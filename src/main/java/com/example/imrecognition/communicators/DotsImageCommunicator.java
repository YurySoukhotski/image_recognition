package com.example.imrecognition.communicators;

import java.util.HashMap;
import java.util.Map;

import com.example.imrecognition.ImageRecognitionManagement;
import com.example.imrecognition.transformers.DotsImageRawDataTransformed;
import com.example.imrecognition.transformers.RawDataTransformer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.paralleldots.paralleldots.App;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;

public class DotsImageCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(DotsImageCommunicator.class);
	@Autowired
	private DotsImageRawDataTransformed dotsImageRawDataTransformed;

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
		String clientApikey = parameters.get(ImageRecognitionManagement.DOTSIMAGE_PROVIDER + CLIENT_API_KEY);
		Map<String, String> answerMap = new HashMap<>();
		App pd = new App(clientApikey);
		try
		{
			String imageInfo = pd.object_recognizer_url(url);
			String faceInfo = pd.facial_emotion_url(url);
			answerMap.put("imageInfo", imageInfo);
			answerMap.put("faceInfo", faceInfo);
			return new Gson().toJson(answerMap);
		}
		catch (Exception e)
		{
			logger.error("Error get info from Dots server", e);
			return "Unexpected error during work CloudSight service \n" + e.getMessage();
		}
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return dotsImageRawDataTransformed;
	}
}
