package com.training.simadmin;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.se.omapi.Channel;
import android.se.omapi.Reader;
import android.se.omapi.SEService;
import android.se.omapi.Session;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.se.omapi.SEService;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.Build.VERSION_CODES.P;

@RequiresApi(api = P)
public class MainActivity extends AppCompatActivity implements SEService.OnConnectedListener {
    private static final int REQUEST_SIM_CONTROL = 22;
    private SEService seService;
    private Button Send;
    private EditText text;
    final String LOG_TAG = "SIM Test";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionVerifier();
        Send = findViewById(R.id.button);
        text = findViewById(R.id.apdu);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        seService = new SEService(MainActivity.this, pool, MainActivity.this);
        Send.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //String cmd = text.getText().toString();
                runOnUiThread(new Runnable() {
                    public void run() {
                        serviceConnected(seService);
                        try{
                            Reader[] readers = seService.getReaders();
                            String t= seService.getVersion();
                            boolean b=seService.isConnected();
                            String c= String.valueOf(b);
                            String r= Arrays.toString(readers);
                            if (readers.length < 1){
                                Log.i(LOG_TAG, "readers: "+ r);
                                Log.i(LOG_TAG, "OMAPI Ver: "+ t + " isConnected?: " + c);
                            }

                            Session session = readers[0].openSession();

                            Channel channel = session.openBasicChannel(new byte[]{
                                    (byte) 0xA0,0x00,0x00,0x00,(byte)0x87,0x10,0x02,(byte)0xFF,(byte)0x86,(byte)0xFF,0x02,(byte)0x89,0x06,0x01,0x00,(byte)0xFF});

                            if(channel != null) {
                                byte[] respApdu = channel.transmit(new byte[]{
                                        (byte) 0x00,(byte) 0xA4, 0x00, 0x0C, 0x02,0x6F,0x07});
                                String s = new String(respApdu);

                                //Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
                                // Parse response APDU and show text but remove SW1 SW2 first
                                // System.arraycopy(respApdu, 0, helloStr, 0, respApdu.length - 2);
                                Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();

                            }
                            else{
                                Toast.makeText(MainActivity.this, "null channel", Toast.LENGTH_LONG).show();
                            }
                            channel.close();
                        } catch (Exception e) {
                            Log.i(LOG_TAG, "Message: " + e.getClass().getCanonicalName());
                            if(e.getClass().isInstance(new NoSuchElementException()))
                                Toast.makeText(MainActivity.this, "AID Cannot be selected", Toast.LENGTH_LONG).show();
                            else if (e.getClass().isInstance(new IOException())){
                                Toast.makeText(MainActivity.this, "Problem with Reader", Toast.LENGTH_LONG).show();
                            }
                            else if (e.getClass().isInstance(new IllegalStateException())){
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            else if (e.getClass().isInstance(new IllegalArgumentException())){
                                Toast.makeText(MainActivity.this, "AID LE not between 5-16", Toast.LENGTH_LONG).show();
                            }
                            else if (e.getClass().isInstance(new SecurityException())){
                                Toast.makeText(MainActivity.this, "cannot be granted access to this AID", Toast.LENGTH_LONG).show();
                            }
                            else if (e.getClass().isInstance(new UnsupportedOperationException())){
                                Toast.makeText(MainActivity.this, "P2 not supported by device", Toast.LENGTH_LONG).show();
                            }
                            else{
                                return;
                                //Toast.makeText(MainActivity.this, "No exception?", Toast.LENGTH_LONG).show();
                            }

                        }
                    }
                });

            }
        });

        //ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.MODIFY_PHONE_STATE},REQUEST_SIM_CONTROL);

}
   // private void SendApdu(){

 //       ExecutorService pool = Executors.newSingleThreadExecutor();
 //       seService = new SEService(this, pool, this);
    //}

    public void serviceConnected(SEService service) {
        Log.i(LOG_TAG, "seviceConnected()");
    }

    private void permissionVerifier() {
        String[] permissions = {Manifest.permission.MODIFY_PHONE_STATE,Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED) {
            Toast toast = Toast.makeText(this, "you have been granted permissions", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_SIM_CONTROL);
            Toast toast = Toast.makeText(this, "THANKS!!!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
@Override
protected void onDestroy() {
        if (seService != null && seService.isConnected()) {
        seService.shutdown();
        }
        super.onDestroy();
        }


    @Override
    public void onConnected() {

    }
}