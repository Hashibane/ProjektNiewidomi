package com.example.cybertech2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cybertech2.ButtonListeners.ImageChangeListener;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    private Button applyButton;
    //private Button cameraButton;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpenCVLoader.initDebug();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //SETUP
        applyButton = (Button)findViewById(R.id.apply_button);
        //cameraButton = (Button)findViewById(R.id.camera_button);
        imageView = (ImageView)findViewById(R.id.imageView);
        //imageView.setImageDrawable(R.drawable.henry);

        addListeners();
    }


    public void addListeners()
    {
        applyButton.setOnClickListener(new ImageChangeListener(imageView));
    }

    public void callCameraActivity(View view)
    {
        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        startActivity(intent);
    }
}