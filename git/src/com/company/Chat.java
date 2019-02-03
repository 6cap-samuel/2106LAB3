package com.company;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;

public class Chat {

    private String username;
    private String groupName;
    private HashMap<String, String> userGroupList;
    private MulticastSocket multicastSocket;
    InetAddress multicastGroup = null;

    private int portNo;

    private Stage stage;

    private Label usernameLabel;
    private Label createGroupLabel;
    private Label joinGroupLabel;
    private Label messageLabel;

    private Label error;

    private Button updateButton;
    private Button createButton;
    private Button joinButton;
    private Button sendButton;

    private TextField usernameTextField;
    private TextField createGroupTextField;
    private TextField joinGroupTextField;
    private TextField messageTextField;

    private GridPane parentGridPane;
    private GridPane inputGridPane;
    private GridPane messageGridPane;

    private TextArea chatlog;


    public Chat(Stage stage){
        portNo = 6789;
        userGroupList = new HashMap<String,String>();

        this.stage = stage;

        usernameLabel = new Label("Username");
        createGroupLabel = new Label("Create Group");
        joinGroupLabel = new Label("Join Group");
        messageLabel = new Label("Message");

        updateButton = new Button("Update");
        createButton = new Button("Create");
        joinButton = new Button("Join");
        sendButton = new Button("Send");

        usernameTextField = new TextField();
        createGroupTextField = new TextField();
        joinGroupTextField = new TextField();
        messageTextField = new TextField();

        parentGridPane = new GridPane();
        inputGridPane = new GridPane();
        messageGridPane = new GridPane();

        error = new Label();

        chatlog = new TextArea();

        setup();
        addButtonListener();
    }

    public void setupInputGridPane(){
        inputGridPane.add(usernameLabel, 0, 0);
        inputGridPane.add(usernameTextField, 1, 0);
        inputGridPane.add(updateButton, 2, 0);

        inputGridPane.add(createGroupLabel, 0, 1);
        inputGridPane.add(createGroupTextField, 1, 1);
        inputGridPane.add(createButton, 2, 1);

        inputGridPane.add(joinGroupLabel, 0, 2);
        inputGridPane.add(joinGroupTextField, 1, 2);
        inputGridPane.add(joinButton, 2, 2);

        inputGridPane.add(error, 0, 3);

        this.parentGridPane.add(inputGridPane, 0, 0);
    }

    public void setupMessagePane(){
        messageGridPane.add(messageLabel,0,0);
        messageGridPane.add(messageTextField ,1, 0);
        messageGridPane.add(sendButton,2,0);
        this.parentGridPane.add(messageGridPane, 0, 2);
    }

    public void setup(){
        try{
            multicastGroup = InetAddress.getByName("228.5.6.7");
            multicastSocket = new MulticastSocket(portNo);

            //Join
            multicastSocket.joinGroup(multicastGroup);
        } catch (IOException ex){
            ex.printStackTrace();
        }

        setupInputGridPane();
        this.parentGridPane.add(chatlog, 0,1);
        setupMessagePane();

        this.stage.setScene(new Scene(parentGridPane, 600, 300));
        this.stage.centerOnScreen();
        this.stage.show();

        listenForMessage();
    }

    public void sendMessage(String message, String username, String group) throws IOException{
        StringBuilder sb = new StringBuilder();
        sb.append("send");
        sb.append(";");
        sb.append(group);
        sb.append(";");
        sb.append(username);
        sb.append(";");
        sb.append(message);

        byte[] buf = sb.toString().getBytes();
        DatagramPacket datagram = new DatagramPacket(buf, buf.length, multicastGroup, portNo);
        multicastSocket.send(datagram);
    }

    public void joinGroup(String groupName){

        this.groupName = groupName;

        if (userGroupList.containsKey(groupName)){
            StringBuilder sb = new StringBuilder();
            sb.append("join");
            sb.append(";");
            sb.append(groupName);

            try {
                byte[] buf = sb.toString().getBytes();
                DatagramPacket datagram = new DatagramPacket(buf, buf.length, multicastGroup, portNo);
                multicastSocket.send(datagram);
            } catch (IOException ip){
                ip.printStackTrace();
            }
        }
    }

    public void createGroup(String groupName){

        this.groupName = groupName;

        if (userGroupList.containsKey(groupName)){
            error.setText("Group exist in network");
        } else{
            StringBuilder sb = new StringBuilder();
            sb.append("create;");
            sb.append(groupName);

            try {
                byte[] buf = sb.toString().getBytes();
                DatagramPacket datagram = new DatagramPacket(buf, buf.length, multicastGroup, portNo);
                multicastSocket.send(datagram);
            } catch (IOException io){
                io.printStackTrace();
            }
        }
    }

    public void listenForMessage(){
        System.out.println("testing");

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte buf1[] = new byte[1000];
                DatagramPacket dataReceive = new DatagramPacket(buf1, buf1.length);

                // Polling for new message
                while (true){
                    try {
                        multicastSocket.receive(dataReceive);
                        byte[] receivedData = dataReceive.getData();
                        int length = dataReceive.getLength();

                        String msg = new String(receivedData, 0, length);
                        String[] splitString = msg.split(";");

                        if (splitString[0].equals("create")){
                            userGroupList.put(splitString[1], "1");
                        } else if (splitString[0].equals("send")) {
                            if (groupName.equals(splitString[1])){
                                chatlog.appendText(splitString[2] + ": " +  splitString[3] + "\n");
                            }
                        } else if (splitString[0].equals("join")){
                            userGroupList.put(splitString[1], String.valueOf(Integer.parseInt(userGroupList.get(splitString[1])) + 1));
                        }

                        System.out.println(userGroupList.toString());

                    } catch (IOException io){
                        io.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void addButtonListener(){

        this.joinButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                joinGroup(joinGroupTextField.getText());
            }
        });

        this.updateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                username = usernameTextField.getText();
            }
        });

        this.createButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                createGroup(createGroupTextField.getText());
            }
        });

        this.sendButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try{
                    if (username.equals("")){
                        error.setText("Please enter your username");
                    } else if (groupName.equals("")){
                        error.setText("Please create/join group");
                    } else {
                        sendMessage(messageTextField.getText(), username, groupName);
                    }
                } catch (IOException io){
                    io.printStackTrace();
                }
            }
        });
    }
}
