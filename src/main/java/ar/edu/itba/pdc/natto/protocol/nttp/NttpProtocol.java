package ar.edu.itba.pdc.natto.protocol.nttp;

import ar.edu.itba.pdc.natto.net.NetAddress;
import ar.edu.itba.pdc.natto.protocol.Protocol;
import ar.edu.itba.pdc.natto.protocol.xmpp.XmppData;

public class NttpProtocol implements Protocol<StringBuilder> {

    private StringBuilder sb;

    private final NttpData nttpData;
    private final XmppData xmppData;
    private final static int supportedVersionMajor = 1;
    private final static int supportedVersionMinor = 1;

    private boolean authorized = false;
    // Siempre que se modifique un comando hay que modificar esto y helps.
    private final static String[] commands = {"hello", "help", "auth", "quit", "silence", "unsilence", "transformation", "metrics", "state", "getSilenced", "multiplex", "getMultiplex"};
    private String[] authenticationMethods = {"simple",};
    private String[] metrics = {"bytes", "accesses", "accepted", };
    private String authMethod = "";
    private String user = "";
    private boolean hello = false;
    private String[] helps = {
            "Initiates dialog with the user. Needs the head values --version along with the version.",
            "Without parameter: shows all the available commands. With parameter @command: shows information about the use of @command",
            "With head --methods and without value: shows the available authentication methods. " +
                    "With head --request and value @method: requests authentication by using the specified @method. " +
                    "With parameters @user and @password and after authentication requested: authenticates user with the specific @user and @password.",
            "Quits and ends connection.",
            "Recieves an @user as parameter. Silences @user",
            "Recieves an @user as parameter. Unsilences @user",
            "Recieves a \"true\" or \"false\" as parameter. Enables l33t transformations if parameter is \"true\", or disables l33t transformations if parameter is \"false\".",
            "Without parameters: shows all available metrics. " +
                    "With parameter @metric: shows the information about @metric. " +
                    "With head --methods and without value: shows the available metrics.",
            "Shows the state of relevant variables.",
            "Shows the silenced users.",
            "With parameters @user, @host and @port: changes host and port of the given @user.",
            "Shows the multiplexed users and the respective addres and port.",
    };

    public NttpProtocol(NttpData nttpData, XmppData xmppData) {
        this.nttpData = nttpData;
        this.xmppData = xmppData;
    }


