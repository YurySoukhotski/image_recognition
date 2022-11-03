package com.example.imrecognition.transformers;

import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TensorRawDataTransformer implements RawDataTransformer
{
	@Override
	public RecognitionModel transform(String rawData)
	{
		RecognitionModel recognitionModel = new RecognitionModel();
		recognitionModel.setRawData(rawData);
		if (rawData.startsWith("Unexpected error"))
		{
			recognitionModel.setStatusInfo("Error during Tensor service work");
		}
		else
		{
			try
			{
				HashMap<String, Object> result = new ObjectMapper().readValue(rawData, HashMap.class);
				String labels = String.valueOf(result.get("labels"));
				if (!labels.equalsIgnoreCase("null"))
				{
					recognitionModel.setIntent(labels);
				}
			}
			catch (Exception e)
			{
				recognitionModel.setStatusInfo("Error converting data \n" + e.getMessage() + rawData);
			}
		}
		MapHelper.cleanValues(recognitionModel);
		return recognitionModel;
	}
}
