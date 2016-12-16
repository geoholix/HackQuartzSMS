import com.esri.arcgisruntime.arcgisservices.ArcGISFeatureLayerInfo;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.security.UserCredential;

import javax.xml.ws.Service;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by maxp8108 on 12/16/16.
 */
public class SMSFeatureTable {

    private ServiceFeatureTable mFeatureTable;

    SMSFeatureTable(String url){
        mFeatureTable = new ServiceFeatureTable(url);
    }

    void loadSMSFeatureTable(SMSFeatureTableLoadListener listener){
        mFeatureTable.loadAsync();
        mFeatureTable.addDoneLoadingListener(listener::onLoaded);
    }

    SpatialReference getSpatialReference(){
        return mFeatureTable.getSpatialReference();
    }


    void queryNearestFeatureInRadius(Point p, double radius, NearestFeatureListener listener){
        Utils.Log(String.format("Getting Nearest Feature for radius: %f", radius));
        Polygon buffer = GeometryEngine.buffer(p, radius);

        ListenableFuture<FeatureQueryResult> tableQueryResult;

        QueryParameters qParams = new QueryParameters();
        qParams.setGeometry(buffer);
        qParams.setSpatialRelationship(QueryParameters.SpatialRelationship.CONTAINS);
//        qParams.setOutSpatialReference(mFeatu);
        qParams.setReturnGeometry(true);

        tableQueryResult = mFeatureTable.queryFeaturesAsync(qParams);
        tableQueryResult.addDoneListener(() -> {
            try {
                FeatureQueryResult result = tableQueryResult.get();
                double minDistance = radius;
                Point closestPoint = null;

                for(Feature f : result){
                    Point nearP = (Point) f.getGeometry();
                    double distance = GeometryEngine.distanceBetween(nearP, p);
                    if(distance < minDistance){
                        minDistance = distance;
                        closestPoint = nearP;
                    }
                }
                listener.onNearestFeatureResult(closestPoint);


            } catch (InterruptedException e) {
                e.printStackTrace();
                listener.onNearestFeatureError(e.getLocalizedMessage());
            } catch (ExecutionException e) {
                e.printStackTrace();
                listener.onNearestFeatureError(e.getLocalizedMessage());
            }

        });
    }


    void pushFeaturePoint(Point p, Map<String, Object> attributes){
        if(p.getSpatialReference() != getSpatialReference()){
            p = (Point) GeometryEngine.project(p, getSpatialReference());
        }
        Feature feature = mFeatureTable.createFeature(attributes, p);
        if(mFeatureTable.canAdd()) {
            ListenableFuture<Void> addResult = mFeatureTable.addFeatureAsync(feature);
            addResult.addDoneListener(() -> applyFeaturePointEdits());
        } else {
            Utils.Log("FAILURE");
        }
    }

    private void applyFeaturePointEdits() {

        // apply the changes to the server
        ListenableFuture<List<FeatureEditResult>> editResult = mFeatureTable.applyEditsAsync();
        editResult.addDoneListener(() -> {
            try {
                List<FeatureEditResult> edits = editResult.get();
                // check if the server edit was successful
                if (edits != null && edits.size() > 0 && edits.get(0).hasCompletedWithErrors()) {
                    throw edits.get(0).getError();
                } else if (edits != null && edits.size() == 0){
                    Utils.Log("COULD NOT UPDATE");
                } else {
                    Utils.Log("FEATURE TABLE UPDATED!!");
                }
            } catch (InterruptedException | ExecutionException e) {
                Utils.Log("Exception applying edits on server" + e.getCause().getMessage());
            }
        });
    }

    static Map<String, Object> createRecordAttrs(Point p, ServiceTypeEnum resourceType){
        Map<String, Object> map = new HashMap<>();
        map.put("Lat", p.getY());
        map.put("Long", p.getX());



//        Date dNow = new Date( );
//        SimpleDateFormat ft =
//                new SimpleDateFormat ("MM/DD/YYYY hh:mm:ss");

//        map.put("Date", Calendar.getInstance().getTime().toString());
//        map.put("Date", new java.util.Date().toString());
        map.put("Date", Calendar.getInstance());
        map.put("Resource", Utils.ServiceTypeEnum_to_str(resourceType));
//        map.put("Lat", p.getY());
//        map.put("Long", p.getY());
//        map.put("Query_String", null);
//        map.put("ID", null);
        return map;
    }
    static Map<String, Object> createCrowdSourceAttrs(Point p){
        Map<String, Object> map = new HashMap<>();
//        map.put("Date", new java.util.Date().toString());
//        map.put("Resource", resource);
        map.put("Lat", p.getY());
        map.put("Lon", p.getY());
        return map;
    }

    void setUserCredential(UserCredential credential){
        mFeatureTable.setCredential(credential);
    }


    public interface NearestFeatureListener{
        void onNearestFeatureResult(Point nearP);
        void onNearestFeatureError(String error);
    }
    public interface SMSFeatureTableLoadListener{
        void onLoaded();
    }
}
