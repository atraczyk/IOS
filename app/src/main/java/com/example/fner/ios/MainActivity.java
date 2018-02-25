package com.example.fner.ios;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity
{
    EditText editTextAddress, editTextPort;
    Button buttonConnect;
    TextView textViewState, textViewRx;

    TcpClientHandler tcpClientHandler;
    TcpClientThread tcpClientThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        buttonConnect = (Button) findViewById(R.id.connect);
        textViewRx = (TextView)findViewById(R.id.received);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);

        tcpClientHandler = new TcpClientHandler(this);
    }

    View.OnClickListener buttonConnectOnClickListener =
            new View.OnClickListener() {


                @Override
                public void onClick(View arg0) {
                    if (tcpClientThread == null) {
                        connect();
                        return;
                    }
                    switch (tcpClientThread.currentState){
                        case DISCONNECTED:
                            connect();
                            break;
                        case CONNECTING:
                        case CONNECTED:
                            disconnect();
                            break;
                        default:
                            break;
                    }
                }
            };

    private void connect() {
        tcpClientThread = new TcpClientThread(
                editTextAddress.getText().toString(),
                Integer.parseInt(editTextPort.getText().toString()),
                tcpClientHandler);
        buttonConnect.setEnabled(false);
        tcpClientThread.start();
    }

    private void disconnect() {
        buttonConnect.setEnabled(false);
        tcpClientThread.disconnect();
    }

    private void updateState() {
        if (tcpClientThread == null) {
            clientReset();
            return;
        }
        switch (tcpClientThread.currentState){
            case DISCONNECTED:
                clientReset();
                break;
            case CONNECTING:
                buttonConnect.setEnabled(true);
                buttonConnect.setText("STOP");
                break;
            case CONNECTED:
                buttonConnect.setEnabled(true);
                buttonConnect.setText("DISCONNECT");
                break;
            default:
                break;
        }
    }

    private void updateRxMsg(String rxmsg) {
        textViewRx.append(rxmsg + "\n");
    }

    private void clientReset(){
        tcpClientThread = null;
        buttonConnect.setText("CONNECT");
        buttonConnect.setEnabled(true);
    }

    public static class TcpClientHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        private MainActivity parent;

        public TcpClientHandler(MainActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_STATE:
                    parent.updateState();
                    break;
                case UPDATE_MSG:
                    parent.updateRxMsg((String)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}