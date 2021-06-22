package com.example.chargelogapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyService extends Service {
    private String fileName;
    public File file;
    private String log_data;
    private String log_data_temp;
    private String pre_log_data;
    private String log_time_info;
    private boolean service_Switch;
    private int pre_bat_level;
    private int pre_bat_status;
    private int battery;
    private int battery_stat;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("デバッグ", "onCreate()");

        SharedPreferences dataStore = getSharedPreferences("DataStore", MODE_PRIVATE);
        fileName=dataStore.getString("fileName","ファイル名");
        service_Switch=dataStore.getBoolean("start_Button",true);
        Log.d("デバッグ","SharedPreference");
   /*     if(service_Switch){
            stopSelf();
            Log.d("デバッグ","サービス終了");
        }*/

        File dir_myApp =new File("/sdcard/充電ログアプリ");
      //  File dir_myApp=new File("/storage/emulated/0/充電ログアプリ");
        if(dir_myApp.exists()){
            Log.d("デバッグ","あるらしい");
        }else {
            dir_myApp.mkdir();
            Log.d("デバッグ","ないから作ったよ");
        }
        file=new File(dir_myApp,fileName);

 /*

        // ファイルのパスを設定
       File path =getExternalFilesDir(null);
        file = new File(path, fileName);

  */




        log_data_temp="";
        pre_log_data="";
        Log.d("デバッグ","ファイルパス指定とか");


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("デバッグ", "onStartCommand()");


        int requestCode = intent.getIntExtra("REQUEST_CODE",0);
        Context context = getApplicationContext();
        String channelId = "default";
        String title = context.getString(R.string.app_name);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Notification　Channel 設定
        NotificationChannel channel = new NotificationChannel(
                channelId, title , NotificationManager.IMPORTANCE_DEFAULT);

        if(notificationManager != null){
            notificationManager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(context, channelId)
                    .setContentTitle(title)
                    // android標準アイコンから
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentText("ログ取得中!!")
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .build();

            // startForeground
            startForeground(1, notification);

            BatteryStatusReceiver();
        }

        //return START_NOT_STICKY;
        //return START_STICKY;
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(BattStatRcv);
        Log.d("デバッグ","OnDestroy");

    }
    //ここからバッテリーレシーバー関係
    private void BatteryStatusReceiver() {

        //Intent Filter
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(BattStatRcv, filter);

    }

    private BroadcastReceiver BattStatRcv =new BroadcastReceiver() {
        @Override
        public void onReceive(Context context,Intent intent){
            //初期化
            log_data="";
            log_time_info="";
            //レシーバーが受け取った時刻を取得
            int battery_temp=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            int status_temp=intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
            Log.d("ChargeApp","battery_temp:"+battery_temp);
            Log.d("ChargeApp","pre_bat_level:"+pre_bat_level);
            Log.d("ChargeApp","status_temp:"+status_temp);
            Log.d("ChargeApp","pre_bat_status:"+pre_bat_status);
            SharedPreferences dataStore = getSharedPreferences("DataStore", MODE_PRIVATE);
            boolean logging_Switch=dataStore.getBoolean("logging_Switch",false);

            if((battery_temp == pre_bat_level &&status_temp==pre_bat_status)
                    ||(status_temp==BatteryManager.BATTERY_STATUS_DISCHARGING&&logging_Switch==true)){

            }
            else{
                battery=battery_temp;
                battery_stat=status_temp;
                pre_bat_level=battery_temp;
                pre_bat_status=status_temp;
                log_time_info=getNowTime();
                log_data=log_time_info+","+battery+","+getBattStat(battery_stat)+"\n";
                //ログを外部ストレージに保存する
                if (isExternalStorageWritable() ) {
                    try (FileOutputStream fileOutputStream =
                                 new FileOutputStream(file, true);
                         OutputStreamWriter outputStreamWriter =
                                 new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                         BufferedWriter bw =
                                 new BufferedWriter(outputStreamWriter);
                    ) {

                        bw.write(log_data);
                        bw.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),"ストレージが見つからないためログを保存できません",Toast.LENGTH_LONG).show();
                    }
                } else {
                    return;
                }

                //StatusがFullになったら通知を送る
                if(battery_stat==BatteryManager.BATTERY_STATUS_FULL) {
                 //   SharedPreferences dataStore = getSharedPreferences("DataStore", MODE_PRIVATE);
                    boolean notice_end = dataStore.getBoolean("end_Switch", true);
                    if (notice_end) {
                        NotificationManager log_end = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        // Notification　Channel 設定
                        String channelId = "default";
                        int requestCode = intent.getIntExtra("RequestCode", 0);
                        // app name
                        String title = context.getString(R.string.app_name);
                        //通知オンの設定
                        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        //通知をクリックしたらメインアクティビティが立ち上がるようにする
                        Intent notice_intent= new Intent(getApplicationContext(),MainActivity.class);


                        PendingIntent pendingIntent =
                                PendingIntent.getActivity(context, requestCode, notice_intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationChannel channel = new NotificationChannel(
                                channelId,
                                title,
                                NotificationManager.IMPORTANCE_DEFAULT);

                        channel.setDescription("充電完了しました!!");
                        channel.enableVibration(true);
                        channel.canShowBadge();
                        channel.enableLights(true);
                        channel.setLightColor(Color.BLUE);
                        // the channel appears on the lockscreen
                        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                        channel.setSound(defaultSoundUri, null);
                        channel.setShowBadge(true);

                        if (log_end != null) {
                            log_end.createNotificationChannel(channel);

                            Notification notification = new Notification.Builder(context, channelId)
                                    .setContentTitle(title)
                                    // android標準アイコンから
                                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                                    .setContentText("充電完了しました!!")
                                    .setAutoCancel(true)
                                    .setContentIntent(pendingIntent)
                                    .setChannelId("default")
                                    .setWhen(System.currentTimeMillis())
                                    .build();

                            // 通知
                            log_end.notify(R.string.app_name, notification);
                            Log.d("デバッグ","完了通知おｋ");

                        }
                    }
                }
            }


        }
    };

    //時刻を取得する関数　ログデータ用日付無し
    public static String getNowTime() {
        final DateFormat df = new SimpleDateFormat("HH:mm:ss");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }
    //Battery Status============================================================================
    private String getBattStat(int batterystat) {
        String battStat = "Error";

        switch (batterystat) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                battStat = "Unknown";
                break;

            case BatteryManager.BATTERY_STATUS_CHARGING:
                battStat = "Charging";
                break;

            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                battStat = "DisCharging";
                break;

            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                battStat = "Not Charging";
                break;

            case BatteryManager.BATTERY_STATUS_FULL:
                battStat = "Full";
                break;
        }
        return battStat;
    }
    //ログ取得の許可に必要な関数
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
