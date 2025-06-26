package com.example.test;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private ViewPager viewPager;
    private Fragment[] fs = new Fragment[3];
    private String[] titles = {"Manual","Automatic","Setting"};
    private final BlueToothFragment blueToothFragment = new BlueToothFragment();
    private final MainFragment mainFragment = new MainFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        fs[0] = new ManualFragment();
        fs[1] = blueToothFragment;
        fs[2] = new SettingFragment();

        initViewPager();
        viewPager.setCurrentItem(1,true);
    }

    private void initViewPager(){
        viewPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
//        viewPager.addOnAdapterChangeListener(new ViewPager.OnAdapterChangeListener() {
//            @Override
//            public void onAdapterChanged(@NonNull ViewPager viewPager, @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter) {
//                viewPager.setAdapter(newAdapter);
//            }
//        });
    }
    public void change_to_mainFragment(){
        fs[1] = mainFragment;
        viewPager.getAdapter().notifyDataSetChanged();
        viewPager.setCurrentItem(1,true);
    }
    public void change_to_bluetoothFragment(){
        fs[1] = blueToothFragment;
        viewPager.getAdapter().notifyDataSetChanged();
        viewPager.setCurrentItem(1,true);
    }
    public ViewPager getViewPager(){
        return viewPager;
    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fs[position];
        }

        @Override
        public int getCount() {
            return fs.length;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            // 強制第 1 頁（index=1）重建
            if (object instanceof ManualFragment && fs[1] instanceof BlueToothFragment) return POSITION_NONE;
            if (object instanceof BlueToothFragment && fs[1] instanceof MainFragment) return POSITION_NONE;
            return POSITION_UNCHANGED;
        }
    }

}