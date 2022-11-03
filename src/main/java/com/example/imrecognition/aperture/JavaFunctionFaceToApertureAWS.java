package com.example.imrecognition.aperture;

import com.meylemueller.common.util.exception.FinderException;
import com.meylemueller.isy3.backend.api.model.datadescriptor.DataItem;
import com.meylemueller.isy3suite.plugin.javafunction.JavaFunction;
import com.meylemueller.suite.core.mandator.LogisticsMandator;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jettison.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class JavaFunctionFaceToApertureAWS implements JavaFunction{

    @Autowired
    private FaceToApertureAWS faceToApertureAWS;

    /**
     * Generate the aperture xmp values for the give DataItems
     * @param var1 LogisticMandator
     * @param var2 DataItems
     * @return Executionresult
     * @throws Exception If errors occure
     */
    public String execute(LogisticsMandator var1, List<DataItem> var2) throws Exception{
        List<String> successResult = new ArrayList<>();
        List<String> errorResult = new ArrayList<>();

        for(DataItem dataItem : var2){
            try{
                faceToApertureAWS.createFaceApertures(dataItem.getIdentity().getIdentity());
                successResult.add(dataItem.getIdentity().getDisplayName());
            } catch (FinderException | JSONException e) {
                errorResult.add(dataItem.getIdentity().getDisplayName());
            }
        }

        return generateResultString(successResult, errorResult);
    }

    private String generateResultString(List<String> success, List<String> error){
        StringBuilder result = new StringBuilder();
        if(CollectionUtils.isNotEmpty(success)){
            result.append("Bildausschnitte erfolgreich generiert für:\n");
            for(String s : success){
                result.append("\t" + s + "\n");
            }
        }

        if(CollectionUtils.isNotEmpty(error)){
            if(result.length() > 0) result.append("\n");
            result.append("Fehler beim Generieren der Bildausschnitte bei:\n");
            for(String e : error){
                result.append("\t" + e + "\n");
            }
        }

        return result.toString();
    }
}
