package ro.tuc.auth.services;

import ro.tuc.auth.entities.Role;
import ro.tuc.common.EndpointPaths;

import java.util.UUID;

/**
 * Decides, at the gateway, whether a validated token may call a backend route.
 * Paths are the ones Traefik sees, i.e. still carrying the /users or /devices prefix.
 */
public final class AccessPolicy {

    private AccessPolicy() {
    }

    public static boolean isAllowed(Role role, UUID userId, String method, String uri) {
        if (role == Role.ADMIN) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(method) || uri == null) {
            return false;
        }
        String path = uri.split("\\?", 2)[0];
        String ownProfile = EndpointPaths.GATEWAY_USERS + EndpointPaths.PERSON + "/" + userId;
        String ownDevices = EndpointPaths.GATEWAY_DEVICES + EndpointPaths.DEVICE
                + EndpointPaths.USER_ID.replace("{userId}", userId.toString());
        return path.equals(ownProfile) || path.equals(ownDevices);
    }
}
