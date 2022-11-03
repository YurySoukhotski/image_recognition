package com.example.imrecognition.aperture;

import com.meylemueller.obj.isy.ISYObjectEntity;
import com.meylemueller.obj.isy.metadata.XMPValue;
import com.meylemueller.obj.isy.metadata.entity.XMPValueEntity;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Transactional
public class XMPApertureValueFacade {

    @Resource(name = "entityManager")
    private EntityManager entityManager;

    private static final String XMP_IDENTIFIER_APERTURE = "Aperture";

    /**
     * Create XMPValues out of the FaceInformations and persist it
     * @param faceInformations Collection of face informations for which aperture XMP values will be created
     * @return List of all created XMP values
     */
    public List<XMPValue> createXMPApertureValue(Collection<FaceInformation> faceInformations){
        List<XMPValue> xmpValues = new ArrayList<>();
        for(FaceInformation faceInformation : faceInformations){
            xmpValues.add(createXMPApertureValue(faceInformation));
        }
        return xmpValues;
    }

    /**
     * Create XMPValue out of the FaceInformation and persist it
     * @param faceInformation Face informations for which aperture XMP value will be created
     * @return Creatd XMP values
     */
    public XMPValue createXMPApertureValue(FaceInformation faceInformation){
        XMPValueEntity xmpValue = new XMPValueEntity();
        xmpValue.setIdentifier(XMP_IDENTIFIER_APERTURE);
        xmpValue.setIsyObjectEntity(faceInformation.getIsyObject());
        xmpValue.setStringValue(createXmpApertureStringValue(faceInformation));
        entityManager.persist(xmpValue);
        return xmpValue;
    }

    /**
     * Delete aperture xmp values from the given isy objects which starts with the given prefix
     * @param isyObjects Objects for which XMP values will be deleted
     * @param prefix Prefix of the aperture value
     */
    public void deleteApertureValues(Collection<ISYObjectEntity> isyObjects, String prefix){
        for(ISYObjectEntity isyObject : isyObjects){
            deleteApertureValues(isyObject, prefix);
        }
    }

    /**
     * Delete aperture xmp values from the given isy object which starts with the given prefix
     * @param isyObject Object for which XMP values will be deleted
     * @param prefix Prefix of the aperture value
     */
    public void deleteApertureValues(ISYObjectEntity isyObject, String prefix){
        List<XMPValueEntity> xmpValues = isyObject.getXmpValues();
        for (Iterator<XMPValueEntity> it = xmpValues.iterator(); it.hasNext();) {
            XMPValueEntity xmpValue = it.next();
            if (XMP_IDENTIFIER_APERTURE.equals(xmpValue.getIdentifier())
                    && xmpValue.getStringValue().startsWith(prefix)) {
                it.remove();
                entityManager.remove(xmpValue);
            }
        }
    }

    /**
     * Generate XMPValue string for aperture which only contains a name, coordinates and sizes
     * @param faceInformation Object which stores the coordinates and names
     * @return Aperture xmp value
     */
    private String createXmpApertureStringValue(FaceInformation faceInformation){
        return faceInformation.getName() +
                "|false||" +
                faceInformation.getWidth() +
                "|false|" +
                faceInformation.getHeight() +
                "|false|0.0|0.0|false|" +
                faceInformation.getX() +
                "|false|" +
                faceInformation.getY() +
                "|false|0.0|false|0.0|false|0.0|false|false|true|0|0|0|||false|-1";
    }
}
