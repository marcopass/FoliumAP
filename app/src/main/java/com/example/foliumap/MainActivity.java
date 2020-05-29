package com.example.foliumap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    String SSID = "FoliumAccessPoint";
    String PASSWORD = "g5i7df0j";

    Button testBT;
    TextView testTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testBT = findViewById(R.id.test_button);
        testTV = findViewById(R.id.test_text);

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!connectedToESP()) {
                        if (!connectedToESP()) {
                            connectToESP();
                        }
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {}
            }
        };

        thread.start();

        sendTestRequest();

        testBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTestRequest();
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

    private void sendTestRequest() {

        StringRequest request = new StringRequest(Request.Method.GET, "http://192.168.4.1/?action=getTimeList",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "getTimeList");
                return params;
            }
        };

        int socketTimeOut = 30000; // milliseconds

        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        queue.add(request);
    }
}
