package com.example.asus.demondk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
        System.loadLibrary("testMessage");
        System.loadLibrary("testOpenCv");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        /*ImageView preImg=(ImageView)findViewById(R.id.img1);
        ImageView pastImg=(ImageView)findViewById(R.id.img2);
        Bitmap bitmap= BitmapFactory.decodeResource(getResources(),R.drawable.test11);
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        int [] resultPixes=gray(pix,w,h);
        Bitmap result = Bitmap.createBitmap(w,h, Bitmap.Config.RGB_565);
        result.setPixels(resultPixes, 0, w, 0, 0,w, h);
        preImg.setImageBitmap(bitmap);
        pastImg.setImageBitmap(result);*/
        if(OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"load",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(),"fail",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native String getTestString();
    public static native int[] gray(int[] buf, int w, int h);
}
