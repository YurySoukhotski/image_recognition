package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.example.imrecognition.transformers.MapHelper.sortMap;

public class EyeEMRawDataTransformer implements RawDataTransformer
{
	@Override
	public RecognitionModel transform(String rawData)
	{
		HashMap<String, Double> answerMap = new HashMap<>();
		RecognitionModel recognitionModel = new RecognitionModel();
		recognitionModel.setRawData(rawData);
		if (rawData.startsWith("Unexpected error"))
		{
			recognitionModel.setStatusInfo("Error during EyeEM service work");
		}
		else
		{
			try
			{
				Map<String, List<Map<String, Object>>> result = new ObjectMapper().readValue(rawData, HashMap.class);
				if (result != null)
				{
					List<Map<String, Object>> responses = result.get("responses");
					if (responses != null)
					{
						Map<String, Object> infoMaps = responses.get(0);
						List<Map<String, Object>> tags = (List<Map<String, Object>>) infoMaps.get("tags");
						if (tags != null)
						{
							for (int i = 0; i < tags.size(); i++)
							{
								Map<String, Object> values = tags.get(i);
								Double value = 100 * Double.valueOf(values.get("probability").toString());
								answerMap.put(String.valueOf(values.get("text")), value);
							}
							recognitionModel.setTagsInfo(sortMap(answerMap));
						}
						List<Map<String, Object>> captions = (List<Map<String, Object>>) infoMaps.get("captions");
						if (captions != null)
						{
							answerMap = new HashMap<>();
							for (int i = 0; i < captions.size(); i++)
							{
								Map<String, Object> values = captions.get(i);
								answerMap.put(String.valueOf(values.get("text")).toLowerCase(), 98d);
							}
							recognitionModel.setCaptionInfo(sortMap(answerMap));
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
