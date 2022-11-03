package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public class IBMVisionRawDataTransformer implements RawDataTransformer
{
	@Override
	public RecognitionModel transform(String rawData)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		HashMap<String, String> faceMap = new HashMap<>();
		recognitionModel.setRawData(rawData);
		if (rawData.startsWith("Unexpected error"))
		{
			recognitionModel.setStatusInfo("Error during IBM service work");
		}
		else
		{
			try
			{
				HashMap<String, Object> result = new ObjectMapper().readValue(rawData, HashMap.class);
				Map<String, Object> label = (Map<String, Object>) result.get("label");
				if (label != null)
				{
					List<Map<String, Object>> classifiers = (List<Map<String, Object>>) label.get("classifiers");
					if (classifiers != null)
					{
						for (int i = 0; i < classifiers.size(); i++)
						{
							Map<String, Object> classifier = classifiers.get(i);
							proccessClasses(classifier, recognitionModel);
						}
					}
				}
				List<Map<String, Object>> face = (List<Map<String, Object>>) result.get("face");
				if (face != null)
				{
					recognitionModel.setFaceRaw(new Gson().toJson(face));
					for (int i = 0; i < face.size(); i++)
					{
						Map<String, Object> faceValue = face.get(i);
						Map<String, Object> age = (Map<String, Object>) faceValue.get("age");
						if (age != null)
						{
							faceMap.put("age", age.get("min") + "-" + age.get("max"));
						}
						Map<String, Object> gender = (Map<String, Object>) faceValue.get("gender");
						if (gender != null)
						{
							faceMap.put("gender " + gender.get("gender"),
								String.format("%.2f", 100 * Double.valueOf(gender.get("score").toString())));
						}
					}
					recognitionModel.setFaceInfo(MapHelper.sortMapByKey(faceMap));
				}
			}
			catch (IOException e)
			{
				recognitionModel.setStatusInfo("Error converting data \n" + e.getMessage());
			}
		}
		MapHelper.cleanValues(recognitionModel);
		return recognitionModel;
	}

	private void proccessClasses(Map<String, Object> classifier, RecognitionModel recognitionModel)
	{
		HashMap<String, Double> answerMap = new HashMap<>();
		HashMap<String, String> categoryMap = new HashMap<>();
		List<Map<String, Object>> classesValue = (List<Map<String, Object>>) classifier.get("classes");
		String modelName = (String) classifier.get("name");
		for (int i = 0; i < classesValue.size(); i++)
		{
			Object typeHierarchy = classesValue.get(i).get("type_hierarchy");
			Double value = 100 * Double.valueOf(classesValue.get(i).get("score").toString());
			if (typeHierarchy != null)
			{
				categoryMap.put(typeHierarchy.toString().replaceAll("/", " ").toLowerCase(), String.format("%.2f", value));
			}
			answerMap.put(String.valueOf(classesValue.get(i).get("class")), value);
		}
		if (modelName.contains("default") && !answerMap.isEmpty() && !categoryMap.isEmpty())
		{
			recognitionModel.setCategoryInfo(categoryMap);
			recognitionModel.setTagsInfo(MapHelper.sortMap(answerMap));
		}
		if (modelName.contains("explicit") && !answerMap.isEmpty())
		{
			recognitionModel.setNswf(MapHelper.sortMap(answerMap));
		}
		if (modelName.contains("food") && !answerMap.isEmpty())
		{
			recognitionModel.setFood(MapHelper.sortMap(answerMap));
		}
	}
}
