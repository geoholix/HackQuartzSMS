import java.util.*;

/**
 * Created by jooh8592 on 12/15/16.
 */
public final class SMSDecoder {
    public static Map<String, List<Object>> decode_sms_msg(String sms_msg) {
        sms_msg = sms_msg.toLowerCase();
        if (check_sms_format_(sms_msg)) {
            return decode_sms_msg_(sms_msg);
        }
        return null;
    }

    private SMSDecoder() {
    }

    // Assumes that the decoded messages are in the format of "current_location, desired_ammenities_1, ..."
    private static Map<String, List<Object>> decode_sms_msg_(String sms_msg) {
        Map<String, List<Object>> decoded_sms = new HashMap<>();
        decoded_sms.computeIfAbsent("address", value -> new ArrayList<>());
        decoded_sms.computeIfAbsent("command", value -> new ArrayList<>());

        List<String> inputs = new ArrayList<>(Arrays.asList(sms_msg.split(";")));

        // decode the current location which is always the first in the list
        String current_location = inputs.get(0);

        List<Object> outputs;
        if (current_location.matches(".*\\b and \\b.*")) {
            outputs = new ArrayList<>(Arrays.asList(current_location.split(" and ")));

            for (int i = 0; i < outputs.size(); i++) {
                outputs.set(i, outputs.get(i).toString().replaceAll("\\s+", ""));
            }
        } else {
            outputs = new ArrayList<>();
            outputs.add(current_location);
        }

        decoded_sms.put("address", outputs);

        if (is_crowdsource_(sms_msg)){
            decode_sms_msg_crowdsource_(decoded_sms, sms_msg);
        } else {
            decode_sms_msg_resource_(decoded_sms, sms_msg);
        }

        return decoded_sms;
    }

    private static void decode_sms_msg_resource_(Map<String, List<Object>> output, String sms_msg){
        List<String> inputs = new ArrayList<>(Arrays.asList(sms_msg.split(";")));
        inputs.remove(0);

        // Parse desired locations
        List<Object> service_type_enums = new ArrayList<>();

        for (String input : inputs) {
            service_type_enums.add(Utils.str_to_ServiceTypeEnum_(input));
        }
        output.put("command", service_type_enums); // The rest should be just desired locations
    }

    private static void decode_sms_msg_crowdsource_(Map<String, List<Object>> output, String sms_msg){
        List<String> inputs = new ArrayList<>(Arrays.asList(sms_msg.split(";")));

        // Parse crowdsource info
        List<String> split_report_arg = new ArrayList<>(Arrays.asList(inputs.get(1).split(" ")));
        int offset = 0;

        for (int i = 0; i < split_report_arg.size(); ++i){
            if (split_report_arg.get(i).isEmpty()) {
                offset++;
            }
            else{
                break;
            }
        }

        List<Object> report_args = new ArrayList<>();
        report_args.add(split_report_arg.get(1 + offset));
        report_args.add(split_report_arg.get(2 + offset));

        output.put("command", report_args);
    }

    private static boolean is_crowdsource_(String sms_msg){
        if (sms_msg.matches(".*\\breport \\b.*")) {
            String[] temp = sms_msg.split(";");
            String[] temp_2 = temp[1].split(" ");
            int offset = 0;

            for (int i = 0; i < temp_2.length; ++i){
                if (temp_2[i].isEmpty()) {
                    offset++;
                } else {
                    break;
                }
            }

            if (temp_2.length != 3 + offset){
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean check_sms_format_(String sms_msg) {
        if (sms_msg.isEmpty()) {
            return false;
        }

        String[] inputs = sms_msg.split(";");

        // should always have at least 2 or more elements after the split
        if (inputs.length < 2) {
            return false;
        }

        // Check if each input actually contains something
        for (String input : inputs) {
            if (input.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
