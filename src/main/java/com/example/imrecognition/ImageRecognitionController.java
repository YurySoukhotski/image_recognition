package com.example.imrecognition;

import com.example.imrecognition.helper.OmnIntegrationHelper;
import com.example.imrecognition.communicators.ImageRecognitionCommunicator;
import com.example.imrecognition.transformers.RecognitionModel;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageRecognitionController implements ImageRecognitionManagement
{
	private Map<String, ImageRecognitionCommunicator> providers = new HashMap<>();

	public ImageRecognitionController(Map<String, ImageRecognitionCommunicator> providers)
	{
		this.providers = providers;
	}

	@Override
	public Map<String, RecognitionModel> recognizeImage(byte[] imageData, List<String> preferredProviders,
		Map<String, String> parameters, String imageUrl)
	{
		Map<String, RecognitionModel> result = new HashMap<>();
		if (preferredProviders != null)
		{
			for (String provider : preferredProviders)
			{
				ImageRecognitionCommunicator communicator = providers.get(provider);
				String rawResult = communicator.recognize(imageData, parameters, imageUrl);
				result.put(provider, communicator.getTransformer().transform(rawResult));
			}
		}
		else
		{
			ImageRecognitionCommunicator communicator = providers.get(CLARIFAI_PROVIDER);
			String rawResult = communicator.recognize(imageData, parameters, imageUrl);
			result.put(CLARIFAI_PROVIDER, communicator.getTransformer().transform(rawResult));
		}
		return result;
	}

	public Map<String, RecognitionModel> recognizeImage(String imageUrl, List<String> preferredProviders,
		Map<String, String> parameters) throws Exception
	{
		URL url = new URL(imageUrl);
		byte[] data = OmnIntegrationHelper.downloadUrl(url);
		return recognizeImage(data, preferredProviders, parameters, imageUrl);
	}
}
