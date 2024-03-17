package server;

import java.util.ArrayList;
import java.util.List;

public class User {

    private final String username;
    private final String password;

    public User(String name, String password) {
        this.username = name;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String toString() {
        return this.username + "," + this.password;
    }
}
