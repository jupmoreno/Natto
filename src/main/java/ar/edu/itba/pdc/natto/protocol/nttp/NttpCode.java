package ar.edu.itba.pdc.natto.protocol.nttp;


public enum NttpCode {

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
    WRONG_ARGS('!', 0, "Wrong arguments."),
    METHOD_NOT_SUPPORTED('!', 1, "Authentication method not supported."),
    ALREADY_AUTHORIZED('!', 2, "Already authorized."),
    WITHOUT_AUTH_METHOD('!', 3, "Authorization method not requested."),
    INCORRECT_USER_PASS('!', 4, "Incorrect user or password."),
    USER_ALREADY_SILENCED('!', 5, "This user is already silenced."),
    USER_ALREADY_UNSILENCED('!', 6, "This user was not silenced."),
    MUST_AUTH('!', 7, "You need to authenticate."),
    TOO_MUCH_OUTPUT('X', 0, "Too much output to process.")
    ;

    private char type;
    private int code;
    private String message;

    NttpCode(char type, int code, String message){
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
