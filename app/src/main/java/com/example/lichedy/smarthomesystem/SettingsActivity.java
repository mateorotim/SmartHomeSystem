package com.example.lichedy.smarthomesystem;

import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class SettingsActivity extends AppCompatActivity {

    postRequest client = new postRequest();
    AsyncHttpResponseHandler response;
    Switch swp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        response = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String input = new String(responseBody,"UTF-8");
                    String iv = input.substring(0, 16);
                    String message = input.substring(16,input.length());
                    System.out.println("input=" + input);
                    String decrypted = decrypt(message,iv);
                    if(decrypted != "OK"){
                        swp.setChecked(false);
                    }
                    Toast.makeText(getApplicationContext(),decrypted,Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(),"Host not responding",Toast.LENGTH_SHORT).show();
                }
                swp.setChecked(false);
            }
            @Override
            public void onRetry(int retryNo) {

            }
        };

        final EditText ipadress = (EditText)findViewById(R.id.IPeditText);
        assert ipadress != null;
        ipadress.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ip adress", "192.168.1.117:3000"));
        Button changeip = (Button)findViewById(R.id.ipChangeBtn);
        assert changeip != null;
        changeip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("ip adress", ipadress.getText().toString()).commit();
                InputMethodManager imm = (InputMethodManager)getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(ipadress.getWindowToken(),InputMethodManager.RESULT_UNCHANGED_SHOWN);
                ipadress.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ip adress", "192.168.1.117:3000"));
                Snackbar.make(v, "Updated", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        swp = (Switch) findViewById(R.id.switchPool);
        swp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sendCommand("4");
                } else {
                    sendCommand("5");
                }
            }
        });
    }

    public class postRequest {
        private static final String BASE_URL = "http://";
        private AsyncHttpClient client = new AsyncHttpClient();

        public void post(String url, AsyncHttpResponseHandler responseHandler, StringEntity message) {
            client.setMaxRetriesAndTimeout(1,1000);
            client.post(getApplicationContext(),getAbsoluteUrl(url),message,"text/plain", responseHandler);
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
            client.post(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ip adress", "192.168.1.117:3000"),response,sEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
