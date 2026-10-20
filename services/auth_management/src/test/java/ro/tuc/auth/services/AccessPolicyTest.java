package ro.tuc.auth.services;

import org.junit.jupiter.api.Test;
import ro.tuc.auth.entities.Role;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessPolicyTest {

    private final UUID me = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void adminCanDoAnything() {
        assertTrue(AccessPolicy.isAllowed(Role.ADMIN, me, "DELETE", "/users/person/" + other));
        assertTrue(AccessPolicy.isAllowed(Role.ADMIN, me, "PUT", "/devices/device/" + other));
    }

    @Test
    void clientCanReadOnlyOwnDevicesAndProfile() {
        assertTrue(AccessPolicy.isAllowed(Role.CLIENT, me, "GET", "/devices/device/user/" + me));
        assertTrue(AccessPolicy.isAllowed(Role.CLIENT, me, "GET", "/users/person/" + me + "?x=1"));

        assertFalse(AccessPolicy.isAllowed(Role.CLIENT, me, "GET", "/devices/device/user/" + other));
        assertFalse(AccessPolicy.isAllowed(Role.CLIENT, me, "GET", "/devices/device"));
        assertFalse(AccessPolicy.isAllowed(Role.CLIENT, me, "GET", "/users/person"));
        assertFalse(AccessPolicy.isAllowed(Role.CLIENT, me, "DELETE", "/devices/device/user/" + me));
        assertFalse(AccessPolicy.isAllowed(Role.CLIENT, me, "GET", null));
    }
}
