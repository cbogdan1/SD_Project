package ro.tuc.common;

public final class EndpointPaths {

    public static final String ROOT = "/";
    public static final String ID = "/{id}";
    public static final String USER_ID = "/user/{userId}";

    public static final String PERSON = "/person";
    public static final String DEVICE = "/device";

    public static final String LOGIN = "/login";
    public static final String REGISTER = "/register";
    public static final String VALIDATE = "/validate";
    public static final String CREDENTIALS = "/credentials";

    // Prefixes added by Traefik in front of each service (stripped before the request reaches it)
    public static final String GATEWAY_USERS = "/users";
    public static final String GATEWAY_DEVICES = "/devices";

    private EndpointPaths() {
    }
}
