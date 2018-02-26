package com.example.fner.ios;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    EditText editTextAddress, editTextPort;
    Button buttonConnect;
    TextView textViewStatus, textViewRx;

    TcpClientHandler tcpClientHandler;
    TcpClientThread tcpClientThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        buttonConnect = (Button) findViewById(R.id.connect);
        textViewStatus = (TextView)findViewById(R.id.status);
        textViewRx = (TextView)findViewById(R.id.received);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
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
        tcpClientHandler = new TcpClientHandler(this);
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
        tcpClientThread = null;
    }

    private void updateState() {
        if (tcpClientThread == null) {
            clientReset();
            textViewStatus.setText("Disconnected");
            return;
        }
        switch (tcpClientThread.currentState){
            case DISCONNECTED:
                clientReset();
                textViewStatus.setText("Disconnected");
                break;
            case CONNECTING:
                buttonConnect.setEnabled(true);
                buttonConnect.setText("STOP");
                textViewStatus.setText("Connecting…");
                break;
            case CONNECTED:
                Log.e("MainActiv", "CONNECTED");
                Toast.makeText( MainActivity.this,
                                "Connected to " + editTextAddress.getText(),
                                Toast.LENGTH_LONG)
                        .show();
                Intent intent = new Intent( MainActivity.this, InputActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                buttonConnect.setEnabled(true);
                buttonConnect.setText("DISCONNECT");
                textViewStatus.setText("Connected");
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
            Log.e("MainActiv", "handleMessage: " + this.hashCode() );
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