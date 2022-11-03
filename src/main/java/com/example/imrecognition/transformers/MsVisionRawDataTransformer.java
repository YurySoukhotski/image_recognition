package com.example.imrecognition.transformers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MsVisionRawDataTransformer implements RawDataTransformer {
    private static final Logger logger = LogManager.getLogger(MsVisionRawDataTransformer.class);

    @Override
    public RecognitionModel transform(String rawData) {
        HashMap<String, Double> answerMap = null;
        HashMap<String, String> mixedMap = null;
        HashMap<String, Double> celebrityMap = new HashMap<>();
        HashMap<String, Double> landmarkMap = new HashMap<>();
        RecognitionModel recognitionModel = new RecognitionModel();
        recognitionModel.setRawData(rawData);
        if (rawData.startsWith("Unexpected error")) {
            recognitionModel.setStatusInfo("Error during MSVision service work");
        } else {
            try {
                HashMap<String, Object> resultCommon = new ObjectMapper().readValue(rawData, HashMap.class);
                String tag = (String) resultCommon.get("tag");
                if (tag != null) {
                    HashMap<String, Object> result = new ObjectMapper().readValue(tag, HashMap.class);
                    if (result != null) {
                        List<Map<String, Object>> categories = (List<Map<String, Object>>) result.get("categories");
                        if (categories != null && !categories.isEmpty()) {
                            answerMap = new HashMap<>();
                            for (int c = 0; c < categories.size(); c++) {
                                Map<String, Object> values = categories.get(c);
                                Double value = 100 * Double.valueOf(values.get("score").toString());
                                answerMap.put(String.valueOf(values.get("name")).replace("_", ""), value);
                                Map<String, Object> detail = (Map<String, Object>) values.get("detail");
                                if (detail != null) {
                                    List<Object> celebrities = (List<Object>) detail.get("celebrities");
                                    if (celebrities != null) {
                                        for (int cel = 0; cel < celebrities.size(); cel++) {
                                            Map<String, Object> celebrity = (Map<String, Object>) celebrities.get(cel);
                                            Double score = 100 * Double.valueOf(celebrity.get("confidence").toString());
                                            celebrityMap.put(String.valueOf(celebrity.get("name")), score);
                                        }
                                    }
                                    List<Object> landmarks = (List<Object>) detail.get("landmarks");
                                    if (landmarks != null) {
                                        for (int lan = 0; lan < landmarks.size(); lan++) {
                                            Map<String, Object> landmark = (Map<String, Object>) landmarks.get(lan);
                                            Double score = 100 * Double.valueOf(landmark.get("confidence").toString());
                                            landmarkMap.put(String.valueOf(landmark.get("name")), score);
                                        }
                                    }
                                }
                            }
                            if (!celebrityMap.isEmpty()) {
                                recognitionModel.setCelebrityInfo(MapHelper.sortMap(celebrityMap));
                            }
                            if (!landmarkMap.isEmpty()) {
                                recognitionModel.setLandmarks(MapHelper.sortMap(landmarkMap));
                            }
                            recognitionModel.setCategoryInfo(MapHelper.sortMap(answerMap));
                        }
                        List<Map<String, Object>> tags = (List<Map<String, Object>>) result.get("tags");
                        if (tags != null && !tags.isEmpty()) {
                            answerMap = new HashMap<>();
                            for (int i = 0; i < tags.size(); i++) {
                                Map<String, Object> mapValue = tags.get(i);
                                answerMap.put(mapValue.get("name").toString(),
                                        100 * Double.valueOf(mapValue.get("confidence").toString()));
                            }
                            recognitionModel.setTagsInfo(MapHelper.sortMap(answerMap));
                        }
                        Map<String, Object> adult = (Map<String, Object>) result.get("adult");
                        if (adult != null) {
                            mixedMap = new HashMap<>();
                            mixedMap.put("is adult content", adult.get("isAdultContent").toString());
                            mixedMap.put("adult score",
                                    String.format("%.2f", 100 * Double.valueOf(adult.get("adultScore").toString())));
                            mixedMap.put("is racy content", adult.get("isRacyContent").toString());
                            mixedMap.put("racy score",
                                    String.format("%.2f", 100 * Double.valueOf(adult.get("racyScore").toString())));
                            recognitionModel.setNswf(mixedMap);
                        }
                        Map<String, Object> imageType = (Map<String, Object>) result.get("imageType");
                        if (imageType != null) {
                            mixedMap = new HashMap<>();
                            mixedMap.put("clip art type", imageType.get("clipArtType").toString());
                            mixedMap.put("line drawing type", imageType.get("lineDrawingType").toString());
                            recognitionModel.setImageType(mixedMap);
                        }
                        Map<String, Object> description = (Map<String, Object>) result.get("description");
                        if (description != null) {
                            List<Object> captions = (List<Object>) description.get("captions");
                            if (captions != null && !captions.isEmpty()) {
                                answerMap = new HashMap<>();
                                for (int i = 0; i < captions.size(); i++) {
                                    Map<String, Object> mapValue = (Map<String, Object>) captions.get(i);
                                    answerMap.put(mapValue.get("text").toString(),
                                            100 * Double.valueOf(mapValue.get("confidence").toString()));
                                }
                                recognitionModel.setCaptionInfo(MapHelper.sortMap(answerMap));
                            }
                        }
                        List<Map<String, Object>> faces = (List<Map<String, Object>>) result.get("faces");
                        if (faces != null) {
                            mixedMap = new HashMap<>();
                            for (int f = 0; f < faces.size(); f++) {
                                Map<String, Object> face = faces.get(f);
                                mixedMap.put("age", face.get("age").toString());
                                mixedMap.put("gender", face.get("gender").toString());
                            }
                            recognitionModel.setFaceInfo(MapHelper.sortMapByKey(mixedMap));
                        }
                        Map<String, Object> colors = (Map<String, Object>) result.get("color");
                        if (colors != null) {
                            mixedMap = new HashMap<>();
                            mixedMap.put("dominantColorForeground", colors.get("dominantColorForeground").toString());
                            mixedMap.put("dominantColorBackground", colors.get("dominantColorBackground").toString());
                            mixedMap.put("accentColor", colors.get("accentColor").toString());
                            mixedMap.put("isBwImg", colors.get("isBwImg").toString());
                            recognitionModel.setColorInfo(MapHelper.sortMapByKey(mixedMap));
                            recognitionModel.setIsBlackAndWhite(colors.get("isBwImg").toString().toLowerCase());
                        }
                    }
                }
                String ocr = (String) resultCommon.get("ocr");
                if (ocr != null) {
                    HashMap<String, Object> resultOcr = new ObjectMapper().readValue(ocr, HashMap.class);
                    HashMap<String, String> ocrInfo = new HashMap<>();
                    if (resultOcr.get("status") != null && resultOcr.get("status").equals("succeeded")) {
                        Map<String, Object> analyzeResult = (Map<String, Object>) resultOcr.get("analyzeResult");
                        if (analyzeResult != null) {
                            List<Map<String, Object>> readResults = (List<Map<String, Object>>) analyzeResult.get("readResults");
                            if (readResults != null && readResults.size() > 0) {
                                Map<String, Object> resultLines = readResults.get(0);
                                List<Object> lines = (List<Object>) resultLines.get("lines");
                                for (int l = 0; l < lines.size(); l++) {
                                    Map<String, Object> line = (Map<String, Object>) lines.get(l);
                                    ocrInfo.put(line.get("text").toString(), "99");
                                }
                            }
                        }
                    } else {
                        ocrInfo.put("OCR result is running", "100");
                    }
                    recognitionModel.setOcrInfo(MapHelper.sortMapByKey(ocrInfo));
                }
                String faceInfo = (String) resultCommon.get("face");
                if (faceInfo != null) {
                    recognitionModel.setFaceRaw(faceInfo);
                    HashMap<String, Double> additionalFaceInfo = new HashMap<>();
                    List<Object> additionalMap = new ObjectMapper().readValue(faceInfo, ArrayList.class);
                    if (additionalMap != null) {
                        for (int i = 0; i < additionalMap.size(); i++) {
                            HashMap<String, Object> additionalFace = (HashMap<String, Object>) additionalMap.get(i);
                            if (additionalFace != null) {
                                HashMap<String, Object> faceAttributes =
                                        (HashMap<String, Object>) additionalFace.get("faceAttributes");
                                if (faceAttributes != null) {
                                    HashMap<String, Object> emotion = (HashMap<String, Object>) faceAttributes.get("emotion");
                                    if (emotion != null) {
                                        for (Map.Entry<String, Object> entry : emotion.entrySet()) {
                                            Double valueScore = (Double) entry.getValue();
                                            if (valueScore > 0) {
                                                additionalFaceInfo.put(entry.getKey(), valueScore * 100);
                                            }
                                        }
                                    }
                                    HashMap<String, Object> facialHair =
                                            (HashMap<String, Object>) faceAttributes.get("facialHair");
                                    if (facialHair != null) {
                                        for (Map.Entry<String, Object> entry : facialHair.entrySet()) {
                                            Double valueScore = (Double) entry.getValue();
                                            if (valueScore > 0) {
                                                additionalFaceInfo.put(entry.getKey(), valueScore * 100);
                                            }
                                        }
                                    }
                                    HashMap<String, Object> makeUp = (HashMap<String, Object>) faceAttributes.get("makeup");
                                    if (makeUp != null) {
                                        for (Map.Entry<String, Object> entry : makeUp.entrySet()) {
                                            additionalFaceInfo.put(entry.getKey() + " " + entry.getValue(), 100d);
                                        }
                                    }
                                    Object smile = faceAttributes.get("smile");
                                    if (smile != null) {
                                        additionalFaceInfo.put("smile", Double.valueOf(smile.toString()) * 100);
                                    }
                                    Object glasses = faceAttributes.get("glasses");
                                    if (glasses != null) {
                                        additionalFaceInfo.put("glasses " + glasses, 100d);
                                    }
                                }
                            }
                        }
                    }
                    recognitionModel.setFaceInfo(MapHelper.sortMap(additionalFaceInfo));
                }
            } catch (IOException e) {
                recognitionModel.setStatusInfo("Error converting data \n" + e.getMessage());
                logger.error("Error while get answer", e);
            }
        }
        MapHelper.cleanValues(recognitionModel);
        return recognitionModel;
    }
}
