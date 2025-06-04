package com.example.test;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class SetColorFragment extends Fragment {
    private View view;
    private ImageView cube_white_imageView, cube_yellow_imageView, cube_green_imageView,
            cube_blue_imageView, cube_red_imageView, cube_orange_imageView;
    private FrameLayout layout_white, layout_yellow, layout_green, layout_blue,
            layout_red, layout_orange;
    private Toolbar toolbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view == null){
            view = inflater.inflate(R.layout.fragment_set_color, container, false);

            cube_white_imageView = view.findViewById(R.id.cube_white_imageView);
            cube_yellow_imageView = view.findViewById(R.id.cube_yellow_imageView);
            cube_green_imageView = view.findViewById(R.id.cube_green_imageView);
            cube_blue_imageView = view.findViewById(R.id.cube_blue_imageView);
            cube_red_imageView = view.findViewById(R.id.cube_red_imageView);
            cube_orange_imageView = view.findViewById(R.id.cube_orange_imageView);
            layout_white = view.findViewById(R.id.white_frameLayout);
            layout_yellow = view.findViewById(R.id.yellow_frameLayout);
            layout_green = view.findViewById(R.id.green_frameLayout);
            layout_blue = view.findViewById(R.id.blue_frameLayout);
            layout_red = view.findViewById(R.id.red_frameLayout);
            layout_orange = view.findViewById(R.id.orange_frameLayout);

            toolbar = view.findViewById(R.id.toolbar);

            layout_white.setOnClickListener(this::SetColor);
            layout_yellow.setOnClickListener(this::SetColor);
            layout_green.setOnClickListener(this::SetColor);
            layout_blue.setOnClickListener(this::SetColor);
            layout_red.setOnClickListener(this::SetColor);
            layout_orange.setOnClickListener(this::SetColor);
            toolbar.setNavigationOnClickListener(this::backPage);

            updateBitmap();
        }
        return view;
    }

    public void SetColor(View view) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
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
        //frameLayout.setVisibility(View.INVISIBLE);
    }

    public void updateBitmap(String color){
        Bitmap bitmap = StorageUtil.loadBitmap(requireContext(), "bitmap_" + color);
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
        //frameLayout.setVisibility(View.VISIBLE);
    }

    private void updateBitmap(){
        if(StorageUtil.loadBitmap(requireContext(), "bitmap_white") != null){
            cube_white_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    requireContext(), "bitmap_white"
            ));
        }
        if(StorageUtil.loadBitmap(requireContext(), "bitmap_yellow") != null){
            cube_yellow_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    requireContext(), "bitmap_yellow"
            ));
        }
        if(StorageUtil.loadBitmap(requireContext(), "bitmap_green") != null){
            cube_green_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    requireContext(), "bitmap_green"
            ));
        }
        if(StorageUtil.loadBitmap(requireContext(), "bitmap_blue") != null){
            cube_blue_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    requireContext(), "bitmap_blue"
            ));
        }
        if(StorageUtil.loadBitmap(requireContext(), "bitmap_red") != null){
            cube_red_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    requireContext(), "bitmap_red"
            ));
        }
        if(StorageUtil.loadBitmap(requireContext(), "bitmap_orange") != null){
            cube_orange_imageView.setImageBitmap(StorageUtil.loadBitmap(
                    requireContext(), "bitmap_orange"
            ));
        }
    }

    public void backPage(View view) {
        requireActivity().finish();
    }
}