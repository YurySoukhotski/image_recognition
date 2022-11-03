package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CloudSightRawDataTransformed implements RawDataTransformer
{
	@Override
	public RecognitionModel transform(String rawData)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		recognitionModel.setRawData(rawData);
		HashMap<String, String> answerMap = new HashMap<>();
		if (rawData.startsWith("Unexpected error"))
		{
			recognitionModel.setStatusInfo("Error during CloudSight service work");
		}
		else
		{
			try
			{
				HashMap<String, Object> result = new ObjectMapper().readValue(rawData, HashMap.class);
				if (result != null)
				{
					String name = String.valueOf(result.get("name"));
					if (name != null)
					{
						answerMap.put(name, "99");
					}
					recognitionModel.setCaptionInfo(answerMap);
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
