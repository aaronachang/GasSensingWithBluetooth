package com.example.david.gassensingwithbluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 0;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public int check = 0;
    public int lastX = 0;
    public double Time = 0;
    public int begin = 0;
    public double lastY = 0.0;
    public double lastYB = 0.0;
    public double lastYC = 0.0;
    public boolean A_Flag = false;
    public boolean B_Flag = false;
    public boolean C_Flag = false;
    public double term = 2000000000;
    public boolean  time_start = false;
    public double x_axis = 0;
    BluetoothDevice mDevice;
    ImageView mBlueIV;
    //make new adapter
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    TextView mBluecon;
    TextView mBluenotcon;
    TextView mBluesocketfailed;
    EditText eText;
    private LineGraphSeries<DataPoint> series;
    private LineGraphSeries<DataPoint> series1;
    private LineGraphSeries<DataPoint> series2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mBlueIV = findViewById(R.id.bluetoothimage);


        //check if bluetooth is supported on current device
        if (mBluetoothAdapter == null) {
            //mBlueIV.setImageResource(R.drawable.bluetoothdisabled);
            //not supported
        }


        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mDevice = device;
            }

            ConnectThread mConnectThread = new ConnectThread(mDevice);
            mConnectThread.start();
        }
        GraphView graph = (GraphView) findViewById(R.id.graph);
        //series = new LineGraphSeries<DataPoint>();
        //graph.addSeries(series);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Concentration (ppb)");
        graph.setTitle("Concentration of Gases");
        Viewport viewport = graph.getViewport();

        viewport.setScalable(true);
        //viewport.setScrollable(true);
       // viewport.setScalableY(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0.0);
        graph.getViewport().setMaxY(7.0);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0.0);
        graph.getViewport().setMaxX(10);

        update_UI mupdate = new update_UI(Time);
        mupdate.start();


    }

    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {


                while (true) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            eText = (EditText) findViewById(R.id.edittext);
                            Button btn = (Button) findViewById(R.id.button);
                            btn.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    term = Double.parseDouble(eText.getText().toString());
                                    check = 0;
                                }
                            });
                            Button strt = (Button) findViewById(R.id.button1);
                            strt.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    GraphView graph = (GraphView) findViewById(R.id.graph);


                                    if(begin == 0){
                                        series = new LineGraphSeries<DataPoint>();
                                        series1 = new LineGraphSeries<DataPoint>();
                                        series2 = new LineGraphSeries<DataPoint>();
                                        series1.setColor(Color.GREEN);
                                        series.setTitle("Gas 1");
                                        series1.setTitle("Gas 2");
                                        series2.setTitle("Gas 3");
                                        series2.setColor(Color.RED);
                                        graph.addSeries(series);
                                        graph.addSeries(series1);
                                        graph.addSeries(series2);
                                        graph.getLegendRenderer().setVisible(true);
                                        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);}
                                    begin = 1;
                                    time_start = true;
                                }

                            });
                            Button rst = (Button) findViewById(R.id.button2);
                            rst.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    begin = 0;
                                    GraphView graph = (GraphView) findViewById(R.id.graph);
                                    graph.removeAllSeries();
                                    lastX = 0;
                                    lastY = 0;
                                    check = 0;
                                    Time = 0;
                                    x_axis = 0;
                                    time_start = false;


                                }
                            });
                            if (begin == 1) {
                                addEntry();
                            }
                        }
                    });

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {

                    }
                }

            }

        }).start();
    }

    private void addEntry() {
        //setContentView(R.layout.activity_main);
        if(x_axis <= Time + 1) {
            x_axis = x_axis + 0.1;
//            Random random = new Random();
//            double avg = 0;
//            for (int i = 0; i < 10; i++) {
//                avg += random.nextDouble()*5;
//            }
//            series.appendData(new DataPoint(x_axis, avg/10), true, 150);
//            avg = 0;
//            for (int i = 0; i < 10; i++) {
//                avg += random.nextDouble()*5;
//            }
//            series1.appendData(new DataPoint(x_axis, avg/10), true, 150);
//            avg = 0;
//            for (int i = 0; i < 10; i++) {
//                avg += random.nextDouble()*5;
//            }
//            series2.appendData(new DataPoint(x_axis, avg/10), true, 150);
                series.appendData(new DataPoint(x_axis, lastY), true, 150);
                series1.appendData(new DataPoint(x_axis, lastYB), true, 150);
                series2.appendData(new DataPoint(x_axis, lastYC), true, 150);
        }

        TextView xdata = (TextView) findViewById(R.id.textView3);
        TextView ydata = (TextView) findViewById(R.id.textView4);
        xdata.setText(String.valueOf(Time));
        ydata.setText(String.valueOf(lastY));
        if ((term <= lastY) && (check == 0)) {
            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("WARNING");
            alertDialog.setMessage("Hazardous concentrations have been detected.");
            alertDialog.setIcon(R.drawable.alert_icon);
            alertDialog.setCanceledOnTouchOutside(true);
            alertDialog.show();
            vib.vibrate(400);
            check = 1;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
        } else {
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                mBluenotcon.setText("RFCOOMM failed");
            }
            mmSocket = tmp;
        }

        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            while (!mmSocket.isConnected()) {
                try {
                    mmSocket.connect();
                    //mBlueIV.setImageResource(R.drawable.bluetoothon);
                } catch (IOException connectException) {
                    //mBluesocketfailed.setText("socket connect failed");
                }
            }
            //mBlueIV.setImageResource(R.drawable.bluetoothon);
            ConnectedThread mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    /*Handler mHandler = new Handle() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;

            switch(msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    break;
            }
        }
    };*/

    private class update_UI extends Thread {
        TextView xdata;
        private int timex = 0;
        public update_UI(double temp) {
        }
        public void run() {
            //setContentView(R.layout.activity_main);
            //xdata.setText(String.valueOf(timex));
            while(true) {
                if(time_start) {
                    try {
                        Thread.sleep(1000);
                        Time = Time + 1;
                        //x_axis = Time;
                    } catch (InterruptedException e) {

                    }
                }
                else {
                    Time = 0;
                }
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            String str = "";
            String newstr = "";
            String newstrb = "";
            String newstrc = "";
            int beginstr = 0;
            int index = 0;
            int bytes = 0;
            NumberFormat format1 = new DecimalFormat("#0.00");
            double averageindex = 0;
            double averageindexb = 0;
            double averageindexc = 0;
            double average = 0;
            double averageb = 0;
            double averagec = 0;
            while (true) {
                try {
                    //buffer = new byte[1024];
                    if(bytes >= 1024){
                        bytes = 0;
                        buffer = new byte[1024];
                    }
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for (int i = 0; i < bytes; i++) {
                        if(averageindex == 10){
                            newstr = format1.format(average/averageindex);
                            if(newstr == ""){
                                newstr = "0";
                            }

                            lastY = Double.parseDouble(newstr);
                            average = 0;
                            averageindex = 0;
                        }
                        if(averageindexb == 10) {
                            newstrb = format1.format(averageb/averageindexb);
                            if(newstrb == ""){
                                newstrb = "0";
                            }

                            lastYB = Double.parseDouble(newstrb);
                            averageb = 0;
                            averageindexb = 0;
                        }
                        if(averageindexc == 10) {
                            newstrc = format1.format(averagec/averageindexc);
                            if(newstrc == ""){
                                newstrc = "0";
                            }

                            lastYC = Double.parseDouble(newstrc);
                            averagec = 0;
                            averageindexc = 0;
                        }
                        if(buffer[i] == 65){
                            //A_Flag = true;
                            //B_Flag = false;
                            //C_Flag = false;
                            beginstr = 0;
                            if(beginstr == 2){
                                beginstr = -1;
                            }
                            if(str == ""){
                                str = "0";
                            }
                            average += Double.parseDouble(str);
                            averageindex+=1;
                            str = "";
                        }
                        else if(buffer[i] == 66){
                            //A_Flag = false;
                            //B_Flag = true;
                            //C_Flag = false;
                            beginstr = 0;
                            if(beginstr == 2){
                                beginstr = -1;
                            }
                            if(str == ""){
                                str = "0";
                            }
                            averageb += Double.parseDouble(str);
                            averageindexb+=1;
                            str = "";
                        }
                        else if(buffer[i] == 67){
                            //A_Flag = false;
                            //B_Flag = false;
                            //C_Flag = true;
                            beginstr = 0;
                            if(beginstr == 2){
                                beginstr = -1;
                            }
                            if(str == ""){
                                str = "0";
                            }
                            averagec += Double.parseDouble(str);
                            averageindexc+=1;
                            str = "";
                        }
                        else{
                            if(buffer[i] == 46){
                                beginstr += 1;
                                if(beginstr <= 1){
                                    str = str + (char) buffer[i];
                                }
                            }
                            else {
                                str = str + (char) buffer[i];
                            }
                        }
                        /*else if(buffer[i] == ".".getBytes()[0]){
                            dec = true;
                            decvalue = 0;
                        }
                        else {
                            if (dec) {
                                lastY = lastY + (buffer[i] - 48) / 10 * decvalue;
                                decvalue += 1;
                            } else {
                                if(bytes - i == 1){
                                    lastY = lastY + (buffer[i] - 48);
                                }
                                else {
                                    lastY = lastY + (buffer[i] - 48) * (bytes - i-1)*10;
                                }
                            }
                        }*/

                        /*if (buffer[i] == "#".getBytes()[0]) {
                            //mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if (i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }*/
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}

