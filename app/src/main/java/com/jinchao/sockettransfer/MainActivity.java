package com.jinchao.sockettransfer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PORT = 5000;

    private TextView message;
    private EditText edit_ip;
    private EditText edit_send;

    private Server server;
    private Client client;
    private boolean isServer = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        message = findViewById(R.id.message);
        Button btn_server = findViewById(R.id.btn_server);
        Button btn_client = findViewById(R.id.btn_client);
        Button btn_send = findViewById(R.id.btn_send);
        edit_ip = findViewById(R.id.edit_ip);
        edit_send = findViewById(R.id.edit_send);

        server = new Server();
        client = new Client();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        btn_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                server.start();
                isServer = true;
            }
        });

        btn_client.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.start();
                isServer = false;
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isServer) {
                            server.send(edit_send.getText().toString());
                        } else {
                            client.send(edit_send.getText().toString());
                        }
                    }
                });

            }
        });
    }

    class Server extends Thread {

        private boolean forceStop = false;
        ServerSocket serverSocket;
        Socket socket;
        OutputStream outputStream;
        InputStream inputStream;

        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress(PORT));
                serverSocket.setReuseAddress(true);
                socket = serverSocket.accept();

                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                while (!forceStop) {
                    byte[] len = new byte[4];
                    int i = inputStream.read(len);
                    Log.d(TAG, "i: " + i);

                    int msgLen = byte2int(len);

                    byte[] msg = new byte[msgLen];
                    int j = inputStream.read(msg);

                    if (msgLen != j) {
                        Log.d(TAG, "msg length error");
                    } else {
                        final String message = new String(msg, StandardCharsets.UTF_8);
                        Log.d(TAG, "msg: " + message);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.message.setText(message);

                            }
                        });
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void send(String msg) {
            byte[] msgData = msg.getBytes(StandardCharsets.UTF_8);
            byte[] msgLength = int2byte(msgData.length);

            try {
                outputStream.write(msgLength);
                outputStream.write(msgData);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Client extends Thread {

        private boolean forceStop = false;
        Socket socket;
        OutputStream outputStream;
        InputStream inputStream;

        @Override
        public void run() {
            super.run();
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(edit_ip.getText().toString(), PORT));
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                while (!forceStop) {
                    byte[] len = new byte[4];
                    int i = inputStream.read(len);
                    Log.d(TAG, "i: " + i);

                    int msgLen = byte2int(len);

                    final byte[] msg = new byte[msgLen];
                    int j = inputStream.read(msg);

                    if (msgLen != j) {
                        Log.d(TAG, "msg length error");
                    } else {
                        final String message = new String(msg, StandardCharsets.UTF_8);
                        Log.d(TAG, "msg: " + message);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.message.setText(message);

                            }
                        });
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void send(String msg) {
            byte[] msgData = msg.getBytes(StandardCharsets.UTF_8);
            byte[] msgLength = int2byte(msgData.length);

            try {
                outputStream.write(msgLength);
                outputStream.write(msgData);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private int byte2int(byte[] bytes) {
        return (((bytes[0] << 24) >>> 24) << 24)
                | (((bytes[1] << 24) >>> 24) << 16)
                | (((bytes[2] << 24) >>> 24) << 8)
                | ((bytes[3] << 24) >>> 24);
    }

    private byte[] int2byte(int i) {
        byte[] res = new byte[4];
        res[3] = (byte) i;
        res[2] = (byte) (i >>> 8);
        res[1] = (byte) (i >>> 16);
        res[0] = (byte) (i >>> 24);
        return res;
    }
}
