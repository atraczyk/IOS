package com.example.fner.ios;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class InputActivity extends AppCompatActivity {
    TcpClientHandler tcpClientHandler;

    Button buttonSend;
    EditText edit;

    boolean lastLCState;

    int lastCursorX;
    int lastCursorY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        tcpClientHandler = new InputActivity.TcpClientHandler(this);

        ConnectionManager.getInstance().tcpClientThread.inputHandler = tcpClientHandler;

        buttonSend = (Button) findViewById(R.id.send);
        buttonSend.setOnClickListener(buttonSendOnClickListener);

        edit = (EditText) findViewById(R.id.hiddenEdit);

        lastLCState = false;

        lastCursorX = 0;
        lastCursorY = 0;
    }

    View.OnClickListener buttonSendOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (edit.getText().length() > 0) {
                        sendData("k:" + edit.getText().toString());
                        edit.setText("");
                    }
//                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
//                            .showSoftInput(edit, InputMethodManager.SHOW_FORCED);
//                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
//                            .toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            };

    @Override
    public void onBackPressed() {
        disconnect();
        Log.e("InputActivity", "DISCONNECT");
        Toast.makeText( InputActivity.this,
                "Disconnected",
                Toast.LENGTH_LONG)
                .show();
        finish();
    }

    private void disconnect() {
        if (ConnectionManager.getInstance().tcpClientThread != null) {
            ConnectionManager.getInstance().tcpClientThread.disconnect();
            ConnectionManager.getInstance().tcpClientThread = null;
        }
    }

    private void sendData(String data) {
        if (ConnectionManager.getInstance().tcpClientThread != null) {
            ConnectionManager.getInstance().tcpClientThread.sendData(data);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean lcState = lastLCState;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(edit.getWindowToken(), 0);
                edit.setText("");
                lastCursorX = x;
                lastCursorY = y;
                if (event.getPointerCount() == 1 && event.getPressure(0) > 0.24) {
                    //Log.e("InputActivity", "PRESSURE:" + event.getPressure(0));
                    lcState = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                lcState = false;
                break;
        }

        int dx = x - lastCursorX;
        int dy = y - lastCursorY;

        boolean leftClickChange = lastLCState != lcState;

        lastLCState = lcState;

        lastCursorX = x;
        lastCursorY = y;

        if (dx != 0 || dy != 0 || leftClickChange) {
            String data = "";
            if (Math.abs(dx + dy) < 10 && leftClickChange) {
                data += lcState ? "ld" : "lu";
            } else {
                data += "m:" + String.valueOf(dx) + "," + String.valueOf(dy);
            }
            sendData(data);
        }

        return false;
    }

    public static class TcpClientHandler extends Handler {
        public static final int UPDATE_END = 0;
        private InputActivity parent;

        public TcpClientHandler(InputActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_END:
                    parent.disconnect();
                    parent.finish();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
