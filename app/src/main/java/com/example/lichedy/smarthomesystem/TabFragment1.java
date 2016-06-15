package com.example.lichedy.smarthomesystem;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class TabFragment1 extends Fragment implements SwipeRefreshLayout.OnRefreshListener{
    Spinner vSpinner;
    ListView alarmlist;
    ArrayList<String> alarmNamesList= new ArrayList<String>();
    ArrayList<String>  valveNamesList= new ArrayList<String>();
    ArrayList<Integer>  startTimeList= new ArrayList<Integer>();
    ArrayList<Integer>  endTimeList= new ArrayList<Integer>();
    ArrayList<Boolean>  alarmEnabledList= new ArrayList<Boolean>();
    MySimpleArrayAdapter adapter;
    postRequest client = new postRequest();
    AsyncHttpResponseHandler response;
    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View myview = inflater.inflate(R.layout.tab_fragment_1, container, false);

        swipeRefreshLayout = (SwipeRefreshLayout) myview.findViewById(R.id.swipe_refresh_layout);

        response = new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String input = new String(responseBody,"UTF-8");
                    String iv = input.substring(0, 16);
                    String message = input.substring(16,input.length());
                    System.out.println("input=" + input);
                    String decrypted = decrypt(message,iv);

                    JsonElement jsonElem = new JsonParser().parse(decrypted);
                    if(jsonElem.isJsonArray()) {
                        // Json data
                        parseJson(decrypted);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }, 500);

                        Snackbar.make(getView(), "Updated", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } else {
                        // message data
                        Snackbar.make(getView(), decrypted, Snackbar.LENGTH_LONG).setAction("Action", null).show();

                        if(decrypted == "OK"){
                            //implement counter
                        }
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
                        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    Snackbar.make(getView(), "Host not responding", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    swipeRefreshLayout.setRefreshing(false);
                }

            }
            @Override
            public void onRetry(int retryNo) {

            }
        };

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = inflater.inflate(R.layout.create_alarm_dialog,null);
        final AlertDialog alarmDialog = builder.create();


        alarmDialog.setView(dialogView);
        alarmDialog.setContentView(R.layout.content_main);

        final ArrayAdapter<CharSequence> valveAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.valves, R.layout.support_simple_spinner_dropdown_item);
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
                alarm.command = "add";
                alarm.device = "";
                alarm.counter = 1;
                alarm.timer.name = alarmName.getText().toString();
                alarm.timer.device = vSpinner.getSelectedItem().toString();
                alarm.timer.startTime = calculateStartTime(npHour.getValue(),npMinute.getValue());
                alarm.timer.endTime = calculateEndTime(npHour.getValue(),npMinute.getValue(),npDuration.getValue());
                alarm.timer.active = true;
                alarm.timer.enabled = true;

                try {
                    String encrypted = encrypt(jsonStringify(alarm));
                    System.out.println("encrypted json=" + jsonStringify(alarm));
                    StringEntity sEntity = new StringEntity(encrypted,"UTF-8");
                    System.out.println("poslo na server" + sEntity);
                    client.post(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("ip adress", "192.168.1.117:3000"),response,sEntity);
                    getlist();
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
        //String JsonTest = "[ { \"name\": \"Kathy\", \"device\": \"Barr\", \"startTime\": 40, \"endTime\": 37, \"enabled\": true }, { \"name\": \"Juanita\", \"device\": \"Reynolds\", \"startTime\": 25, \"endTime\": 40, \"enabled\": false }, { \"name\": \"Adrian\", \"device\": \"Lesa\", \"startTime\": 20, \"endTime\": 38, \"enabled\": false }, { \"name\": \"Rita\", \"device\": \"Terri\", \"startTime\": 38, \"endTime\": 34, \"enabled\": false } ]";

        /*
        try {
            parseJson(JsonTest);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(true);

                                        getlist();
                                    }
                                }
        );


        adapter = new MySimpleArrayAdapter(getContext(), alarmNamesList, valveNamesList, startTimeList, endTimeList, alarmEnabledList);
        alarmlist = (ListView)myview.findViewById(R.id.alarmsList);
        alarmlist.setAdapter(adapter);
        final SwipeDetector sw = new SwipeDetector();
        alarmlist.setOnTouchListener(sw);
        alarmlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {
                if(sw.swipeDetected()) {
                    if(sw.getAction() == SwipeDetector.Action.LR) {


                        final Animation animation = AnimationUtils.loadAnimation(getActivity(),android.R.anim.slide_out_right);
                        v.startAnimation(animation);
                        Handler handle = new Handler();
                        handle.postDelayed(new Runnable() {

                            @Override
                            public void run() {



                                Message listaalarma = new Message();
                                listaalarma.command = "remove";
                                listaalarma.timer.name = alarmNamesList.get(position);
                                try {
                                    String encrypted = encrypt(jsonStringify(listaalarma));
                                    System.out.println(jsonStringify(listaalarma));
                                    StringEntity sEntity = new StringEntity(encrypted,"UTF-8");
                                    System.out.println("poslo na server" + sEntity);
                                    client.post(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("ip adress", "192.168.1.117:3000"),response,sEntity);
                                    getlist();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                animation.cancel();
                            }
                        },400);

                    } else {

                    }
                }

            }
        });

        FloatingActionButton fab = (FloatingActionButton) myview.findViewById(R.id.fabbb);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmDialog.show();
                adapter.notifyDataSetChanged();

            }
        });
        return myview;
    }

    @Override
    public void onRefresh() {
        getlist();
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

    public int calculateStartTime(int hours, int minutes){
        return hours * 60 * 60 + minutes * 60;
    }

    public int calculateEndTime(int hours, int minutes, int duration){
        return hours * 60 * 60 + minutes * 60 + duration * 60;
    }

    public class MySimpleArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final ArrayList<String> names;
        private final ArrayList<String> valves;
        private final ArrayList<Integer> start;
        private final ArrayList<Integer> end;
        private final ArrayList<Boolean> isEnabled;

        public MySimpleArrayAdapter(Context context, ArrayList<String>values, ArrayList<String> valves, ArrayList<Integer> start, ArrayList<Integer> end, ArrayList<Boolean> isEnabled) {
            super(context, R.layout.list_layout, values);
            this.context = context;
            this.names = values;
            this.valves = valves;
            this.start = start;
            this.end = end;
            this.isEnabled = isEnabled;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_layout, parent, false);
            final TextView alarmname = (TextView) rowView.findViewById(R.id.alarmNameBox);
            TextView valvename = (TextView) rowView.findViewById(R.id.valveNameBox);
            TextView starttime = (TextView) rowView.findViewById(R.id.startTimeBox);
            TextView endtime = (TextView) rowView.findViewById(R.id.endTimeBox);
            Switch togglealarm = (Switch) rowView.findViewById(R.id.toggleAlarmSwitch);
            alarmname.setText(names.get(position));
            valvename.setText(valves.get(position));
            starttime.setText(start.get(position).toString());
            endtime.setText(end.get(position).toString());
            togglealarm.setChecked(isEnabled.get(position));
            togglealarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        Message togglealarmmessage = new Message();
                        togglealarmmessage.command = "toggle";
                        togglealarmmessage.timer.name = alarmname.getText().toString();
                        togglealarmmessage.timer.enabled = true;
                        try {
                            String encrypted = encrypt(jsonStringify(togglealarmmessage));
                            System.out.println(jsonStringify(togglealarmmessage));
                            StringEntity sEntity = new StringEntity(encrypted,"UTF-8");
                            System.out.println("poslo na server" + sEntity);
                            client.post(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("ip adress", "192.168.1.117:3000"),response,sEntity);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        Message togglealarmmessage = new Message();
                        togglealarmmessage.command = "toggle";
                        togglealarmmessage.timer.name = alarmname.getText().toString();
                        togglealarmmessage.timer.enabled = true;
                        try {
                            String encrypted = encrypt(jsonStringify(togglealarmmessage));
                            System.out.println(jsonStringify(togglealarmmessage));
                            StringEntity sEntity = new StringEntity(encrypted,"UTF-8");
                            System.out.println("poslo na server" + sEntity);
                            client.post(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("ip adress", "192.168.1.117:3000"),response,sEntity);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    getlist();
                }
            });


            return rowView;
        }
    }

    public void parseJson(String Json) throws JSONException {
        alarmNamesList.clear();
        valveNamesList.clear();
        startTimeList.clear();
        endTimeList.clear();
        alarmEnabledList.clear();
        JSONArray jsonarray = new JSONArray(Json);

        for (int i = 0; i < jsonarray.length(); i++) {
            JSONObject jsonobject = jsonarray.getJSONObject(i);
            alarmNamesList.add(jsonobject.getString("name"));
            valveNamesList.add(jsonobject.getString("device"));
            startTimeList.add(jsonobject.getInt("startTime"));
            endTimeList.add(jsonobject.getInt("endTime"));
            alarmEnabledList.add(jsonobject.getBoolean("enabled"));
        }
    }

    public void getlist(){

        Message listaalarma = new Message();
        listaalarma.command = "list";

        try {
            String encrypted = encrypt(jsonStringify(listaalarma));
            System.out.println(jsonStringify(listaalarma));
            StringEntity sEntity = new StringEntity(encrypted,"UTF-8");
            System.out.println("poslo na server" + sEntity);
            client.post(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("ip adress", "192.168.1.117:3000"),response,sEntity);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
