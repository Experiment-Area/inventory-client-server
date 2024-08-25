package lk.ijse.dep12.server;

import lk.ijse.dep12.shared.to.Item;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerAppInitializer {
    private static final List<Socket> CLIENT_LIST = new CopyOnWriteArrayList<>();
    private static final File DB_FILE = new File("inventory.db");

    public static void main(String[] args) throws IOException {

        DB_FILE.createNewFile();

        try (ServerSocket serverSocket = new ServerSocket(5050)) {
            System.out.println("Server is started");

            while (true) {
                Socket locatSocket = serverSocket.accept();
                CLIENT_LIST.add(locatSocket);
                sendCurrentItemList(locatSocket);

                new Thread(() -> {
                    try {
                        InputStream is = locatSocket.getInputStream();
                        ObjectInputStream ois = new ObjectInputStream(is);

                        while (true) {
                            List<Item> itemList = (List<Item>) ois.readObject();
                            try (FileOutputStream fos = new FileOutputStream(DB_FILE);
                                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                                oos.writeObject(itemList);
                            }
                            broadcastItemList(locatSocket);
                        }

                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        }
    }

    private static void sendCurrentItemList(Socket client) throws IOException {

        if (DB_FILE.length()== 0) {
            new ObjectOutputStream(client.getOutputStream()).writeObject(new ArrayList<Item>());
            return;
        }
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DB_FILE))) {

            List<Item> itemList = (List<Item>) ois.readObject();
            new ObjectOutputStream(client.getOutputStream()).writeObject(itemList);

        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void broadcastItemList(Socket socket) {

        new Thread(() -> {

            for (Socket client : CLIENT_LIST) {
                if (client.isConnected()) {
                    try (FileInputStream fis = new FileInputStream(DB_FILE);
                         ObjectInputStream ois = new ObjectInputStream(fis)) {
                        var oos = new ObjectOutputStream(client.getOutputStream());

                        List<Item> itemList = (List<Item>) ois.readObject();
                        oos.writeObject(itemList);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
}
