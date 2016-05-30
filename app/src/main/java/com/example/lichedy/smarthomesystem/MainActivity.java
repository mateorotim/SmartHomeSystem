package com.example.lichedy.smarthomesystem;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MainActivity extends AppCompatActivity {
    String ipAdress="192.168.1.117:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ipAdress = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ip adress", "192.168.1.117:3000");
        final EditText messageBox = (EditText)findViewById(R.id.messageBox);
        final EditText ipBox = (EditText)findViewById(R.id.ipBox);
        ipBox.setText(ipAdress);
        Button sendMsg = (Button)findViewById(R.id.button);

        final postRequest client = new postRequest();
        final AsyncHttpResponseHandler response = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String input = new String(responseBody,"UTF-8");
                    String iv = input.substring(0, 16);
                    String message = input.substring(16,input.length());
                    System.out.println("input=" + input);
                    String decrypted = decrypt(message,iv);
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
                } else Toast.makeText(getApplicationContext(),"Host not responding",Toast.LENGTH_SHORT).show();

            }
            @Override
            public void onRetry(int retryNo) {

            }
        };

        sendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAdress = ipBox.getText().toString();
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("ip adress", ipAdress).commit();
                InputMethodManager imm = (InputMethodManager)getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(ipBox.getWindowToken(),InputMethodManager.RESULT_UNCHANGED_SHOWN);
                try {
                    String crypted = encrypt(messageBox.getText().toString());
                    StringEntity sEntity = new StringEntity(crypted,"UTF-8");
                    client.post(ipAdress,response,sEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            String crypted = encrypt("stipe");
            StringEntity sEntity = new StringEntity(crypted,"UTF-8");
            client.post(ipAdress,response,sEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class postRequest {
        private static final String BASE_URL = "http://";
        private AsyncHttpClient client = new AsyncHttpClient();

        public void post(String url, AsyncHttpResponseHandler responseHandler, StringEntity message) {
            client.setMaxRetriesAndTimeout(1,1000);
            client.post(getBaseContext(),getAbsoluteUrl(url),message,"text/plain", responseHandler);
        }

        private  String getAbsoluteUrl(String relativeUrl) {
            return BASE_URL + relativeUrl;
        }
    }

    public String decrypt(String message, String iv) throws Exception{
        CryptLib _crypt = new CryptLib();
        String key = CryptLib.SHA256("my secret key", 32); //32 bytes = 256 bit
        String output = _crypt.decrypt(message, key,iv); //decrypt
        System.out.println("decrypted text=" + output);
        return output;
    }

    public String encrypt(String message) throws Exception{
        String iv = CryptLib.generateRandomIV(16);
        CryptLib _crypt = new CryptLib();
        String key = CryptLib.SHA256("my secret key", 32); //32 bytes = 256 bit
        System.out.println("key=" + key);
        String output = _crypt.encrypt(message, key,iv); //decrypt
        System.out.println("iv=" + iv);
        System.out.println("rest" + output);
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
        public String name,
                device;
        public int startTime,
                endTime;
        boolean active,
                enabled;
        public Timer(){
            super();
        }
    }
}
