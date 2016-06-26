package com.example.lichedy.smarthomesystem;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


public class TabFragment2 extends Fragment {

    postRequest client = new postRequest();
    AsyncHttpResponseHandler response;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View myview2 = inflater.inflate(R.layout.tab_fragment_2, container, false);
        response = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String input = new String(responseBody,"UTF-8");
                    String iv = input.substring(0, 16);
                    String message = input.substring(16,input.length());
                    System.out.println("input=" + input);
                    String decrypted = decrypt(message,iv);
                    Snackbar.make(getView(), decrypted, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if(responseBody != null){
                    try {
                        String message = new String(responseBody,"UTF-8");
                        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    Snackbar.make(getView(), "Host not responding", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
            @Override
            public void onRetry(int retryNo) {

            }
        };
        Button btnrg = (Button)myview2.findViewById(R.id.btnRg);
        Button btnlg = (Button)myview2.findViewById(R.id.btnLg);
        Button btng = (Button)myview2.findViewById(R.id.btnK);

        btnrg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("1");
            }
        });

        btnlg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("2");
            }
        });
        btng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand("3");
            }
        });




        return myview2;
    }

    public class postRequest {
        private static final String BASE_URL = "http://";
        private AsyncHttpClient client = new AsyncHttpClient();

        public void post(String url, AsyncHttpResponseHandler responseHandler, StringEntity message) {
            client.setMaxRetriesAndTimeout(1,1000);
            client.post(getContext(),getAbsoluteUrl(url),message,"text/plain", responseHandler);
        }

        private  String getAbsoluteUrl(String relativeUrl) {
            return BASE_URL + relativeUrl;
        }
    }

    public String decrypt(String message, String iv) throws Exception{
        CryptLib _crypt = new CryptLib();
        String key = CryptLib.SHA256("my secret key", 32); //32 bytes = 256 bit
        String output = _crypt.decrypt(message, key,iv); //decrypt
        return output;
    }

    public String encrypt(String message) throws Exception{
        String iv = CryptLib.generateRandomIV(16);
        CryptLib _crypt = new CryptLib();
        String key = CryptLib.SHA256("my secret key", 32); //32 bytes = 256 bit
        System.out.println("key=" + key);
        String output = _crypt.encrypt(message, key,iv); //decrypt
        System.out.println("iv=" + iv);
        System.out.println("output " + iv + output + " lenght: " + output.length());
        return iv + output;
    }

    public String jsonStringify(Object o){
        Gson gson = new Gson();
        String json = gson.toJson(o);
        return json;
    }

    public class Message{
        public String command;
        public Timer timer;
        public String device;
        public int counter;

        public Message(){
            super();
            timer = new Timer();

        }
    }
    class Timer{
        public String name = "placeholder",
                device = "placeholder";
        public int startTime,
                endTime;
        boolean active,
                enabled;
        public Timer(){
            super();
        }
    }

    public void sendCommand(String device){
        Message alarm = new Message();
        alarm.command = "trigger";
        alarm.device = device;

        try {
            String encrypted = encrypt(jsonStringify(alarm));
            System.out.println("encrypted json=" + jsonStringify(alarm));
            StringEntity sEntity = new StringEntity(encrypted,"UTF-8");
            System.out.println("poslo na server" + sEntity);
            client.post(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("ip adress", "192.168.1.117:3000"),response,sEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
