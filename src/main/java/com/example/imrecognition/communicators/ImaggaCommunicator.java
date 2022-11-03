package com.example.imrecognition.communicators;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.example.imrecognition.ImageRecognitionManagement;
import com.example.imrecognition.transformers.ImaggaRawDataTransformer;
import com.example.imrecognition.transformers.RawDataTransformer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.CLIENT_API_URL;
import static com.example.imrecognition.communicators.ConstantHelper.EMPTY_SECRET_KEY;
import static com.example.imrecognition.communicators.ConstantHelper.EMPTY_URL;

public class ImaggaCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(ImaggaCommunicator.class);
	private static final String AUTHORIZATION = "Authorization";
	private static final String BASIC = "Basic ";
	@Autowired
	private ImaggaRawDataTransformer imaggaRawDataTransformer;
	private Integer countAttempt = 3;

	@Override
	public String recognize(byte[] imageData, Map<String, String> parameters, String baseUrl)
	{
		String answer = "";
		HashMap<String, Object> answerMap = new HashMap();
		try
		{
			String credentialsToEncode = parameters.get(ImageRecognitionManagement.IMAGGA_PROVIDER + CLIENT_API_KEY);
			String apiUrl = parameters.get(ImageRecognitionManagement.IMAGGA_PROVIDER + CLIENT_API_URL);
			if (credentialsToEncode != null && apiUrl != null)
			{
				String basicAuth = Base64.getEncoder().encodeToString(credentialsToEncode.getBytes(StandardCharsets.UTF_8));
				String endpoint = "/uploads";
				String crlf = "\r\n";
				String twoHyphens = "--";
				String boundary = "Image Upload";
				URL urlObject = new URL("https://api.imagga.com/v2" + endpoint);
				HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
				connection.setRequestProperty(AUTHORIZATION, BASIC + basicAuth);
				connection.setUseCaches(false);
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Connection", "Keep-Alive");
				connection.setRequestProperty("Cache-Control", "no-cache");
				connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
				DataOutputStream request = new DataOutputStream(connection.getOutputStream());
				request.writeBytes(twoHyphens + boundary + crlf);
				request.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + baseUrl + "\"" + crlf);
				request.writeBytes(crlf);
				InputStream inputStream = new ByteArrayInputStream(imageData);
				int bytesRead;
				byte[] dataBuffer = new byte[1024];
				while (( bytesRead = inputStream.read(dataBuffer) ) != -1)
				{
					request.write(dataBuffer, 0, bytesRead);
				}
				request.writeBytes(crlf);
				request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
				request.flush();
				request.close();
				InputStream responseStream = new BufferedInputStream(connection.getInputStream());
				BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				ObjectMapper objectMapper = new ObjectMapper();
				ImaggaAnswer imaggaAnswer = objectMapper.readValue(connectionInput.readLine(), ImaggaAnswer.class);
				connection.disconnect();
				responseStream.close();
				boolean success = false;
				if (countAttempt != 3)
				{
					countAttempt = 3;
				}
				while (!success && countAttempt > 0)
				{
					try
					{
						TimeUnit.MILLISECONDS.sleep(5000);
						answerMap.put("tags", getContent(imaggaAnswer.getResult().getUploadId(), apiUrl, basicAuth));
                        TimeUnit.MILLISECONDS.sleep(1000);
						answerMap.put("categories", getCategory(imaggaAnswer.getResult().getUploadId(), apiUrl, basicAuth));
                        TimeUnit.MILLISECONDS.sleep(1000);
						answerMap.put("colors", getColors(imaggaAnswer.getResult().getUploadId(), apiUrl, basicAuth));
                        TimeUnit.MILLISECONDS.sleep(1000);
						answerMap.put("nsfw", getNsfwInfo(imaggaAnswer.getResult().getUploadId(), apiUrl, basicAuth));
						answer = new Gson().toJson(answerMap);
						success = true;
					}
					catch (Exception e)
					{
						logger.error("Error while get response from Imagga server. Attempt :" + countAttempt, e);
						countAttempt--;
					}
				}
			}
			else
			{
				answer = EMPTY_SECRET_KEY + " or " + EMPTY_URL;
			}
		}
		catch (Exception e)
		{
			answer = "Unexpected error during work Imagga service \n" + e.getMessage();
			logger.error("Error while get answer", e);
		}
		return answer;
	}

	private Object getNsfwInfo(String id, String apiUrl, String basicAuth) throws IOException
	{
		StringBuilder answer = new StringBuilder();
		String url = apiUrl + "categories/nsfw_beta?image_upload_id=" + id;
		URL urlObject = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
		connection.setRequestProperty(AUTHORIZATION, BASIC + basicAuth);
		BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		answer.append(connectionInput.readLine());
		connectionInput.close();
		return answer;
	}

	private Object getContent(String id, String apiUrl, String basicAuth) throws IOException
	{
		StringBuilder answer = new StringBuilder();
		String url = apiUrl + "tags?image_upload_id=" + id;
		URL urlObject = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
		connection.setRequestProperty(AUTHORIZATION, BASIC + basicAuth);
		BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		answer.append(connectionInput.readLine());
		connectionInput.close();
		return answer;
	}

	private String getColors(String id, String apiUrl, String basicAuth) throws IOException
	{
		StringBuilder answer = new StringBuilder();
		String url = apiUrl + "colors?image_upload_id=" + id;
		URL urlObject = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
		connection.setRequestProperty(AUTHORIZATION, BASIC + basicAuth);
		BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		answer.append(connectionInput.readLine());
		connectionInput.close();
		return answer.toString();
	}

	private Object getCategory(String id, String apiUrl, String basicAuth) throws IOException
	{
		StringBuilder answer = new StringBuilder();
		String url = apiUrl + "categories/personal_photos?image_upload_id=" + id;
		URL urlObject = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
		connection.setRequestProperty("Authorization", "Basic " + basicAuth);
		BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		answer.append(connectionInput.readLine());
		connectionInput.close();
		return answer;
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return imaggaRawDataTransformer;
	}

	static class ImaggaAnswer
	{
		@JsonProperty("result")
		private Result result;
		@JsonProperty("status")
		private Status status;

		public Result getResult()
		{
			return result;
		}

		public void setResult(Result result)
		{
			this.result = result;
		}

		public Status getStatus()
		{
			return status;
		}

		public void setStatus(Status status)
		{
			this.status = status;
		}
	}

	static class Result
	{
		@JsonProperty("upload_id")
		private String uploadId;

		String getUploadId()
		{
			return uploadId;
		}

		public void setUploadId(String uploadId)
		{
			this.uploadId = uploadId;
		}
	}

	static class Status
	{
		@JsonProperty("text")
		private String text;
		@JsonProperty("type")
		private String type;

		public String getText()
		{
			return text;
		}

		public void setText(String text)
		{
			this.text = text;
		}

		public String getType()
		{
			return type;
		}

		public void setType(String type)
		{
			this.type = type;
		}
	}
}
