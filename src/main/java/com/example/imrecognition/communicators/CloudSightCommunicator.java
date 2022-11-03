package com.example.imrecognition.communicators;

import com.example.imrecognition.ImageRecognitionManagement;
import com.example.imrecognition.transformers.CloudSightRawDataTransformed;
import com.example.imrecognition.transformers.RawDataTransformer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class CloudSightCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(CloudSightCommunicator.class);
	@Autowired
	private CloudSightRawDataTransformed cloudSightRawDataTransformed;

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
		String clientApikey = parameters.get(ImageRecognitionManagement.CLOUDSIGHT_PROVIDER + CLIENT_API_KEY);
		String clientApiUrl = parameters.get(ImageRecognitionManagement.CLOUDSIGHT_PROVIDER + CLIENT_API_URL);
		String answer = "";
		try
		{
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add("Authorization", "CloudSight " + clientApikey);
			headers.add("cache-control", "no-cache");
			String imageRequestString = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(imageData);
			String json = "{\"image\": \"" + imageRequestString + "\",\"locale\": \"en_US\"}";
			RestTemplate restTemplateAnalize = new RestTemplate();
			HttpEntity httpEntityAnalize = new HttpEntity(json, headers);
			ResponseEntity responseEntityAnalize =
				restTemplateAnalize.exchange(clientApiUrl, HttpMethod.POST, httpEntityAnalize, String.class);
			answer = responseEntityAnalize.getBody().toString();
		}
		catch (Exception e)
		{
			answer = "Unexpected error during work CloudSight service \n" + e.getMessage();
			logger.error("Error while get answer", e);
		}
		return answer;
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return cloudSightRawDataTransformed;
	}
}
