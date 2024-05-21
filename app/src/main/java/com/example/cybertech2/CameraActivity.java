package com.example.cybertech2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.cybertech2.CameraClasses.ImageUtilClass;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraActivity extends AppCompatActivity {
//Konrad
    private static final int REQUEST_CAMERA_PERMISSION = 0;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private final CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDeviceCallback();
    private CameraCaptureSession cameraCaptureSession;
    private final TextureView.SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListener();
    private String cameraID;
    private Size previewSize;
    //We dont want to overload the main thread, hence the background thread
    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;
    private CaptureRequest.Builder captureRequestBuilder;
    private ImageReader imageReader;
    private final OnImageAvailableListener onImageAvailableListener = new OnImageAvailableListener();
    //Sensor translation
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    @Override
    protected  void onResume() {
        super.onResume();

        startBackgroundThread();
        if (textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "App won't run without camera permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
    public void callMainActivity(View view)
    {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private class SurfaceTextureListener implements TextureView.SurfaceTextureListener
    {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {}
    }


    private class CameraDeviceCallback extends CameraDevice.StateCallback
    {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
            //Toast.makeText(getApplicationContext(), "Camera connection established", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
    private class CameraSessionStateCallback extends CameraCaptureSession.StateCallback
    {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            //TODO: We want to do a proper listener in the future - it's the part that deals with image processing
            try {
                session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
            } catch (CameraAccessException e) {
                //TODO: LOGGING
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(getApplicationContext(), "Unable to set up camera preview", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCamera(int width, int height)
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String i : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(i);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                //checks for lanscape / portrait mode swap
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation)
                {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                cameraID = i;
                return;
            }
        }
        catch (CameraAccessException | NullPointerException e)
        {
            //TODO: LOGGING
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //PERMISSION CHECK
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)  {
                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                cameraManager.openCamera(cameraID, cameraDeviceStateCallback, backgroundHandler);
            }
            else  {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    Toast.makeText(this, "Video app reqires access to camera", Toast.LENGTH_SHORT).show();
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }

        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        if (surfaceTexture != null) {
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        }
        else {
            Toast.makeText(getApplicationContext(), "SurfaceTexture has not been properly initalized", Toast.LENGTH_SHORT).show();
            return;
        }
        Surface previewSurface = new Surface(surfaceTexture);

        imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 5);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

        try {

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_MONO);
            
            //Session setup
            //previewSurface
            cameraDevice.createCaptureSession(Arrays.asList( imageReader.getSurface()), new CameraSessionStateCallback(), null);
        } catch (CameraAccessException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }
    private void closeCamera() {
        if (cameraDevice != null)
        {
            cameraDevice.close();
            cameraDevice = null;
            imageReader.close();
            imageReader = null;
        }
    }

    private void startBackgroundThread() {
        backgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        backgroundHandlerThread.start();
        //handler pointing to thread
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
            backgroundHandlerThread = null;
            backgroundHandler = null;
        }
        catch (InterruptedException e)
        {
            //TODO: LOGGING
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static class CompareSizeByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getWidth() * o1.getHeight() / (long) o2.getWidth() * o2.getHeight());
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height)
    {
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            if ((option.getHeight() == option.getWidth() * height / width) && (option.getWidth() >= width && option.getHeight() >= height)) {
                bigEnough.add(option);
            }
        }
        if (!bigEnough.isEmpty())
        {
            return Collections.min(bigEnough, new CompareSizeByArea());
        }
        else
        {
            return choices[0];
        }
    }
    private class OnImageAvailableListener implements ImageReader.OnImageAvailableListener {
        Matrix rotationMatrix = new Matrix();
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    byte[] jpegBytes = ImageUtilClass.toJpeg(image).toByteArray();
                    Bitmap imageBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
                    Mat matObject = new Mat();
                    Utils.bitmapToMat(imageBitmap, matObject);

                    Mat destination = new Mat();

                    Imgproc.cvtColor(matObject, destination, Imgproc.COLOR_RGB2GRAY);

                    Utils.matToBitmap(destination, imageBitmap);
                    //imageBitmap = Bitmap.createScaledBitmap(imageBitmap, textureView.getHeight(), textureView.getWidth(), true);
                    int rotation = 0;
                    try {
                        rotation = sensorToDeviceRotation(((CameraManager) getSystemService(Context.CAMERA_SERVICE)).getCameraCharacteristics(cameraID), getWindowManager().getDefaultDisplay().getRotation());
                        System.out.println(rotation);
                    }
                    catch (CameraAccessException e)
                    {
                        Log.e("DRAW BITMAP ON TEXTUREVIEW", "Unable to draw");
                    }

                    rotationMatrix.reset();
                    rotationMatrix.postRotate(rotation + 180);
                    imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), rotationMatrix, true);
                    //imageBitmap = Bitmap.createScaledBitmap(imageBitmap, textureView.getHeight(), textureView.getWidth(), true);
                    final Bitmap bitmap = imageBitmap;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawBitmapOnTextureView(bitmap);
                        }
                    });


                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }

        private void drawBitmapOnTextureView(Bitmap bitmap) {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture != null) {
                Canvas canvas = textureView.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    textureView.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

}