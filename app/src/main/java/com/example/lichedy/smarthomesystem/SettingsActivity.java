package com.example.lichedy.smarthomesystem;

import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final EditText ipadress = (EditText)findViewById(R.id.IPeditText);
        ipadress.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ip adress", "192.168.1.117:3000"));
        Button changeip = (Button)findViewById(R.id.ipChangeBtn);
        changeip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("ip adress", ipadress.getText().toString()).commit();
                InputMethodManager imm = (InputMethodManager)getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(ipadress.getWindowToken(),InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }
        });
    }
}
