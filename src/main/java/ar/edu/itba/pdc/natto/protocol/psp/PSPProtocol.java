package ar.edu.itba.pdc.natto.protocol.psp;

import ar.edu.itba.pdc.natto.protocol.Protocol;

import static com.google.common.base.Preconditions.checkState;

// #1XX Success
// #2XX Client Error
// #3XX Server Error

public class PSPProtocol implements Protocol<String> {
    private static final String messageFinalizer = "\n.\n";

    private enum ErrorCode {
        InvalidParameters(200, "Invalid parameters");

        private int code;
        private String message;

        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public int getCode() {
            return code;
        }
    }

    @Override
    public String process(String message) {
        String[] values = message.split(" ");
        String command = values[0];

        if (command.equalsIgnoreCase("SILENCE")) {
            if (values.length != 3) {
                return error(ErrorCode.InvalidParameters);
            }

            return silence(values[1], values[2]);
        }

        checkState(false);

        return null;
    }

    private String silence(String jidFrom, String jidTo) {
        // TODO: # message
        return "OK 100" + jidFrom + " will not bother " + jidTo + messageFinalizer;
    }

    private String error(ErrorCode error) {
        // TODO: # message
        return error.getCode() + " " + error.getMessage() + messageFinalizer;
    }
}
