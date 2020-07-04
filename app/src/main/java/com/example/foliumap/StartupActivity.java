package com.example.foliumap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.List;

public class StartupActivity extends AppCompatActivity {

    String SSID = "FoliumAccessPoint";
    String PASSWORD = "g5i7df0j";

    Button intentButton;

    int MAX_TIMES = 8;
    int[] hourArray = new int[MAX_TIMES];
    int[] minuteArray = new int[MAX_TIMES];
    int[] durationArray = new int[MAX_TIMES];
    int numMemTimes = 0;
    String rawTimeList = "rawTimeList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        intentButton = findViewById(R.id.intent_button);

        SharedPreferences preferences = getSharedPreferences("FoliumAP_Data", MODE_PRIVATE);
        rawTimeList = preferences.getString("timeList", "0,");
        numMemTimes = Integer.parseInt(rawTimeList.split(",")[0]);

        for (int i = 0; i < numMemTimes; i++) {
            String pckg = rawTimeList.split(",")[i+1];

            hourArray[i] = Integer.parseInt(pckg.split("-")[0]);
            minuteArray[i] = Integer.parseInt(pckg.split("-")[1]);
            durationArray[i] = Integer.parseInt(pckg.split("-")[2]);
        }


        intentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(StartupActivity.this, MainActivity.class);
                startActivity(intent);

            }
        });

    }

    private void connectToESP() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // WIFI HANDLING FOR VERSIONS <10 (Q), API 29 ----------------------------------------------------------------------------------------------------------
            // my target is version 6.0 (Marshmellow), API 23
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + SSID + "\"";
            conf.preSharedKey = "\""+ PASSWORD +"\"";

            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.addNetwork(conf);

            if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                wifiManager.setWifiEnabled(true);
            }

            while (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {}

            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                wifiManager.disconnect();
                if(i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
                    wifiManager.enableNetwork(i.networkId, true);
                    break;
                }
            }

        } else {
            // WIFI HANDLING FOR VERSIONS >=10 (Q), API 29 --------------------------------------------------------------------------------------------------------------
            // No Code => Manual Connection to ESP Access Point
        }
    }

    private boolean connectedToESP() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        boolean connected = false;

        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            if (wifiManager.getConnectionInfo().getSSID().equals("\"" + SSID + "\"")) {
                connected = true;
            }
        }

        return connected;
    }
}
