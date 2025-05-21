package com.example.test;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class SetColorActivity extends AppCompatActivity {
    private ImageView cube_white_imageView, cube_yellow_imageView, cube_green_imageView,
            cube_blue_imageView, cube_red_imageView, cube_orange_imageView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_color);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_setColor), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        cube_white_imageView = findViewById(R.id.cube_white_imageView);
        cube_yellow_imageView = findViewById(R.id.cube_yellow_imageView);
        cube_green_imageView = findViewById(R.id.cube_green_imageView);
        cube_blue_imageView = findViewById(R.id.cube_blue_imageView);
        cube_red_imageView = findViewById(R.id.cube_red_imageView);
        cube_orange_imageView = findViewById(R.id.cube_orange_imageView);

        updateBitmap();
    }

    public void SetColor(View view) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Bundle bundle = new Bundle();
        if(view.getId() == R.id.white_frameLayout){
            bundle.putString("color", "white");
        }
        else if(view.getId() == R.id.yellow_frameLayout){
            bundle.putString("color", "yellow");
        }
        else if(view.getId() == R.id.green_frameLayout){
            bundle.putString("color", "green");
        }
        else if(view.getId() == R.id.blue_frameLayout){
            bundle.putString("color", "blue");
        }
        else if(view.getId() == R.id.red_frameLayout){
            bundle.putString("color", "red");
        }
        else if(view.getId() == R.id.orange_frameLayout){
            bundle.putString("color", "orange");
        }
        SetSingleColorFragment singleColorFragment = new SetSingleColorFragment();
        singleColorFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.main_setColor, singleColorFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void updateBitmap(String color){
        Bitmap bitmap = StorageUtil.loadBitmap(this, "bitmap_" + color);
        switch (color){
            case "white":
                cube_white_imageView.setImageBitmap(bitmap);
                break;
            case "yellow":
                cube_yellow_imageView.setImageBitmap(bitmap);
                break;
            case "green":
                cube_green_imageView.setImageBitmap(bitmap);
                break;
            case "blue":
                cube_blue_imageView.setImageBitmap(bitmap);
                break;
            case "red":
                cube_red_imageView.setImageBitmap(bitmap);
                break;
            case "orange":
                cube_orange_imageView.setImageBitmap(bitmap);
                break;
                default:
                    break;
        }
    }
    private void updateBitmap(){
        if(StorageUtil.loadBitmap(this, "bitmap_white") != null){
            cube_white_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    this, "bitmap_white"
            ));
        }
        if(StorageUtil.loadBitmap(this, "bitmap_yellow") != null){
            cube_yellow_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    this, "bitmap_yellow"
            ));
        }
        if(StorageUtil.loadBitmap(this, "bitmap_green") != null){
            cube_green_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    this, "bitmap_green"
            ));
        }
        if(StorageUtil.loadBitmap(this, "bitmap_blue") != null){
            cube_blue_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    this, "bitmap_blue"
            ));
        }
        if(StorageUtil.loadBitmap(this, "bitmap_red") != null){
            cube_red_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    this, "bitmap_red"
            ));
        }
        if(StorageUtil.loadBitmap(this, "bitmap_orange") != null){
            cube_orange_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    this, "bitmap_orange"
            ));
        }
    }

    public void backPage(View view) {
        finish();
    }
}