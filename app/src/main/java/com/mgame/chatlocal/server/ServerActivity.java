package com.mgame.chatlocal.server;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mgame.chatlocal.R;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ServerActivity extends AppCompatActivity implements View.OnClickListener {

    public static void start(Context context) {
        Intent starter = new Intent(context, ServerActivity.class);
        context.startActivity(starter);
    }

    static final int SocketServerPORT = 8080;

    TextView infoIp, infoPort, chatMsg;
    EditText edtMsg;
    ImageView btnSendMsg;
    ScrollView scrollView2;

    String msgLog = "";

    List<ChatClient> userList;

    ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        scrollView2 = findViewById(R.id.scrollView2);
        infoIp = findViewById(R.id.infoIp);
        infoPort = findViewById(R.id.infoPort);
        edtMsg = findViewById(R.id.edtMsg);
        chatMsg = findViewById(R.id.chatMsg);
        btnSendMsg = findViewById(R.id.btnSendMsg);
        infoIp.setText(getIpAddress());
        btnSendMsg.setOnClickListener(this);
        userList = new ArrayList<ChatClient>();

        ChatServerThread chatServerThread = new ChatServerThread();
        chatServerThread.start();
    }

    @Override
    public void onClick(View view) {
        broadcastMsg("Chủ group: " + edtMsg.getText().toString() + "\n");
        msgLog += "Chủ group: " + edtMsg.getText().toString() + "\n";
        chatMsg.setText(msgLog);
        edtMsg.setText("");
        scrollView2.smoothScrollTo(0, scrollView2.getBottom());
    }

    private class ChatServerThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                ServerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoPort.setText("Cổng hiện tại: "
                                + serverSocket.getLocalPort());
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    ChatClient client = new ChatClient();
                    userList.add(client);
                    ConnectThread connectThread = new ConnectThread(client, socket);
                    connectThread.start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    private class ConnectThread extends Thread {

        Socket socket;
        ChatClient connectClient;
        String msgToSend = "";

        ConnectThread(ChatClient client, Socket socket) {
            connectClient = client;
            this.socket = socket;
            client.socket = socket;
            client.chatThread = this;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                String n = dataInputStream.readUTF();

                connectClient.name = n;

                msgLog += connectClient.name + " connected@" +
                        connectClient.socket.getInetAddress() +
                        ":" + connectClient.socket.getPort() + "\n";
                ServerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        chatMsg.setText(msgLog);
                    }
                });

                dataOutputStream.writeUTF("Chào mừng " + n + "\n");
                dataOutputStream.flush();

                broadcastMsg(n + " kết nối phòng chat.\n");

                while (true) {
                    if (dataInputStream.available() > 0) {
                        String newMsg = dataInputStream.readUTF();


                        msgLog += n + ": " + newMsg;
                        ServerActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMsg.setText(msgLog);
                                scrollView2.smoothScrollTo(0, scrollView2.getBottom());
                            }
                        });

                        broadcastMsg(n + ": " + newMsg);
                    }

                    if (!msgToSend.equals("")) {
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        msgToSend = "";
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                userList.remove(connectClient);
                ServerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ServerActivity.this,
                                connectClient.name + " thoát.", Toast.LENGTH_LONG).show();

                        msgLog += "-- " + connectClient.name + " thoát\n";
                        ServerActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMsg.setText(msgLog);
                            }
                        });

                        broadcastMsg("-- " + connectClient.name + " thoát\n");
                    }
                });
            }

        }

        private void sendMsg(String msg) {
            msgToSend = msg;
        }

    }

    private void broadcastMsg(String msg) {
        for (int i = 0; i < userList.size(); i++) {
            userList.get(i).chatThread.sendMsg(msg);
//            msgLog += "- send to " + userList.get(i).name + "\n";
        }

        ServerActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                chatMsg.setText(msgLog);
            }
        });
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "Địa chỉ group: "
                                + inetAddress.getHostAddress();
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    class ChatClient {
        String name;
        Socket socket;
        ConnectThread chatThread;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
