package com.example.test;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class SetColorActivity extends AppCompatActivity {

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_color);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_setColor), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
}