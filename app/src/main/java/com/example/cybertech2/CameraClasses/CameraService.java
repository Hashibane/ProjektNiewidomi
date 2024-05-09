package com.example.cybertech2.CameraClasses;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
//WRAPPER CLASS
public class CameraService {
    private String cameraID; //unikalny identyfikator
    private CameraDevice mCameraDevice;
    private boolean frontCamera;
    private boolean flashSupported;
    private CameraCaptureSession captureSession;
    private int sensorOrientation;

    public String getCameraID() {
        return cameraID;
    }

    public void setCameraID(String cameraIDD) {
        this.cameraID = cameraIDD;
    }

    public CameraDevice getmCameraDevice() {
        return mCameraDevice;
    }

    public void setmCameraDevice(CameraDevice mCameraDevice) {
        this.mCameraDevice = mCameraDevice;
    }

    public boolean isFrontCamera() {
        return frontCamera;
    }

    public void setFrontCamera(boolean frontCamera) {
        this.frontCamera = frontCamera;
    }

    public boolean isFlashSuppoeted() {
        return flashSupported;
    }

    public void setFlashSuppoeted(boolean flashSuppoeted) {
        this.flashSupported = flashSuppoeted;
    }

    public CameraCaptureSession getCaptureSession() {
        return captureSession;
    }

    public void setCaptureSession(CameraCaptureSession captureSession) {
        this.captureSession = captureSession;
    }

    public int getSensorOrientation() {
        return sensorOrientation;
    }

    public void setSensorOrientation(int sensorOrientation) {
        this.sensorOrientation = sensorOrientation;
    }
    public CameraService(String cameraID, boolean frontCamera) {
        this.cameraID = cameraID;
        this.frontCamera = frontCamera;
    }

    public void setFlashSupported(boolean flashSupported) {
        this.flashSupported = flashSupported;
    }
}
