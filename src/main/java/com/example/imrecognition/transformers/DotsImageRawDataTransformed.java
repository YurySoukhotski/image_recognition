package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DotsImageRawDataTransformed implements RawDataTransformer
{
	@Override
	public RecognitionModel transform(String rawData)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		recognitionModel.setRawData(rawData);
		HashMap<String, Double> answerMap;
		if (rawData.startsWith("Unexpected error"))
		{
			recognitionModel.setStatusInfo("Error during Dots service work");
		}
		else
		{
			try
			{
				HashMap<String, Object> result = new ObjectMapper().readValue(rawData, HashMap.class);
				if (result != null)
				{
					String imageInfo = String.valueOf(result.get("imageInfo"));
					if (imageInfo != null)
					{
						answerMap = new HashMap<>();
						JsonElement imageInfoRaw = new JsonParser().parse(imageInfo).getAsJsonObject().get("output");
						if (imageInfoRaw!=null) {
							for (JsonElement jsonElementInner : imageInfoRaw.getAsJsonArray()) {
								answerMap.put(jsonElementInner.getAsJsonObject().get("tag").getAsString(), 100 * jsonElementInner
										.getAsJsonObject().get("score").getAsDouble());
							}
							recognitionModel.setTagsInfo(MapHelper.sortMap(answerMap));
						}
					}
					String faceInfo = String.valueOf(result.get("faceInfo"));
					if (faceInfo != null)
					{
						answerMap = new HashMap<>();
						JsonElement imageInfoRaw = new JsonParser().parse(faceInfo).getAsJsonObject().get("facial_emotion");
						if (imageInfoRaw != null)
						{
							for (JsonElement jsonElementInner : imageInfoRaw.getAsJsonArray())
							{
								answerMap.put(jsonElementInner.getAsJsonObject().get("tag").getAsString(), 100 * jsonElementInner
									.getAsJsonObject().get("score").getAsDouble());
							}
							recognitionModel.setFaceInfo(MapHelper.sortMap(answerMap));
						}
					}
				}
			}
			catch (IOException e)
			{
				recognitionModel.setStatusInfo("Error converting data \n" + e.getMessage() + rawData);
			}
		}
		return recognitionModel;
	}
}
