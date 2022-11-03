package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public class GoogleVisionRawDataTransformer implements RawDataTransformer
{
	private static final String SCORE = "score";

	@Override
	public RecognitionModel transform(String rawData)
	{
		HashMap<String, Double> answerMap = new HashMap<>();
		HashMap<String, String> mixedMap = new HashMap<>();
		RecognitionModel recognitionModel = new RecognitionModel();
		recognitionModel.setRawData(rawData);
		if (rawData.startsWith("Unexpected error"))
		{
			recognitionModel.setStatusInfo("Error during Google service work");
		}
		else
		{
			try
			{
				HashMap<String, List<Map<String, List<HashMap<String, Object>>>>> result =
					new ObjectMapper().readValue(rawData, HashMap.class);
				if (!result.isEmpty())
				{
					List<Map<String, List<HashMap<String, Object>>>> responses = result.get("responses");
					if (responses != null && !responses.isEmpty())
					{
						Map<String, List<HashMap<String, Object>>> labels = responses.get(0);
						if (!labels.isEmpty())
						{
							List<HashMap<String, Object>> logoAnnotations = labels.get("logoAnnotations");
							if (logoAnnotations != null)
							{
								for (int i = 0; i < logoAnnotations.size(); i++)
								{
									Map<String, Object> values = logoAnnotations.get(i);
									Double value = 100 * Double.valueOf(values.get(SCORE).toString());
									answerMap.put(String.valueOf(logoAnnotations.get(i).get("description")), value);
								}
								recognitionModel.setLogo(MapHelper.sortMap(answerMap));
							}
							List<HashMap<String, Object>> localizedObjectAnnotations = labels.get("localizedObjectAnnotations");
							if (localizedObjectAnnotations != null)
							{
								Map<String, List<String>> annotationsMap = new HashMap<>();
								for (int i = 0; i < localizedObjectAnnotations.size(); i++)
								{
									Map<String, Object> values = localizedObjectAnnotations.get(i);
									Map<String, Object> boundingPoly = (Map<String, Object>) values.get("boundingPoly");
									List<Map<String, Double>> coordinates =
										(List<Map<String, Double>>) boundingPoly.get("normalizedVertices");
									List<String> value = new ArrayList<>();
									for (int j = 0; j < coordinates.size(); j++)
									{
										Map<String, Double> point = coordinates.get(j);
										value.add(point.get("x") + ":" + point.get("y"));
									}
									Double score = 100 * Double.valueOf(values.get(SCORE).toString());
									annotationsMap.put(String.valueOf(values.get("name") + ":" + String.format("%.2f", score)),
										value);
								}
								recognitionModel.setObjectAnnotations(annotationsMap);
							}
							List<HashMap<String, Object>> labelAnnotations = labels.get("labelAnnotations");
							if (labelAnnotations != null)
							{
								for (int i = 0; i < labelAnnotations.size(); i++)
								{
									Map<String, Object> values = labelAnnotations.get(i);
									Double value = 100 * Double.valueOf(values.get(SCORE).toString());
									answerMap.put(String.valueOf(labelAnnotations.get(i).get("description")), value);
								}
								recognitionModel.setTagsInfo(MapHelper.sortMap(answerMap));
							}
							HashMap<String, HashMap<String, Object>> imageAnnotations =
								(HashMap<String, HashMap<String, Object>>) labels.get("imagePropertiesAnnotation");
							if (imageAnnotations != null)
							{
								String red;
								String green;
								String blue;
								answerMap = new HashMap<>();
								HashMap<String, Object> dominantColors = imageAnnotations.get("dominantColors");
								if (dominantColors != null)
								{
									List<Object> colors = (List<Object>) dominantColors.get("colors");
									if (colors != null && !colors.isEmpty())
									{
										for (int i = 0; i < colors.size(); i++)
										{
											Map<String, Object> color = (Map<String, Object>) colors.get(i);
											Map<String, Object> colorElement = (Map<String, Object>) color.get("color");
											if (colorElement != null)
											{
												red = "RGB(" + colorElement.get("red") + ",";
												green = colorElement.get("green") + ",";
												blue = colorElement.get("blue") + ")";
												Double score = (Double) color.get(SCORE);
												answerMap.put(red + green + blue, score * 100);
											}
										}
									}
								}
								recognitionModel.setColorInfo(MapHelper.sortMap(answerMap));
							}
							List<HashMap<String, Object>> faceAnnotations = labels.get("faceAnnotations");
							if (faceAnnotations != null)
							{
								recognitionModel.setFaceRaw(new Gson().toJson(faceAnnotations));
								for (int i = 0; i < faceAnnotations.size(); i++)
								{
									HashMap<String, Object> faceAnnotation = faceAnnotations.get(i);
									mixedMap.put("joy", MapHelper.doPrettyValue(faceAnnotation.get("joyLikelihood")));
									mixedMap.put("sorrow", MapHelper.doPrettyValue(faceAnnotation.get("sorrowLikelihood")));
									mixedMap.put("anger", MapHelper.doPrettyValue(faceAnnotation.get("angerLikelihood")));
									mixedMap.put("surprise", MapHelper.doPrettyValue(faceAnnotation.get("surpriseLikelihood")));
									mixedMap.put("underExposed",
										MapHelper.doPrettyValue(faceAnnotation.get("underExposedLikelihood")));
									mixedMap.put("blurred", MapHelper.doPrettyValue(faceAnnotation.get("blurredLikelihood")));
									mixedMap.put("headwear", MapHelper.doPrettyValue(faceAnnotation.get("headwearLikelihood")));
								}
								recognitionModel.setFaceInfo(MapHelper.sortMapByKey(mixedMap));
							}
							Object webDetection = labels.get("webDetection");
							if (webDetection != null)
							{
								StringBuilder entitiesBuilder = new StringBuilder();
								Map<String, Object> webEntities = (Map<String, Object>) webDetection;
								List<Object> webEntitiesList = (List<Object>) webEntities.get("webEntities");
								if (webEntitiesList != null)
								{
									entitiesBuilder.append("webEntities\n");
									for (int i = 0; i < webEntitiesList.size(); i++)
									{
										Map<String, Object> element = (Map<String, Object>) webEntitiesList.get(i);
										entitiesBuilder.append(element.get("description")).append(":")
											.append(String.format("%.2f", 100 * (Double) element.get(SCORE))).append("\n");
									}
								}
								List<Object> fullMatchingImages = (List<Object>) webEntities.get("fullMatchingImages");
								if (fullMatchingImages != null)
								{
									entitiesBuilder.append("\nfullMatchingImages\n");
									for (int i = 0; i < fullMatchingImages.size(); i++)
									{
										Map<String, String> element = (Map<String, String>) fullMatchingImages.get(i);
										entitiesBuilder.append(element.get("url")).append("\n");
									}
								}
								List<Object> partialMatchingImages = (List<Object>) webEntities.get("partialMatchingImages");
								if (partialMatchingImages != null)
								{
									entitiesBuilder.append("\npartialMatchingImages\n");
									for (int i = 0; i < partialMatchingImages.size(); i++)
									{
										Map<String, String> element = (Map<String, String>) partialMatchingImages.get(i);
										entitiesBuilder.append(element.get("url")).append("\n");
									}
								}
								List<Object> visuallySimilarImages = (List<Object>) webEntities.get("visuallySimilarImages");
								if (visuallySimilarImages != null)
								{
									entitiesBuilder.append("\nvisuallySimilarImages\n");
									for (int i = 0; i < visuallySimilarImages.size(); i++)
									{
										Map<String, String> element = (Map<String, String>) visuallySimilarImages.get(i);
										entitiesBuilder.append(element.get("url")).append("\n");
									}
								}
								List<Object> bestGuessLabels = (List<Object>) webEntities.get("bestGuessLabels");
								if (bestGuessLabels != null)
								{
									entitiesBuilder.append("\nbestGuessLabels\n");
									for (int i = 0; i < bestGuessLabels.size(); i++)
									{
										Map<String, String> element = (Map<String, String>) bestGuessLabels.get(i);
										entitiesBuilder.append(element.get("label")).append(" : ")
											.append(element.get("languageCode")).append("\n");
									}
								}
								List<Object> pagesWithMatchingImages = (List<Object>) webEntities.get("pagesWithMatchingImages");
								if (pagesWithMatchingImages != null)
								{
									entitiesBuilder.append("\npagesWithMatchingImages\n");
									for (int i = 0; i < pagesWithMatchingImages.size(); i++)
									{
										Map<String, String> element = (Map<String, String>) pagesWithMatchingImages.get(i);
										entitiesBuilder.append(element.get("url")).append("\n").append(element.get("pageTitle"))
											.append("\n");
									}
								}
								recognitionModel.setEntities(entitiesBuilder.toString());
							}
							HashMap<String, Object> nsfw = (HashMap<String, Object>) labels.get("safeSearchAnnotation");
							if (nsfw != null)
							{
								mixedMap = new HashMap<>();
								mixedMap.put("adult", MapHelper.doPrettyValue(nsfw.get("adult")));
								mixedMap.put("spoof", MapHelper.doPrettyValue(nsfw.get("spoof")));
								mixedMap.put("medical", MapHelper.doPrettyValue(nsfw.get("medical")));
								mixedMap.put("violence", MapHelper.doPrettyValue(nsfw.get("violence")));
								mixedMap.put("racy", MapHelper.doPrettyValue(nsfw.get("racy")));
								recognitionModel.setNswf(mixedMap);
							}
							List<HashMap<String, Object>> landmarkAnnotations = labels.get("landmarkAnnotations");
							if (landmarkAnnotations != null)
							{
								answerMap = new HashMap<>();
								for (int i = 0; i < landmarkAnnotations.size(); i++)
								{
									HashMap<String, Object> landmarkAnnotation = landmarkAnnotations.get(i);
									Object infoKey = landmarkAnnotation.get("description");
									if (infoKey == null)
										infoKey = "No description";
									answerMap.put(infoKey.toString(),
										100 * Double.valueOf(landmarkAnnotation.get(SCORE).toString()));
								}
								recognitionModel.setLandmarks(MapHelper.sortMap(answerMap));
							}
							Map<String, String> textMap = new HashMap<>();
							StringBuilder textOcr = new StringBuilder();
							HashMap<String, Object> fullTextAnnotation =
								(HashMap<String, Object>) labels.get("fullTextAnnotation");
							if (fullTextAnnotation != null)
							{
								if (fullTextAnnotation.get("text") != null)
								{
									textOcr.append(fullTextAnnotation.get("text").toString()).append(" ");
								}
							}
							textMap.put("text", textOcr.toString());
							recognitionModel.setOcrInfo(textMap);
						}
					}
				}
			}
			catch (IOException e)
			{
				recognitionModel.setStatusInfo("Error converting data \n" + e.getMessage());
			}
			MapHelper.cleanValues(recognitionModel);
		}
		return recognitionModel;
	}
}
