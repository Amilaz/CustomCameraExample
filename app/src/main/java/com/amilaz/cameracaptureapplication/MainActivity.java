package com.amilaz.cameracaptureapplication;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 609;
    private Camera mCamera;
    private RelativeLayout rootView;
    private ImageView imgcaptureView;
    private Button captureBtn;
    private boolean safeToTakePicture = false;
    private boolean cameraLock = true;
    private SurfaceView view;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        confirmPermission();
    }

    private void initListener() {
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (safeToTakePicture && !cameraLock) {
                    Log.d("Action", "DO");
                    captureBtn.setClickable(false);
                    try{
                        safeToTakePicture = false;
                        mCamera.takePicture(null, null, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                saveImageFormCamera(data);
                                previewImage();
                                deleteImage();
                                mCamera.stopPreview();
                                mCamera.startPreview();
                                Log.d("Take a photo", "DONE");
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    safeToTakePicture = true;
                    captureBtn.setClickable(true);
                }

            }
        });
    }

    private void deleteImage() {
        File file = new File(filename);
        boolean deleted = file.delete();
    }

    boolean save = true;

    private void saveImageFormCamera(byte[] data){
        File pictureFileDir = getDir();
        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            save = false;
            Toast.makeText(getApplicationContext() , "Can't create directory to save image.",
                    Toast.LENGTH_LONG).show();
            //Log.d("File Path", filename);
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "picture_" + date + ".jpg";
        filename = pictureFileDir.getAbsolutePath() + File.separator + photoFile;
        Log.d("File Path", filename);
        File pictureFile = new File(filename);
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            Toast.makeText(getApplicationContext() , "New Image saved:" + pictureFileDir + " " + photoFile,
                    Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(getApplicationContext() , "Image could not be saved.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void previewImage(){
        if(save){
            Bitmap realImage;
            File imgFile = new  File(filename);
            if(imgFile.exists()) {
                realImage = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ExifInterface ei = null;
                int orientation = -1;
                try {
                    ei = new ExifInterface(filename);
                    orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(orientation != -1) {
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            Log.d("Exif",Integer.toString(ExifInterface.ORIENTATION_ROTATE_90));
                            rotateImage(realImage, 90);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            Log.d("Exif",Integer.toString(ExifInterface.ORIENTATION_ROTATE_90));
                            rotateImage(realImage, 180);
                            break;
                    }
                    imgcaptureView.setImageBitmap(realImage);
                }
            }
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Bitmap retVal;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        return retVal;
    }

    private void initInstance() {
        rootView = (RelativeLayout) findViewById(R.id.root_view);
        imgcaptureView = (ImageView) findViewById(R.id.img_cap);
        captureBtn = (Button) findViewById(R.id.cap_btn);
    }

    private void initSurfaceView(){
        view = (SurfaceView) findViewById(R.id.img_view);
        view.getHolder().addCallback(surfaceCallback);
        view.getHolder().setFormat(PixelFormat.TRANSPARENT);
        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void confirmPermission(){
            // Here, thisActivity is the current activity
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
            } else {
                cameraInstance();
                initInstance();
                initListener();
            }
    }

    private void cameraInstance(){
        Log.d("PER","2");
        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG)
                    .show();
            Log.d("PER","3");
        } else {
            Log.d("PER","4");
            try {
                Log.d("PER","5");
                mCamera = getCameraInstance();
                initSurfaceView();
                cameraLock = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void requestCameraPermission() {
            // BEGIN_INCLUDE(camera_permission_request)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // For example if the user has previously denied the permission.
                Snackbar.make(rootView, "Resuest Camera permission",
                        Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                })
                        .show();
            } else {
                // Camera permission has not been granted yet. Request it directly.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }
        // END_INCLUDE(camera_permission_request)
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraInstance();
                    initInstance();
                    initListener();
                } else {
                    initInstance();
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        mCamera = getCameraInstance();
        super.onResume();
    }

    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    private void setCameraDisplayOrientation(Activity activity, int cameraId,
                                             android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            if (Camera.getNumberOfCameras() >= 2) {
                //c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        // returns null if camera is unavailable
        return c;
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                Log.d("CallBack", "Cre");
                setCameraDisplayOrientation(MainActivity.this, Camera.CameraInfo.CAMERA_FACING_FRONT, mCamera);
                mCamera.setPreviewDisplay(view.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("CallBack", "Cha");
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> previewSize = params.getSupportedPreviewSizes();
            List<Camera.Size> pictureSize = params.getSupportedPictureSizes();
            params.setPictureSize(pictureSize.get(0).width,pictureSize.get(0).height);
            params.setPreviewSize(previewSize.get(0).width,previewSize.get(0).height);
            params.setJpegQuality(100);
            params.setPictureFormat(PixelFormat.JPEG);
            mCamera.setParameters(params);
            mCamera.startPreview();
            safeToTakePicture = true;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("CallBack", "Des");
        }
    };

    private File getDir() {
        File dir;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)){
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        } else {
            dir = getApplicationContext().getFilesDir();
        }
        return new File(dir, "CameraApp");
    }

    @Override
    public void onBackPressed() {
        Snackbar.make(rootView, "Do you want to exit?",
                Snackbar.LENGTH_SHORT).setAction(R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        }).show();
    }
}
