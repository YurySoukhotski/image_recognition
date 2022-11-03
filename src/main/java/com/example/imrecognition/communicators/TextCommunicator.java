package com.example.imrecognition.communicators;

import com.example.imrecognition.transformers.RecognitionModel;

import java.util.Map;

public interface TextCommunicator
{
	RecognitionModel recognize(Long productId, Map<String, String> productAttributes, Map<String, String> configAttributes);
}
