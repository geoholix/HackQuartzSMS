import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.security.UserCredential;

import javax.xml.ws.Service;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by maxp8108 on 12/16/16.
 */
public class SMSFeatureTableStore {
    private static final String SD_HOSPITALS_URL =
			      "http://services7.arcgis.com/UrPx33dk4laskhGV/ArcGIS/rest/services/medical_facilities_berlin/FeatureServer/0";
    private static final String REFUGEE_REQUESTS_URL =
            "http://services7.arcgis.com/UrPx33dk4laskhGV/arcgis/rest/services/Refugee_Requests/FeatureServer/0";

    private static Map<ServiceTypeEnum, String> getFeatureTableDict(){
        Map<ServiceTypeEnum, String> urlDict = new HashMap<>();

        urlDict.put(ServiceTypeEnum.HOSPITALS, SD_HOSPITALS_URL);
        urlDict.put(ServiceTypeEnum.REPORT, REFUGEE_REQUESTS_URL);

        return urlDict;
    }

    static SMSFeatureTable getFeatureTableForEnumType(ServiceTypeEnum type, UserCredential credential){
        SMSFeatureTable table = getFeatureTableForEnumType(type);
        if (table != null) {
            table.setUserCredential(credential);
        }
        return table;
    }

    static SMSFeatureTable getFeatureTableForEnumType(ServiceTypeEnum type){
        Map<ServiceTypeEnum, String> dict = getFeatureTableDict();
        String url = dict.get(type);
        if (url != null){
            return new SMSFeatureTable(url);
        } else {
            return null;
        }
    }

}
