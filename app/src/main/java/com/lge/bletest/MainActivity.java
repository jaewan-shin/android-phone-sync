package com.lge.bletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity {

    private BLEPeripheral blePeri;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.AdvBtn).setOnClickListener(AdvBtnClickListener);
        findViewById(R.id.SendBtn).setOnClickListener(SendBtnClickListener);
        findViewById(R.id.clearBtn).setOnClickListener(ClearBtnClickListener);
        Button SendBtn = (Button)findViewById(R.id.SendBtn);
        SendBtn.setEnabled(false);


        blePeri = new BLEPeripheral();

        //bluetooth auto on(without alert)
        if(!BLEPeripheral.isEnableBluetooth())
        {
            BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
            mAdapter.enable();

        }

       initBLE();
       blePeri.setConnectionCallback(new BLEPeripheral.ConnectionCallback() {
                                         @Override
                                         public void onConnectionStateChange(BluetoothDevice device, int newState) {
                                             Message msg = new Message();
                                             msg.what = newState;
                                             mConnectTextHandler.sendMessage(msg);
                                         }
                                     }

       );
       //Notification Access Setting Auto
//       Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
  //     startActivity(intent);
       LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));

   }

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String pack = intent.getStringExtra("package");
        //    String ticker = intent.getStringExtra("ticker");
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            TextView notiText = (TextView)findViewById(R.id.notiText);

            String notiInfo = pack+"\n"+title +"\n" +text;
            notiText.setText(notiText.getText()+"\n"+notiInfo);
            blePeri.sendBLEMessage(notiInfo);
        }
    };



    Button.OnClickListener AdvBtnClickListener = new View.OnClickListener(){
        public void onClick(View v){
            if(!BLEPeripheral.isEnableBluetooth()){
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Bluetooth를 키는 중입니다.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
            initBLE();

            TextView ConnText = (TextView)findViewById(R.id.Conn);
            Button AdvBtn = (Button)findViewById(R.id.AdvBtn);

            if(AdvBtn.getText().toString() == "Disconnet"){
                ConnText.setText("Status : disconnected!");
                AdvBtn.setEnabled(true);
                BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
                mAdapter.disable();
                //need to disconnect device function in BLEPeripheral
            }else {

                ConnText.setText("Advertising...");
                AdvBtn.setEnabled(false);
                blePeri.setService();
                blePeri.startAdvertise();
            }
        }
    };


    Button.OnClickListener SendBtnClickListener = new View.OnClickListener(){
        public void onClick(View v){

            EditText msgText = (EditText)findViewById(R.id.msgText);
            String msg = msgText.getText().toString();
            blePeri.sendBLEMessage(msg);
        }
    };

    Button.OnClickListener ClearBtnClickListener = new View.OnClickListener(){
        public void onClick(View v){
            TextView notiText = (TextView)findViewById(R.id.notiText);
            String notiInfo = "++ Notification Information ++";
            notiText.setText(notiInfo);
        }
    };

    Handler mConnectTextHandler = new Handler(){

        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);

            String data;
            Button AdvBtn = (Button)findViewById(R.id.AdvBtn);
            Button SendBtn = (Button)findViewById(R.id.SendBtn);
            switch(msg.what){

                case 0:
                    data = new String("Status : disconnected!");
                    AdvBtn.setEnabled(true);
                    SendBtn.setEnabled(false);
                    break;
                case 2:
                    data = new String("Status : connected!");
                    AdvBtn.setEnabled(false);
                    SendBtn.setEnabled(true);
                    break;
                default:
                    data = "";
                    break;
            }
            TextView ConnText = (TextView)findViewById(R.id.Conn);
            ConnText.setText(data);
        }
    };

    @Override
    public void onResume(){
        super.onResume();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    public void initBLE(){
        blePeri.init(this);
    }


}
