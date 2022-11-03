package com.example.imrecognition;

import java.util.List;
import java.util.Map;

import com.example.imrecognition.transformers.RecognitionModel;

public interface TextRecognitionManagement
{
	String TEXTENGINE_PROVIDER = "TEXTENGINE";
	String COLLECTION_ID = "COLLECTION_ID";
	String MSTAGS_PROVIDER = "MSTAGS";
	String IBMTAGS_PROVIDER = "IBMTAGS";
	String DOTS_PROVIDER = "DOTS";
	String TEXTOMATIC_PROVIDER="TEXTOMATIC";
    String GOOGLETAGS_PROVIDER="GOOGLETAGS";
	String AWSTAGS_PROVIDER="AWSTAGS";
	String AYLIEN_PROVIDER ="AYLIEN";
	String SERVICE_NOT_SUPPORTED= "Service is not supported";

	Map<String, RecognitionModel> recognize(Long productId, Map<String, String> productAttributes,
		Map<String, String> configAttributes, List<String> preferredProviders);

	Map<String, RecognitionModel> recognizeTags(String description, Map<String, String> configAttributes,
		List<String> preferredProviders);
}
