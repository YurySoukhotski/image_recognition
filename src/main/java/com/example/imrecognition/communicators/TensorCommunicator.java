package com.example.imrecognition.communicators;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.imrecognition.ImageRecognitionManagement;
import com.example.imrecognition.transformers.TensorRawDataTransformer;
import com.google.gson.Gson;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import com.example.imrecognition.transformers.RawDataTransformer;

public class TensorCommunicator implements ImageRecognitionCommunicator
{
	private static final Logger logger = LogManager.getLogger(TensorCommunicator.class);
	@Autowired
	private TensorRawDataTransformer tensorRawDataTransformer;

	@Override
	public String recognize(byte[] imageData, Map<String, String> parameters, String url)
	{
		String answer = "";
		Map<String, Object> answerMap = new HashMap<>();
		String modelFile = parameters.get(ImageRecognitionManagement.TENSOR_PROVIDER + "MODEL");
		String labelFile = parameters.get(ImageRecognitionManagement.TENSOR_PROVIDER + "LABEL");
		byte[] graphDef;
		List<String> labels;
		graphDef = readAllBytesOrExit(Paths.get(modelFile));
		labels = readAllLinesOrExit(Paths.get(labelFile));
		try (Tensor image = Tensor.create(imageData))
		{
			float[] labelProbabilities = executeInceptionGraph(graphDef, image);
			int bestLabelIdx = maxIndex(labelProbabilities);
			labels.get(bestLabelIdx);
			answerMap.put("labels",String.format("%s : %.2f", labels.get(bestLabelIdx),labelProbabilities[bestLabelIdx] * 100f));
			answer = new Gson().toJson(answerMap);
		}
		catch (Exception e)
		{
			answer = "Unexpected error during work Tensor service \n" + e.getMessage();
			logger.error("Error while get answer", e);
		}
		return answer;
	}

	private static float[] executeInceptionGraph(byte[] graphDef, Tensor image)
	{
		try (Graph g = new Graph())
		{
			g.importGraphDef(graphDef);
			try (Session s = new Session(g) ;
				Tensor<Float> result =
					s.runner().feed("DecodeJpeg/contents", image).fetch("softmax").run().get(0).expect(Float.class))
			{
				final long[] rshape = result.shape();
				if (result.numDimensions() != 2 || rshape[0] != 1)
				{
					throw new RuntimeException(
						String
							.format(
								"Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
								Arrays.toString(rshape)));
				}
				int nlabels = (int) rshape[1];
				return result.copyTo(new float[1][nlabels])[0];
			}
		}
	}

	private static int maxIndex(float[] probabilities)
	{
		int best = 0;
		for (int i = 1; i < probabilities.length; ++i)
		{
			if (probabilities[i] > probabilities[best])
			{
				best = i;
			}
		}
		return best;
	}

	private static byte[] readAllBytesOrExit(Path path)
	{
		try
		{
			return Files.readAllBytes(path);
		}
		catch (IOException e)
		{
			logger.error("Failed to read [" + path + "]: " + e.getMessage());
		}
		return null;
	}

	private static List<String> readAllLinesOrExit(Path path)
	{
		try
		{
			return Files.readAllLines(path, Charset.forName("UTF-8"));
		}
		catch (IOException e)
		{
			logger.error("Failed to read [" + path + "]: " + e.getMessage());
		}
		return null;
	}

	@Override
	public RawDataTransformer getTransformer()
	{
		return tensorRawDataTransformer;
	}
}
