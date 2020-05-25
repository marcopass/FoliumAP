package com.example.foliumap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

    Button testBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testBT = findViewById(R.id.test_button);

        String ssid = "FoliumAccessPoint";
        String password = "g5i7df0j";

        //String ssid = "Koleci";
        //String password = "Adry'1997";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // WIFI HANDLING FOR VERSIONS <10 (Q), API 29
            // my target is version 6.0 (Marshmellow), API 23
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.preSharedKey = "\""+ password +"\"";

            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.addNetwork(conf);

            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }

            while (!wifiManager.isWifiEnabled()) {} // wait until wifi is on

            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();

                    Toast.makeText(this, "Connected to ESP!", Toast.LENGTH_SHORT).show();

                    break;
                } else {
                    wifiManager.disableNetwork(i.networkId);
                }
            }
        } else {
            // WIFI HANDLING FOR VERSIONS >=10 (Q), API 29
        }


        testBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTestRequest();
            }
        });



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
