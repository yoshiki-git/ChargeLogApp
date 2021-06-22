package com.example.chargelogapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.MODE_PRIVATE;

public class MyReceiver extends BroadcastReceiver {

    // BroadcastIntentを受信した場合の処理 //

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())
        || Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
        //Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
            {
             Log.d("デバッグ","BOOT_COMPLETED RECEIVE!");
             SharedPreferences dataStore = context.getSharedPreferences("DataStore", MODE_PRIVATE);
             boolean service_Switch=dataStore.getBoolean("start_Button",true);
             Log.d("デバッグ","Receiver SharedPreference");
             if(!service_Switch) {
                 Intent f_intent = new Intent(context.getApplicationContext(), MyService.class);
                 f_intent.putExtra("REQUEST_CODE", 1);
                 context.startForegroundService(f_intent);
                 Log.d("デバッグ", "サービス起動");
             }else{
                 Log.d("デバッグ","サービス起動なし");
             }
        }

    }
}
