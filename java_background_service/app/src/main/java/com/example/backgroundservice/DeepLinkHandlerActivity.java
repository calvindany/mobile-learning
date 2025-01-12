package com.example.backgroundservice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class DeepLinkHandlerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the deep link data if needed
        Uri data = getIntent().getData();

        if (data != null) {
            String base64Data = data.getQueryParameter("data"); // Extract 'data' parameter
            System.out.println(base64Data);
            if (base64Data != null) {
                // Start a service to handle conversion logic
                Intent serviceIntent = new Intent(this, DeepLinkService.class);
                serviceIntent.putExtra("base64Data", base64Data); // Pass Base64 string to service
                startService(serviceIntent);
            }
        }

        // Close the activity immediately
        finish();
    }
}

