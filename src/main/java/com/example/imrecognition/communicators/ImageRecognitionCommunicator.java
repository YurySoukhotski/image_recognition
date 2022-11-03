package com.example.imrecognition.communicators;

import com.example.imrecognition.transformers.RawDataTransformer;

import java.util.Map;

public interface ImageRecognitionCommunicator
{
	String recognize(byte[] imageData, Map<String, String> parameters, String url);

	RawDataTransformer getTransformer();
}
