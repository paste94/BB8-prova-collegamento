package com.example.ricca.bb8_prova_collegamento;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.orbotix.ConvenienceRobot;
import com.orbotix.Sphero;
import com.orbotix.common.DiscoveryAgentEventListener;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.le.RobotRadioDescriptor;

import java.util.List;

public class MainActivity extends AppCompatActivity implements RobotChangedStateListener {

    // View
    private TextView daStatus;
    private TextView robotStatus;
    private Button btnRed;
    private Button btnBlue;
    private Button btnYellow;

    // Activity
    private BluetoothAdapter mBluetoothAdapter;

    // Orbotix classes
    private static ConvenienceRobot _robot;
    private DiscoveryAgentLE _discoveryAgent;
    private DiscoveryAgentEventListener _discoveryAgentEventListener = new DiscoveryAgentEventListener() {
        @Override
        public void handleRobotsAvailable(List<Robot> robots) {
            daStatus.setText("Found " + robots.size() + " robots");

            for (Robot robot : robots) {
                daStatus.setText(daStatus.getText().toString() + "\n" + robot.getName());
            }
        }

    };
    // The Robot listener to handle state changes
    private RobotChangedStateListener _robotStateListener = new RobotChangedStateListener() {
        @Override
        public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType robotChangedStateNotificationType) {
            switch (robotChangedStateNotificationType) {
                case Online:
                    stopDiscovery();


                    robotStatus.setText("Robot " + robot.getName() + " is Online!");
                    _robot = new Sphero(robot);

                    // Finally for visual feedback let's turn the robot green saying that it's been connected
                    _robot.setLed(0f, 1f, 0f);

                    _robot.enableCollisions(true); // Enabling the collisions detector

                    enableColorButtons(true);

                    //newGameButton.setEnabled(true);
                    //newGameButton.setClickable(true);
                    break;
                case Offline:
                    robotStatus.setText("Robot " + robot.getName() + " is now Offline!");
                    //newGameButton.setClickable(false);
                    //newGameButton.setEnabled(false);
                    startDiscovery();
                    break;
                case Connecting:
                    robotStatus.setText("Connecting to " + robot.getName());
                    //newGameButton.setClickable(false);
                    //newGameButton.setEnabled(false);
                    break;
                case Connected:
                    robotStatus.setText("Connected to " + robot.getName());
                    //newGameButton.setClickable(false);
                    //newGameButton.setEnabled(false);
                    break;
                case Disconnected:
                    robotStatus.setText("Disconnected from " + robot.getName());
                    //newGameButton.setClickable(false);
                    //newGameButton.setEnabled(false);
                    startDiscovery();
                    break;
                case FailedConnect:
                    robotStatus.setText("Failed to connect to " + robot.getName());
                    //newGameButton.setClickable(false);
                    //newGameButton.setEnabled(false);
                    startDiscovery();
                    break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        reactivateBluetoothOrLocation();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    private void reactivateBluetoothOrLocation(){
        // Check if location is still enabled
        final LocationManager mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(MainActivity.this, BluetoothConnectionActivity.class));
        }

        // Check if bluetooth is still enabled
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()){
            startActivity(new Intent(MainActivity.this, BluetoothConnectionActivity.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRed = (Button) findViewById(R.id.btnRed);
        btnBlue = (Button) findViewById(R.id.btnBlue);
        btnYellow = (Button) findViewById(R.id.btnYellow);

        enableColorButtons(false);

        daStatus = (TextView) findViewById(R.id.dastatus);
        robotStatus = (TextView) findViewById(R.id.robotstatus);

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        // Check if bluetooth or location are still enabled
        reactivateBluetoothOrLocation();

        DiscoveryAgentLE.getInstance().addRobotStateListener(this);

        startDiscovery();
    }

    private void enableColorButtons(boolean b){
        btnRed.setEnabled(b);
        btnBlue.setEnabled(b);
        btnYellow.setEnabled(b);
    }

    @Override
    public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType robotChangedStateNotificationType) {

    }

    private void startDiscovery() {
        try {
            _discoveryAgent = DiscoveryAgentLE.getInstance();

            // DiscoveryAgentLE serve a mandare una notifica appena trova un robot.
            // Per fare ciò ha bisogno di un elenco di handler degli eventi, fornitigli
            // dall'implementazione del metodo handleRobotsAvailable nella classe DiscoveryAgentEventListener
            _discoveryAgent.addDiscoveryListener(_discoveryAgentEventListener);

            // Allo stesso modo settiamo l'handler per il cambiamento di stato del robot
            _discoveryAgent.addRobotStateListener(_robotStateListener);

            // Creating a new radio descriptor to be able to connect to the BB8 robots
            RobotRadioDescriptor robotRadioDescriptor = new RobotRadioDescriptor();
            robotRadioDescriptor.setNamePrefixes(new String[]{"BB-"});
            _discoveryAgent.setRadioDescriptor(robotRadioDescriptor);

            // Then to start looking for a BB8, you use DiscoveryAgent#startDiscovery()
            // You do need to handle the discovery exception. This can occur in cases where the user has
            // Bluetooth off, or when the discovery cannot be started for some other reason.
            _discoveryAgent.startDiscovery(this);
        } catch (DiscoveryException e) {
            Log.e("Sphero", "Discovery Error: " + e);
            e.printStackTrace();
        }
    }

    private void stopDiscovery() {
        // When a robot is connected, this is a good time to stop discovery. Discovery takes a lot of system
        // resources, and if left running, will cause your app to eat the user's battery up, and may cause
        // your application to run slowly. To do this, use DiscoveryAgent#stopDiscovery().
        _discoveryAgent.stopDiscovery();

        // It is also proper form to not allow yourself to re-register for the discovery listeners, so let's
        // unregister for the available notifications here using DiscoveryAgent#removeDiscoveryListener().
        _discoveryAgent.removeDiscoveryListener(_discoveryAgentEventListener);
        _discoveryAgent.removeRobotStateListener(_robotStateListener);
        _discoveryAgent = null;
    }

    public void ledColorRed(View v){
        _robot.setLed(1,0,0);
    }

    public void ledColorBlue(View v){
        _robot.setLed(0, 0, 1);
    }

    public void ledColorYellow(View v){
        _robot.setLed(0, 1, 1);
    }
}
