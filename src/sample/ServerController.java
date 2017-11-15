package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerController {

    public TextArea serverArea;
    public TextArea clientArea;
    public TextField portField;
    private ServerModel sm;
    private boolean isServerRun;

    @FXML
    private Button startAndStop;

    public void startAndStopServer(ActionEvent actionEvent) {
        if (sm == null) {
            if (startServer()) {
                startAndStop.setStyle("-fx-background-color: #333232;" +
                        "-fx-border-color: red;");
                portField.setDisable(true);
                startAndStop.setText("Остановить сервер");
                runningServer();
            }
        } else stopServer();
    }

    public boolean startServer() {
        sm = new ServerModel(Integer.parseInt(portField.getText().trim()));
        serverArea.clear();
        try {
            sm.sc = new ServerSocket(sm.getPort());
        } catch (IOException e) {
            appendToServerArea("Не удалось запустить сервер. Возможно выбранный порт занят");
            sm = null;
            return false;
        }
        return true;
    }

    public void stopServer() {
        System.out.println("allClients size: "  + ServerModel.allClients.size());
        try {
            for (ClientThread ct : ServerModel.allClients) {
                try {
                    ct.in.close();
                    ct.out.close();
                    ct.socket.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
            sm.sc.close();
            isServerRun = false;
            Thread.currentThread().interrupt();
            sm = null;
            portField.setDisable(false);
            startAndStop.setStyle("-fx-background-color: #1d1d1d;" +
                    "-fx-border-color: white;");
            startAndStop.setText("Запустить сервер");
            appendToServerArea("Сервер остановлен");
        } catch (IOException e) {
            appendToClientArea("Exception closing the server and clients: " + e);
        }
    }

    public void runningServer() {

       new Thread(() -> {
            try {
                // create socket server and wait for connection requests
                isServerRun = true;
                while (isServerRun) {
                    appendToServerArea("Server waiting for client...");
                    Socket socket = sm.sc.accept(); // accept connection
                    if (!isServerRun) {
                        break;
                    }
                    ClientThread newClient = new ClientThread(socket);
                    ServerModel.allClients.add(newClient);
                    newClient.start();
                }
            } catch (IOException e) {
                appendToClientArea("Exception on new ServerSocket: " + e);
            }

        }).start();
    }

    private void appendToServerArea(String msg) {
        serverArea.appendText(msg + "\n");
    }

    private void appendToClientArea(String msg) {
        clientArea.appendText(msg + "\n");
    }

    public class ClientThread extends Thread {

        ObjectInputStream in;
        ObjectOutputStream out;
        Socket socket;
        String username;
        int id;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        ClientThread(Socket socket) {
            id = ++ServerModel.uniqueId;
            this.socket = socket;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                Message msg = (Message) in.readObject();
                username = msg.getText().trim();
                appendToServerArea(username + " just connected");
                broadcast(username + " just connected");
            } catch (IOException e) {
                // display("Exception in input/output streams");
            } catch (ClassNotFoundException e) {
                // do nothing
            }
        }

        @Override
        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                Message message;
                try {
                    message = (Message) in.readObject();
                } catch (IOException e) {
                    appendToClientArea("Exception reading stream: " + e);
                    break;
                } catch (ClassNotFoundException e) {
                    appendToClientArea("Exception reading stream: " + e);
                    break;
                }
                String ms;
                switch (message.getType()) {
                    case Message.EXIT:
                        ms = username + " just disconnected";
                        broadcast(ms);
                        keepGoing = false;
                        break;
                    case Message.MESSAGE:
                        ms = username + ": " + message.getText();
                        broadcast(ms);
                        break;
                    case Message.WHOISONLINE:
                        writeMsg(ServerModel.whoIsOnline());
                        break;
                }
            }
            remove(id);
            close();
        }

        /*
        send messages to each clients in ArrayList<ClientThread> allClients
         */
        public synchronized void broadcast(String message) {
            String time = sdf.format(new Date());
            String preparedMessage = time + " " + message;
            appendToClientArea(preparedMessage);
            for (int i = ServerModel.allClients.size(); --i >= 0; ) {
                ClientThread ct = ServerModel.allClients.get(i);
                // try to write to the Client if it fails remove it from the list
                if (!ct.writeMsg(preparedMessage)) {
                    ServerModel.allClients.remove(i);
                    appendToClientArea("Disconnected Client " + username + " removed from list.");
                }
            }
        }

        /*
         close ObjectInputStream in, ObjectOutputStream out and socket
        */
        public void close() {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                appendToClientArea("Ошибка при закрытии выходного потока клиента");
                e.printStackTrace();
            }
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                appendToClientArea("Ошибка при закрытии входного потока клиента");
                e.printStackTrace();
            }
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                appendToClientArea("Ошибка при закрытии сокета клиента");
                e.printStackTrace();
            }
        }

        /*
        remove client who logoff using the LOGOUT message from ArrayList<ClientThread> allClients
         */
        public synchronized void remove(int id) {
            // try ti find the Id in array list
            for (int i = 0; i < ServerModel.allClients.size(); ++i) {
                ClientThread ct = ServerModel.allClients.get(i);
                // if found - remove it
                if (ct.id == id) {
                    ServerModel.allClients.remove(i);
                    return;
                }
            }
        }

        public boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write a message to the stream
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                appendToClientArea("Error sending message to " + username);
                appendToClientArea(e.toString());
            }
            return true;
        }
    }

}
