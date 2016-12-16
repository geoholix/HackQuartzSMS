import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by maxp8108 on 12/15/16.
 */
public class Utils {
    public static void Log(String str){
        System.out.println(str);
    }

    public static String ServiceTypeEnum_to_str(ServiceTypeEnum service_type_enum)
    {
        switch (service_type_enum)
        {
            case AIRPORTS:
                return "AIRPORTS";

            case BUS_STOPS:
                return "BUS_STOPS";

            case HOSPITALS:
                return "HOSPITALS";

            case SHELTERS:
                return "SHELTERS";

            case UNKNOWN: // INTENTIONAL FALLTHROUGH!!!!
            default:
                return null;
        }
    }

    public static ServiceTypeEnum str_to_ServiceTypeEnum_(String string)
    {
        string = string.toUpperCase();

        if (is_hospital_related_(string))
        {
            return ServiceTypeEnum.HOSPITALS;
        }
        else if (is_bus_stop_related(string))
        {
            return ServiceTypeEnum.BUS_STOPS;
        }
        else if (is_airplane_related(string))
        {
            return ServiceTypeEnum.AIRPORTS;
        }
        else if (is_shelter_related(string))
        {
            return ServiceTypeEnum.SHELTERS;
        }
        else
        {
            return ServiceTypeEnum.UNKNOWN;
        }
    }

    // TODO: Make smarter
    private static boolean is_hospital_related_(String string)
    {
        return string.contains("HOSPITAL") ||
               string.contains("CLINIC") ||
               string.contains("DOCTOR") ||
               string.contains("MEDIC") ||
               string.contains("SICK");
    }

    // TODO: Make smarter
    private static boolean is_bus_stop_related(String string)
    {
        return string.contains("BUS") ||
               string.contains("TRANSPORT") ||
               string.contains("CAR");
    }

    // TODO: Make smarter
    private static boolean is_airplane_related(String string)
    {
        return string.contains("PLANE") ||
               string.contains("FLY");
    }

    // TODO: Make smarter
    private static boolean is_shelter_related(String string)
    {
        return string.contains("SHELTER") ||
               string.contains("HOME") ||
               string.contains("HOUSE") ||
               string.contains("FOOD") ||
               string.contains("NUTRI") ||
               string.contains("SUSTENANCE");
    }
}
