package com.example.imrecognition.communicators;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

import com.example.imrecognition.TextRecognitionManagement;
import com.example.imrecognition.transformers.RecognitionModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import static com.example.imrecognition.TextRecognitionManagement.GETSENTIMENT_PROVIDER;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;

public class GetSentimentCommunicator implements TagsCommunicator
{
	private static final Logger logger = LogManager.getLogger(GetSentimentCommunicator.class);

	@Override
	public RecognitionModel recognizeTags(String description, Map<String, String> configAttributes)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		String key = configAttributes.get(GETSENTIMENT_PROVIDER + CLIENT_API_KEY);
		String url = configAttributes.get(GETSENTIMENT_PROVIDER + CLIENT_API_URL);
		try
		{
			String categories = "1";
			String sentiment = "1";
			String annotate = "1";
			String params =
				"text=" + URLEncoder.encode(description, "UTF-8") + "&categories=" + URLEncoder.encode(categories, "UTF-8")
					+ "&sentiment=" + URLEncoder.encode(sentiment, "UTF-8") + "&annotate=" + URLEncoder.encode(annotate, "UTF-8");
			String response = makeCall(url, params, key);
			System.out.println("Response: " + response);
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

	private String makeCall(String targetURL, String urlParameters, String key)
	{
		URL url;
		HttpURLConnection connection = null;
		try
		{
			// create connection
			url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("X-RapidAPI-Key", key);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			// send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			// get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while (( line = rd.readLine() ) != null)
			{
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			if (connection != null)
			{
				connection.disconnect();
			}
		}
	}
}
