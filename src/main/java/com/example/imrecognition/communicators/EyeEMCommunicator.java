package com.example.imrecognition.communicators;

import com.example.imrecognition.ImageRecognitionManagement;
import com.example.imrecognition.transformers.EyeEMRawDataTransformer;
import com.example.imrecognition.transformers.RawDataTransformer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class EyeEMCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(EyeEMCommunicator.class);
	@Autowired
	private EyeEMRawDataTransformer eyeEMRawDataTransformer;

	/**
	 * Send byte to service
	 * @param imageData
	 *            image
	 * @param parameters
	 *            all parameters for service
	 * @return json map answer or message with error
	 */
	@Override
	public String recognize(byte[] imageData, Map<String, String> parameters, String baseUrl)
	{
		String clientApikey = parameters.get(ImageRecognitionManagement.EYEEM_PROVIDER + CLIENT_API_KEY);
		String clientApiUrl = parameters.get(ImageRecognitionManagement.EYEEM_PROVIDER + CLIENT_API_URL);
		String[] split = clientApikey.split(":");
		String answer = null;
		try
		{
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity httpEntity = new HttpEntity(null, headers);
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
			String url = clientApiUrl + "token?clientId=" + split[0] + "&clientSecret=" + split[1];
			ResponseEntity responseEntity = restTemplate.exchange(url, HttpMethod.POST, (HttpEntity<?>) httpEntity, Map.class);
			Map<String, String> tokenBody = (Map<String, String>) responseEntity.getBody();
			if (tokenBody != null)
			{
				String accessToken = tokenBody.get("access_token");
				headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				headers.add("Authorization", "Bearer " + accessToken);
				String imageRequestString = Base64.getEncoder().encodeToString(imageData);
				String json =
					"{\"requests\":[{\"tasks\":[{\"type\":\"TAGS\"},{\"type\":\"CAPTIONS\"},{\"type\":\"AESTHETIC_SCORE\"}],\"image\":{\"content\":\""
						+ imageRequestString + "\"}}]}";
				RestTemplate restTemplateAnalize = new RestTemplate();
				HttpEntity httpEntityAnalize = new HttpEntity(json, headers);
				url = clientApiUrl + "analyze";
				ResponseEntity responseEntityAnalize =
					restTemplateAnalize.exchange(url, HttpMethod.POST, httpEntityAnalize, String.class);
				answer = responseEntityAnalize.getBody().toString();
			}
			else
				answer = "Wrong AUTH key";
		}
		catch (Exception e)
		{
			answer = "Unexpected error during work EYEEM service \n" + e.getMessage();
			logger.error("Error while get answer", e);
		}
		return answer;
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return eyeEMRawDataTransformer;
	}
}
