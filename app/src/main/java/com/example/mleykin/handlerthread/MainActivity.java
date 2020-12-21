package com.example.mleykin.handlerthread;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private Bitmap currentBitmap = null;
    private Button startButton;
    private Button stopButton;
    ArrayList<String> imagePaths;
    private static final int GALLERY_PERMISSION = 0;
    private Subscription subscription = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        GALLERY_PERMISSION
                );
            }
        }
        else {
            run();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case GALLERY_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    run();
                } else {
                    // permission denied
                }
                return;
            }
        }
    }

    public void run(){
        // get images
        String[] projection = new String[] {
            MediaStore.Images.Media.DATA,
        };

        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cur = managedQuery(images, projection,"",null,"");

        imagePaths = new ArrayList<String>();
        if (cur.moveToFirst()) {
            int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                imagePaths.add(cur.getString(dataColumn));
            } while (cur.moveToNext());
        }
        cur.close();

        startButton.setOnClickListener(v -> startShowing());
        stopButton.setOnClickListener(v -> stopShowing());
    }

    private void startShowing() {
        if (subscription != null) {
            subscription.unsubscribe();
        }

        Collections.shuffle(imagePaths);
        Observable<String> observableImagePaths = Observable.from(imagePaths);
        subscription = observableImagePaths
                .subscribeOn(Schedulers.io())
                .doOnNext(s -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(imagePath -> {
                    try {
                        currentBitmap = BitmapFactory.decodeFile(imagePath);
                        imageView.setImageBitmap(currentBitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void stopShowing() {
        subscription.unsubscribe();
        imageView.setImageResource(android.R.color.transparent);
    }
}
