package com.example.asus.demondk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");// Need load the lib if you did't install opencv manager.apk
        System.loadLibrary("native-lib");
    }
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
        /*getDetectImg();
        imageProcessing(detectMat.getNativeObjAddr());
        ImageView img=findViewById(R.id.imageView);
        Utils.matToBitmap(detectMat, footbm);
        img.setImageBitmap( footbm );
        img.invalidate();
        img.setVisibility(View.VISIBLE);*/
    }
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
        imageProcessing(cameraFrame.getNativeObjAddr());
        //imageDetect(cameraFrame.getNativeObjAddr(),detectMat.getNativeObjAddr());
        return cameraFrame;
    }
    public void getDetectImg(){
        // read image from resource
        InputStream is = this.getResources().openRawResource(R.raw.adata);
        footbm = BitmapFactory.decodeStream(is);
        //convert bitmap to opencv Mat
        Utils.bitmapToMat(footbm, detectMat);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void imageProcessing(long matAddress);
}
