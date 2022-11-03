package com.example.imrecognition;

import com.example.imrecognition.transformers.RecognitionModel;

import java.util.List;
import java.util.Map;

public interface ImageRecognitionManagement
{
	String CLARIFAI_PROVIDER = "CLARIFAI";
	String IMAGGA_PROVIDER = "IMAGGA";
	String MSVISION_PROVIDER = "MSVISION";
	String IBMVISION_PROVIDER = "IBMVISION";
	String GOOGLE_PROVIDER = "GOOGLEVISION";
	String EYEEM_PROVIDER = "EYEEM";
	String CLOUDSIGHT_PROVIDER = "CLOUDSIGHT";
	String AWS_PROVIDER = "AWS";
	String DOTSIMAGE_PROVIDER = "DOTSIMAGE";
	String TENSOR_PROVIDER ="TENSOR";

	/**
	 * Does recognition and returns generalized JSON results for each used provider.
	 * @param imageData
	 *            image data
	 * @param preferredProviders
	 *            filter for preferred providers to use. If null or empty, all available will be used
	 * @param imageUrl
	 *            based image URL for fixing Clarifai service
	 * @return Map with key - image provider identifier, generalized JSON result
	 */
	Map<String, RecognitionModel> recognizeImage(byte[] imageData, List<String> preferredProviders,
		Map<String, String> parameters, String imageUrl);
}
