package com.example.imrecognition.helper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import com.example.imrecognition.transformers.MapHelper;

public class TranslatorHelper
{
	private static final Logger logger = LogManager.getLogger(TranslatorHelper.class);

	public static Map<String, String> translate(String subscriptionKey, String host, String path, String params, String textArray)
	{
		try
		{
			URL url = new URL(host + path + params);
			List<RequestBody> objList = new ArrayList<>();
			objList.add(new RequestBody(textArray));
			String content = new Gson().toJson(objList);
			String response = post(url, content, subscriptionKey);
			return prettify(response);
		}
		catch (Exception e)
		{
			logger.error("Error while get translation", e);
			return null;
		}
	}

	public static String translateWord(String subscriptionKey, String host, String path, String params, String word)
	{
		try
		{
			URL url = new URL(host + path + params);
			List<RequestBody> objList = new ArrayList<>();
			objList.add(new RequestBody(word));
			String content = new Gson().toJson(objList);
			String response = post(url, content, subscriptionKey);
			return prettifyWord(response);
		}
		catch (Exception e)
		{
			logger.error("Error while get translation", e);
			return null;
		}
	}

	static String prettifyWord(String jsonText) throws IOException
	{ // todo change
		String text = null;
		Map<String, Double> mapAnswer = new HashMap<>();
		Map<String, String> mapAnswerWrong = new HashMap<>();
		List<Object> result = new ObjectMapper().readValue(jsonText, List.class);
		if (result != null && !result.isEmpty())
		{
			Map<String, Object> translation = (Map<String, Object>) result.get(0);
			List<Map<String, String>> translations = (List<Map<String, String>>) translation.get("translations");
			if (translations != null && !translations.isEmpty())
			{
				Map<String, String> textMap = translations.get(0);
				text = textMap.get("text");
			}
		}

		return text;
	}

	static Map<String, String> prettify(String jsonText) throws IOException
	{ // todo change
		String text = null;
		Map<String, Double> mapAnswer = new HashMap<>();
		Map<String, String> mapAnswerWrong = new HashMap<>();
		List<Object> result = new ObjectMapper().readValue(jsonText, List.class);
		if (result != null && !result.isEmpty())
		{
			Map<String, Object> translation = (Map<String, Object>) result.get(0);
			List<Map<String, String>> translations = (List<Map<String, String>>) translation.get("translations");
			if (translations != null && !translations.isEmpty())
			{
				Map<String, String> textMap = translations.get(0);
				text = textMap.get("text");
			}
		}
		if (text != null)
		{
			String[] strings = text.split(";");
			for (int i = 0; i < strings.length; i++)
			{
				String keyValue = strings[i];
				String[] values = keyValue.split(":");
				if (values.length > 1)
				{
					String correctDouble = values[values.length - 1].replace(",", ".").replace(" ", "");
					String correctTag = values[0].trim().toLowerCase();
					if (!correctTag.isEmpty())
					{
						if (isCorrectDouble(correctDouble))
						{
							mapAnswer.put(correctTag, Double.valueOf(correctDouble));
						}
						else
						{
							mapAnswerWrong.put(correctTag, correctDouble.toLowerCase());
						}
					}
				}
			}
		}
		Map<String, String> sortMap = MapHelper.sortMap(mapAnswer);
		sortMap.putAll(mapAnswerWrong);
		return sortMap;
	}

	static boolean isCorrectDouble(String value)
	{
		Double doubleValue;
		try
		{
			doubleValue = Double.valueOf(value);
		}
		catch (NumberFormatException e)
		{
			return false;
		}
		return doubleValue != null ? true : false;
	}

	static String post(URL url, String content, String subscriptionKey) throws Exception
	{
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Content-Length", Integer.toString(content.length()));
		connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
		connection.setRequestProperty("X-ClientTraceId", java.util.UUID.randomUUID().toString());
		connection.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		byte[] encodedContent = content.getBytes("UTF-8");
		wr.write(encodedContent, 0, encodedContent.length);
		wr.flush();
		wr.close();
		StringBuilder response = new StringBuilder();
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
		String line;
		while (( line = in.readLine() ) != null)
		{
			response.append(line);
		}
		in.close();
		return response.toString();
	}
}

class RequestBody
{
	String text;

	public RequestBody(String text)
	{
		this.text = text;
	}
}
