import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.networkanalysis.*;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;

import javax.xml.ws.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by maxp8108 on 12/15/16.
 */
public class GeoEngine {
    private static final String ROUTE_TASK_URL =
//            "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route";
                "http://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World";
//            "http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/Network/USA/NAServer/Route";
    private static final SpatialReference WGS84 = SpatialReferences.getWgs84();
    private static final SpatialReference ESPG_3857 = SpatialReference.create(102100);
    private static final String LOCATOR_URL = "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer";
    private static final double BUFFER_STEP = 2000;
    private static final double MAX_BUFFER = 50000;

    UserCredential mUserCredential;
    SpatialReference mSR;
    RouteResultListener mListener;


    GeoEngine(){
        this("user", "pass");
    }
    GeoEngine(String un, String pw){
        mUserCredential = new UserCredential(un, pw);
    }

    void runResourceGeoEngine(String loc, ServiceTypeEnum resourceType, RouteResultListener listener){
        Utils.Log("Initializing Feature layer");
        mListener = listener;

        SMSFeatureTable resourceTable = SMSFeatureTableStore.
                getFeatureTableForEnumType(resourceType, mUserCredential);

        if(resourceTable == null){
            Utils.Log("COULD NOT FIND RESOURCE TABLE");
            mListener.onRouteNoResult("ER: Could not find resource data");
            return;
        }

        resourceTable.loadSMSFeatureTable(()->{
            mSR = resourceTable.getSpatialReference();
            Point p = geoCodeLocation(loc);
            if (p != null){
                recordResourceRequest(p, resourceType);
                Utils.Log("Finding nearest feature for point: " + p.toString());
                getNearestFeature(p, BUFFER_STEP, resourceTable);

            } else {
                Utils.Log("Geocoder returned null");
                mListener.onRouteResultError("ER: Could not find your location");
            }
        });

    }

//    void runCrowdSourceGeoEngine(String loc, String utility, String message, CrowdSourceListener listener){
//
//        SMSFeatureTable crowdSourceTable = SMSFeatureTableStore.
//                getFeatureTableForEnumType(SMSFeatureTableStore.FeatureTableType.CROWD_SOURCE, mUserCredential);
//
//        if (crowdSourceTable == null){
//            listener.onCrowdSourceError("Could not find table");
//            return;
//        }
//
//        crowdSourceTable.loadSMSFeatureTable(()-> {
//            mSR = crowdSourceTable.getSpatialReference();
//            Point p = geoCodeLocation(loc);
//            if(p != null){
//                Map<String,Object> attrs = SMSFeatureTable.createCrowdSourceAttrs(p);
//                crowdSourceTable.pushFeaturePoint(p, attrs);
//                listener.onCrowdSourceSuccess();
//            } else {
//                listener.onCrowdSourceError("Error Processing Location");
//            }
//        });
//    }

    private void recordResourceRequest(Point p, ServiceTypeEnum type){
        SMSFeatureTable trackTable = SMSFeatureTableStore.
                getFeatureTableForEnumType(ServiceTypeEnum.REPORT, mUserCredential);
        if (trackTable != null){
            trackTable.loadSMSFeatureTable(()->{
                Map<String,Object> attrs = SMSFeatureTable.createRecordAttrs(p, type);
                trackTable.pushFeaturePoint(p, attrs);
            });
        } else{
            Utils.Log("COULD NOT LOAD RECORDING TABLE");
        }
    }

    //  Point p = new Point(-13041560, 3868820, mSR);
    private Point geoCodeLocation(String loc){
         Utils.Log("Geocoding: " + loc);
         GeocodeParameters geocodeParameters = new GeocodeParameters();
         geocodeParameters.setOutputSpatialReference(mSR);
         LocatorTask locatorTask = new LocatorTask(LOCATOR_URL);
         try {
             List<GeocodeResult> resultList = locatorTask.geocodeAsync(loc, geocodeParameters).get();
             GeocodeResult result = resultList.get(0);
             Point resultLoc = result.getDisplayLocation();
             return resultLoc;
         } catch (InterruptedException | ExecutionException e) {
             e.printStackTrace();
             return null;
         }

    }

    private void getNearestFeature(Point p, double radius, SMSFeatureTable table){

        table.queryNearestFeatureInRadius(p, radius,
                new SMSFeatureTable.NearestFeatureListener() {
            @Override
            public void onNearestFeatureResult(Point nearP) {
                if(nearP != null){
                    generateRoute(p, nearP);
                } else if (radius < MAX_BUFFER){
                    double newRadius = radius + BUFFER_STEP;
                    getNearestFeature(p, newRadius, table);
                } else {
                    Utils.Log("ERROR No nearby points within buffer");
                    mListener.onRouteNoResult("ER: Could not find nearby resources");
                }
            }

            @Override
            public void onNearestFeatureError(String error) {
                Utils.Log("ERROR finding Nearest Feature");
                mListener.onRouteResultError("ER: Nearest Feature Exception");
            }
        });
    }

    private void generateRoute(Point a, Point b){
        Utils.Log("Generating Route for points:" + a.toString() + " " + b.toString());
        RouteTask routeTask = new RouteTask(ROUTE_TASK_URL);
        routeTask.setCredential(mUserCredential);

        try {
            routeTask.loadAsync();
            routeTask.addDoneLoadingListener(() -> {
                try {
                    RouteParameters routeParameters = routeTask.createDefaultParametersAsync().get();
                    List<TravelMode> travelModes = routeTask.getRouteTaskInfo().getTravelModes();
                    routeParameters.setTravelMode(travelModes.get(5));

                    routeParameters.setReturnDirections(true);
                    routeParameters.setReturnRoutes(true);

                    List<Stop> stops = new ArrayList<Stop>();
                    Stop stop1 = new Stop(a);
                    Stop stop2 = new Stop(b);
                    stops.add(stop1);
                    stops.add(stop2);
                    routeParameters.getStops().addAll(stops);
                    RouteResult result = routeTask.solveRouteAsync(routeParameters).get();
                    if (result.getRoutes().size() > 0){
                        Route route = result.getRoutes().get(0);
                        mListener.onRouteResultReturned(route);
                    } else {
                        mListener.onRouteNoResult("ER: No available routes");
                    }

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    mListener.onRouteResultError("ER: Route Parameter Exception");
                }
            });
        } catch (Exception ex) {
            Utils.Log("ERROR Exception creating route: " + ex.getLocalizedMessage());
            mListener.onRouteResultError("ER: Route Task Exception");
        }
    }

    public interface RouteResultListener{
        void onRouteResultReturned(Route result);
        void onRouteResultError(String error);
        void onRouteNoResult(String msg);
    }

    public interface CrowdSourceListener{
        void onCrowdSourceSuccess();
        void onCrowdSourceError(String err);
    }

}

