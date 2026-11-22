package ro.tuc.auth.services;

import ro.tuc.auth.entities.Role;
import ro.tuc.common.EndpointPaths;

import java.util.UUID;

/**
 * Decides, at the gateway, whether a validated token may call a backend route.
 * Paths are the ones Traefik sees, i.e. still carrying the /users, /devices or /monitoring prefix.
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
        String ownUserPath = EndpointPaths.USER_ID.replace("{userId}", userId.toString());
        String ownProfile = EndpointPaths.GATEWAY_USERS + EndpointPaths.PERSON + "/" + userId;
        String ownDevices = EndpointPaths.GATEWAY_DEVICES + EndpointPaths.DEVICE + ownUserPath;
        // consumption of the client's own devices (?deviceId=...&date=...); monitoring-service checks the device owner
        String ownConsumption = EndpointPaths.GATEWAY_MONITORING + EndpointPaths.CONSUMPTION + ownUserPath;
        return path.equals(ownProfile) || path.equals(ownDevices) || path.equals(ownConsumption);
    }
}
