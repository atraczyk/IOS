package com.example.fner.ios;

public class ConnectionManager {
    TcpClientThread tcpClientThread;

    private static final ConnectionManager ourInstance = new ConnectionManager();

    static ConnectionManager getInstance() {
        return ourInstance;
    }

    private ConnectionManager() {
    }
}
