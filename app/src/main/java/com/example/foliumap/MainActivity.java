package com.example.foliumap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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

import org.w3c.dom.Text;

import java.io.IOError;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    // CONSTANTS
    String URL = "http://192.168.4.1/";
    String GET_DATE_TIME = "getDateTime";
    String GET_LAST_WATER = "getLastWater";
    String GET_TIME_LIST = "getTimeList";
    String SET_DATE_TIME = "setDateTime";
    String SET_TIME_LIST = "setTimeList";
    String WATER_NOW = "waterNow";
    String STOP_WATER_NOW = "stopWaterNow";

    int SOCKET_TIMEOUT = 10000; // ms

    SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    int MAX_TIMES = 8;

    int DEFAULT_NEW_HOUR = 0;
    int DEFAULT_NEW_MINUTE = 0;
    int DEFAULT_NEW_DURATION = 5;

    int MAX_MSG = 10;

    // WIDGETS
    // Top Section
    TextView lastWaterTV, nextWaterTV;
    TextView lastWaterTitleTV, nextWaterTitleTV;
    // Middle Section - TimeList
    ScrollView timeListSV;
    View[] timeListLL = new LinearLayout[MAX_TIMES];
    LinearLayout[] leftLL = new LinearLayout[MAX_TIMES];
    LinearLayout[] rightLL = new LinearLayout[MAX_TIMES];
    NumberPicker[] hourNP = new NumberPicker[MAX_TIMES];
    NumberPicker[] minuteNP = new NumberPicker[MAX_TIMES];
    NumberPicker[] durationNP = new NumberPicker[MAX_TIMES];
    Button[] deleteBT = new Button[MAX_TIMES];
    LinearLayout timeListPlusLL;
    Button timeListPlusBT;
    // Middle Section - Message Log
    LinearLayout messageLogLLExt;
    ScrollView messageLogSV;
    LinearLayout messageLogLL;
    TextView[] messageLogTV = new TextView[MAX_MSG];
    // Bottom Section
    LinearLayout bottomControlsLL;
    Button submitChangesBT;
    Button waterNowBT;
    Button testBT;

    // Others
    BroadcastReceiver broadcastReceiver;
    LinearLayout progressBarLL;
    ProgressBar progressBar;

    // VARIABLES
    int[] hourArray = new int[MAX_TIMES];
    int[] minuteArray = new int[MAX_TIMES];
    int[] durationArray = new int[MAX_TIMES];
    int numMemTimes = 0;
    String rawTimeList = "rawTimeList";
    String lastWaterTimestamp = "lastWaterTimestamp";
    int activeRequestCounter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Top Section
        lastWaterTV = findViewById(R.id.last_water_text_view);
        nextWaterTV = findViewById(R.id.next_water_text_view);
        lastWaterTitleTV = findViewById(R.id.last_water_title_text_view);
        nextWaterTitleTV = findViewById(R.id.next_water_title_text_view);
        // Middle Section - TimeList
        timeListSV = findViewById(R.id.time_list_scroll);
        initTimeListViews();
        timeListPlusLL = findViewById(R.id.time_list_plus_layout);
        timeListPlusBT = findViewById(R.id.time_list_plus_button);
        // Middle Section - Message Log
        messageLogLLExt = findViewById(R.id.message_log_layout_ext);
        messageLogSV = findViewById(R.id.message_log_scroll);
        messageLogLL = findViewById(R.id.message_log_layout);
        // Bottom Section
        bottomControlsLL = findViewById(R.id.bottom_controls_layout);
        submitChangesBT = findViewById(R.id.submit_changes_button);
        waterNowBT = findViewById(R.id.water_now_button);
        testBT = findViewById(R.id.test_button);

        // Others
        progressBarLL = findViewById(R.id.progress_bar_layout);
        progressBar = findViewById(R.id.progress_bar);

        // VARIABLES
        SharedPreferences preferences = getSharedPreferences("FoliumAP_Data", MODE_PRIVATE);
        rawTimeList = preferences.getString("timeList", "0,");
        lastWaterTimestamp = preferences.getString("lastWaterTimestamp", "");
        numMemTimes = Integer.parseInt(rawTimeList.split(",")[0]);

        for (int i = 0; i < numMemTimes; i++) {
            String pckg = rawTimeList.split(",")[i+1];

            hourArray[i] = Integer.parseInt(pckg.split("-")[0]);
            minuteArray[i] = Integer.parseInt(pckg.split("-")[1]);
            durationArray[i] = Integer.parseInt(pckg.split("-")[2]);
        }

        updateTimeListView();

        getDateTime();
        getLastWater();
        getTimeList();

        // Delete Button Listeners
        for (int i = 0; i < MAX_TIMES; i++) {
            final int I = i;
            deleteBT[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // shift arrays
                    for (int j = I; j < MAX_TIMES; j++) {
                        if (j < MAX_TIMES - 1) {
                            hourArray[j] = hourArray[j + 1];
                            minuteArray[j] = minuteArray[j + 1];
                            durationArray[j] = durationArray[j + 1];
                        }
                    }

                    // update numMemTimes
                    numMemTimes--;

                    // update view
                    updateTimeListView();

                    logMessage("numMemTimes: " + numMemTimes, "INFO");
                }
            });
        }

        // Plus Button Listener
        timeListPlusBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Add default values to arrays
                hourArray[numMemTimes] = DEFAULT_NEW_HOUR;
                minuteArray[numMemTimes] = DEFAULT_NEW_MINUTE;
                durationArray[numMemTimes] = DEFAULT_NEW_DURATION;

                // update numMemTimes
                numMemTimes++;

                // update view
                updateTimeListView();

                // scroll to bottom
                timeListSV.post(new Runnable() {
                    @Override
                    public void run() {
                        timeListSV.fullScroll(View.FOCUS_DOWN);
                    }
                });

                logMessage("numMemTimes: " + numMemTimes, "INFO");
            }
        });

        submitChangesBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimeList();
            }
        });

        waterNowBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "waterNow", Toast.LENGTH_SHORT).show();

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View popupView = inflater.inflate(R.layout.popup_water_now, null);

                // Get display dimensions
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int displayWidth = size.x;
                int displayHeight = size.y;

                int width = Math.round(displayWidth * 0.8f);
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                boolean focusable = true;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
                popupWindow.setElevation(20);

                final NumberPicker durationPopupNP = popupView.findViewById(R.id.picker_popup_duration);
                Button waterNowPopupBT = popupView.findViewById(R.id.water_now_popup_button);

                durationPopupNP.setMinValue(1);
                durationPopupNP.setMaxValue(99);
                durationPopupNP.setWrapSelectorWheel(false);
                durationPopupNP.setFormatter(new NumberPicker.Formatter() {
                    @Override
                    public String format(int i) {
                        return i + " min";
                    }
                });

                waterNowPopupBT.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        waterNow(durationPopupNP.getValue());

                        popupWindow.dismiss();
                    }
                });

                ConstraintLayout mainActivityCL = findViewById(R.id.activity_main_layout);
                popupWindow.showAsDropDown(mainActivityCL, displayWidth/2-width/2, Math.round(displayHeight * 0.15f));
            }
        });

        testBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDateTime();
                getLastWater();
                getTimeList();
            }
        });


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater =getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_show_log:
                messageLogLLExt.setVisibility(messageLogLLExt.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                break;
            case R.id.menu_item_set_date_time:
                setDateTime();
                break;
            case R.id.menu_item_get_last_water:
                getLastWater();
                break;
            case R.id.menu_item_stop_water_now:
                stopWaterNow();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateLastWater();
                    updateNextWater();
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }


    private void updateTimeListView() {
        for (int i = 0; i < MAX_TIMES; i++) {
            if (i < numMemTimes) {
                timeListLL[i].setVisibility(View.VISIBLE);
                hourNP[i].setValue(hourArray[i]);
                minuteNP[i].setValue(minuteArray[i]);
                durationNP[i].setValue(durationArray[i]);
            } else {
                timeListLL[i].setVisibility(View.GONE);
            }
        }

        if (numMemTimes < 8) {
            timeListPlusLL.setVisibility(View.VISIBLE);
        } else {
            timeListPlusLL.setVisibility(View.GONE);
        }
    }

    private void initTimeListViews() {
        timeListLL[0] = findViewById(R.id.time_list_layout_0);
        timeListLL[1] = findViewById(R.id.time_list_layout_1);
        timeListLL[2] = findViewById(R.id.time_list_layout_2);
        timeListLL[3] = findViewById(R.id.time_list_layout_3);
        timeListLL[4] = findViewById(R.id.time_list_layout_4);
        timeListLL[5] = findViewById(R.id.time_list_layout_5);
        timeListLL[6] = findViewById(R.id.time_list_layout_6);
        timeListLL[7] = findViewById(R.id.time_list_layout_7);

        for (int i = 0; i < MAX_TIMES; i++) {
            // Pickers
            hourNP[i] = timeListLL[i].findViewById(R.id.picker_hour);
            minuteNP[i] = timeListLL[i].findViewById(R.id.picker_minute);
            durationNP[i] = timeListLL[i].findViewById(R.id.picker_duration);

            hourNP[i].setMinValue(0);
            hourNP[i].setMaxValue(23);
            hourNP[i].setFormatter(new NumberPicker.Formatter() {
                @Override
                public String format(int i) {
                    return String.format("%02d", i);
                }
            });

            minuteNP[i].setMinValue(0);
            minuteNP[i].setMaxValue(59);
            minuteNP[i].setFormatter(new NumberPicker.Formatter() {
                @Override
                public String format(int i) {
                    return String.format("%02d", i);
                }
            });

            durationNP[i].setMinValue(1);
            durationNP[i].setMaxValue(99);
            durationNP[i].setWrapSelectorWheel(false);
            durationNP[i].setFormatter(new NumberPicker.Formatter() {
                @Override
                public String format(int i) {
                    return i + " min";
                }
            });

            // Buttons
            deleteBT[i] = timeListLL[i].findViewById(R.id.time_list_delete_button);
        }
    }



    private void logMessage(String msg, String type) {
        TextView newMsgTV = new TextView(getApplicationContext());

        newMsgTV.setText("New Message");
        newMsgTV.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.textSizeSmall));
        switch (type) {
            case "INFO":
                newMsgTV.setText("[INFO] " + msg);
                newMsgTV.setTextColor(getResources().getColor(R.color.colorTextLog));
                break;
            case "WARN":
                newMsgTV.setText("[WARN] " + msg);
                newMsgTV.setTextColor(getResources().getColor(R.color.colorAccent));
                //newMsgTV.setTypeface(Typeface.DEFAULT_BOLD);
                break;
            case "ERROR":
                newMsgTV.setText("[ERROR] " + msg);
                newMsgTV.setTextColor(getResources().getColor(R.color.colorAccent));
                newMsgTV.setTypeface(Typeface.DEFAULT_BOLD);
                break;
        }

        messageLogLL.addView(newMsgTV);
        messageLogSV.post(new Runnable() {
            @Override
            public void run() {
                messageLogSV.fullScroll(View.FOCUS_DOWN);
            }
        });
    }


    private void getDateTime() {
        activeRequestCounter++;
        progressBarLL.setVisibility(View.VISIBLE);

        final String requestURL = URL + "?action=" + GET_DATE_TIME;

        StringRequest request = new StringRequest(Request.Method.GET, requestURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("getDateTime: " + response, "INFO");
                        // expected response format: yyyy-MM-dd HH:mm:ss
                        // check if dateTime is reasonable, if not RTC battery is empty

                        try {
                            Calendar currentTime = Calendar.getInstance();
                            Calendar receivedTime = Calendar.getInstance();
                            receivedTime.set(Calendar.YEAR, Integer.parseInt(response.substring(0, 4)));
                            receivedTime.set(Calendar.MONTH, Integer.parseInt(response.substring(5, 7)) - 1);
                            receivedTime.set(Calendar.DATE, Integer.parseInt(response.substring(8, 10)));
                            receivedTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(response.substring(11, 13)));
                            receivedTime.set(Calendar.MINUTE, Integer.parseInt(response.substring(14, 16)));
                            receivedTime.set(Calendar.SECOND, Integer.parseInt(response.substring(17, 19)));

                            long diffInMillis = Math.abs(currentTime.getTimeInMillis() - receivedTime.getTimeInMillis());

                            //logMessage("Current date time: " + DATE_TIME_FORMAT.format(currentTime.getTime()), "INFO");
                            //logMessage("curent in millis: " + currentTime.getTimeInMillis(), "INFO");
                            //logMessage("Received date time: " + DATE_TIME_FORMAT.format(receivedTime.getTime()), "INFO");
                            //logMessage("received in millis: " + receivedTime.getTimeInMillis(), "INFO");
                            logMessage("Diff in millis: " + diffInMillis, "INFO");

                            if (diffInMillis > 1000 * 60 * 5) {
                                // Send error message to user -> RTC battery probably empty
                                logMessage("A wrong date time was detected! Check RTC battery level!", "WARN");

                                messageLogLLExt.setVisibility(View.VISIBLE);
                            }

                            // send setDateTime() to automatically correct RTC time.
                            setDateTime();
                        } catch (IOError error) {
                            logMessage("A wrong date time was detected! Check RTC battery level or board connections!", "WARN");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("getDateTime: " + error.toString(), "ERROR");
                        messageLogLLExt.setVisibility(View.VISIBLE);
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                return params;
            }
        };

        int socketTimeOut = SOCKET_TIMEOUT; // milliseconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }

    private void getLastWater() {
        activeRequestCounter++;
        progressBarLL.setVisibility(View.VISIBLE);

        String requestURL = URL + "?action=" + GET_LAST_WATER;

        StringRequest request = new StringRequest(Request.Method.GET, requestURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("getLastWater: " + response, "INFO");

                        lastWaterTimestamp = response;

                        updateLastWater();

                        // save lastWater timestamp on device
                        SharedPreferences preferences = getSharedPreferences("FoliumAP_Data", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("lastWaterTimestamp", lastWaterTimestamp);
                        editor.apply();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("getLastWater: " + error.toString(), "ERROR");
                        messageLogLLExt.setVisibility(View.VISIBLE);
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                return params;
            }
        };

        int socketTimeOut = SOCKET_TIMEOUT; // milliseconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }

    private void getTimeList() {
        activeRequestCounter++;
        progressBarLL.setVisibility(View.VISIBLE);

        String requestURL = URL + "?action=" + GET_TIME_LIST;

        StringRequest request = new StringRequest(Request.Method.GET, requestURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("getTimeList: " + response, "INFO");

                        numMemTimes = Integer.parseInt(response.split(",")[0]);
                        for (int i = 0; i < numMemTimes; i++) {
                            String pckg = response.split(",")[i+1];
                            hourArray[i] = Integer.parseInt(pckg.split("-")[0]);
                            minuteArray[i] = Integer.parseInt(pckg.split("-")[1]);
                            durationArray[i] = Integer.parseInt(pckg.split("-")[2]);
                            updateTimeListView();
                        }

                        updateNextWater();

                        // save time list on device
                        SharedPreferences preferences = getSharedPreferences("FoliumAP_Data", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("timeList", response);
                        editor.apply();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("getTimeList: " + error.toString(), "ERROR");
                        messageLogLLExt.setVisibility(View.VISIBLE);
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                return params;
            }
        };

        int socketTimeOut = SOCKET_TIMEOUT; // milliseconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }

    private void setDateTime() {
        //activeRequestCounter++;
        progressBarLL.setVisibility(View.VISIBLE);

        Calendar calendar = Calendar.getInstance();

        String requestURL = URL + "?action=" + SET_DATE_TIME + "&datetime=" + DATE_TIME_FORMAT.format(calendar.getTime());
        //Toast.makeText(this, requestURL, Toast.LENGTH_SHORT).show();

        StringRequest request = new StringRequest(Request.Method.GET, requestURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //activeRequestCounter--;
                        //if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("setDateTime: " + response, "INFO");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //activeRequestCounter--;
                        //if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("setDateTime: " + error.toString(), "ERROR");
                        messageLogLLExt.setVisibility(View.VISIBLE);
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                return params;
            }
        };

        int socketTimeOut = SOCKET_TIMEOUT; // milliseconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }

    private void setTimeList() {
        activeRequestCounter++;
        progressBarLL.setVisibility(View.VISIBLE);

        String timeListString = numMemTimes + ",";
        for (int i = 0; i < numMemTimes; i++) {
            hourArray[i] = hourNP[i].getValue();
            minuteArray[i] = minuteNP[i].getValue();
            durationArray[i] = durationNP[i].getValue();

            timeListString += hourArray[i] + "-" + minuteArray[i] + "-" + durationArray[i] + ",";
        }

        String requestURL = URL + "?action=" + SET_TIME_LIST + "&timelist=" + timeListString;

        final String timeListStringFinal = timeListString;

        StringRequest request = new StringRequest(Request.Method.GET, requestURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        switch (response) {
                            case "200 OK":
                                logMessage("setTimeList: " + response, "INFO");
                                break;
                            case "202 Accepted":
                                logMessage("Cannot update timelist when watering!", "WARN");
                                messageLogLLExt.setVisibility(View.VISIBLE);
                                break;
                            default:
                                logMessage("Response not recognized: " + response, "ERROR");
                                messageLogLLExt.setVisibility(View.VISIBLE);
                        }

                        updateNextWater();

                        // save time list on device
                        SharedPreferences preferences = getSharedPreferences("FoliumAP_Data", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("timeList", timeListStringFinal);
                        editor.apply();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("setTimeList: " + error.toString(), "ERROR");
                        messageLogLLExt.setVisibility(View.VISIBLE);
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                return params;
            }
        };

        int socketTimeOut = SOCKET_TIMEOUT; // milliseconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }

    private void waterNow(int duration) {
        activeRequestCounter++;
        progressBarLL.setVisibility(View.VISIBLE);

        // REMEMBER TO SET DURATION (in seconds) !!!
        String requestURL = URL + "?action=" + WATER_NOW + "&duration=" + String.valueOf(duration * 60);

        StringRequest request = new StringRequest(Request.Method.GET, requestURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        switch (response) {
                            case "200 OK":
                                logMessage("waterNow: " + response, "INFO");
                                break;
                            case "202 Accepted":
                                logMessage("Already watering!", "WARN");
                                messageLogLLExt.setVisibility(View.VISIBLE);
                                break;
                            default:
                                logMessage("Response not recognized: " + response, "ERROR");
                                messageLogLLExt.setVisibility(View.VISIBLE);
                        }

                        getLastWater();
                        /*updateLastWater();

                        // save lastWater timestamp on device
                        SharedPreferences preferences = getSharedPreferences("FoliumAP_Data", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("lastWaterTimestamp", lastWaterTimestamp);
                        editor.apply();*/
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("waterNow: " + error.toString(), "ERROR");
                        messageLogLLExt.setVisibility(View.VISIBLE);
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                return params;
            }
        };

        int socketTimeOut = SOCKET_TIMEOUT; // milliseconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }

    private void stopWaterNow() {
        activeRequestCounter++;
        progressBarLL.setVisibility(View.VISIBLE);

        String requestURL = URL + "?action=" + STOP_WATER_NOW;

        StringRequest request = new StringRequest(Request.Method.GET, requestURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        switch (response) {
                            case "200 OK":
                                logMessage("stopWaterNow: " + response, "INFO");
                                break;
                            default:
                                logMessage("Response not recognized: " + response, "ERROR");
                                messageLogLLExt.setVisibility(View.VISIBLE);
                        }

                        getLastWater();
                        /*updateLastWater();

                        // save lastWater timestamp on device
                        SharedPreferences preferences = getSharedPreferences("FoliumAP_Data", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("lastWaterTimestamp", lastWaterTimestamp);
                        editor.apply();*/
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        activeRequestCounter--;
                        if (activeRequestCounter == 0) { progressBarLL.setVisibility(View.INVISIBLE); }

                        logMessage("stopWaterNow: " + error.toString(), "ERROR");
                        messageLogLLExt.setVisibility(View.VISIBLE);
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                return params;
            }
        };

        int socketTimeOut = SOCKET_TIMEOUT; // milliseconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(request);
    }


    private void updateLastWater() {
        Calendar currentTime = Calendar.getInstance();
        Calendar lastWaterTime = Calendar.getInstance();
        lastWaterTime.set(Calendar.YEAR, Integer.parseInt(lastWaterTimestamp.substring(0,4)));
        lastWaterTime.set(Calendar.MONTH, Integer.parseInt(lastWaterTimestamp.substring(5,7)) - 1);
        lastWaterTime.set(Calendar.DATE, Integer.parseInt(lastWaterTimestamp.substring(8,10)));
        lastWaterTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(lastWaterTimestamp.substring(11,13)));
        lastWaterTime.set(Calendar.MINUTE, Integer.parseInt(lastWaterTimestamp.substring(14,16)));
        lastWaterTime.set(Calendar.SECOND, Integer.parseInt(lastWaterTimestamp.substring(17,19)));

        String date = "";
        int diffDay = currentTime.get(Calendar.DATE) - lastWaterTime.get(Calendar.DATE);
        switch (diffDay) {
            case -1:
                date = "tomorrow";
                logMessage("Something wrong with lastWater!", "WARN");
                break;
            case 0:
                date = "today";
                break;
            case 1:
                date = "yesterday";
                break;
            default:
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
                date = dateFormat.format(lastWaterTime.getTime());
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String time = timeFormat.format(lastWaterTime.getTime());

        int diffInSec = (int) (Math.abs(currentTime.getTimeInMillis() - lastWaterTime.getTimeInMillis())) / 1000;
        //int sec = diffInSec % 60;
        int min = (diffInSec / 60) % 60;
        int hour = (diffInSec / 60 / 60) % 24;
        int day = diffInSec / 60 / 60 / 24;

        String minStr = String.format("%2d", min) + "m";
        String hourStr = String.format("%2d", hour) + "h";
        String dayStr = day + "d";
        if (day == 0) {
            dayStr = "";
            if (hour == 0) {
                hourStr = "";
                if (min == 0) {
                    minStr = "";
                }
            }
        }

        String timeAgo = dayStr + hourStr + minStr + (minStr.isEmpty() ? "now" : " ago");

        lastWaterTV.setText(date + ", " + time + "\n" + timeAgo);

    }

    private void updateNextWater() {
        Calendar currentTime = Calendar.getInstance();

        int candidatesDiff[] = new int[2*numMemTimes];
        int diffInMillis = 100000000; // needs to be greater than 1000*3600*24 = 86400000
        int diffDay = -1;
        Calendar nextWaterTime = Calendar.getInstance();

        for (int i = 0; i < 2*numMemTimes; i++) {
            Calendar candidateTime = Calendar.getInstance();
            candidateTime.set(Calendar.HOUR_OF_DAY, hourArray[i]);
            candidateTime.set(Calendar.MINUTE, minuteArray[i]);
            candidateTime.set(Calendar.SECOND, 0);

            if (i < numMemTimes) {
                candidatesDiff[i] = (int) (candidateTime.getTimeInMillis() - currentTime.getTimeInMillis());
            } else {
                candidateTime.set(Calendar.DATE, candidateTime.get(Calendar.DATE) + 1);
                candidateTime.set(Calendar.HOUR_OF_DAY, hourArray[i - numMemTimes]);
                candidateTime.set(Calendar.MINUTE, minuteArray[i - numMemTimes]);
                candidatesDiff[i] = (int) (candidateTime.getTimeInMillis() - currentTime.getTimeInMillis());
            }

            if (candidatesDiff[i] >= 0 && candidatesDiff[i] < diffInMillis) {
                diffInMillis = candidatesDiff[i];
                diffDay = i < numMemTimes ? 0 : -1;
                nextWaterTime = candidateTime;
            }
        }

        String date = "";
        switch (diffDay) {
            case -1:
                date = "tomorrow";
                break;
            case 0:
                date = "today";
                break;
            case 1:
                date = "yesterday";
                logMessage("Something wrong with lastWater!", "WARN");
                break;
            default:
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                date = dateFormat.format(nextWaterTime.getTime());
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String time = timeFormat.format(nextWaterTime.getTime());

        int diffInSec = (int) (Math.abs(currentTime.getTimeInMillis() - nextWaterTime.getTimeInMillis())) / 1000;
        //int sec = diffInSec % 60;
        int min = (diffInSec / 60) % 60;
        int hour = (diffInSec / 60 / 60) % 24;
        int day = diffInSec / 60 / 60 / 24;

        String minStr = String.format("%2d", min) + "m";
        String hourStr = String.format("%2d", hour) + "h";
        String dayStr = day + "d";
        if (day == 0) {
            dayStr = "";
            if (hour == 0) {
                hourStr = "";
                if (min == 0) {
                    minStr = "";
                }
            }
        }

        String timeIn = (minStr.isEmpty() ? "now" : "in ") + dayStr + hourStr + minStr;

        nextWaterTV.setText(date + ", " + time + "\n" + timeIn);

    }

}
