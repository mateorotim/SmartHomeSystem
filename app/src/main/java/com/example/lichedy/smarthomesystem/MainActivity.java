package com.example.lichedy.smarthomesystem;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MainActivity extends AppCompatActivity {
    String ipAdress;
    int counter = 0;
    Spinner vSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ipAdress = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ip adress", "192.168.1.117:3000");

        final SharedPreferences counterPreference = getSharedPreferences("counter number", Activity.MODE_PRIVATE);

        final EditText messageBox = (EditText)findViewById(R.id.messageBox);
        final EditText ipBox = (EditText)findViewById(R.id.ipBox);
        assert ipBox != null;
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
                    if(decrypted == "OK"){
                        SharedPreferences.Editor editor = counterPreference.edit();
                        editor.putInt("counter number", counter+1);
                        editor.commit();
                    }
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

        //////////////THIS IS ONLY FOR TEST PURPOSES/////////////////
        assert sendMsg != null;
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
        ///////////////////////////////////////////////////////////

        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this,R.style.AppTheme_Dialog));
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.create_alarm_dialog,null);
        final AlertDialog alarmDialog = builder.create();


        alarmDialog.setView(dialogView);
        alarmDialog.setContentView(R.layout.content_main);

        final ArrayAdapter<CharSequence> valveAdapter = ArrayAdapter.createFromResource(this, R.array.valves, R.layout.support_simple_spinner_dropdown_item);
        valveAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        vSpinner = (Spinner)dialogView.findViewById(R.id.valveSpinner);
        vSpinner.setAdapter(valveAdapter);

        final TextView alarmName = (TextView)dialogView.findViewById(R.id.alarmNameBox);

        final NumberPicker npHour = (NumberPicker)dialogView.findViewById(R.id.hourNP);
        final NumberPicker npMinute = (NumberPicker)dialogView.findViewById(R.id.minuteNP);
        final NumberPicker npDuration = (NumberPicker)dialogView.findViewById(R.id.durationNP);
        npHour.setMaxValue(23);
        npHour.setMinValue(0);
        npMinute.setMaxValue(23);
        npMinute.setMinValue(0);
        npDuration.setMaxValue(23);
        npDuration.setMinValue(0);

        NumberPicker.Formatter nf = new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        };
        npHour.setFormatter(nf);
        npMinute.setFormatter(nf);
        npDuration.setFormatter(nf);

        alarmDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


                Message alarm = new Message();
                alarm.command = "1";
                alarm.device = "";
                alarm.counter = counterPreference.getInt("counter number", -1);
                alarm.timer.name = alarmName.getText().toString();
                alarm.timer.device = vSpinner.getSelectedItem().toString();
                alarm.timer.startTime = calculateStartTime(npHour.getValue(),npMinute.getValue());
                alarm.timer.endTime = calculateEndTime(npHour.getValue(),npMinute.getValue(),npDuration.getValue());
                alarm.timer.active = true;
                alarm.timer.enabled = true;

                try {
                    String encrypted = encrypt(jsonStringify(alarm));
                    StringEntity sEntity = new StringEntity(encrypted,"UTF-8");
                    client.post(ipAdress,response,sEntity);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        alarmDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancle", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });






        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                alarmDialog.show();

                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
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
        System.out.println("message " + output);
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

    public int calculateStartTime(int hours, int minutes){
        return hours * 60 * 60 + minutes * 60;
    }

    public int calculateEndTime(int hours, int minutes, int duration){
        return hours * 60 * 60 + minutes * 60 + duration * 60;
    }
}
