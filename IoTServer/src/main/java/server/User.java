package server;

import java.util.ArrayList;
import java.util.List;

public class User {

    private final String username;
    private String password;

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

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof User user)) {
            return false;
        }
        return user.getUsername().equals(this.username);
    }

    public String toString() {
        return this.username + "," + this.password;
    }

}
