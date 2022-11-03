package com.example.imrecognition.transformers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapHelper {
    public static Map<String, String> sortMap(Map<String, Double> answeMap) {
        LinkedHashMap<String, String> reverseSortedMap = new LinkedHashMap<>();
        answeMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey().toLowerCase(), String.format("%.2f", x.getValue()).toLowerCase()));
        return reverseSortedMap;
    }


    public static List<String> mapToList(Map<String, String> answeMap) {
        List<String> list = new ArrayList<>();
        for (String key: answeMap.keySet()){
            list.add(key+":"+answeMap.get(key));
        }
        return list;
    }


    public static Map<String, String> sortMapByKey(Map<String, String> answeMap) {
        LinkedHashMap<String, String> reverseSortedMap = new LinkedHashMap<>();
        answeMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey().toLowerCase(), x.getValue().toLowerCase()));
        return reverseSortedMap;
    }


    /**
     * For google values
     *
     * @return
     */
    public static String doPrettyValue(Object value) {
        if (value != null) {
            return value.toString().replace("_", " ").toLowerCase();
        }
        return "";
    }


    public static void cleanValues(RecognitionModel recognitionModel) {
        if (recognitionModel.getFaceInfo() != null && recognitionModel.getFaceInfo().isEmpty()) {
            recognitionModel.setFaceInfo(null);
        }
        if (recognitionModel.getCelebrityInfo() != null && recognitionModel.getCelebrityInfo().isEmpty()) {
            recognitionModel.setCelebrityInfo(null);
        }
        if (recognitionModel.getOcrInfo() != null && recognitionModel.getOcrInfo().isEmpty()) {
            recognitionModel.setOcrInfo(null);
        }
        if (recognitionModel.getLogo() != null && recognitionModel.getLogo().isEmpty()) {
            recognitionModel.setLogo(null);
        }
        if (recognitionModel.getColorInfo() != null && recognitionModel.getColorInfo().isEmpty()) {
            recognitionModel.setColorInfo(null);
        }
        if (recognitionModel.getPortraitQuality() != null && recognitionModel.getPortraitQuality().isEmpty()) {
            recognitionModel.setPortraitQuality(null);
        }
        if (recognitionModel.getImageType() != null && recognitionModel.getImageType().isEmpty()) {
            recognitionModel.setImageType(null);
        }
        if (recognitionModel.getLandscape() != null && recognitionModel.getLandscape().isEmpty()) {
            recognitionModel.setLandscape(null);
        }
        if (recognitionModel.getNswf() != null && recognitionModel.getNswf().isEmpty()) {
            recognitionModel.setNswf(null);
        }
        if (recognitionModel.getModeration() != null && recognitionModel.getModeration().isEmpty()) {
            recognitionModel.setModeration(null);
        }

    }
}
