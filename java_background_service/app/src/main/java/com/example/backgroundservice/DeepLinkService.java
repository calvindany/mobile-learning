package com.example.backgroundservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class DeepLinkService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding required
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DeepLinkService", "Service started");
        createNotificationChannel();

        startForegroundServiceWithNotification();
        new Thread(() -> {
            // Perform your background task here
            doBackgroundWork(intent);

            // Stop the service when the task is done
            stopSelf();
        }).start();

        return START_NOT_STICKY;
    }

    private void startForegroundServiceWithNotification() {
        Notification notification = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("DeepLink Service")
                .setContentText("Processing Base64 to PDF")
                .setSmallIcon(R.drawable.ic_notification) // Replace with your icon
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1, notification); // Start the service as foreground
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "channel_id"; // The same ID as used in the Notification Builder
            String channelName = "DeepLinkServiceChannel";
            String channelDescription = "Channel for DeepLink Service notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void doBackgroundWork(Intent intent) {
        System.out.println("Deeplink triggered");
        // Your background task logic, e.g., API calls, data processing, etc.
        if (intent != null && intent.hasExtra("base64Data")) {
            String base64Data = intent.getStringExtra("base64Data");
            System.out.println(base64Data);

            // Decode and save the data as a PDF
            String result = convertBase64ToPng(base64Data);
//            boolean success = true;
            boolean success;
            if(!result.isEmpty()) {
                success = printPng(result);
            } else {
                success = false;
            }
            System.out.println("Is Success convert Image: " + success);
            // Optionally, notify the user or perform other actions
            if (success) {
                showToast("PDF created successfully!");
            } else {
                showToast("Failed to create PDF.");
            }
        }

        stopSelf();
    }

    public static boolean doesFileExist(String filename) {
        // Get the path to the Downloads directory
        File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // Create a File object for the target file
        File file = new File(downloadDirectory, filename); // Replace "filename" with the actual file name

        // Check if the file exists
        return file.exists();
    }

    private boolean printPng(String filePath) {
        File file = new File(filePath);

        // Check if the file exists
        if (file.exists()) {
            try {
                // Create a FileInputStream from the file
                FileInputStream fileInputStream = new FileInputStream(file);

                // Decode the file into a Bitmap
                Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);

                // Close the FileInputStream
                fileInputStream.close();

                EscPosPrinter printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 400, 48f, 32);
                printer.printFormattedText(
                        "[L]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap)+"</img>\n"
                );

                return true;
            } catch (IOException | EscPosConnectionException | EscPosParserException |
                     EscPosEncodingException | EscPosBarcodeException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            // File doesn't exist
            System.out.println("File not found");
            return false;
        }
    }
    private String convertBase64ToPng(String base64Data) {
        try {
            UUID guid = UUID.randomUUID();

            // Decode Base64 to byte array
            byte[] pdfData = Base64.decode(base64Data, Base64.DEFAULT);

            // Define output file path
            File pdfFile = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), guid.toString() + ".png");
            String path  = pdfFile.getAbsolutePath();
            System.out.println(path);
            // Write the byte array to a file
            FileOutputStream fos = new FileOutputStream(pdfFile);
            fos.write(pdfData);
            fos.close();

            return path; // Indicate success
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Indicate failure
        }
    }

    private void showToast(String message) {
        // Display a toast (optional)
        Notification notification = new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("DeepLink Service")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.notify(2, notification);
    }
}

