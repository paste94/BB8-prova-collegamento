package com.example.ricca.bb8_prova_collegamento;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

/**
 * Created by ricca on 31/07/17.
 */

public class BluetoothConnectionActivity extends AppCompatActivity {

    private int BLUETOOTH_ENABLE_RCODE = 1;
    private int LOCATION_PERMISSIONS_RCODE = 2;
    private int LOCATION_ENABLE_RCODE = 3;

    private TextView btStatus;
    private TextView lcStatus;

    private Boolean btIsOn = false;
    private Boolean lcIsOn = false;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        btStatus = (TextView) findViewById(R.id.btstatus);
        lcStatus = (TextView) findViewById(R.id.lcstatus);

        // Location
        requestLocationPermissions();

        // Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()){
            enableBluetooth();
        }else{
            btIsOn = true;
        }

        checkToContinue();
    }

    // Richiede l'autorizzazione all'applicazione per accedere ai servizi del GPS
    private void requestLocationPermissions(){
        if (Build.VERSION.SDK_INT >= 23){
            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                enableLocation();
            } else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                        && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)){
                    lcStatus.setText("Location services permissions required to use this app");
                    lcIsOn = false;
                }
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSIONS_RCODE);
            }
        } else {
            enableLocation();
        }
    }

    // Se bluetooth e GPS sono entrambi attivi, ritorna all'activity principale.
    private void checkToContinue(){
        if(btIsOn && lcIsOn){
            startActivity(new Intent(BluetoothConnectionActivity.this, MainActivity.class));
        }
    }

    // Attiva il GPS
    private void enableLocation() {
        final LocationManager mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        /* mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
         * Restituisce true se il GPS è attivo, false altrimenti
         */
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            displayLocationSettingsRequest(getApplicationContext());
        }else{
            lcStatus.setText("Location: ON");
            lcIsOn = true;
        }
    }

    // Chiede all'utente di attivare il bluetooth, se acconsente lo attiva automaticamente
    private void enableBluetooth(){
        if(bluetoothAdapter == null){
            btStatus.setText(R.string.bluetooth_not_found);
        } else { //check the status and set the button text accordingly
            if (!bluetoothAdapter.isEnabled()) {
                btStatus.setText(R.string.bluetooth_is_currently_switched_off);
                Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                /* Lancia l'activity BluetoothActivity.
                 *  void startActivityForResult (Intent intent, int requestCode)
                 *      intent: The intent to start.
                 *      requestCode: If >= 0, this code will be returned in onActivityResult() when the activity exits.
                 */
                startActivityForResult(bluetoothIntent, BLUETOOTH_ENABLE_RCODE);
            }
        }
    }

    // Chiede all'utente di attivare il GPS
    private void displayLocationSettingsRequest(Context context) {
        /* GoogleApiClient:
         * Classe usata per collegarsi ai Google Play Services per attivare automaticamente
         * il GPS.
         */
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect(); //Avvia la connessione a Play Services

        /* LocationRequest: Usato per richiedere una certa precisione del GPS
         * https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest
         */
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        /* LocationSettingsRequest: Specifica il tipo di servizio di localizzazione di cui l'applicazione
         * ha bisogno
         */
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        /* LocationSettingsResult: Classe statica che rappresenta il risultato del metodo checkLocationSettings.
         *      Indica se deve essere mostrato un dialog in cui viene richiesto all'utente di modificare
         *      le sue impostazioni (in questo caso, di attivare il GPS)
         * PendingResult: Rappresenta un set di risultati derivanti dalla chiamata di un metodo ai
         *      Google Play services.
         */
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //Toast.makeText(getApplicationContext(), "All location settings are satisfied.", Toast.LENGTH_SHORT).show();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //Toast.makeText(getApplicationContext(), "Location settings are not satisfied. Show the user a dialog to upgrade location settings", Toast.LENGTH_SHORT).show();

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(BluetoothConnectionActivity.this, LOCATION_ENABLE_RCODE);
                        } catch (IntentSender.SendIntentException e) {
                            //Toast.makeText(getApplicationContext(), "PendingIntent unable to execute request.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

}
