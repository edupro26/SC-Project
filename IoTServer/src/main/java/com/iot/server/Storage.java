package com.iot.server;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Storage {
    public static final String USERS_FILE = "src/main/resources/login_data.csv";

    public static User findUser(String name) {
        Scanner scanner = new Scanner(USERS_FILE);

        while (scanner.hasNextLine()) {
            String[] parts = scanner.nextLine().split(",");
            if (parts[0].equals(name)) {
                return new User(parts[0], parts[1]);
            }
        }

        return null;
    }

    public static void saveUser(User users) {
        try {
            FileWriter fileWriter = new FileWriter(USERS_FILE, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(users);
            printWriter.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

}