    public StringBuilder process(StringBuilder message) {
        if (message == null)
            return null;


        sb = message;

        if (message.toString().equals("\nerror\n")) {
            handleTooBig();
            return sb;
        }

        String[] messageVec = sb.toString().split(" ");

        if (messageVec.length == 0) {
            sb.setLength(0);
            return message;
        }

        if (messageVec[0].compareToIgnoreCase("silence") == 0) {
            handleSilence(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("unsilence") == 0) {
            handleUnsilence(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("hello") == 0) {
            handleHello(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("transformation") == 0) {
            handleTransformation(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("metrics") == 0) {
            handleMetrics(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("auth") == 0) {
            handleAuth(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("quit") == 0) {
            handleQuit(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("help") == 0) {
            handleHelp(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("state") == 0) {
            handleState(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("getMultiplex") == 0) {
            handleGetMultiplex(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("getSilenced") == 0) {
            handleGetSilenced(messageVec);
        } else if (messageVec[0].compareToIgnoreCase("multiplex") == 0) {
            handleMultiplex(messageVec);
        } else {
            handleDefault();
        }

        return sb;
    }

    private void handleHello(String[] messageVec) {

        if(messageVec.length != 3){
            formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[0]});
            return;
        }

        if(messageVec[1].compareToIgnoreCase("--version") != 0){
            formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[0]});
            return;
        }

        int major = 0;
        int minor = 0;
        String[] version = messageVec[2].split("\\.");

        if(version.length == 1){
            try {
                major = Integer.valueOf(version[0]);
                if(major > supportedVersionMajor){
                    formulateResponse(NttpCode.VERSION_NOT_SUPPORTED, null);
                    return;
                }else if(major < 0){
                    formulateResponse(NttpCode.VERSION_NOT_SUPPORTED, null);
                    return;
                }
            }catch (NumberFormatException e){
                formulateResponse(NttpCode.VERSION_NOT_SUPPORTED, null);
                return;
            }
        }else if(version.length == 2){
            try {
                major = Integer.valueOf(version[0]);
                minor = Integer.valueOf(version[1]);
                if(major > supportedVersionMajor){
                    formulateResponse(NttpCode.VERSION_NOT_SUPPORTED, null);
                    return;
                }else if(minor < 0 || major < 0){
                    formulateResponse(NttpCode.VERSION_NOT_SUPPORTED, null);
                    return;
                }else if(major == supportedVersionMajor && minor > supportedVersionMinor){
                    formulateResponse(NttpCode.VERSION_NOT_SUPPORTED, null);
                    return;
                }
            }catch (NumberFormatException e){
                formulateResponse(NttpCode.VERSION_NOT_SUPPORTED, null);
                return;
            }
        }else if(version.length != 2 && version.length != 1){
            formulateResponse(NttpCode.VERSION_NOT_SUPPORTED, null);
            return;
        }

        hello = true;
        authorized = false;
        authMethod = "";
        user = "";

        formulateResponse(NttpCode.WELCOME, null);

    }

    private void handleHelp(String[] messageVec) {
        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (messageVec.length == 1) {
            formulateResponse(NttpCode.OK, commands);
            return;
        }


        if (messageVec.length == 2) {
            for (int i = 0; i < commands.length; i++) {
                if (messageVec[1].compareToIgnoreCase(commands[i]) == 0) {
                    formulateResponse(NttpCode.OK, new String[] {
                            commands[i] + ":",
                            helps[i],
                    });
                    return;
                }
            }
        }

        formulateResponse(NttpCode.WRONG_ARGS, null);

    }

    private void handleAuth(String[] messageVec) {
        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if(authorized){
            formulateResponse(NttpCode.ALREADY_AUTHORIZED, null);
            return;
        }

        if(messageVec.length == 1){
            formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[2]});
            return;
        }

        if (messageVec.length >= 2 && messageVec[1].compareToIgnoreCase("--methods") == 0) {

            if (messageVec.length != 2) {
                formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[2]});
                return;
            }

            formulateResponse(NttpCode.AUTH_METHODS, authenticationMethods);
            return;
        }

        if (messageVec.length >= 3 && messageVec[1].compareToIgnoreCase("--request") == 0) {

            if (messageVec.length != 3) {
                formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[2]});
                return;
            }

            if (messageVec[2].compareToIgnoreCase("simple") == 0) {
                authMethod = "simple";
                formulateResponse(NttpCode.GO_AHEAD, null);
                return;
            } else {
                formulateResponse(NttpCode.METHOD_NOT_SUPPORTED, null);
                return;
            }
        }

        if (messageVec.length == 3) {

            if (authMethod.equals("")) {
                formulateResponse(NttpCode.WITHOUT_AUTH_METHOD, new String[] {"Use auth --request to select the authentication method. Use auth --methods to see available authentication methods."});
                return;
            }

            //Ahora vaildo usuario y contraseña según el método
            if (authMethod.compareToIgnoreCase("simple") == 0) {
                if (simpleAuth(messageVec[1], messageVec[2])) {
                    formulateResponse(NttpCode.LOGGED_IN, null);
                    return;
                } else {
                    formulateResponse(NttpCode.INCORRECT_USER_PASS, null);
                    return;
                }
            }

        }

        formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[2]});


    }

    private boolean simpleAuth(String user, String password) {
        String pass = nttpData.getPassword(user);
        if(pass != null && pass.equals(password)){
            this.user = user;
            authorized = true;
            return true;
        }

        return false;
    }

    private void handleQuit(String[] messageVec) {


        if (messageVec.length != 1) {
            formulateResponse(NttpCode.WRONG_ARGS, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.BYE_BYE, null);
            return;
        }

        //TODO: desloguarse bien y terminar la conección  JPM
        user = "";
        authMethod = "";
        authorized = false;
        formulateResponse(NttpCode.BYE_BYE, null);

    }

    private void handleMetrics(String[] messageVec) {
        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.MUST_AUTH, null);
            return;
        }

        if (messageVec.length == 1) {
            String[] metricsRet = {
                    "bytes: " + xmppData.getBytesTransferred(),
                    "acceses: " + xmppData.getAccessesAmount(),
                    "accepted: " + xmppData.getAcceptedAmount(),
            };
            formulateResponse(NttpCode.OK, metricsRet);
            return;
        }

        if (messageVec.length == 2) {
            if (messageVec[1].compareToIgnoreCase("--methods") == 0) {
                formulateResponse(NttpCode.METRIC_METHODS, metrics);
                return;
            } else if (messageVec[1].compareToIgnoreCase("bytes") == 0) {
                formulateResponse(NttpCode.OK, new String[] {"bytes: " + xmppData.getBytesTransferred()});
                return;
            } else if (messageVec[1].compareToIgnoreCase("accesses") == 0) {
                formulateResponse(NttpCode.OK, new String[] {"accesses: " + xmppData.getAccessesAmount()});
                return;
            } else if (messageVec[1].compareToIgnoreCase("accepted") == 0){
                formulateResponse(NttpCode.OK, new String[] {"accepted: " + xmppData.getAcceptedAmount()});
                return;
            }
        }

        formulateResponse(NttpCode.WRONG_ARGS, null);

    }

    private void handleTransformation(String[] messageVec) {
        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.MUST_AUTH, null);
            return;
        }
        if (messageVec.length != 2) {
            formulateResponse(NttpCode.WRONG_ARGS, null);
            return;
        }

        if (messageVec[1].compareToIgnoreCase("true") == 0) {
            xmppData.setTransformation(true);
            formulateResponse(NttpCode.TRANSFORMATION_ENABLED, null);
            return;
        } else if (messageVec[1].compareToIgnoreCase("false") == 0) {
            xmppData.setTransformation(false);
            formulateResponse(NttpCode.TRANSFORMATION_DISABLED, null);
            return;
        }

        formulateResponse(NttpCode.WRONG_ARGS, null);

    }

    private void handleSilence(String[] messageVec) {
        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.MUST_AUTH, null);
            return;
        }

        if (messageVec.length != 2) {
            formulateResponse(NttpCode.WRONG_ARGS, null);
            return;
        }

        if (xmppData.isUserSilenced(messageVec[1])) {
            formulateResponse(NttpCode.USER_ALREADY_SILENCED, null);
            return;
        }

        xmppData.silenceUser(messageVec[1]);
        formulateResponse(NttpCode.USER_SILENCED, null);

    }

    private void handleUnsilence(String[] messageVec) {
        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.MUST_AUTH, null);
            return;
        }

        if (messageVec.length != 2) {
            formulateResponse(NttpCode.WRONG_ARGS, null);
            return;
        }

        if (!xmppData.isUserSilenced(messageVec[1])) {
            formulateResponse(NttpCode.USER_ALREADY_UNSILENCED, null);
            return;
        }

        xmppData.unsilenceUser(messageVec[1]);
        formulateResponse(NttpCode.USER_UNSILENCED, null);
    }

    private void handleGetSilenced(String[] messageVec) {
        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.MUST_AUTH, null);
            return;
        }
        if (messageVec.length != 1) {
            formulateResponse(NttpCode.WRONG_ARGS, null);
            return;
        }

        formulateResponse(NttpCode.OK, xmppData.getUsersSilenced());
    }


    private void handleState(String[] messageVec) {
        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.MUST_AUTH, null);
            return;
        }

        if (messageVec.length != 1) {
            formulateResponse(NttpCode.WRONG_ARGS, null);
            return;
        }
        String[] states = {
                "Transformation: " + (xmppData.isTransformEnabled() ? "enabled" : "disabled"),
                "User: " + user,
        };
        formulateResponse(NttpCode.OK, states);
    }

    private void handleMultiplex(String[] messageVec) {

        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.MUST_AUTH, null);
            return;
        }

        if(messageVec.length != 4){
            formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[10]});
            return;
        }

        String user = messageVec[1];
        String host = messageVec[2];
        int port = 0;

        try{
            port = Integer.valueOf(messageVec[3]);
        }catch (NumberFormatException e){
            formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[10]});
            return;
        }

        if(port < 0 || port > 65535){
            formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[10]});
            return;
        }

        xmppData.setUserAddress(user, new NetAddress(host,port));
        formulateResponse(NttpCode.OK, null);

    }

    private void handleGetMultiplex(String[] messageVec) {

        if(!hello){
            formulateResponse(NttpCode.HELLO_FIRST, null);
            return;
        }

        if (!authorized) {
            formulateResponse(NttpCode.MUST_AUTH, null);
            return;
        }

        if(messageVec.length != 1){
            formulateResponse(NttpCode.WRONG_ARGS, new String[]{helps[11]});
            return;
        }

        formulateResponse(NttpCode.OK, xmppData.getMultiplex());

    }

    private void handleDefault() {
        formulateResponse(NttpCode.WHAT, null);
    }

    private void handleTooBig() {
        formulateResponse(NttpCode.TOO_MUCH_OUTPUT, null);
    }

    private void formulateResponse(NttpCode c, String[] lines) {
        sb.setLength(0);

        if (lines != null && lines.length > 0) {
            sb.append("+" + lines.length + " ");
        }

        sb.append(c.getType()).append(" ").append(c.getCode()).append(" ").append(c.getMessage()).append("\n");

        if (lines != null) {
            for (int i = 0; i < lines.length; i++) {
                sb.append(lines[i]).append("\n");
            }
        }

    }

}
