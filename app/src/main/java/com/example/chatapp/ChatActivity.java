package com.example.chatapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    MessageDataPost msg = new MessageDataPost(this);
    private Button buttonProfile;
    private Button buttonLogout;
    private Button send , buttonLobby;
    private WebSocket webSocket;
    private MessageAdapter adapter;
    private EditText messageBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ListView messageList = findViewById(R.id.messageList);
        messageBox = findViewById(R.id.messageBox);
        send = findViewById(R.id.buttonSend);
        buttonProfile = findViewById(R.id.buttonProfile);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonLobby = findViewById(R.id.buttonLobby);
        buttonLobby.setOnClickListener(this);
        buttonProfile.setOnClickListener(this);
        buttonLogout.setOnClickListener(this);
        send.setOnClickListener(this);
        instantiateWebSocket();
        adapter = new MessageAdapter();
        messageList.setAdapter(adapter);

        if (!SharedPrefManager.getInstance(this).isLoggedIn()) {
            finish();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
    }

    private void instantiateWebSocket() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("ws://192.168.1.104:8080").build();
        SocketListener socketListener = new SocketListener(this);

        webSocket = client.newWebSocket(request, socketListener);
    }

    @Override
    public void onClick(View v) {
        if (v == send) {
            String username = SharedPrefManager.getInstance(this).getUsername();
            String message ="("+username+")" +":"+ messageBox.getText().toString();
            String id = SharedPrefManager.getInstance(this).getId();
            msg.MessageData(message, id);

            if (!message.isEmpty()) {
               webSocket.send(message);

                messageBox.setText("");
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("message",message); //kullanıcının yazdıgı veriyi sunucuya işler
                    jsonObject.put("byServer", false);

                    adapter.addItem(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


        }
        if(v == buttonLobby){
            startActivity(new Intent(this,LobbyActivity.class));
          }
        if (v == buttonProfile)

            startActivity(new Intent(this, ProfileUpdateActivity.class));
        if (v == buttonLogout) {
            SharedPrefManager.getInstance(this).logout();
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    public class SocketListener extends WebSocketListener {
        public ChatActivity activity;

        public SocketListener(ChatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Connection Established!", Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, final String text ) {
            // super.onMessage(webSocket, text);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {          //Serverdan gelen veriyi adaptere put işlemi yapar
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("message", text);
                        jsonObject.put("byServer", true);


                        adapter.addItem(jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }




        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
        }
    }

    public class MessageAdapter extends BaseAdapter {

        List<JSONObject> messageList = new ArrayList<>();

        @Override
        public int getCount() {
            return messageList.size();
        }

        @Override
        public Object getItem(int i) {
            return messageList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.message_list_item, viewGroup, false);
                TextView sentMessage = view.findViewById(R.id.sentMessage);
                TextView receivedMessage = view.findViewById(R.id.receivedMessage);
                JSONObject item = messageList.get(i);


                try {
                    if (item.getBoolean("byServer")) {
                        receivedMessage.setVisibility(View.VISIBLE);
                        receivedMessage.setText(item.getString("message"));
                        sentMessage.setVisibility(View.INVISIBLE);
                    } else {
                        sentMessage.setVisibility(View.VISIBLE);
                        sentMessage.setText(item.getString("message"));
                        receivedMessage.setVisibility(View.INVISIBLE);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            return view;
        }

        void addItem(JSONObject item) {
            messageList.add(item);
            notifyDataSetChanged();
        }
    }

}

