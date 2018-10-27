package com.example.asus.demondk;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");// Need load the lib if you did't install opencv manager.apk
        System.loadLibrary("native-lib");
    }
    Button btn;
    Button scanButton;
    Button disconveryButton;
    TextView textView;
    TextView texP;
    //BlueTooth
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothDevice device;
    private static BluetoothSocket socket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 一定要是這組
    private static OutputStream outputStream = null;
    private static InputStream inputStream = null;
    private final int REQUEST_ENABLE_BT=1;
    private static AlertDialog.Builder dialog;
    private static byte[] readBuffer;
    private static int readBufferPosition;
    private static Thread workerThread;
    private final static int MESSAGE_READ = 2;
    Handler handler;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;//------------------------------------------------------------
    int pointX=0;
    int pointY=0;
    int predistance=0;
    static Point point;
    //private static ArrayList<BluetoothDevice> deviceArrayList;

    private static JavaCameraView javaCameraView;
    private static Mat cameraFrame;


    Mat detectMat = new Mat();
    Bitmap footbm;
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    javaCameraView.enableView();
                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        javaCameraView = (JavaCameraView) findViewById(R.id.javaCameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        //BlueTooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        dialog=new AlertDialog.Builder(MainActivity.this);
        if (bluetoothAdapter == null) {//device doesn't support BT
            dialog.setMessage("Device doesn't support bluetooth");
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            dialog.show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            //開啟一個Intent去開啟藍芽
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
        Set<BluetoothDevice> devices=bluetoothAdapter.getBondedDevices();
        String tempS="";
        for(BluetoothDevice tempDevice:devices){
            tempS+=tempDevice.getAddress()+"  "+tempDevice.getName()+"\n";
        }
        dialog.setMessage(tempS);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        dialog.show();
        disconveryButton=(Button) findViewById(R.id.btn_discovery);
        disconveryButton.setOnClickListener(discoveryButtonListener);
        //-------------------------------------------------------------------------------------------copy past follw-------------------------------------------------------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//如果 API level 是大于等于 23(Android 6.0) 时
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要向用户解释为什么需要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Log.e("Message","自Android 6.0开始需要打开位置权限才可以搜索到Ble设备");
                }
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }
        boolean a=isLocationEnable(MainActivity.this);
        String s=String.valueOf(a);
        Toast.makeText(MainActivity.this,s,Toast.LENGTH_LONG).show();
        //-------------------------------------------------------------------------------------------copy past above-------------------------------------------------------------------------------------------------
        scanButton=(Button)findViewById(R.id.btn_scan);
        scanButton.setOnClickListener(scanButtonListener);
        btn=(Button)findViewById(R.id.btn_send);
        btn.setOnClickListener(send);
        textView=(TextView)findViewById(R.id.tex1);
        texP=(TextView)findViewById(R.id.tex2);
        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    int distance=0;
                    try {
                        readMessage = new String((byte[]) msg.obj, "ASCII");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //textView.setText(readMessage+i++);
                    distance = readBuffer [0] & 0xFF;//直接使用別人的有時間再研究......................................

                    textView.setText("前方超音波距離:"+distance);//這邊一定要有String不能單純只有數字
                    sendInstruction(distance);//x是距離
                }
            }
        };
    }

    public void sendInstruction(int distance){
        try{
            //outputStream = socket.getOutputStream();
            String message=null;
            Log.i("information",":distance"+distance+",x:"+pointX);
            if(distance<5){//後退
                message="b";
            }else if((distance>=5 && distance<11) || (pointX==0 && pointY==0)){//停止
                message="s";
            }else{//distance>=8
                //px,py
                if(pointX<230){//左轉


                    message="l";
                }else if(pointX>570){//右轉
                    message="d";
                }else{//前進
                    message="f";
                }
            }
            outputStream.write(message.getBytes());
        }catch(IOException e){

        }
    }

    View.OnClickListener discoveryButtonListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(intent);
        }
    };
    View.OnClickListener scanButtonListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.e("Start Receiver:","-------------------");
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            //deviceArrayList=new ArrayList<BluetoothDevice>();
            BroadcastReceiver mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        dialog.setMessage(device.getAddress()+"  "+device.getName());
                        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        dialog.show();
                        try{
                            bluetoothAdapter.cancelDiscovery();//取消搜尋
                            socket=device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                            socket.connect();//連接上去
                            outputStream = socket.getOutputStream();
                            inputStream=socket.getInputStream();

                            readBufferPosition = 0;
                            workerThread=new Thread(new Runnable() {
                                @Override
                                public void run() {//---------------------------------------------------開啟接收訊息的Thread---------------------------------------------------------------
                                    while(true){
                                        try{
                                            readBufferPosition = inputStream.available();
                                            if(readBufferPosition != 0) {
                                                readBuffer = new byte[1024];
                                                SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                                                readBufferPosition = inputStream.available(); // how many bytes are ready to be read?
                                                readBufferPosition = inputStream.read(readBuffer, 0, readBufferPosition); // record how many bytes we actually read
                                                handler.obtainMessage(MESSAGE_READ, readBufferPosition, -1, readBuffer)
                                                        .sendToTarget(); // Send the obtained bytes to the UI activity
                                            }

                                        }catch(IOException e){
                                            Log.e("while break ",":"+e);
                                            break;
                                        }
                                    }
                                }
                            });
                            workerThread.start();
                        }catch (IOException e){
                            Log.e("getInputStream is error",":"+e);
                        }
                    }
                }
            };
            registerReceiver(mReceiver, filter);
            bluetoothAdapter.startDiscovery(); //開始搜尋裝置
        };
    };
    //-------------------------------------------------------------------------------------------copy past follw-------------------------------------------------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //permission was granted, yay! Do the contacts-related task you need to do.
                //这里进行授权被允许的处理
            } else {
                //permission denied, boo! Disable the functionality that depends on this permission.
                //这里进行权限被拒绝的处理
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    public static final boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }
    //-------------------------------------------------------------------------------------------copy past above-------------------------------------------------------------------------------------------------
    View.OnClickListener send=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try{
                //outputStream = socket.getOutputStream();

                // 送出訊息
                String message ="A";
                outputStream.write(message.getBytes());
            }catch(IOException e){

            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        cameraFrame=inputFrame.rgba();
        int[] arr;
        arr=imageProcessing(cameraFrame.getNativeObjAddr());
        //imageDetect(cameraFrame.getNativeObjAddr(),detectMat.getNativeObjAddr());
        pointX=arr[0];
        pointY=arr[1];
        return cameraFrame;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native int[] imageProcessing(long matAddress);
}
