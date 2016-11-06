package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.protocol.Protocol;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppParserFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by user on 05/11/16.
 */
public class NttpProtocol implements Protocol<StringBuilder> {

    private StringBuilder sb;
    private XmppData xmppData;

    private boolean authorized = false;
    // Siempre que se modifique un comando hay que modificar esto y helps.
    private final static String[] commands = {"help", "silence", "unsilence", "transformation", "metrics", "auth", "state", "getSilenced", "server"};
    private String[] authenticationMethods = {"plain", };
    private String[] metrics = {"bytes", "accesses", };
    private String authMethod = "";
    private String user = "";
    private String[] helps = {
        "Without parameter: shows all the available commands. With parameter @command: shows information about the use of @command",
        "Recieves an @user as parameter. Silences @user",
        "Recieves an @user as parameter. Unsilences @user",
        "Recieves an @user as parameter. Unsilences @user",
        "Recieves a \"true\" or \"false\" as parameter. Enables l33t transformations if parameter is \"true\", or disables l33t transformations if parameter is \"false\".",
        "Without parameters: shows all available metrics. " +
                "With parameter @metric: shows the information about @metric. +" +
                "With head value --methods and without parameters: shows the available metrics.",
        "With head value --methods: shows the available authentication methods. " +
                "With head value --request and parameter @method: requests authentication by using the specified @method. +" +
                "With parameters @user and @password and after authentication requested: authenticates user with the specific @user and @password.",
        "Shows the state of relevant variables.",
        "Shows the silenced users.",
        "",
    };

    public NttpProtocol(XmppData xmppData){
        this.xmppData = xmppData;
    }

    private enum Codes{
        OK('.', 0, "OK."),
        GO_AHEAD('.', 1, "Go Ahead."),
        LOGGED_IN('.', 2, "Logged in."),
        AUTH_METHODS('.', 3, "Authentication methods."),
        USER_SILENCED('.', 4, "User silenced."),
        USER_UNSILENCED('.', 5, "User unsilenced."),
        TRANSFORMATION_ENABLED('.', 6, "Transformation Enabled."),
        TRANSFORMATION_DISABLED('.', 7, "Transformation Disabled."),
        METRIC_METHODS('.', 8, "Metric methods."),
        WHAT('?', 0, "What?."),
        WRONG_ARGS('?', 0, "Wrong arguments."),
        METHOD_NOT_SUPPORTED('!', 1, "Authentication method not supported."),
        ALREADY_AUTHORIZED('!', 2, "Already authorized."),
        WITHOUT_AUTH_METHOD('!', 3, "Authorization method not requested."),
        INCORRECT_USER_PASS('!', 4, "Incorrect user or password."),
        USER_ALREADY_SILENCED('!', 5, "This user is already silenced."),
        USER_ALREADY_UNSILENCED('!', 6, "This user was not silenced."),
        MUST_AUTH('!', 07, "You need to authenticate."),
        TOO_MUCH_OUTPUT('X', 0, "Too much output to process.")
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

        if(message.toString().equals("\nerror\n")){
            handleTooBig();
            return sb;
        }

        String[] messageVec = sb.toString().split(" ");

        if(messageVec.length == 0){
            //TODO
            sb.setLength(0);
            return message;
        }

        if(messageVec[0].compareToIgnoreCase("silence") == 0){
            handleSilence(messageVec);
        }else if(messageVec[0].compareToIgnoreCase("unsilence") == 0){
            handleUnsilence(messageVec);
        }else if(messageVec[0].compareToIgnoreCase("transformation") == 0){
            handleTransformation(messageVec);
        }else if(messageVec[0].compareToIgnoreCase("metrics") == 0){
            handleMetrics(messageVec);
        }else if(messageVec[0].compareToIgnoreCase("auth") == 0){
            handleAuth(messageVec);
        }else if(messageVec[0].compareToIgnoreCase("server") == 0){
            handleServer(messageVec);
        }else if(messageVec[0].compareToIgnoreCase("help") == 0){
            handleHelp(messageVec);
        }else if(messageVec[0].compareToIgnoreCase("state") == 0){
            handleState(messageVec);
        }else if(messageVec[0].compareToIgnoreCase("getSilenced") == 0){
            handleGetSilenced(messageVec);
        }else{
            handleDefault();
        }

        return sb;
    }

    private void handleHelp(String[] messageVec) {
        if(messageVec.length == 1){
            formulateResponse(Codes.OK, commands);
            return;
        }


        if(messageVec.length == 2){
            for(int i = 0; i < commands.length; i++){
                if(messageVec[1].compareToIgnoreCase(commands[i]) == 0){
                    formulateResponse(Codes.OK, new String[]{
                        commands[i] + ":",
                        helps[i],
                    });
                    return;
                }
            }
        }

        formulateResponse(Codes.WRONG_ARGS, null);

    }

