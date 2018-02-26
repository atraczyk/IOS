package com.example.fner.ios;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

enum ConnectionState {
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected");

    private String stringValue;
    private int intValue;
    private ConnectionState(String toString) {
        stringValue = toString;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}

public class TcpClientThread extends Thread {

    //public static final int TIMEOUTMS = 10000;

    String dstAddress;
    int dstPort;
    private boolean running;
    MainActivity.TcpClientHandler handler;
    String serverMessage;
    Socket socket;

    private PrintWriter bufferOut;
    private BufferedReader bufferIn;

    ConnectionState currentState;

    public TcpClientThread(String addr, int port, MainActivity.TcpClientHandler handler) {
        super();
        dstAddress = addr;
        dstPort = port;
        this.handler = handler;
        currentState = ConnectionState.DISCONNECTED;

        socket = new Socket();
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TCP", "C: Error closing socket", e);
        }
    }

    private void updateState(ConnectionState state) {
        currentState = state;
        Log.e("TCP Client", "C: " + state.toString());
        handler.sendMessage(
                Message.obtain(handler, MainActivity.TcpClientHandler.UPDATE_STATE, null));
    }

    public void stopClient() {
        Log.e("TCP Client", "C: Stopping");
        updateState(ConnectionState.DISCONNECTED);

        if (bufferOut != null) {
            bufferOut.flush();
            bufferOut.close();
        }

        bufferIn = null;
        bufferOut = null;
        serverMessage = null;
    }

    public void sendData(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (bufferOut != null) {
                    Log.e("TCP Client", "Sending: " + message);
                    bufferOut.println(message + "\0");
                    bufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @Override
    public void run() {
        updateState(ConnectionState.CONNECTING);
        setRunning(true);

        try {
            InetAddress address = InetAddress.getByName(dstAddress);
            socket.connect(new InetSocketAddress(address, dstPort));

            try {
                updateState(ConnectionState.CONNECTED);

                bufferOut =
                        new PrintWriter(
                            new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream())),true);

                bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (running) {
                    serverMessage = bufferIn.readLine();

                    if (serverMessage != null && handler != null) {
                        handler.sendMessage(
                                Message.obtain(handler, MainActivity.TcpClientHandler.UPDATE_MSG, serverMessage));
                        Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
                    }
                    serverMessage = null;
                }
                socket.close();
                Log.e("TCP Client", "C: Normal shutdown");
                stopClient();

            } catch (SocketException e) {
                e.printStackTrace();
                Log.e("TCP", "S: Error", e);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e("TCP", "S: Error", e);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TCP", "S: Error", e);
            } finally {
                socket.close();
                stopClient();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TCP", "C: Error", e);
            Log.e("TCP Client", "C: Socket closed shutdown");
            stopClient();
        }
    }
}