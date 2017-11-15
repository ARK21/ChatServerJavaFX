package sample;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerModel {

    static int uniqueId;
    static ArrayList<ServerController.ClientThread> allClients;
    private static ArrayList<String> serverCommands = new ArrayList<>();
    private int port;
    boolean isServerRun;
    ServerSocket sc;

    //Server commands are here
    static {
        serverCommands.add("Enter command: ");
        serverCommands.add("-exit - to close the server connection.");
        serverCommands.add("-cmd - to show all possible commands.");
        serverCommands.add("-wio - to show all online clients.");
    }

    //Constructor
    public ServerModel(int port) {
        this.port = port;
        allClients = new ArrayList<>();
    }

    /*
     * For the GUI to stop the server
     */
    public void stop() {
        isServerRun = false;
        // connect to myself as Client to exit statement
        // Socket socket = serverSocket.accept();
        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {
            // nothing I can really do
        }
    }


    /**
     * Show all commands which you can use in console version of chat
     */
    public static void getServerCommands() {
        for (String command : serverCommands) {
            System.out.println(command);
        }
    }


    /**
     * Show who is online now
     */
    public static String whoIsOnline() {
        String message = "Online: " + "\n";
        for (int i = 0; i < allClients.size() ; i++) {
            message += i+1 + ". " + allClients.get(i).username;
            if (i + 1 < allClients.size()) {
                message += "\n";
            }
        }
        return message;
    }

    public int getPort() {
        return port;
    }

}
