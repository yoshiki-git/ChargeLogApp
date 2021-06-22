package com.example.chargelogapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//2020年　9月24日作成　By佐藤嘉起
//2020年　10月6日　ログSTOP時に再起動するとアプリがクラッシュする不具合を修正
//2021/6/18 Android11でAndroid/data以下のストレージが見れなくなる問題の対応
public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE = 1000;

    private Button start_Button;
    private Button stop_Button;
 //   private Switch name_Switch_btn;
    private Switch end_Switch_btn;
    private Switch not_logging_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        checkPermission(permissions, REQUEST_CODE);

        //Android11以降対象
        //直パスを使えるようにするpermissionの付与
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R) {
            checkPermission();
        }



        startApplication();
    }

    private void startApplication(){

     start_Button=findViewById(R.id.start_Button);
     stop_Button=findViewById(R.id.stop_Button);
  //   name_Switch_btn=findViewById(R.id.switch1);
     end_Switch_btn=findViewById(R.id.switch2);
     not_logging_btn=findViewById(R.id.switch3);



        // "DataStore"という名前でインスタンスを生成
        SharedPreferences dataStore = getSharedPreferences("DataStore", MODE_PRIVATE);
        //エディターの登録
        final SharedPreferences.Editor editor =dataStore.edit();

        //各種設定の呼び出し
        boolean start_Switch=dataStore.getBoolean("start_Button",true);
        boolean stop_Switch=dataStore.getBoolean("stop_Button",false);
    //    boolean name_Switch=dataStore.getBoolean("name_Switch",true);
        boolean end_Switch=dataStore.getBoolean("end_Switch",true);
        boolean logging_Switch=dataStore.getBoolean("logging_Switch",false);


        //設定をSwitchに反映させる
        if(start_Switch){
            start_Button.setEnabled(true);
            stop_Button.setEnabled(false);
        }
        if(stop_Switch){
            stop_Button.setEnabled(true);
            start_Button.setEnabled(false);
        }
  /*      if(name_Switch){
            name_Switch_btn.setChecked(true);
        }else {
            name_Switch_btn.setChecked(false);
        }*/
        if(end_Switch){
            end_Switch_btn.setChecked(true);
        }else {
            end_Switch_btn.setChecked(false);
        }
        if(logging_Switch){
            not_logging_btn.setChecked(true);
        }else {
            not_logging_btn.setChecked(false);
        }

    /*    //NameSwitchのリスナ
        name_Switch_btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(name_Switch_btn.isChecked()){
                    editor.putBoolean("name_Switch",true);
                }
                else {
                    editor.putBoolean("name_Switch",false);
                }
                editor.commit();
            }
        }); */

        //EndSwitchのリスナ
        end_Switch_btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(end_Switch_btn.isChecked()){
                    editor.putBoolean("end_Switch",true);
                }
                else {
                    editor.putBoolean("end_Switch",false);
                }
                editor.commit();
            }
        });
        //LoggingSwitchのリスナ
        not_logging_btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (not_logging_btn.isChecked()) {
                    editor.putBoolean("logging_Switch",true);
                }
                else {
                    editor.putBoolean("logging_Switch",false);
                }
                editor.commit();
            }
        });

     start_Button.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
             String nowTime=getFileName();
             String fileName=nowTime+".txt";
             editor.putString("fileName",fileName);
             editor.putBoolean("start_Button",false);
             editor.putBoolean("stop_Button",true);
             editor.commit();
             start_Button.setEnabled(false);
             stop_Button.setEnabled(true);
             Intent intent=new Intent(getApplication(),MyService.class);
             intent.putExtra("REQUEST_CODE", 1);

             // Serviceの開始
             //startService(intent);
             startForegroundService(intent);
         }
     });

     stop_Button=findViewById(R.id.stop_Button);
     stop_Button.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
             editor.putBoolean("stop_Button",false);
             editor.putBoolean("start_Button",true);
             editor.commit();
             stop_Button.setEnabled(false);
             start_Button.setEnabled(true);
             Intent intent = new Intent(getApplication(), MyService.class);
             // Serviceの停止
             stopService(intent);
         }
     });

    }

    public void checkPermission(final String[] permissions,final int request_code){
        // 許可されていないものだけダイアログが表示される
        ActivityCompat.requestPermissions(this, permissions, request_code);
    }

    // requestPermissionsのコールバック
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {

            case REQUEST_CODE:
                for(int i = 0; i < permissions.length; i++ ){
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                   /*     Toast toast = Toast.makeText(this,
                                "Added Permission: " + permissions[i], Toast.LENGTH_SHORT);
                        toast.show(); */
                    } else {
                        Toast toast = Toast.makeText(this,
                                "設定より権限をオンにした後、アプリを再起動してください", Toast.LENGTH_LONG);
                        toast.show();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        //Fragmentの場合はgetContext().getPackageName()
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
                break;
            default:
                break;
        }
    }

    //直パス使えるようにするpermissionの付与
    @TargetApi(Build.VERSION_CODES.R)
    public void checkPermission() {

        if (Environment.isExternalStorageManager()) {
            //todo when permission is granted
            Log.d("デバッグ","MANAGE_EXTERNAL_STORAGE is granted");
        } else {
            //request for the permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    }




    //時刻を取得する関数　ファイル名用日付と/と:が無い
    public static String getFileName() {
        final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }
}