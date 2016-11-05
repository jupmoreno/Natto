package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Protocol;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppParserFactory;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by user on 05/11/16.
 */
public class NttpProtocol implements Protocol<StringBuilder> {

    private StringBuilder sb;
    XmppData xmppData;

    private boolean authorized = false;
    // Siempre que se modifique un comando hay que modificar esto.
    private final static String[] commands = {"help", "silence", "transformation", "metrics", "auth", "server"};
    private String[] authenticationMethods = {"plain", };
    private String authMethod = "";

    public NttpProtocol(XmppData xmppData){
        this.xmppData = xmppData;
    }

    private enum Codes{
        OK('.', 00, "OK"),
        GO_AHEAD('.', 01, "Go Ahead"),
        LOGGED_IN('.', 02, "Logged in"),
        AUTH_METHODS('.', 03, "Authentication methods"),
        USER_SILENCED('.', 04, "User silenced"),
        WHAT('?', 00, "What?"),
        WRONG_ARGS('!', 00, "Wrong arguments"),
        METHOD_NOT_SUPPORTED('!', 01, "Authentication method not supported"),
        ALREADY_AUTHORIZED('!', 02, "Already authorized"),
        WITHOUT_AUTH_METHOD('!', 03, "Authorization method not requested"),
        INCORRECT_USER_PASS('!', 04, "Incorrect user or password"),
        USER_ALREADY_SILENCED('!', 05, "The user is already silenced"),
        ;

        private char type;
        private int code;
        private String message;

        Codes(char type, int code, String message){
            this.type = type;
            this.code = code;
            this.message = message;
        }

        public char getType() {
            return type;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    @Override
    public StringBuilder process(StringBuilder message) {
        System.out.println("Procesando...");
        if(message == null)
            return null;

        sb = message;
        String[] messageVec = sb.toString().split(" ");

        if(messageVec.length == 0){
            //TODO
            sb.setLength(0);
            return message;
        }

        if(messageVec[0].compareToIgnoreCase("silence") == 0){
            handleSilence(messageVec);
            return sb;
        }else if(messageVec[0].compareToIgnoreCase("transformation") == 0){
            handleTransformation(messageVec);
            return sb;
        }else if(messageVec[0].compareToIgnoreCase("metrics") == 0){
            handleMetrics(messageVec);
            return sb;
        }else if(messageVec[0].compareToIgnoreCase("auth") == 0){
            handleAuth(messageVec);
            return sb;
        }else if(messageVec[0].compareToIgnoreCase("server") == 0){
            handleServer(messageVec);
            return sb;
        }else if(messageVec[0].compareToIgnoreCase("help") == 0){
            handleHelp(messageVec);
            return sb;
        }else{
            handleDefault(messageVec);
            return sb;
        }
    }

    private void handleHelp(String[] messageVec) {
        if(messageVec.length > 1){
            formulateResponse(Codes.WRONG_ARGS, null);
            return;
        }

        formulateResponse(Codes.OK, commands);
    }

    private void handleDefault(String[] messageVec) {
        formulateResponse(Codes.WHAT, null);
    }

    private void handleServer(String[] messageVec) {
    }

    private void handleAuth(String[] messageVec) {
        if(messageVec.length == 2 && messageVec[1].compareToIgnoreCase("--methods") == 0){
            formulateResponse(Codes.AUTH_METHODS, authenticationMethods);
            return;
        }

        if(messageVec.length == 3 && messageVec[1].compareToIgnoreCase("--request") == 0){
            if(messageVec[2].compareToIgnoreCase("plain") == 0){
                authMethod = "plain";
                formulateResponse(Codes.GO_AHEAD, null);
                return;
            }else{
                formulateResponse(Codes.METHOD_NOT_SUPPORTED, null);
                return;
            }
        }

        if(messageVec.length == 3){
            if(authorized){
                formulateResponse(Codes.ALREADY_AUTHORIZED, null);
                return;
            }

            if(authMethod.equals("")){
                formulateResponse(Codes.WITHOUT_AUTH_METHOD, new String[]{"Use auth --request to select the authentication method. Use auth --methods to see available authentication methods."});
                return;
            }

            //Ahora vaildo usuario y contraseña según el método
            if(authMethod.compareToIgnoreCase("plain") == 0){
                if(plainAuth(messageVec[1], messageVec[2])){
                    formulateResponse(Codes.LOGGED_IN, null);
                    return;
                }else{
                    formulateResponse(Codes.INCORRECT_USER_PASS, null);
                    return;
                }
            }

            formulateResponse(Codes.WRONG_ARGS, null);
        }



    }

    private boolean plainAuth(String user, String password) {
        //TODO
        authorized = true;
        return true;
    }


    private void handleMetrics(String[] messageVec) {
    }

    private void handleTransformation(String[] messageVec) {
    }

    private void handleSilence(String[] messageVec) {
        if(messageVec.length > 2){
            formulateResponse(Codes.WRONG_ARGS, null);
            return;
        }

        if(xmppData.isUserSilenced(messageVec[1])){
            formulateResponse(Codes.USER_ALREADY_SILENCED, null);
            return;
        }

        xmppData.silenceUser(messageVec[1]);
        formulateResponse(Codes.USER_SILENCED, null);
        return;

    }

    private void formulateResponse(Codes c, String[] lines){
        sb.setLength(0);

        if(lines != null && lines.length > 0){
            sb.append("+" + lines.length + " ");
        }

        sb.append(c.getType()).append(" ").append(c.getCode()).append(" ").append(c.getMessage());

        if(lines != null){
            for(int i = 0; i < lines.length; i++){
                sb.append(lines[i]).append("\n");
            }
        }

    }
}
