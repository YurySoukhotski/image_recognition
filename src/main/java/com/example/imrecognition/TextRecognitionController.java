package com.example.imrecognition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.imrecognition.communicators.TagsCommunicator;
import com.example.imrecognition.communicators.TextCommunicator;
import com.example.imrecognition.transformers.RecognitionModel;

public class TextRecognitionController implements TextRecognitionManagement
{
	private Map<String, TextCommunicator> providers;
	private Map<String, TagsCommunicator> providersTags;

	public TextRecognitionController(Map<String, TextCommunicator> providers, Map<String, TagsCommunicator> providersTags)
	{
		this.providers = providers;
		this.providersTags = providersTags;
	}

	@Override
	public Map<String, RecognitionModel> recognize(Long productId, Map<String, String> productAttributes,
		Map<String, String> configAttributes, List<String> preferredProviders)
	{
		Map<String, RecognitionModel> result = new HashMap<>();
		if (preferredProviders != null)
		{
			for (String provider : preferredProviders)
			{
				TextCommunicator communicator = providers.get(provider);
				result.put(provider, communicator.recognize(productId, productAttributes, configAttributes));
			}
		}
		else
		{
			TextCommunicator communicator = providers.get(TEXTENGINE_PROVIDER);
			result.put(TEXTENGINE_PROVIDER, communicator.recognize(productId, productAttributes, configAttributes));
		}
		return result;
	}

	@Override
	public Map<String, RecognitionModel> recognizeTags(String description, Map<String, String> configAttributes,
		List<String> preferredProviders)
	{
		Map<String, RecognitionModel> result = new HashMap<>();
		if (preferredProviders != null)
		{
			for (String provider : preferredProviders)
			{
				TagsCommunicator communicator = providersTags.get(provider);
				result.put(provider, communicator.recognizeTags(description, configAttributes));
			}
		}
		else
		{
			TagsCommunicator communicator = providersTags.get(MSTAGS_PROVIDER);
			result.put(MSTAGS_PROVIDER, communicator.recognizeTags(description, configAttributes));
		}
		return result;
	}
}
