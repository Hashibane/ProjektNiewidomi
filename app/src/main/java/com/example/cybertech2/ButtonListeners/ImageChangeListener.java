package com.example.cybertech2.ButtonListeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ImageChangeListener implements View.OnClickListener {
    private ImageView imageView;
    public ImageChangeListener (ImageView imageView)
    {
        this.imageView = imageView;
    }
    @Override
    public void onClick(View v) {
        Bitmap image = null;
        //PNG, JPGS .. are bitmap drawables! DONT DO XMLS, Vector graphics etc
        if (imageView.getDrawable() instanceof BitmapDrawable)
        {
            image = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        }
        //validity check
        if (image == null)
            return;
        Mat matObject = new Mat();
        Utils.bitmapToMat(image, matObject);

        Mat destination = new Mat();

        Imgproc.cvtColor(matObject, destination, Imgproc.COLOR_RGB2GRAY);

        Utils.matToBitmap(destination, image);
        image = Bitmap.createScaledBitmap(image, 600, 600, true);
        imageView.setImageBitmap(image);
    }
}
