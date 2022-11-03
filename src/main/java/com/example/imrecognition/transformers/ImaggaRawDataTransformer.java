package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.example.imrecognition.transformers.MapHelper.sortMap;

public class ImaggaRawDataTransformer implements RawDataTransformer
{
	@Override
	public RecognitionModel transform(String rawData)
	{
		HashMap<String, Double> answerMap = new HashMap<>();
		HashMap<String, String> colorMapValues = new HashMap<>();
		RecognitionModel recognitionModel = new RecognitionModel();
		recognitionModel.setRawData(rawData);
		if (rawData.startsWith("Unexpected error"))
		{
			recognitionModel.setStatusInfo("Error during Imagga service work");
		}
		else
		{
			try
			{
				HashMap<String, String> resultCommon = new ObjectMapper().readValue(rawData, HashMap.class);
				String resultTags = resultCommon.get("tags");
				if (resultTags != null)
				{
					HashMap<String, Object> tagsMapValue = new ObjectMapper().readValue(resultTags, HashMap.class);
					if (tagsMapValue != null)
					{
						Map<String, Object> results = (Map<String, Object>) tagsMapValue.get("result");
						if (results != null)
						{
							List<Object> tagsList = (List<Object>) results.get("tags");
							if (tagsList != null)
							{
								for (int i = 0; i < tagsList.size(); i++)
								{
									Map<String, Object> mapValue = (Map<String, Object>) tagsList.get(i);
									Double value = Double.valueOf(mapValue.get("confidence").toString());
									Map<String, String> valueResult = (Map<String, String>) mapValue.get("tag");
									answerMap.put(valueResult.get("en"), value);
								}
							}
							recognitionModel.setTagsInfo(sortMap(answerMap));
						}
					}
				}
				String categories = resultCommon.get("categories");
				if (categories != null)
				{
					HashMap<String, Object> tagsCategories = new ObjectMapper().readValue(categories, HashMap.class);
					if (tagsCategories != null)
					{
						Map<String, Object> tagsList = (Map<String, Object>) tagsCategories.get("result");
						if (tagsList != null)
						{
							answerMap = new HashMap<>();
							List<Object> categoriesList = (List<Object>) tagsList.get("categories");
							if (categoriesList != null)
							{
								for (int i = 0; i < categoriesList.size(); i++)
								{
									Map<String, Object> mapValue = (Map<String, Object>) categoriesList.get(i);
									Double value = Double.valueOf(mapValue.get("confidence").toString());
									Map<String, String> valueResult = (Map<String, String>) mapValue.get("name");
									answerMap.put(valueResult.get("en"), value);
								}
								recognitionModel.setCategoryInfo(sortMap(answerMap));
							}
						}
					}
				}
				String nsfw = resultCommon.get("nsfw");
				if (nsfw != null)
				{
					HashMap<String, Object> nsfwValues = new ObjectMapper().readValue(nsfw, HashMap.class);
					if (nsfwValues != null)
					{
						Map<String, Object> results = (Map<String, Object>) nsfwValues.get("result");
						if (results != null)
						{
							List<Map<String, Object>> valueCategories = (List<Map<String, Object>>) results.get("categories");
							if (valueCategories != null)
							{
								answerMap = new HashMap<>();
								for (int cat = 0; cat < valueCategories.size(); cat++)
								{
									Map<String, Object> valueFromMap = valueCategories.get(cat);
									Map<String, String> resultValue = (Map<String, String>) valueFromMap.get("name");
									answerMap.put(resultValue.get("en"),
										Double.valueOf(valueFromMap.get("confidence").toString()));
								}
							}
							recognitionModel.setNswf(sortMap(answerMap));
						}
					}
				}
				String colorsInfo = resultCommon.get("colors");
				if (colorsInfo != null)
				{
					HashMap<String, Object> tagsColors = new ObjectMapper().readValue(colorsInfo, HashMap.class);
					if (tagsColors != null)
					{
						Map<String, Object> results = (Map<String, Object>) tagsColors.get("result");
						if (results != null)
						{
							Map<String, Object> colors = (Map<String, Object>) results.get("colors");
							if (colors != null)
							{
								List<Map<String,Object>> backgroundColors = (List<Map<String, Object>>) colors.get("background_colors");
								if (backgroundColors!=null) {

								for (int j = 0; j < backgroundColors.size(); j++)
								{
									Map<String, Object> backgroundColor = backgroundColors.get(j);
									colorMapValues.put("background colors " + backgroundColor.get("html_code").toString(),String.format("%.2f", Double.valueOf(backgroundColor.get("percent").toString())));
								}
								}

								List<Map<String, Object>> imageColors = (List<Map<String, Object>>) colors.get("image_colors");
								if (imageColors != null)
								{
									for (int j = 0; j < imageColors.size(); j++)
									{
										Map<String, Object> colorMap = imageColors.get(j);
										colorMapValues.put("image colors " + colorMap.get("html_code").toString(),
												String.format("%.2f", Double.valueOf(colorMap.get("percent").toString())));
									}
								}
								recognitionModel.setColorInfo(MapHelper.sortMapByKey(colorMapValues));
							}
						}
					}
				}
			}
			catch (IOException e)
			{
				recognitionModel.setStatusInfo("Error converting data \n" + e.getMessage());
			}
		}
		return recognitionModel;
	}
}
