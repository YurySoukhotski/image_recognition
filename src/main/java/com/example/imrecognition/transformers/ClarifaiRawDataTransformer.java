package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ClarifaiRawDataTransformer implements RawDataTransformer
{
	@Override
	public RecognitionModel transform(String rawData)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		RecognitionModel recognitionModelDe = new RecognitionModel();
		recognitionModel.setRawData(rawData);
		if (rawData.startsWith("Unexpected error"))
		{
			recognitionModel.setStatusInfo("Error during Clarifai service work");
		}
		else
		{
			try
			{
				HashMap<String, Object> result = new ObjectMapper().readValue(rawData, HashMap.class);
				String enRaw = String.valueOf(result.get("en"));
				if (!enRaw.equalsIgnoreCase("No generated text "))
				{
					processRawNew(recognitionModel, enRaw);
				}
				String deRaw = String.valueOf(result.get("de"));
				if (!deRaw.equalsIgnoreCase("No generated text "))
				{
					processRawNew(recognitionModelDe, deRaw);
				}
				String additionalRaw = String.valueOf(result.get("additional"));
				if (!additionalRaw.equalsIgnoreCase("No generated text "))
				{
					processRawNew(recognitionModel, additionalRaw);
				}
				recognitionModel.setTagsInfoDe(recognitionModelDe.getTagsInfoDe());
			}
			catch (Exception e)
			{
				recognitionModel.setStatusInfo("Error converting data \n" + e.getMessage() + rawData);
			}
		}
		MapHelper.cleanValues(recognitionModel);
		return recognitionModel;
	}

	/**
	 * Process output data
	 * @param dataMap
	 * @return
	 */
	private Map<String, String> processData(Map<String, Object> dataMap)
	{
		HashMap<String, Double> answerMap = new HashMap<>();
		if (dataMap != null)
		{
			List<Object> regions = (List<Object>) dataMap.get("regions");
			if (regions != null)
			{
				HashMap<String, Object> dataRegion = (HashMap<String, Object>) regions.get(0);
				if (dataRegion != null)
				{
					HashMap<String, Object> dataObject = (HashMap<String, Object>) dataRegion.get("data");
					if (dataObject != null)
					{
						List<HashMap<String, Object>> concepts = (List<HashMap<String, Object>>) dataObject.get("concepts");
						if (concepts != null)
						{
							for (int i = 0; i < concepts.size(); i++)
							{
								HashMap<String, Object> mapWithValues = concepts.get(i);
								Double value = Double.valueOf(mapWithValues.get("value").toString());
								answerMap.put(String.valueOf(mapWithValues.get("name")).toLowerCase(), value * 100);
							}
						}
						HashMap<String, Object> face = (HashMap<String, Object>) dataObject.get("face");
						if (face != null)
						{
							Map<String, Map<String, String>> answerCommonMap = new HashMap<>();
							processFace(face, answerCommonMap);
							if (answerCommonMap.get("face") != null)
							{
								return answerCommonMap.get("face");
							}
							if (answerCommonMap.get("celebrities") != null)
							{
								return answerCommonMap.get("celebrities");
							}
						}
					}
					else
					{
						List<Object> faceRegion = (List<Object>) dataMap.get("regions");
						if (faceRegion != null)
						{
							HashMap<String, Object> stringObjectHashMap = (HashMap<String, Object>) faceRegion.get(0);
							HashMap<String, Object> regionInfo = (HashMap<String, Object>) stringObjectHashMap.get("region_info");
							if (regionInfo != null)
							{
								Object boundingBox = regionInfo.get("bounding_box");
								if (boundingBox != null)
								{
									HashMap<String, String> faceRegionMap = new HashMap<>();
									faceRegionMap.put("faceRegionInfo", boundingBox.toString());
									return faceRegionMap;
								}
							}
						}
					}
				}
			}
		}
		List<Object> concepts = (List<Object>) dataMap.get("concepts");
		if (concepts != null)
		{
			for (int i = 0; i < concepts.size(); i++)
			{
				HashMap<String, Object> mapWithValues = (HashMap<String, Object>) concepts.get(i);
				Double value = Double.valueOf(mapWithValues.get("value").toString());
				answerMap.put(String.valueOf(mapWithValues.get("name")).toLowerCase(), value * 100);
			}
		}
		List<HashMap<String, Object>> colors = (List<HashMap<String, Object>>) dataMap.get("colors");
		if (colors != null)
		{
			for (int i = 0; i < colors.size(); i++)
			{
				HashMap<String, Object> mapWithValues = colors.get(i);
				Double value = Double.valueOf(mapWithValues.get("value").toString());
				answerMap.put(String.valueOf(mapWithValues.get("raw_hex")).toLowerCase(), value * 100);
			}
		}
		return MapHelper.sortMap(answerMap);
	}

	private void processFace(HashMap<String, Object> face, Map<String, Map<String, String>> answerCommonMap)
	{
		Map<String, String> answerStringMap = new HashMap<>();
		if (face != null)
		{
			Map<String, Object> celebrities = (Map<String, Object>) face.get("identity");
			if (celebrities != null)
			{
				Map<String, Double> answerMapCelebrity = new HashMap<>();
				List<Map<String, Object>> listCelebrities = (List<Map<String, Object>>) celebrities.get("concepts");
				if (listCelebrities != null)
				{
					for (int c = 0; c < listCelebrities.size(); c++)
					{
						Map<String, Object> celebrity = listCelebrities.get(c);
						Double value = Double.valueOf(celebrity.get("value").toString());
						answerMapCelebrity.put(String.valueOf(celebrity.get("name")).toLowerCase(), value * 100);
					}
				}
				answerCommonMap.put("celebrities", MapHelper.sortMap(answerMapCelebrity));
			}
			Map<String, Object> ageAppearance = (Map<String, Object>) face.get("age_appearance");
			if (ageAppearance != null)
			{
				List<Map<String, String>> conceptAges = (List<Map<String, String>>) ageAppearance.get("concepts");
				if (conceptAges != null)
				{
					Map<String, String> value = conceptAges.get(0);
					answerStringMap.put("age appearance", value.get("name"));
				}
			}
			Map<String, Object> genderAppearance = (Map<String, Object>) face.get("gender_appearance");
			if (genderAppearance != null)
			{
				List<Map<String, String>> conceptGenres = (List<Map<String, String>>) genderAppearance.get("concepts");
				if (conceptGenres != null)
				{
					Map<String, String> value = conceptGenres.get(0);
					answerStringMap.put("gender appearance", value.get("name"));
				}
			}
			Map<String, Object> multiculturalAppearance = (Map<String, Object>) face.get("multicultural_appearance");
			if (multiculturalAppearance != null)
			{
				List<Map<String, String>> multiculturals = (List<Map<String, String>>) multiculturalAppearance.get("concepts");
				if (multiculturals != null)
				{
					Map<String, String> value = multiculturals.get(0);
					answerStringMap.put("multicultural appearance", value.get("name"));
				}
			}
			if (!answerStringMap.isEmpty())
			{
				answerCommonMap.put("face", answerStringMap);
			}
		}
	}

	private void processRawNew(RecognitionModel recognitionModel, String rawData) throws IOException
	{
		HashMap<String, Object> result = new ObjectMapper().readValue(rawData, HashMap.class);
		if (result != null)
		{
			result.remove("input");
			List<HashMap<String, Object>> outputs = (List<HashMap<String, Object>>) result.get("outputs");
			if (outputs != null)
			{
				for (int j = 0; j < outputs.size(); j++)
				{
					HashMap<String, Object> dataMap = outputs.get(j);
					if (dataMap != null)
					{
						String modelName = "";
						HashMap<String, Object> modelObj = (HashMap<String, Object>) dataMap.get("model");
						if (modelObj != null)
						{
							if (modelObj.get("name") != null)
							{
								modelName = (String) modelObj.get("name");
							}
						}
						HashMap<String, Object> data = (HashMap<String, Object>) dataMap.get("data");
						if (data != null)
						{
							if (modelName.toLowerCase().contains("logo"))
							{
								recognitionModel.setLogo(processData(data));
							}
							if (modelName.toLowerCase().contains("portrait"))
							{
								recognitionModel.setPortraitQuality(processData(data));
							}
							if (modelName.toLowerCase().contains("landscape"))
							{
								recognitionModel.setLandscape(processData(data));
							}
							if (modelName.toLowerCase().contains("innoday"))
							{
								recognitionModel.setTagsInfoDe(processData(data));
							}
							if (modelName.toLowerCase().contains("nsfw"))
							{
								recognitionModel.setNswf(processData(data));
							}
							if (modelName.toLowerCase().contains("moderation"))
							{
								recognitionModel.setModeration(processData(data));
							}
							if (modelName.toLowerCase().contains("general"))
							{
								recognitionModel.setTagsInfo(processData(data));
							}
							if (modelName.toLowerCase().contains("demographics"))
							{
								recognitionModel.setFaceInfo(processData(data));
							}
							if (modelName.toLowerCase().contains("color"))
							{
								recognitionModel.setColorInfo(processData(data));
							}
							if (modelName.toLowerCase().contains("celeb"))
							{
								recognitionModel.setCelebrityInfo(processData(data));
							}
							if (modelName.toLowerCase().contains("face"))
							{
								recognitionModel.setFaceRaw(processData(data).get("faceRegionInfo"));
							}
							if (modelName.toLowerCase().contains("bike"))
							{
								recognitionModel.setBikes(processData(data));
							}
						}
					}
				}
			}
		}
	}
}
