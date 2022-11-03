package com.example.imrecognition.aperture;

import com.meylemueller.common.util.exception.FinderException;
import com.meylemueller.obj.isy.ISYImageObjectEntity;
import com.meylemueller.obj.isy.ISYObjectEntity;
import com.meylemueller.obj.isy.ISYObjectFacade;
import com.meylemueller.obj.isy.metadata.entity.MetaDataAttributeValueEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class FaceToApertureAWS {

    private static final Logger logger = LogManager.getLogger(FaceToApertureAWS.class);

    @Autowired
    private ISYObjectFacade isyObjectFacade;

    @Autowired
    private XMPApertureValueFacade xmpApertureValueFacade;

    private static final String ATTR_IDENTIFIER_AMAZON_RAW_INFO = "AWS_INFO_RAW";
    private static final String JSON_KEY_CELEB = "celebrity";
    private static final String JSON_KEY_CELEB_NAME = "name";
    private static final String JSON_KEY_FACE = "face";
    private static final String JSON_KEY_FACE_BOUNDINGBOX = "boundingBox";
    private static final String JSON_KEY_FACE_BOUNDINGBOX_X = "left";
    private static final String JSON_KEY_FACE_BOUNDINGBOX_Y = "top";
    private static final String JSON_KEY_FACE_BOUNDINGBOX_WIDTH = "width";
    private static final String JSON_KEY_FACE_BOUNDINGBOX_HEIGHT = "height";

    private static final String APERTURE_NAME_PREFIX = "Amazon";

    /**
     * Get the raw data of amazon recognition service and generate the aperture values for the faces and celebrities
     * @param isyObjectId ID of the isy object for which aperture values will be generated
     */
    public void createFaceApertures(Long isyObjectId) throws FinderException, JSONException{
        ISYImageObjectEntity isyObject = isyObjectFacade.findISYObjectByIdentity(isyObjectId);
        String amazonRaw = getAmazonRawValue(isyObject);

        if(StringUtils.isNotEmpty(amazonRaw)){
            Double fileWidth = isyObject.getWidthInPixel().doubleValue();
            Double fileHeight = isyObject.getHeightInPixel().doubleValue();

            JSONObject json = new JSONObject(amazonRaw);
            List<FaceInformation> faceInformationList = new ArrayList<>();

            if(json.has(JSON_KEY_CELEB)){
                addJsonCelebFaceInformation(faceInformationList, json.getJSONArray(JSON_KEY_CELEB), fileWidth, fileHeight, isyObject);
            }

            if(json.has(JSON_KEY_FACE)){
                addJsonFaceInformation(faceInformationList, json.getJSONArray(JSON_KEY_FACE), fileWidth, fileHeight, isyObject);
            }

            createApertureXmpValues(faceInformationList, isyObject);
            logger.info("Found " + faceInformationList.size() + " faces");
        } else {
            logger.info("No amazon raw value found for " + isyObjectId);
        }
    }

    /**
     * Remove old existing xmp values and create new one
     * @param faceInformationList List of all face information for which xmp values will e generated
     * @param isyObject Isy object for which old values will be deleted
     */
    private void createApertureXmpValues(List<FaceInformation> faceInformationList, ISYObjectEntity isyObject){
        xmpApertureValueFacade.deleteApertureValues(isyObject, APERTURE_NAME_PREFIX);
        xmpApertureValueFacade.createXMPApertureValue(faceInformationList);
    }

    /**
     * Add the celebrities face information to the given list.
     * Hint: Amazon store percentage for face information. It has to be multipled to image width and heigth
     * @param faceInformationList List where new cface information will be added
     * @param jsonCeleb JSON object for the celebrities
     * @param fileWidth Width of the source image
     * @param fileHeight Heigth of the source image
     * @param isyObject Related isy object
     * @throws JSONException if there are structure mistakes
     */
    protected void addJsonCelebFaceInformation(List<FaceInformation> faceInformationList, JSONArray jsonCeleb, double fileWidth, double fileHeight, ISYObjectEntity isyObject) throws JSONException {
        for(int i = 0; i < jsonCeleb.length(); i++){
            JSONObject celeb = jsonCeleb.getJSONObject(i);
            JSONObject boundingBox = celeb.getJSONObject(JSON_KEY_FACE).getJSONObject(JSON_KEY_FACE_BOUNDINGBOX);
            String name = celeb.getString(JSON_KEY_CELEB_NAME);
            addFaceInformation(faceInformationList, name, boundingBox, fileWidth, fileHeight, isyObject);
        }
    }

    /**
     * Add face information to the given list.
     * Hint: Amazon store percentage for face information. It has to be multipled to image width and heigth
     * @param faceInformationList List where new cface information will be added
     * @param jsonFace JSON object for the faces
     * @param fileWidth Width of the source image
     * @param fileHeight Heigth of the source image
     * @param isyObject Related isy object
     * @throws JSONException if there are structure mistakes
     */
    protected void addJsonFaceInformation(List<FaceInformation> faceInformationList, JSONArray jsonFace, double fileWidth, double fileHeight, ISYObjectEntity isyObject) throws JSONException {
        for(int i = 0; i < jsonFace.length(); i++){
            JSONObject face = jsonFace.getJSONObject(i);
            JSONObject boundingBox = face.getJSONObject(JSON_KEY_FACE_BOUNDINGBOX);
            String name = "Unknown (" + face.getJSONObject("gender").getString("value") + ")";
            addFaceInformation(faceInformationList, name, boundingBox, fileWidth, fileHeight, isyObject);
        }
    }

    /**
     * Add face information to the given list.
     * Hint: Amazon store percentage for face information. It has to be multipled to image width and heigth
     * @param faceInformationList List where new cface information will be added
     * @param boundingBox JSON object which contains percentage coordinates of the faces
     * @param fileWidth Width of the source image
     * @param fileHeight Heigth of the source image
     * @param isyObject Related isy object
     * @throws JSONException if there are structure mistakes
     */
    protected void addFaceInformation(List<FaceInformation> faceInformationList, String name, JSONObject boundingBox, double fileWidth, double fileHeight, ISYObjectEntity isyObject) throws JSONException {
        double xPercentage = Double.parseDouble(boundingBox.getString(JSON_KEY_FACE_BOUNDINGBOX_X));
        double yPercentage = Double.parseDouble(boundingBox.getString(JSON_KEY_FACE_BOUNDINGBOX_Y));
        double widthPercentage = Double.parseDouble(boundingBox.getString(JSON_KEY_FACE_BOUNDINGBOX_WIDTH));
        double heightPercentage = Double.parseDouble(boundingBox.getString(JSON_KEY_FACE_BOUNDINGBOX_HEIGHT));

        faceInformationList.add(
                new FaceInformation(APERTURE_NAME_PREFIX + " " + faceInformationList.size() + ": " + name,
                        xPercentage * fileWidth,
                        yPercentage * fileHeight,
                        widthPercentage * fileWidth,
                        heightPercentage * fileHeight,
                        isyObject));
    }

    /**
     * Get amazon raw value for face calculation
     * @param isyObject Object for which raw data be searched
     * @return String if found, else null
     */
    protected String getAmazonRawValue(ISYImageObjectEntity isyObject){
        for(MetaDataAttributeValueEntity value : isyObject.getMetaDataAttributeValues()){
            if(ATTR_IDENTIFIER_AMAZON_RAW_INFO.equals(value.getIdentifier())){
                return value.getClobValue();
            }
        }
        return null;
    }
}
