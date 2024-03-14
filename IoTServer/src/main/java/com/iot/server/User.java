//package com.iot.server;
//
//import java.util.List;
//
//public class User {
//    private String name;
//
//    private String password;
//
//    public User(String name, String password) {
//        this.name = name;
//        this.password = password;
//
//        Storage.saveUser(this);
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//
//    public static User findUser(String name) {
//        return Storage.findUser(name);
//
//    }
//
//    public String toString() {
//        return this.name + ":" + this.password;
//    }
//}
