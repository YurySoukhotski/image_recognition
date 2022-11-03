package com.example.imrecognition.communicators;

import com.example.imrecognition.transformers.RecognitionModel;

import java.util.Map;

public interface TagsCommunicator
{
	RecognitionModel recognizeTags(String description, Map<String, String> configAttributes);
}
