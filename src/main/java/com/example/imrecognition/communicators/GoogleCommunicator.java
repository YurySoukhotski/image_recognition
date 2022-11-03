package com.example.imrecognition.communicators;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;

import com.example.imrecognition.ImageRecognitionManagement;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.imrecognition.transformers.GoogleVisionRawDataTransformer;
import com.example.imrecognition.transformers.RawDataTransformer;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;
import static com.example.imrecognition.communicators.ConstantHelper.EMPTY_SECRET_KEY;

public class GoogleCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(GoogleCommunicator.class);
	@Autowired
	private GoogleVisionRawDataTransformer googleVisionRawDataTransformer;

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
		String clientApikey = parameters.get(ImageRecognitionManagement.GOOGLE_PROVIDER + CLIENT_API_KEY);
		String clientApiUrl = parameters.get(ImageRecognitionManagement.GOOGLE_PROVIDER + CLIENT_API_URL);
		String answer;
		if (clientApikey != null && clientApiUrl != null)
		{
			try
			{
				URL serverUrl = new URL(clientApiUrl + clientApikey);
				URLConnection urlConnection = serverUrl.openConnection();
				HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
				httpConnection.setRequestMethod("POST");
				httpConnection.setRequestProperty("Content-Type", "application/json");
				httpConnection.setDoOutput(true);
				BufferedWriter httpRequestBodyWriter =
					new BufferedWriter(new OutputStreamWriter(httpConnection.getOutputStream()));
				String imageRequestString = Base64.getEncoder().encodeToString(imageData);
				String json =
					"{\"requests\":[{\"features\":[ " + "{\"type\": \"SAFE_SEARCH_DETECTION\"},"
						+ "{\"type\": \"WEB_DETECTION\"}," + "{\"type\": \"LANDMARK_DETECTION\"} ,"
						+ "{\"type\": \"LABEL_DETECTION\"}," + "{\"type\": \"LOGO_DETECTION\"},"
						+ "{\"type\": \"OBJECT_LOCALIZATION\"}," + "{\"type\": \"CROP_HINTS\"},"
						+ "{\"type\": \"IMAGE_PROPERTIES\"}," + "{\"type\": \"DOCUMENT_TEXT_DETECTION\"},"
						+ "{\"type\": \"FACE_DETECTION\"}], \"image\": {\"content\": \"" + imageRequestString + "\"}}]}";
				httpRequestBodyWriter.write(json);
				httpRequestBodyWriter.close();
				Scanner httpResponseScanner = new Scanner(httpConnection.getInputStream());
				StringBuilder resp = new StringBuilder();
				while (httpResponseScanner.hasNext())
				{
					String line = httpResponseScanner.nextLine();
					resp.append(line);
				}
				answer = resp.toString();
				httpResponseScanner.close();
			}
			catch (Exception e)
			{
				answer = "Unexpected error during work Google service \n" + e.getMessage();
				logger.error("Error while get answer", e);
			}
		}
		else
		{
			answer = EMPTY_SECRET_KEY;
		}
		return answer;
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return googleVisionRawDataTransformer;
	}
}
