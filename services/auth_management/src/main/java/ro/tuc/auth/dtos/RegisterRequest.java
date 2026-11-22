package ro.tuc.auth.dtos;

import ro.tuc.auth.entities.Role;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

// Credentials plus the profile, which travels to user-service inside the USER_CREATED event.
// The profile is validated here, because user-service has no chance to reject an event.
public class RegisterRequest {

    @NotBlank
    private String username;
    @NotBlank
    @Size(min = 4)
    private String password;
    private Role role;

    @NotBlank
    private String name;
    @NotBlank
    private String address;
    // a missing age is 0, so it is rejected too
    @Min(value = 18, message = "must be at least 18")
    private int age;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
