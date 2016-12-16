import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.twilio.sdk.*;

import java.util.*;

import com.twilio.sdk.resource.factory.MessageFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import static spark.Spark.*;


public class HelloSpark {

    public static final String ACCOUNT_SID = "AC824252bb5f11f9084a43dcbeb0a31219";
    public static final String AUTH_TOKEN = "c1a60ae3a110a75473087ec7bf460183";
    private static final String HELP_TEXT = "\nPlease try this format\n<Location>; <Resource>";
    private static final int POST_DELAY = 500;

    public static void main(String[]args) throws TwilioRestException {
        TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
        String ngrokURL = "http://c4a6a0e4.ngrok.io";

        post("/", (req, res) -> {
            String body = req.queryParams("Body");
            String to = req.queryParams("From");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("To", to));
            params.add(new BasicNameValuePair("From", "+17162612954"));
            MessageFactory messageFactory = client.getAccount().getMessageFactory();

            //address and desiredLocation
            Map<String, List<Object>> msgParseDict = SMSDecoder.decode_sms_msg(body);

            if(msgParseDict == null){
                send_text_msg(messageFactory, params, "ER: Could not read input" + HELP_TEXT);
                return "ERROR";
            }

            String adr = msgParseDict.get("address").get(0).toString();
            ServiceTypeEnum resourceType = (ServiceTypeEnum) msgParseDict.get("command").get(0);

            runGeoEngine(adr, resourceType, new GeoEngine.RouteResultListener() {
                @Override
                public void onRouteResultReturned(Route result) {
                    List<String> out = SMSEncoder.encode_sms_msg(result);

                    Runnable r = new Runnable(){
                        @Override
                        public void run() {
                            for (String s : out){
                                send_text_msg(messageFactory, params, s);
                                try{
                                    Thread.sleep(POST_DELAY);
                                } catch(InterruptedException e){
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    };
                    new Thread(r).start();
                }

                @Override
                public void onRouteResultError(String error) {
                    send_text_msg(messageFactory, params, error);
                    Utils.Log("OH NO IT DIDN'T WORK! Error: " + error);
                }

                @Override
                public void onRouteNoResult(String msg) {
                    send_text_msg(messageFactory, params, msg + HELP_TEXT);
                    Utils.Log("OH NO NO RESULTS: " + msg);
                }

            });

            return("PROCESSING");
        });

    }

    private static void send_text_msg(MessageFactory message_factory,List<NameValuePair> txt_msg, String message)
    {
        txt_msg.add(new BasicNameValuePair("Body", message));
        try
        {
            message_factory.create(txt_msg);

        } catch (TwilioRestException e) {

            e.printStackTrace();
        } finally{
            txt_msg.remove(txt_msg.size() - 1);
            Utils.Log(message);

        }


    }


    private static void runGeoEngine(String adr, ServiceTypeEnum resourceType, GeoEngine.RouteResultListener listener){
        GeoEngine geoEngine = new GeoEngine();
        geoEngine.runResourceGeoEngine(adr, resourceType, listener);

    }

}