    private void handleAuth(String[] messageVec) {
        if(messageVec.length >= 2 && messageVec[1].compareToIgnoreCase("--methods") == 0){

            if(messageVec.length != 2){
                formulateResponse(Codes.WRONG_ARGS, null);
                return;
            }

            formulateResponse(Codes.AUTH_METHODS, authenticationMethods);
            return;
        }

        if(messageVec.length >= 3 && messageVec[1].compareToIgnoreCase("--request") == 0){

            if(messageVec.length != 3){
                formulateResponse(Codes.WRONG_ARGS, null);
                return;
            }

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
                    user = messageVec[1];
                    return;
                }else{
                    formulateResponse(Codes.INCORRECT_USER_PASS, null);
                    return;
                }
            }

        }

        formulateResponse(Codes.WHAT, null);


    }

    private boolean plainAuth(String user, String password) {
        //TODO
        authorized = true;
        return true;
    }

    private void handleServer(String[] messageVec) {
    }

    private void handleMetrics(String[] messageVec) {
        if(user.equals("")){
            formulateResponse(Codes.MUST_AUTH, null);
            return;
        }

        if(messageVec.length == 1){
            String[] metricsRet = {
                "bytes: " + xmppData.getBytesTransferred(),
                "acceses: " + xmppData.getAccessesAmount(),
            };
            formulateResponse(Codes.OK, metricsRet);
            return;
        }

        if(messageVec.length == 2){
            if(messageVec[1].compareToIgnoreCase("--methods") == 0){
                formulateResponse(Codes.METRIC_METHODS, metrics);
                return;
            }else if(messageVec[1].compareToIgnoreCase("bytes") == 0){
                formulateResponse(Codes.OK, new String[]{"bytes: " + xmppData.getBytesTransferred()});
                return;
            }else if(messageVec[1].compareToIgnoreCase("accesses") == 0){
                formulateResponse(Codes.OK, new String[]{"accesses: " + xmppData.getAccessesAmount()});
                return;
            }
        }

        formulateResponse(Codes.WRONG_ARGS, null);

    }

    private void handleTransformation(String[] messageVec) {
        if(user.equals("")){
            formulateResponse(Codes.MUST_AUTH, null);
            return;
        }
        if(messageVec.length != 2 ){
            formulateResponse(Codes.WRONG_ARGS, null);
            return;
        }

        if( messageVec[1].compareToIgnoreCase("true") == 0 ){
            xmppData.setTransformation(true);
            formulateResponse(Codes.TRANSFORMATION_ENABLED, null);
            return;
        }

        else if(messageVec[1].compareToIgnoreCase("false") == 0 ){
            xmppData.setTransformation(false);
            formulateResponse(Codes.TRANSFORMATION_DISABLED, null);
            return;
        }

        formulateResponse(Codes.WRONG_ARGS, null);

    }

    private void handleSilence(String[] messageVec) {
        if(user.equals("")){
            formulateResponse(Codes.MUST_AUTH, null);
            return;
        }

        if(messageVec.length != 2){
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


    private void handleUnsilence(String[] messageVec) {
        System.out.println("el usuario que quiero unsilenciar es " + messageVec[1]);
        if(user.equals("")){
            formulateResponse(Codes.MUST_AUTH, null);
            return;
        }

        if(messageVec.length != 2){
            formulateResponse(Codes.WRONG_ARGS, null);
            return;
        }

        if(!xmppData.isUserSilenced(messageVec[1])){
            formulateResponse(Codes.USER_ALREADY_UNSILENCED, null);
            return;
        }

        xmppData.unsilenceUser(messageVec[1]);
        formulateResponse(Codes.USER_UNSILENCED, null);
        return;
    }

    private void handleGetSilenced(String[] messageVec){
        if(user.equals("")){
            formulateResponse(Codes.MUST_AUTH, null);
            return;
        }
        if(messageVec.length != 1){
            formulateResponse(Codes.WRONG_ARGS, null);
            return;
        }

        String[] users = xmppData.getUsersSilenced().toArray(new String[xmppData.getUsersSilenced().size()]);
        formulateResponse(Codes.OK, users);
    }

    private void handleDefault() {
        formulateResponse(Codes.WHAT, null);
    }

    private void handleTooBig() {
        formulateResponse(Codes.TOO_MUCH_OUTPUT, null);
    }

    private void formulateResponse(Codes c, String[] lines){
        sb.setLength(0);

        if(lines != null && lines.length > 0){
            sb.append("+" + lines.length + " ");
        }

        sb.append(c.getType()).append(" ").append(c.getCode()).append(" ").append(c.getMessage()).append("\n");

        if(lines != null){
            for(int i = 0; i < lines.length; i++){
                sb.append(lines[i]).append("\n");
            }
        }

    }

    private void handleState(String[] messageVec){
        if(user.equals("")){
            formulateResponse(Codes.MUST_AUTH, null);
            return;
        }
        if(messageVec.length != 1){
            formulateResponse(Codes.WRONG_ARGS, null);
            return;
        }
        String[] states = {
                "Transformation: " + (xmppData.isTransformEnabled() ? "enabled" : "disabled"),
                "User: " + user,
        };
        formulateResponse(Codes.OK, states);
    }

}
