package ro.tuc.ds2020.entities;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.UUID;

@Entity
public class Person  implements Serializable{

    private static final long serialVersionUID = 1L;

    // same id as the login in auth-service (it comes in the USER_CREATED event), so it is not generated here
    @Id
    @Type(type = "uuid-binary")
    // a UUID is 16 bytes; without a length the H2 test DB would make a zero-padded binary(255) column
    @Column(length = 16)
    private UUID id;

    // same username as the login in auth-service
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "age", nullable = false)
    private int age;


    public Person() {
    }

    public Person(UUID id, String username, String name, String address, int age) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.address = address;
        this.age = age;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
