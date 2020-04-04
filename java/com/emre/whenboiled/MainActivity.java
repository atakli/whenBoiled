package com.emre.whenboiled;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    public String address = null;
    private ProgressDialog progressDialog;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ListView lv_BtDevices;
    public BluetoothAdapter myBluetooth = BluetoothAdapter.getDefaultAdapter();
    private ArrayList<String> list_paired_devices;
    public TextView showTemp;
    public String readMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        showTemp = findViewById(R.id.showTemp);
        lv_BtDevices = (ListView) findViewById(R.id.list1);

        Button baglantiyi_kes = findViewById(R.id.baglantiyi_kes);
        list_paired_devices = new ArrayList<>();

        baglantiyi_kes.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { Disconnect(); }
        });
        if(myBluetooth == null)
        {
            Toast.makeText(getApplicationContext(),"Bluetooth özelliği bulunamadı", Toast.LENGTH_LONG).show();
            //finish();
        }
        else if(!myBluetooth.isEnabled())
        {
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }
        else
        {
            Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
            if(pairedDevices.size() > 0)
            for(BluetoothDevice bt : pairedDevices)     list_paired_devices.add(bt.getName() + "\n" + bt.getAddress());
            else
                Toast.makeText(getApplicationContext(), "Eşleştirilmiş Bluetooth Aygıtı Bulunamadı", Toast.LENGTH_LONG).show();
            ArrayAdapter<String> adapter_btn_devices = new ArrayAdapter<String>(MainActivity.this,R.layout.list_item_bt_devices,R.id.btn_devices,list_paired_devices);
            lv_BtDevices.setAdapter(adapter_btn_devices);
            lv_BtDevices.setOnItemClickListener(ListClickListener_bt_devices);
        }
        registerReceiver(mReceiver,filter);
    }
    public String receiveData(BluetoothSocket socket) throws IOException {
        InputStream socketInputStream = socket.getInputStream();
        byte[] buffer = new byte[256];
        int bytes;
        while (true)
            try
            {
                bytes = socketInputStream.read(buffer);
                readMessage = new String(buffer, 0, bytes);
                Log.i("logging", readMessage + "");
            } catch (IOException e) { break; }
        return readMessage;
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                switch(bluetoothState)
                {
                    case BluetoothAdapter.STATE_ON:
                    {
                        Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
                        if(pairedDevices.size() > 0)
                        {for(BluetoothDevice bt : pairedDevices)     list_paired_devices.add(bt.getName() + "\n" + bt.getAddress());}
                        else
                            Toast.makeText(getApplicationContext(), "Eşleştirilmiş Bluetooth Aygıtı Bulunamadı", Toast.LENGTH_LONG).show();
                        ArrayAdapter<String> adapter_btn_devices = new ArrayAdapter<String>(MainActivity.this,R.layout.list_item_bt_devices,R.id.btn_devices,list_paired_devices);
                        lv_BtDevices.setAdapter(adapter_btn_devices);
                        lv_BtDevices.setOnItemClickListener(ListClickListener_bt_devices);
                    }
                        break;
                }
            }
        }
    };
    /*@Override
    public void onResume()
    {
        super.onResume();
        try {
            showTemp.setText(receiveData(btSocket));
        } catch (IOException e) { e.printStackTrace(); }
    }*/
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
    private AdapterView.OnItemClickListener ListClickListener_bt_devices = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View v, int arg2, long arg3)
        {
            //String info = ((TextView) v).getText().toString();
            String info = (String) adapterView.getItemAtPosition(arg2);
            address = info.substring(info.length() - 17);
            new ConnectBT().execute();
        }
    };
    private void Disconnect()
    {
        if(btSocket != null)
        {
            try { btSocket.close();}
            catch(IOException e)
            {Toast.makeText(getApplicationContext(),"Hata",Toast.LENGTH_LONG).show();}
        }
        else    Toast.makeText(getApplicationContext(),"Zaten mevcut bir bağlantı yok",Toast.LENGTH_LONG).show();
        finish();
    }
    /*public void onPause()
    {
        super.onPause();
        try
        {
            btSocket.close();
        }catch(IOException e2){e2.printStackTrace();}//insert here something to deal with the exception
    }*/

    private class ConnectBT extends AsyncTask<Void, Void, Void>
    {
        private boolean ConnectSuccess = true;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute(); //tutorialda yok
            progressDialog = new ProgressDialog(MainActivity.this);
            //progressDialog.setTitle("NAMAZ VAKİTLERİ");
            progressDialog.setMessage("Bağlanıyor, Lütfen Bekleyin...");
            progressDialog.setIndeterminate(false);
            progressDialog.show();
        }
        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                if(btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
                showTemp.setText(receiveData(btSocket));
            }
            catch(IOException e){ConnectSuccess = false;}
            return null;
        }
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            if(!ConnectSuccess)
            {
                Toast.makeText(getApplicationContext(),"Bağlantı başarısız. Tekrar deneyin",Toast.LENGTH_LONG).show();
                //finish();
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Bağlantı başarılı. Şimdi sıcaklık değerlerini okuyabilirsiniz.",Toast.LENGTH_LONG).show();
                isBtConnected = true;
            }
            progressDialog.dismiss();
            //showTemp.setText("deneme");
        }
    }
}
