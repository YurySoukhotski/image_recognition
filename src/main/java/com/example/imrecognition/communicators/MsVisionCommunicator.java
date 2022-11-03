package com.example.imrecognition.communicators;

import com.example.imrecognition.ImageRecognitionManagement;
import com.example.imrecognition.transformers.MsVisionRawDataTransformer;
import com.example.imrecognition.transformers.RawDataTransformer;
import com.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.imrecognition.communicators.ConstantHelper.*;

public class MsVisionCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(MsVisionCommunicator.class);
	private static final String CONTENT = "Content-Type";
	private static final String OCT = "application/octet-stream";
	private static final String KEY = "Ocp-Apim-Subscription-Key";
	@Autowired
	private MsVisionRawDataTransformer msVisionRawDataTransformer;

	private static String convertStreamToString(InputStream is) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line;
		while (( line = reader.readLine() ) != null)
		{
			sb.append(line + "\n");
		}
		return sb.toString();
	}

	@Override
	public String recognize(byte[] imageData, Map<String, String> parameters, String url)
	{
		String answer;
		Map<String, Object> answerMap = new HashMap<>();
		String subscriptionKey = parameters.get(ImageRecognitionManagement.MSVISION_PROVIDER + CLIENT_API_KEY);
		String uriBase = parameters.get(ImageRecognitionManagement.MSVISION_PROVIDER + CLIENT_API_URL);
		String delay = parameters.get(ImageRecognitionManagement.MSVISION_PROVIDER + "DELAY");
		Integer attempt = Integer.valueOf(parameters.get(ImageRecognitionManagement.MSVISION_PROVIDER + "ATTEMPT"));
		String faceUrl = parameters.get(ImageRecognitionManagement.MSVISION_PROVIDER + CLIENT_API_URL + "FACE");
		String faceKey = parameters.get(ImageRecognitionManagement.MSVISION_PROVIDER + CLIENT_API_KEY + "FACE");
		if (subscriptionKey != null && uriBase != null)
		{
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			try
			{
				ByteArrayEntity byteArrayEntity = new ByteArrayEntity(imageData);
				/**
				 * First send request to OCR workflow
				 */
				CloseableHttpClient httpClientOcr = HttpClientBuilder.create().build();
				URIBuilder builderOcr = new URIBuilder(uriBase + "/read/analyze");
				builderOcr.setParameter("mode", "Printed");
				URI uriOcr = builderOcr.build();
				HttpPost requestOcr = new HttpPost(uriOcr);
				requestOcr.setHeader(CONTENT, OCT);
				requestOcr.setHeader(KEY, subscriptionKey);
				requestOcr.setEntity(byteArrayEntity);
				HttpResponse responseOcr = httpClientOcr.execute(requestOcr);
				//TimeUnit.MILLISECONDS.sleep(5000);
				Header[] headers = responseOcr.getHeaders("Operation-Location");
				String codeId = null;
				if (headers != null && responseOcr.getStatusLine().getStatusCode() == 202)
				{
					String value = headers[0].getValue();
					String[] strings = value.split("analyzeResults/");
					codeId = strings[1];
				}
				/**
				 * Request for tags
				 */
				URIBuilder builder = new URIBuilder(uriBase + "/analyze");
				builder.setParameter("visualFeatures", "ImageType,Categories,Description,Color,Tags,Faces,Adult");
				builder.setParameter("details", "Celebrities");
				builder.setParameter("language", "en");
				URI uri = builder.build();
				HttpPost request = new HttpPost(uri);
				request.setHeader(CONTENT, OCT);
				request.setHeader(KEY, subscriptionKey);
				request.setEntity(byteArrayEntity);
				HttpResponse response = httpClient.execute(request);
				HttpEntity entity = response.getEntity();
				if (entity != null)
				{
					String jsonString = EntityUtils.toString(entity);
					JSONObject json = new JSONObject(jsonString);
					answerMap.put("tag", json.toString(2));
				}
				TimeUnit.MILLISECONDS.sleep(Long.valueOf(delay));
				/**
				 * Request for result of OCR
				 */
				String answerText = "";
				do
				{
					CloseableHttpClient httpClientOcrResult = HttpClientBuilder.create().build();
					URIBuilder builderOcrResult = new URIBuilder(uriBase + "/read/analyzeResults/" + codeId);
					URI uriOcrResult = builderOcrResult.build();
					HttpGet requestOcrResult = new HttpGet(uriOcrResult);
					requestOcrResult.setHeader(KEY, subscriptionKey);
					HttpResponse responseOcrResult = httpClientOcrResult.execute(requestOcrResult);
					HttpEntity entityResult = responseOcrResult.getEntity();
					if (entityResult != null)
					{
						InputStream instream = entityResult.getContent();
						answerText = convertStreamToString(instream);
					}
					attempt--;
				}
				while (answerText.contains("Running") && attempt > -1);
				answerMap.put("ocr", answerText);
				answerMap.put("face", faceDetectionCall(faceUrl, faceKey, byteArrayEntity));
				answer = new Gson().toJson(answerMap);
			}
			catch (Exception e)
			{
				answer = "Unexpected error during work MsVision service \n" + e.getMessage();
				logger.error("Error while get answer", e);
			}
		}
		else
		{
			answer = EMPTY_SECRET_KEY + " or " + EMPTY_URL;
		}
		return answer;
	}

	private Object faceDetectionCall(String url, String key, ByteArrayEntity byteArrayEntity)
	{
		String faceAttributes =
			"age,gender,headPose,smile,facialHair,glasses,emotion,hair,makeup,occlusion,accessories,blur,exposure,noise";
		String answer = "";
		try
		{
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			URIBuilder builder = new URIBuilder(url);
			builder.setParameter("returnFaceId", "true");
			builder.setParameter("returnFaceLandmarks", "true");
			builder.setParameter("returnFaceAttributes", faceAttributes);
			URI uri = builder.build();
			HttpPost request = new HttpPost(uri);
			request.setHeader("Content-Type", "application/octet-stream");
			request.setHeader("Ocp-Apim-Subscription-Key", key);
			request.setEntity(byteArrayEntity);
			HttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			if (entity != null)
			{
				return EntityUtils.toString(entity).trim();
			}
		}
		catch (Exception e)
		{
			answer = "Unexpected error during work MsVision FaceApi service \n" + e.getMessage();
			logger.error("Error while get Face api answer", e);
		}
		return answer;
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return msVisionRawDataTransformer;
	}
}
