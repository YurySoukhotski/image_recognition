package com.example.imrecognition.communicators;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.example.imrecognition.ImageRecognitionManagement;
import com.example.imrecognition.transformers.ClarifaiRawDataTransformer;
import com.example.imrecognition.transformers.RawDataTransformer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class ClarifaiCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(ClarifaiCommunicator.class);
	@Autowired
	private ClarifaiRawDataTransformer clarifaiRawDataTransformer;
	private static final String JSON = "application/json";
	private static final String CONTENT = "content-Type";

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
		String clientApikey = parameters.get(ImageRecognitionManagement.CLARIFAI_PROVIDER + ConstantHelper.CLIENT_API_KEY);
		String clientApiUrl = parameters.get(ImageRecognitionManagement.CLARIFAI_PROVIDER + ConstantHelper.CLIENT_API_URL);
		String clientModel = parameters.get(ImageRecognitionManagement.CLARIFAI_PROVIDER + ConstantHelper.CLIENT_API_MODEL);
		String clientModelEn = parameters.get(ImageRecognitionManagement.CLARIFAI_PROVIDER + ConstantHelper.CLIENT_API_MODEL + "EN");
		String clientApiKeyEn = parameters.get(ImageRecognitionManagement.CLARIFAI_PROVIDER + ConstantHelper.CLIENT_API_KEY + "EN");
		String clientModelEnAdditional = parameters.get(ImageRecognitionManagement.CLARIFAI_PROVIDER + ConstantHelper.CLIENT_API_MODEL + "ADDITIONAL");
		String modelEbike = parameters.get(ImageRecognitionManagement.CLARIFAI_PROVIDER + "MODEL_EBIKE");
		String modelEbikeKey = parameters.get(ImageRecognitionManagement.CLARIFAI_PROVIDER + "MODEL_EBIKE_KEY");
		String answer;
		Map<String, Object> mapAnswer = new HashMap<>();
		try
		{
			if (clientApikey != null)
			{
				mapAnswer.put("de", makeCall(clientApiUrl, clientModel, clientApikey, url));
				mapAnswer.put("additional", makeCall(clientApiUrl, clientModelEnAdditional, clientApiKeyEn, url));
				mapAnswer.put("en",  makeCall(clientApiUrl, clientModelEn, clientApiKeyEn, url));
			}
			else
			{
				mapAnswer.put("additional", makeCall(clientApiUrl, modelEbike, modelEbikeKey, url));
			}
			answer = new Gson().toJson(mapAnswer);
		}
		catch (Exception e)
		{
			answer = "Unexpected error during Clarifai work \n" + e.getMessage();
			logger.error("Error while get answer", e);
		}
		return answer;
	}
	private String makeCall(String targetURL, String workflowId, String key, String imgUrl) throws IOException {
		Map<String, Object> urlLevel = new HashMap<>();
		urlLevel.put("url", imgUrl);
		Map<String, Object> imgLevel = new HashMap<>();
		imgLevel.put("image", urlLevel);

		Map<String, Object> dataLevel = new HashMap<>();
		dataLevel.put("data", imgLevel);

		Object[] dataArray = new Object[1];
		dataArray[0] = dataLevel;
		Map<String, Object> inputsLevel = new HashMap<>();
		inputsLevel.put("inputs", dataArray);
		Gson gson = new Gson();
		String json = gson.toJson(inputsLevel);
		OkHttpClient client = new OkHttpClient();
		MediaType mediaType = MediaType.parse(JSON);
		RequestBody body = RequestBody.create(mediaType, json);
		Request request = new Request.Builder().addHeader("authorization", "Key " + key).url(targetURL + workflowId + "/results").post(body).addHeader(CONTENT, JSON).build();
		Response response = client.newCall(request).execute();
		if (response.body() != null) {
			JsonObject jsonObject = new JsonParser().parse(response.body().string()).getAsJsonObject();
			if (jsonObject.get("results") != null && !jsonObject.get("results").getAsJsonArray().isJsonNull()) {
				return jsonObject.get("results").getAsJsonArray().get(0).getAsJsonObject().toString();
			}
		}
		return "No generated text ";
	}
	@Override
	public RawDataTransformer getTransformer()
	{
		return clarifaiRawDataTransformer;
	}
}
