package com.lge.bletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Created by hawkeye1 on 2016-03-24.
 */
public class BLEPeripheral {



    public interface ConnectionCallback {
        void onConnectionStateChange(BluetoothDevice device, int newState);
    }

    BluetoothManager mManager;
    BluetoothAdapter mAdapter;

    BluetoothLeAdvertiser mLeAdvertiser;

    AdvertiseSettings.Builder settingBuilder;
    AdvertiseData.Builder advBuilder;

    BluetoothGattServer mGattServer;

    ConnectionCallback mConnectionCallback;
    BluetoothGattCharacteristic notifyCharacteristic;

    private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {

        @Override
        public void onStartFailure(int errorCode){
            Log.d("advertise", "onStartFailure");
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect){
            Log.d("advertise", "onStartSuccess");
        };
    };



    private final BluetoothGattServerCallback mGattServerCallback
            = new BluetoothGattServerCallback(){

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState){
            Log.d("GattServer", "Our gatt server connection state changed, new state ");
            Log.d("GattServer", Integer.toString(newState));

            if(null != mConnectionCallback && BluetoothGatt.GATT_SUCCESS == status)
                mConnectionCallback.onConnectionStateChange(device, newState);

            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d("GattServer", "Our gatt server service was added.");
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d("GattServer", "Our gatt characteristic was read.");
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    characteristic.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status)
        {
            Log.d("GattServer", "onNotificationSent");
            super.onNotificationSent(device, status);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d("GattServer", "Our gatt server descriptor was read.");
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

        }



    };

    public void setConnectionCallback(ConnectionCallback callback)
    {
        mConnectionCallback = callback;
    }


    public static boolean isEnableBluetooth(){
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }


    public int init(Context context){

        if(null == mManager)
        {
            mManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);

            if(null == mManager)
                return -1;

            if(false == context.getPackageManager().
                    hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
                return -2;
        }

        if(null == mAdapter)
        {
            mAdapter = mManager.getAdapter();

            if(false == mAdapter.isMultipleAdvertisementSupported())
                return -3;
        }

        if(null == settingBuilder)
        {
            settingBuilder = new AdvertiseSettings.Builder();
            settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
            settingBuilder.setConnectable(true);
            settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        }

        if(null == advBuilder)
        {
            advBuilder = new AdvertiseData.Builder();
            mAdapter.setName("G3 Demo");
            advBuilder.setIncludeDeviceName(true);
        }


        if(null == mGattServer)
        {
            mGattServer = mManager.openGattServer(context, mGattServerCallback);

            if(null == mGattServer)
                return -4;

       //     addDeviceInfoService();
        }

        return 0;
    }

   public void setService(){
       if(null == mGattServer)
           return;
       stopAdvertise();

       final String  SERVICE = "0000ff00-0000-1000-8000-00805f9b34fb";
       final String  CHAR_NOTIFY = "0000ff01-0000-1000-8000-00805f9b34fb";

       BluetoothGattService previousService =
               mGattServer.getService( UUID.fromString(SERVICE));

       if(null != previousService)
           mGattServer.removeService(previousService);

       BluetoothGattService Service = new BluetoothGattService(
               UUID.fromString(SERVICE),
               BluetoothGattService.SERVICE_TYPE_PRIMARY);

       notifyCharacteristic = new BluetoothGattCharacteristic(
               UUID.fromString(CHAR_NOTIFY),
               BluetoothGattCharacteristic.PROPERTY_NOTIFY,
               BluetoothGattCharacteristic.PERMISSION_READ
       );

       notifyCharacteristic.setValue(new String("empty"));
       Service.addCharacteristic(notifyCharacteristic);

       mGattServer.addService(Service);
   }

    public void startAdvertise(){

        if(null == mAdapter)
            return;

        if (null == mLeAdvertiser)
            mLeAdvertiser = mAdapter.getBluetoothLeAdvertiser();

        if(null == mLeAdvertiser)
            return;

        mLeAdvertiser.startAdvertising(settingBuilder.build(),
                advBuilder.build(), mAdvCallback);

    }

    public void stopAdvertise()
    {
        if(null != mLeAdvertiser)
            mLeAdvertiser.stopAdvertising(mAdvCallback);

        mLeAdvertiser = null;
    }

    public void sendBLEMessage(String msg){

        List<BluetoothDevice> connectedDevices
                = mManager.getConnectedDevices(BluetoothProfile.GATT);

        if(null != connectedDevices)
        {
            notifyCharacteristic.setValue(msg.getBytes());

            if(0 != connectedDevices.size())
                mGattServer.notifyCharacteristicChanged(connectedDevices.get(0),
                        notifyCharacteristic, false);

        }

    }

}
