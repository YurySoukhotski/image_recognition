package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public class AWSRawDataTransformer implements RawDataTransformer
{
	@Override
	public RecognitionModel transform(String rawData)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		recognitionModel.setRawData(rawData);
		HashMap<String, Double> answerMap;
		HashMap<String, String> answerStringMap;
		if (rawData.startsWith("Unexpected error"))
		{
		    recognitionModel.setStatusInfo("Error during Amazon service work");
		}
		else
		{
			try
			{
				HashMap<String, Object> result = new ObjectMapper().readValue(rawData, HashMap.class);
				if (result != null)
				{
					List<Map<String, Object>> detectLabelsResultLabels = (List<Map<String, Object>>) result.get("label");
					if (detectLabelsResultLabels != null)
					{
						answerMap = new HashMap<>();
						for (int i = 0; i < detectLabelsResultLabels.size(); i++)
						{
							Map<String, Object> label = detectLabelsResultLabels.get(i);
							answerMap.put(label.get("name").toString(), Double.valueOf(label.get("confidence").toString()));
						}
						recognitionModel.setTagsInfo(MapHelper.sortMap(answerMap));
					}
					List<Map<String, Object>> textDetections = (List<Map<String, Object>>) result.get("text");
					if (textDetections != null)
					{
						answerMap = new HashMap<>();
						for (int i = 0; i < textDetections.size(); i++)
						{
							Map<String, Object> text = textDetections.get(i);
							answerMap.put(text.get("detectedText").toString(), Double.valueOf(text.get("confidence").toString()));
						}
						recognitionModel.setOcrInfo(MapHelper.sortMap(answerMap));
					}
					List<Map<String, Object>> faceDetails = (List<Map<String, Object>>) result.get("face");
					if (faceDetails != null)
					{
						recognitionModel.setFaceRaw(new Gson().toJson(faceDetails));
						answerStringMap = new HashMap<>();
						for (int i = 0; i < faceDetails.size(); i++)
						{
							Map<String, Object> faceDetail = faceDetails.get(i);
							Map<String, Object> gender = (Map<String, Object>) faceDetail.get("gender");
							if (gender != null)
							{
								answerStringMap.put("gender " + gender.get("value"),
									String.format("%.2f", Double.valueOf(gender.get("confidence").toString())));
							}
							Map<String, Object> ageRange = (Map<String, Object>) faceDetail.get("ageRange");
							if (ageRange != null)
							{
								answerStringMap.put("age range ", ageRange.get("low") + "-" + ageRange.get("high"));
							}
							List<Map<String, Object>> emotions = (List<Map<String, Object>>) faceDetail.get("emotions");
							if (emotions != null)
							{
								for (int emo = 0; emo < emotions.size(); emo++)
								{
									Map<String, Object> emotion = emotions.get(emo);
									answerStringMap.put(emotion.get("type").toString(),
										String.format("%.2f", Double.valueOf(emotion.get("confidence").toString())));
								}
							}
							Map<String, Object> mustache = (Map<String, Object>) faceDetail.get("mustache");
							if (mustache != null)
							{
								answerStringMap.put("mustache " + mustache.get("value"),
									String.format("%.2f", Double.valueOf(mustache.get("confidence").toString())));
							}
							Map<String, Object> beard = (Map<String, Object>) faceDetail.get("beard");
							if (beard != null)
							{
								answerStringMap.put("beard " + beard.get("value"),
									String.format("%.2f", Double.valueOf(beard.get("confidence").toString())));
							}
							Map<String, Object> smile = (Map<String, Object>) faceDetail.get("smile");
							if (smile != null)
							{
								answerStringMap.put("smile " + smile.get("value"),
									String.format("%.2f", Double.valueOf(smile.get("confidence").toString())));
							}
							Map<String, Object> eyesOpen = (Map<String, Object>) faceDetail.get("eyesOpen");
							if (eyesOpen != null)
							{
								answerStringMap.put("eyesOpen " + eyesOpen.get("value"),
									String.format("%.2f", Double.valueOf(eyesOpen.get("confidence").toString())));
							}
							Map<String, Object> eyeglasses = (Map<String, Object>) faceDetail.get("eyeglasses");
							if (eyeglasses != null)
							{
								answerStringMap.put("eyeglasses " + eyeglasses.get("value"),
									String.format("%.2f", Double.valueOf(eyeglasses.get("confidence").toString())));
							}
							Map<String, Object> sunglasses = (Map<String, Object>) faceDetail.get("sunglasses");
							if (sunglasses != null)
							{
								answerStringMap.put("sunglasses " + sunglasses.get("value"),
									String.format("%.2f", Double.valueOf(sunglasses.get("confidence").toString())));
							}
						}
						recognitionModel.setFaceInfo(MapHelper.sortMapByKey(answerStringMap));
					}
					List<Map<String, Object>> celebs = (List<Map<String, Object>>) result.get("celebrity");
					if (celebs != null)
					{
						answerStringMap = new HashMap<>();
						for (int i = 0; i < celebs.size(); i++)
						{
							Map<String, Object> celebrity = celebs.get(i);
							answerStringMap.put(celebrity.get("name").toString(), celebrity.get("matchConfidence").toString());
						}
						recognitionModel.setCelebrityInfo(answerStringMap);
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
