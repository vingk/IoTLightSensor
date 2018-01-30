package com.cmput660a1.www.iot660a1;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private IotCoreCommunicator communicator;
    private Handler handler;
    private SensorManager SenManager;
    private Sensor proxSensor;
    private Sensor lightSensor;
    private float proxval;
    private float lightval;
    private String payloadMsg;

    TextView proxv=null;
    TextView lightv=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        proxv = (TextView) findViewById(R.id.prox);
        lightv= (TextView) findViewById(R.id.light);
        // Setup the communication with your Google IoT Core details
        communicator = new IotCoreCommunicator.Builder()
                .withContext(this)
                .withCloudRegion("us-central1") // ex: europe-west1
                .withProjectId("cmput660a1")   // ex: supercoolproject23236
                .withRegistryId("cmput660IoTA1") // ex: my-devices
                .withDeviceId("A001") // ex: my-test-raspberry-pi
                .withPrivateKeyRawFileId(R.raw.rsa_private)
                .build();


        SenManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proxSensor = SenManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = SenManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        SenManager.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_NORMAL);
        SenManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        Log.d("Main","Registered Devices");
       // communicator.connect();
        //Log.d("Main","Connected");
    }



    private final Runnable connectOffTheMainThread = new Runnable() {
        @Override
        public void run() {
            communicator.connect();
            Log.d("Main","Connected");
            handler.post(sendMqttMessage);
            Log.d("Main","Message send");
            //communicator.disconnect();
            //Log.d("Main","Disc");
        }
    };

    private final Runnable sendMqttMessage = new Runnable() {
        private int i=0;

        /**
         * We post 100 messages as an example, 1 a second
         */

        @Override
        public void run() {
            if (i == 2) {
                return;
            }

            Log.d("Main","Sending Message");
            // events is the default topic for MQTT communication
            String subtopic = "";
            // Your message you want to send
            String message = payloadMsg;
            i++;
            communicator.publishMessage(subtopic, message);
            handler.postDelayed(this, TimeUnit.SECONDS.toMillis(1));
        }
    };

    @Override
    protected void onDestroy() {
        communicator.disconnect();
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        Log.d("Main","Sensor Values have changed");
        if(sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY){
            this.proxval = sensorEvent.values[0];
            proxv.setText(""+String.valueOf(sensorEvent.values[0]));

            Log.d("Main","New Proximity value"+String.valueOf(this.proxval));
        }
        if(sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT){
            // check if the phone is in pocket, dont send light data when phone is inside.
            if(this.proxval>0){
                // check the change is significant
               if(Math.abs(sensorEvent.values[0]-this.lightval)>20 ){
                   lightv.setText(""+String.valueOf(sensorEvent.values[0]));
                   this.lightval = sensorEvent.values[0];
                   Log.d("Main","New Light value"+String.valueOf(this.lightval));
                   if(this.lightval <70)payloadMsg = "{Msg:There is NO light,val:"+String.valueOf(this.lightval)+"}";
                   else payloadMsg="{Msg:There is light,val:"+String.valueOf(this.lightval)+"}";

                   //communicator.publishMessage("", payloadMsg);

                   HandlerThread thread = new HandlerThread("MyBackgroundThread");
                   thread.start();
                   handler = new Handler(thread.getLooper());
                   handler.post(connectOffTheMainThread); // Use whatever threading mechanism you want
               }
            }
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
