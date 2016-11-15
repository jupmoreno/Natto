package ar.edu.itba.pdc.natto.protocol.xmpp;

public enum XmppErrors {
    // ERRORS
    INVALID_NAMESPACE("<invalid-namespace xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    BAD_FORMAT("<bad-format xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    HOST_UNKNOWN("<host-unknown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    BAD_NAMESPACE_PREFIX("<bad-namespace-prefix xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    //    HOST_GONE("<host-gone xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"),
    INTERNAL_SERVER("<internal-server-error xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    NOT_AUTHORIZED("<not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    REMOTE_CONNECTION_FAILED("<remote-connection-failed xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    //    RESOURCE_CONSTRAIN("<resource-constraint xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>"),
    RESTRICTED_XML("<restricted-xml xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    SYSTEM_SHUTDOWN("<system-shutdown xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    UNSUPPORTED_ENCODING("<unsupported-encoding xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    UNSUPPORTED_VERSION("<unsupported-version xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    INVALID_XML("<invalid-xml xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    CONFLICT("<conflict xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),
    UNSOPPORTED_STANZA_TYPE("<unsupported-stanza-type xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>", true),

    // FAILURES
    MALFORMED_REQUEST("<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><malformed-request/></failure>", false),
    INVALID_MECHANISM("<invalid-mechanism/>", false),
    INCORRECT_ENCODING("<incorrect-encoding/>", false);

    private final byte[] bytes;
    private final boolean shouldClose;

    XmppErrors(String message, boolean shouldClose) {
        if (shouldClose) {
            message = "<stream:error>" + message + "</stream:error>";
        } else {
            message = "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" + message + "</failure>";
        }

        this.bytes = message.getBytes();
        this.shouldClose = shouldClose;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean shouldClose() {
        return shouldClose;
    }
}
