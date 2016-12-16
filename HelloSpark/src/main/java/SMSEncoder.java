import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jooh8592 on 12/15/16.
 */
public final class SMSEncoder
{
    public static List<String> encode_sms_msg(Route route)
    {
        if (route != null)
        {
            return encode_sms_msg_(route);
        }
        return failed_message_handler_();
    }

    private SMSEncoder() {}

    private static List<String> encode_sms_msg_(Route route)
    {
        List<DirectionManeuver> directions = route.getDirectionManeuvers();
        List<String> sms_msgs = new ArrayList<>();
        String sms_msg = "";

        for (DirectionManeuver direction : directions)
        {
            String direction_msg = direction.getDirectionText();

            if (sms_msg.length() + direction_msg.length() + 1 <= 153 - 38)
            {
                sms_msg += direction_msg + '\n';
            }
            else if (direction_msg.length() + 1 <= 153 - 38)
            {
                // Remove \n at the end of the msg
                sms_msg = sms_msg.substring(0, sms_msg.length() - 1);
                sms_msgs.add(sms_msg + '\n');
                sms_msg = direction_msg;
            }
            else
            {
                if (!sms_msg.isEmpty())
                {
                    // Remove \n at the end of the msg
                    sms_msg = sms_msg.substring(0, sms_msg.length() - 2);
                    sms_msgs.add(sms_msg);
                }
                sms_msg = direction_msg;

                // If the msg is longer than 160 char, go to half point then start searching for space. Cut the msg at
                // the space
                for (int i = sms_msg.length() / 2; i < sms_msg.length(); i ++)
                {
                    if (sms_msg.charAt(i) == ' ')
                    {
                        sms_msgs.add(sms_msg.substring(0, i));
                        sms_msgs.add(sms_msg.substring(i, sms_msg.length() - 1));
                    }
                }
            }
        }
        for(int i = 0; i < sms_msgs.size(); i++)
        {
            sms_msgs.set(i, number_text_(i, sms_msgs.size()) + sms_msgs.get(i));
        }

        return sms_msgs;
    }

    private static List<String> failed_message_handler_()
    {
        // If we have more time add a better handler
        List<String> output = new ArrayList<>();

        output.add("No route exists");

        return output;
    }

    private static String number_text_(int num_of_text, int num_of_total_text)
    {
        return '(' + Integer.toString(num_of_text + 1) + '/' + Integer.toString(num_of_total_text) + ')';
    }
}
