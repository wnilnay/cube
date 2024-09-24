package com.example.test;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.LongFunction;

public class MainActivity extends AppCompatActivity {

    View[] blue_views = new View[9];
    View[] orange_views = new View[9];
    View[] white_views = new View[9];
    View[] green_views = new View[9];
    View[] red_views = new View[9];
    View[] yellow_views = new View[9];
    int[] bs = {R.id.blue_1,R.id.blue_2,R.id.blue_3,R.id.blue_4,R.id.blue_5,R.id.blue_6,R.id.blue_7,R.id.blue_8,R.id.blue_9};
    int[] os = {R.id.orange_1,R.id.orange_2,R.id.orange_3,R.id.orange_4,R.id.orange_5,R.id.orange_6,R.id.orange_7,R.id.orange_8,R.id.orange_9};
    int[] ws = {R.id.white_1,R.id.white_2,R.id.white_3,R.id.white_4,R.id.white_5,R.id.white_6,R.id.white_7,R.id.white_8,R.id.white_9};
    int[] gs = {R.id.green_1,R.id.green_2,R.id.green_3,R.id.green_4,R.id.green_5,R.id.green_6,R.id.green_7,R.id.green_8,R.id.green_9};
    int[] rs = {R.id.red_1,R.id.red_2,R.id.red_3,R.id.red_4,R.id.red_5,R.id.red_6,R.id.red_7,R.id.red_8,R.id.red_9};
    int[] ys = {R.id.yellow_1,R.id.yellow_2,R.id.yellow_3,R.id.yellow_4,R.id.yellow_5,R.id.yellow_6,R.id.yellow_7,R.id.yellow_8,R.id.yellow_9};
    //String[] type = {"white","red","green","orange","blue"};
    private TextView turn_of_code;
    private int click = -1,location = 0,white = 0,red = 0,green = 0,orange = 0,blue = 0,yellow = 0;
    int[] click_button = new int[48];
    int click_times = -1;
    int[] last = {white,red,green,orange,blue,yellow};
    View[][] views = {white_views,red_views,green_views,orange_views,blue_views,yellow_views};
    private boolean isOk = false;
    private boolean isSolve = false;
    private TextView lock;
    int[] color_put_into_block = new int[54];
    private String turn_code = "";
    private String cubeStatus = "";
    private Button button_lest, button_next;
    private TextView textView_Solve;
    private int Solution_position = -1;

    private BluetoothSocket socket;
    private OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        socket = BluetoothSocketManager.getSocket();

        button_lest = findViewById(R.id.button_lest);
        button_next = findViewById(R.id.button_next);
        textView_Solve = findViewById(R.id.TextView_Solve);
        button_next.setText(">");
        button_lest.setText("<");
        for(int i = 0;i<9;i++){
            blue_views[i] = findViewById(bs[i]);
            orange_views[i] = findViewById(os[i]);
            white_views[i] = findViewById(ws[i]);
            green_views[i] = findViewById(gs[i]);
            red_views[i] = findViewById(rs[i]);
            yellow_views[i] = findViewById(ys[i]);
        }
        lock = findViewById(R.id.lock);
        lock.setVisibility(View.INVISIBLE);
        turn_of_code = findViewById(R.id.turn_of_code);
        //initCube();

        button_lest.setVisibility(View.INVISIBLE);
        button_next.setVisibility(View.INVISIBLE);
        Toast.makeText(this,"請確認魔術方塊放置於解魔方機上",Toast.LENGTH_LONG).show();

    }
    private void sendString(String dataToSend){
        if (socket != null && socket.isConnected()) {
            try {
                outputStream = socket.getOutputStream();

                outputStream.write(dataToSend.getBytes("utf-8"));

                Toast.makeText(this, "已傳送字串", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                //Log.d("BActivity", "IOException: " + e.getMessage());
                Toast.makeText(this, "傳送失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "藍牙未連線", Toast.LENGTH_SHORT).show();
        }
    }
    private String getString(){
        try {
            if(socket.isConnected()){
                char read;
                String temp = "";
                InputStream is = socket.getInputStream();
                while (true){
                    if(is.available() == 0){
                        break;
                    }
                    read = (char)is.read();
                    if(read == '\0') break;
                    temp += read;
                    //Log.d("wnilnay",(int)read+"");
                }
                Log.d("wnilnay",temp);
                return temp;
            }
        }
        catch (IOException | NullPointerException e) {
            Log.d("wnilnay",e.getMessage());
            return "Error";
        }
        return "";
    }

    private void initCube(){
        orange_button(null);green_button(null);white_button(null);
        red_button(null);yellow_button(null);
        orange_button(null);white_button(null);red_button(null);

        green_button(null);white_button(null);green_button(null);
        blue_button(null);green_button(null);
        white_button(null);red_button(null);orange_button(null);

        yellow_button(null);orange_button(null);blue_button(null);
        orange_button(null);green_button(null);
        white_button(null);white_button(null);orange_button(null);

        yellow_button(null);orange_button(null);blue_button(null);
        yellow_button(null);red_button(null);
        blue_button(null);blue_button(null);yellow_button(null);

        red_button(null);red_button(null);white_button(null);
        yellow_button(null);yellow_button(null);
        red_button(null);orange_button(null);red_button(null);

        blue_button(null);green_button(null);yellow_button(null);
        blue_button(null);white_button(null);
        green_button(null);blue_button(null);green_button(null);
    }
    private void change_color(int id){
        if(click<8)
            click++;
        else {
            click = 0;
            location++;
        }
        if(click == 4){
            click = 5;
        }

        //Log.d("wnilnay","location = "+location+" click = "+click);
        switch (location){
            case 0:
                white_views[click].setBackground(getDrawable(id));
                break;
            case 1:
                red_views[click].setBackground(getDrawable(id));
                break;
            case 2:
                green_views[click].setBackground(getDrawable(id));
                break;
            case 3:
                orange_views[click].setBackground(getDrawable(id));
                break;
            case 4:
                blue_views[click].setBackground(getDrawable(id));
                break;
            case 5:
                yellow_views[click].setBackground(getDrawable(id));
                break;
            default:
                break;
        }
    }
    private Drawable[] get_back_ground(int color_from,int color_camp1,int color_camp2,int color_end,int[] position){
        /*for (int i = 0;i<12;i++){
            Log.d("wnilnay",position[i]+"");
        }*/

        Drawable[] drawables = new Drawable[12];
        for(int i = 0;i<12;i++){
            switch (i){
                case 0:
                case 1:
                case 2:
                    drawables[i] = views[color_from][position[i]].getBackground();
                    break;
                case 3:
                case 4:
                case 5:
                    drawables[i] = views[color_camp1][position[i]].getBackground();
                    break;
                case 6:
                case 7:
                case 8:
                    drawables[i] = views[color_camp2][position[i]].getBackground();
                    break;
                case 9:
                case 10:
                case 11:
                    drawables[i] = views[color_end][position[i]].getBackground();
                    break;
                default:
                    break;
            }
        }
        return drawables;

    }
    private Drawable[] get_color(int color_from){
        Drawable[] drawables = new Drawable[9];
        for(int i = 0;i<9;i++){
            drawables[i] = views[color_from][i].getBackground();
        }
        return drawables;
    }

    /*旋轉方向與顏色填寫方向相反==>為順時針轉*/
    /*旋轉方向與顏色填寫方向相同==>為逆時針轉*/
    private void up(){
        //Log.d("wnilnay","up");
        //region 變更顏色
        //轉動邊相對位置
        int[] position_order = {6,7,8,6,7,8,6,7,8,6,7,8};
        //轉動邊顏色變更的順序
        int[] k = {3,4,5,6,7,8,9,10,11,0,1,2};
        int l = 0;
        //轉動魔方歷經顏色變更的順序
        int[] color_order = {1,2,3,4};
        //轉動面
        int[] up_color_position = {6,3,0,7,4,1,8,5,2};

        Drawable[] ups = get_back_ground(1,2,3,4,position_order);
        Drawable[] up_colors = get_color(5);
        for(int i = 0;i<9;i++){
            views[5][up_color_position[i]].setBackground(up_colors[i]);
        }
        //Log.d("wnilnay","finish");
        int j = 0,m = 0;
        for(int i = 0;i<12;i++){
           if(m == 3){
               j++;
               m = 0;
           }
           m++;
           views[color_order[j]][position_order[i]].setBackground(ups[k[l]]);
           l++;
        }
        //endregion

        //region 更改陣列

        //region 更改四邊陣列
        //絕對座標
        int[] absolute_coordinates_left = {24,15,42,33};
        int[] absolute_coordinates_middle = {25,16,43,34};
        int[] absolute_coordinates_right = {26,17,44,35};
        int[] left = new int[4];
        int[] middle = new int[4];
        int[] right = new int[4];
        for(int i = 0;i<4;i++){
            left[i] = color_put_into_block[absolute_coordinates_left[i]];
            middle[i] = color_put_into_block[absolute_coordinates_middle[i]];
            right[i] = color_put_into_block[absolute_coordinates_right[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_left[i1 +1]] = left[i1];
                    color_put_into_block[absolute_coordinates_middle[i1 +1]] = middle[i1];
                    color_put_into_block[absolute_coordinates_right[i1 +1]] = right[i1];
                }
                color_put_into_block[absolute_coordinates_left[0]] = left[3];
                color_put_into_block[absolute_coordinates_middle[0]] = middle[3];
                color_put_into_block[absolute_coordinates_right[0]] = right[3];
            }
        }
        //endregion

        //region 更改轉動面陣列
        int[] absolute_coordinates_corner = {47,45,51,53};
        int[] absolute_coordinates_side = {46,48,52,50};
        int[] corner = new int[4];
        int[] side = new int[4];
        for(int i = 0;i<4;i++){
            corner[i] = color_put_into_block[absolute_coordinates_corner[i]];
            side[i] = color_put_into_block[absolute_coordinates_side[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_corner[i1 +1]] = corner[i1];
                    color_put_into_block[absolute_coordinates_side[i1 +1]] = side[i1];
                }
                color_put_into_block[absolute_coordinates_corner[0]] = corner[3];
                color_put_into_block[absolute_coordinates_side[0]] = side[3];
            }
        }
        //endregion

        //endregion
    }
    private void right(){
        int[] k = {3,4,5,6,7,8,9,10,11,0,1,2};
        int[] color_order = {2,0,4,5};
        int[] position_order = {2,5,8,2,5,8,6,3,0,2,5,8};
        int l = 0;
        int[] right_color_position = {6,3,0,7,4,1,8,5,2};
        Drawable[] rights = get_back_ground(2,0,4,5,position_order);
        Drawable[] right_colors = get_color(3);
        for(int i = 0;i<9;i++){
            views[3][right_color_position[i]].setBackground(right_colors[i]);
        }
        int j = 0,m = 0;
        for(int i = 0;i<12;i++){
            if(m == 3){
                j++;
                m = 0;
            }
            m++;
            views[color_order[j]][position_order[i]].setBackground(rights[k[l]]);
            l++;
        }
        //region 更改陣列

        //region 更改四邊陣列
        //絕對座標
        int[] absolute_coordinates_top = {26,53,36,8};
        int[] absolute_coordinates_middle = {23,50,39,5};
        int[] absolute_coordinates_bottom = {20,47,42,2};
        int[] top = new int[4];
        int[] middle = new int[4];
        int[] bottom = new int[4];
        for(int i = 0;i<4;i++){
            top[i] = color_put_into_block[absolute_coordinates_top[i]];
            middle[i] = color_put_into_block[absolute_coordinates_middle[i]];
            bottom[i] = color_put_into_block[absolute_coordinates_bottom[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_top[i1 +1]] = top[i1];
                    color_put_into_block[absolute_coordinates_middle[i1 +1]] = middle[i1];
                    color_put_into_block[absolute_coordinates_bottom[i1 +1]] = bottom[i1];
                }
                color_put_into_block[absolute_coordinates_top[0]] = top[3];
                color_put_into_block[absolute_coordinates_middle[0]] = middle[3];
                color_put_into_block[absolute_coordinates_bottom[0]] = bottom[3];
            }
        }
        //endregion

        //region 更改轉動面陣列
        int[] absolute_coordinates_corner = {29,27,33,35};
        int[] absolute_coordinates_side = {28,30,34,32};
        int[] corner = new int[4];
        int[] side = new int[4];
        for(int i = 0;i<4;i++){
            corner[i] = color_put_into_block[absolute_coordinates_corner[i]];
            side[i] = color_put_into_block[absolute_coordinates_side[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_corner[i1 +1]] = corner[i1];
                    color_put_into_block[absolute_coordinates_side[i1 +1]] = side[i1];
                }
                color_put_into_block[absolute_coordinates_corner[0]] = corner[3];
                color_put_into_block[absolute_coordinates_side[0]] = side[3];
            }
        }
        //endregion

        //endregion
    }
    private void down(){
        int[] k = {3,4,5,6,7,8,9,10,11,0,1,2};
        int[] color_order = {2,1,4,3};
        int[] position_order = {0,1,2,0,1,2,0,1,2,0,1,2};
        int l = 0;
        int[] down_color_position = {6,3,0,7,4,1,8,5,2};
        Drawable[] downs = get_back_ground(2,1,4,3,position_order);
        Drawable[] down_colors = get_color(0);
        for(int i = 0;i<9;i++){
            views[0][down_color_position[i]].setBackground(down_colors[i]);
        }
        int j = 0,m = 0;
        for(int i = 0;i<12;i++){
            if(m == 3){
                j++;
                m = 0;
            }
            m++;
            views[color_order[j]][position_order[i]].setBackground(downs[k[l]]);
            l++;
        }

        //region 更改陣列

        //region 更改四邊陣列
        //絕對座標
        int[] absolute_coordinates_left = {18,27,36,9};
        int[] absolute_coordinates_middle = {19,28,37,10};
        int[] absolute_coordinates_right = {20,29,38,11};
        int[] left = new int[4];
        int[] middle = new int[4];
        int[] right = new int[4];
        for(int i = 0;i<4;i++){
            left[i] = color_put_into_block[absolute_coordinates_left[i]];
            middle[i] = color_put_into_block[absolute_coordinates_middle[i]];
            right[i] = color_put_into_block[absolute_coordinates_right[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_left[i1 +1]] = left[i1];
                    color_put_into_block[absolute_coordinates_middle[i1 +1]] = middle[i1];
                    color_put_into_block[absolute_coordinates_right[i1 +1]] = right[i1];
                }
                color_put_into_block[absolute_coordinates_left[0]] = left[3];
                color_put_into_block[absolute_coordinates_middle[0]] = middle[3];
                color_put_into_block[absolute_coordinates_right[0]] = right[3];
            }
        }
        //endregion

        //region 更改轉動面陣列
        int[] absolute_coordinates_corner = {2,0,6,8};
        int[] absolute_coordinates_side = {1,3,7,5};
        int[] corner = new int[4];
        int[] side = new int[4];
        for(int i = 0;i<4;i++){
            corner[i] = color_put_into_block[absolute_coordinates_corner[i]];
            side[i] = color_put_into_block[absolute_coordinates_side[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_corner[i1 +1]] = corner[i1];
                    color_put_into_block[absolute_coordinates_side[i1 +1]] = side[i1];
                }
                color_put_into_block[absolute_coordinates_corner[0]] = corner[3];
                color_put_into_block[absolute_coordinates_side[0]] = side[3];
            }
        }
        //endregion

        //endregion
    }
    private void left(){
        int[] k = {3,4,5,6,7,8,9,10,11,0,1,2};
        int[] color_order = {2,5,4,0};
        int[] position_order = {0,3,6,0,3,6,8,5,2,0,3,6};
        int l = 0;
        int[] left_color_position = {6,3,0,7,4,1,8,5,2};
        Drawable[] lefts = get_back_ground(2,5,4,0,position_order);
        Drawable[] left_colors = get_color(1);
        for(int i = 0;i<9;i++){
            views[1][left_color_position[i]].setBackground(left_colors[i]);
        }
        int j = 0,m = 0;
        for(int i = 0;i<12;i++){
            if(m == 3){
                j++;
                m = 0;
            }
            m++;
            views[color_order[j]][position_order[i]].setBackground(lefts[k[l]]);
            l++;
        }
        //region 更改陣列

        //region 更改四邊陣列
        //絕對座標
        int[] absolute_coordinates_top = {24,6,38,51};
        int[] absolute_coordinates_middle = {21,3,41,48};
        int[] absolute_coordinates_bottom = {18,0,44,45};
        int[] top = new int[4];
        int[] middle = new int[4];
        int[] bottom = new int[4];
        for(int i = 0;i<4;i++){
            top[i] = color_put_into_block[absolute_coordinates_top[i]];
            middle[i] = color_put_into_block[absolute_coordinates_middle[i]];
            bottom[i] = color_put_into_block[absolute_coordinates_bottom[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_top[i1 +1]] = top[i1];
                    color_put_into_block[absolute_coordinates_middle[i1 +1]] = middle[i1];
                    color_put_into_block[absolute_coordinates_bottom[i1 +1]] = bottom[i1];
                }
                color_put_into_block[absolute_coordinates_top[0]] = top[3];
                color_put_into_block[absolute_coordinates_middle[0]] = middle[3];
                color_put_into_block[absolute_coordinates_bottom[0]] = bottom[3];
            }
        }
        //endregion

        //region 更改轉動面陣列
        int[] absolute_coordinates_corner = {11,9,15,17};
        int[] absolute_coordinates_side = {10,12,16,14};
        int[] corner = new int[4];
        int[] side = new int[4];
        for(int i = 0;i<4;i++){
            corner[i] = color_put_into_block[absolute_coordinates_corner[i]];
            side[i] = color_put_into_block[absolute_coordinates_side[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_corner[i1 +1]] = corner[i1];
                    color_put_into_block[absolute_coordinates_side[i1 +1]] = side[i1];
                }
                color_put_into_block[absolute_coordinates_corner[0]] = corner[3];
                color_put_into_block[absolute_coordinates_side[0]] = side[3];
            }
        }
        //endregion

        //endregion
    }
    private void front(){
        int[] k = {3,4,5,6,7,8,9,10,11,0,1,2};
        int[] color_order = {5,1,0,3};
        int[] position_order = {0,1,2,2,5,8,8,7,6,6,3,0};
        int[] front_color_position = {6,3,0,7,4,1,8,5,2};
        int l = 0;
        Drawable[] fronts = get_back_ground(5,1,0,3,position_order);
        Drawable[] front_colors = get_color(2);
        int j = 0,m = 0;
        for(int i = 0;i<12;i++){
            if(m == 3){
                j++;
                m = 0;
            }
            m++;
            views[color_order[j]][position_order[i]].setBackground(fronts[k[l]]);
            l++;
        }
        for(int i = 0;i<9;i++){
            views[2][front_color_position[i]].setBackground(front_colors[i]);
        }
        //region 更改陣列

        //region 更改四邊陣列
        //絕對座標
        int[] absolute_coordinates_left = {45,33,8,11};
        int[] absolute_coordinates_middle = {46,30,7,14};
        int[] absolute_coordinates_right = {47,27,6,17};
        int[] left = new int[4];
        int[] middle = new int[4];
        int[] right = new int[4];
        for(int i = 0;i<4;i++){
            left[i] = color_put_into_block[absolute_coordinates_left[i]];
            middle[i] = color_put_into_block[absolute_coordinates_middle[i]];
            right[i] = color_put_into_block[absolute_coordinates_right[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_left[i1 +1]] = left[i1];
                    color_put_into_block[absolute_coordinates_middle[i1 +1]] = middle[i1];
                    color_put_into_block[absolute_coordinates_right[i1 +1]] = right[i1];
                }
                color_put_into_block[absolute_coordinates_left[0]] = left[3];
                color_put_into_block[absolute_coordinates_middle[0]] = middle[3];
                color_put_into_block[absolute_coordinates_right[0]] = right[3];
            }
        }
        //endregion

        //region 更改轉動面陣列
        int[] absolute_coordinates_corner = {20,18,24,26};
        int[] absolute_coordinates_side = {19,21,25,23};
        int[] corner = new int[4];
        int[] side = new int[4];
        for(int i = 0;i<4;i++){
            corner[i] = color_put_into_block[absolute_coordinates_corner[i]];
            side[i] = color_put_into_block[absolute_coordinates_side[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_corner[i1 +1]] = corner[i1];
                    color_put_into_block[absolute_coordinates_side[i1 +1]] = side[i1];
                }
                color_put_into_block[absolute_coordinates_corner[0]] = corner[3];
                color_put_into_block[absolute_coordinates_side[0]] = side[3];
            }
        }
        //endregion

        //endregion
    }
    private void back(){
        int[] k = {3,4,5,6,7,8,9,10,11,0,1,2};
        int[] color_order = {5,3,0,1};
        int[] position_order = {6,7,8,8,5,2,2,1,0,0,3,6};
        int l = 0;
        int[] back_color_position = {6,3,0,7,4,1,8,5,2};
        Drawable[] backs = get_back_ground(5,3,0,1,position_order);
        Drawable[] back_colors = get_color(4);
        for(int i = 0;i<9;i++){
            views[4][back_color_position[i]].setBackground(back_colors[i]);
        }
        int j = 0,m = 0;
        for(int i = 0;i<12;i++){
            if(m == 3){
                j++;
                m = 0;
            }
            m++;
            views[color_order[j]][position_order[i]].setBackground(backs[k[l]]);
            l++;
        }

        //region 更改陣列

        //region 更改四邊陣列
        //絕對座標
        int[] absolute_coordinates_left = {51,9,2,35};
        int[] absolute_coordinates_middle = {52,12,1,32};
        int[] absolute_coordinates_right = {53,15,0,29};
        int[] left = new int[4];
        int[] middle = new int[4];
        int[] right = new int[4];
        for(int i = 0;i<4;i++){
            left[i] = color_put_into_block[absolute_coordinates_left[i]];
            middle[i] = color_put_into_block[absolute_coordinates_middle[i]];
            right[i] = color_put_into_block[absolute_coordinates_right[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_left[i1 +1]] = left[i1];
                    color_put_into_block[absolute_coordinates_middle[i1 +1]] = middle[i1];
                    color_put_into_block[absolute_coordinates_right[i1 +1]] = right[i1];
                }
                color_put_into_block[absolute_coordinates_left[0]] = left[3];
                color_put_into_block[absolute_coordinates_middle[0]] = middle[3];
                color_put_into_block[absolute_coordinates_right[0]] = right[3];
            }
        }
        //endregion

        //region 更改轉動面陣列
        int[] absolute_coordinates_corner = {38,36,42,44};
        int[] absolute_coordinates_side = {37,39,43,41};
        int[] corner = new int[4];
        int[] side = new int[4];
        for(int i = 0;i<4;i++){
            corner[i] = color_put_into_block[absolute_coordinates_corner[i]];
            side[i] = color_put_into_block[absolute_coordinates_side[i]];
            if(i == 3){
                for(int i1 = 0; i1 <3; i1++){
                    color_put_into_block[absolute_coordinates_corner[i1 +1]] = corner[i1];
                    color_put_into_block[absolute_coordinates_side[i1 +1]] = side[i1];
                }
                color_put_into_block[absolute_coordinates_corner[0]] = corner[3];
                color_put_into_block[absolute_coordinates_side[0]] = side[3];
            }
        }
        //endregion

        //endregion
    }


    public void redo(View view) {
        if(!isOk){
            if(location>=0&&click>=0){
                //Log.d("wnilnay","location = "+location+" click = "+click);
                //Drawable a = white_views[click].getBackground();
                switch (click_button[click_times]){
                    case 0:
                        white--;
                        click_button[click_times] = -1;
                        click_times--;
                        break;
                    case 1:
                        red--;
                        click_button[click_times] = -1;
                        click_times--;
                        break;
                    case 2:
                        green--;
                        click_button[click_times] = -1;
                        click_times--;
                        break;
                    case 3:
                        orange--;
                        click_button[click_times] = -1;
                        click_times--;
                        break;
                    case 4:
                        blue--;
                        click_button[click_times] = -1;
                        click_times--;
                        break;
                    case 5:
                        yellow--;
                        click_button[click_times] = -1;
                        click_times--;
                        break;
                    default:
                        break;
                }
                switch (location){
                    case 0:
                        white_views[click].setBackground(getDrawable(R.drawable.rectangle_gray));
                        break;
                    case 1:
                        red_views[click].setBackground(getDrawable(R.drawable.rectangle_gray));
                        break;
                    case 2:
                        green_views[click].setBackground(getDrawable(R.drawable.rectangle_gray));
                        break;
                    case 3:
                        orange_views[click].setBackground(getDrawable(R.drawable.rectangle_gray));
                        break;
                    case 4:
                        blue_views[click].setBackground(getDrawable(R.drawable.rectangle_gray));
                        break;
                    case 5:
                        yellow_views[click].setBackground(getDrawable(R.drawable.rectangle_gray));
                        break;
                    default:
                        break;
                }
                if(click>0)
                    click--;
                else {
                    click = 8;
                    location--;
                }
                if(click == 4){
                    click = 3;
                }
            }
        }
    }
    public void yellow_button(View view) {
        if(!isOk){
            if(yellow<8){
                change_color(R.drawable.rectangle_yellow);
                yellow++;
                click_times++;
                click_button[click_times] = 5;
            }
        }
    }

    public void white_button(View view) {
        if(!isOk){
            if(white<8){
                change_color(R.drawable.rectangle_white);
                white++;
                click_times++;
                click_button[click_times] = 0;
            }
        }
    }

    public void blue_button(View view) {
        if(!isOk){
            if(blue<8){
                change_color(R.drawable.rectangle_blue);
                blue++;
                click_times++;
                click_button[click_times] = 4;
            }
        }
    }

    public void orange_button(View view) {
        if(!isOk){
            if(orange<8){
                change_color(R.drawable.rectangle_orange);
                orange++;
                click_times++;
                click_button[click_times] = 3;
            }
        }
    }

    public void green_button(View view) {
        if(!isOk){
            if(green<8){
                change_color(R.drawable.rectangle_green);
                green++;
                click_times++;
                click_button[click_times] = 2;
            }
        }
    }

    public void red_button(View view) {
        if(!isOk){
            if(red<8){
                change_color(R.drawable.rectangle_red);
                red++;
                click_times++;
                click_button[click_times] = 1;
            }
        }
    }


    public void up_button(View view) {
        if(isOk){
            up();
            turn_code+="U";
            turn_of_code.setText(turn_code);
        }
    }

    public void right_button(View view) {
        if(isOk){
            right();
            turn_code+="R";
            turn_of_code.setText(turn_code);
        }
    }

    public void down_button(View view) {
        if(isOk){
            down();
            turn_code+="D";
            turn_of_code.setText(turn_code);
        }
    }

    public void left_button(View view) {
        if(isOk){
            left();
            turn_code+="L";
            turn_of_code.setText(turn_code);
        }
    }

    public void front_button(View view) {
        if(isOk){
            front();
            turn_code+="F";
            turn_of_code.setText(turn_code);
        }
    }

    public void back_button(View view) {
        if(isOk){
            back();
            turn_code+="B";
            turn_of_code.setText(turn_code);
        }
    }

    public void up_bar_button(View view) {
        if(isOk){
            up();
            up();
            up();
            turn_code+="u";
            turn_of_code.setText(turn_code);
        }
    }

    public void down_bar_button(View view) {
        if(isOk){
            down();
            down();
            down();
            turn_code+="d";
            turn_of_code.setText(turn_code);
        }
    }

    public void right_bar_button(View view) {
        if(isOk){
            right();
            right();
            right();
            turn_code+="r";
            turn_of_code.setText(turn_code);
        }
    }

    public void left_bar_button(View view) {
        if(isOk){
            left();
            left();
            left();
            turn_code+="l";
            turn_of_code.setText(turn_code);
        }
    }

    public void front_bar_button(View view) {
        if(isOk){
            front();
            front();
            front();
            turn_code+="f";
            turn_of_code.setText(turn_code);
        }
    }

    public void back_bar_button(View view) {
        if(isOk){
            back();
            back();
            back();
            turn_code+="b";
            turn_of_code.setText(turn_code);
        }
    }

    public void ok_button(View view) {
        int j = 0;
        if(white == 8 && green == 8 && red == 8 && orange == 8 && blue == 8 && yellow == 8) {
            isOk = true;
            lock.setVisibility(View.VISIBLE);
            for (int i = 0; i < 48; i = i) {
                switch (j) {
                    case 4:
                        color_put_into_block[4] = 0;
                        j = 5;
                        break;
                    case 13:
                        color_put_into_block[13] = 1;
                        j = 14;
                        break;
                    case 22:
                        color_put_into_block[22] = 2;
                        j = 23;
                        break;
                    case 31:
                        color_put_into_block[31] = 3;
                        j = 32;
                        break;
                    case 40:
                        color_put_into_block[40] = 4;
                        j = 41;
                        break;
                    case 49:
                        color_put_into_block[49] = 5;
                        j = 50;
                        break;
                    default:
                        color_put_into_block[j] = click_button[i];
                        j++;
                        i++;
                        break;
                }
            }
        }

    }

    public void showLog(View view) {
        if (isOk) {
            String a = "";
            String b = "";
            for (int i = 0; i < color_put_into_block.length; i++) {
                a = a + color_put_into_block[i] + "\t";
                b = b + i + "\t";
            }
            Log.d("wnilnay", a);
            Log.d("wnilnay", b);
        }
    }

    public String solve(View view) {
        if(isOk){
            int[] cube_position = new int[]{51,52,53,48,49,50,45,46,47,33,34,35,30,31,32,27,28,29,24,25,26,21,22,23,18,19,20,6,7,8,3,4,5,0,1,2,15,16,17,12,13,14,9,10,11,42,43,44,39,40,41,36,37,38};
            cubeStatus = "";
            for(int i = 0;i<cube_position.length;i++){
                switch (color_put_into_block[cube_position[i]]){
                    case 0:
                        cubeStatus += "D";
                        break;
                    case 1:
                        cubeStatus += "L";
                        break;
                    case 2:
                        cubeStatus += "F";
                        break;
                    case 3:
                        cubeStatus += "R";
                        break;
                    case 4:
                        cubeStatus += "B";
                        break;
                    case 5:
                        cubeStatus += "U";
                        break;
                    default:
                        break;
                }
            }
            //cubeStatus = "FBFBUDBFUBBUURLURBDDRRFFURBRDLLDURFDDLRBLFFDFLRLUBULLD";
            Log.d("wnilnay",cubeStatus);
            String solution = new Search().solution(cubeStatus,20,1000000,10000,0);
            solution = solution.replaceAll("  "," ");
            Log.d("wnilnay",solution);
            isSolve = true;
            Solution_position = -1;
//            button_lest.setVisibility(View.VISIBLE);
//            button_right.setVisibility(View.VISIBLE);
//            textView_Solve.setVisibility(View.VISIBLE);
            String[] solutions = solution.split(" ");
            String newSolution = "";
            for(int i = 0;i<solutions.length;i++){
                if(i%9 == 8){
                    newSolution += "\n";
                }
                newSolution += solutions[i];
                newSolution += " ";
            }
            textView_Solve.setText(newSolution);

            return solution;
        }
        return null;
    }

    public void lest(View view) {
        if(isSolve){
            String solution = textView_Solve.getText().toString().replaceAll("\n","");
            String[] solutions = solution.split(" ");
            if(Solution_position != -1){
                SolveCube(solutions[Solution_position],false);
                Solution_position--;
                SolutionText(Solution_position);
            }
        }
    }

    public void next(View view) {
        if(isSolve){
            String solution = textView_Solve.getText().toString().replaceAll("\n","");
            String[] solutions = solution.split(" ");
            Solution_position++;
            if(Solution_position < solutions.length)
                SolveCube(solutions[Solution_position],true);
            SolutionText(Solution_position);
        }
    }
    private void SolutionText(int solution_position){
        String solution = textView_Solve.getText().toString();
        String[] solutions = solution.split(" ");
        if(solution_position >= solutions.length){
            Solution_position--;
            return;
        }
        SpannableString spannableString = new SpannableString(solution);
        int char_position = 0;
        for(int i = 0;i<solution_position+1;i++){
            char_position += solutions[i].length();
            char_position += 1;
        }
        spannableString.setSpan(new ForegroundColorSpan(Color.GREEN),0,char_position,0);

        textView_Solve.setText(spannableString);
    }
    private void SolveCube(String turn_code,boolean isPositive){
        //Log.d("wnilnay",turn_code);
        if(isPositive){
            switch (turn_code){
                case "R2":
                    right_button(null);
                case "R":
                    right_button(null);
                    break;
                case "U2":
                    up_button(null);
                case "U":
                    up_button(null);
                    break;
                case "F2":
                    front_button(null);
                case "F":
                    front_button(null);
                    break;
                case "L2":
                    left_button(null);
                case "L":
                    left_button(null);
                    break;
                case "D2":
                    down_button(null);
                case "D":
                    down_button(null);
                    break;
                case "B2":
                    back_button(null);
                case "B":
                    back_button(null);
                    break;
                case "R'":
                    right_bar_button(null);
                    break;
                case "U'":
                    up_bar_button(null);
                    break;
                case "F'":
                    front_bar_button(null);
                    break;
                case "L'":
                    left_bar_button(null);
                    break;
                case "D'":
                    down_bar_button(null);
                    break;
                case "B'":
                    back_bar_button(null);
                    break;
            }
        }
        else {
            switch (turn_code){
                case "R2":
                    right_bar_button(null);
                case "R":
                    right_bar_button(null);
                    break;
                case "U2":
                    up_bar_button(null);
                case "U":
                    up_bar_button(null);
                    break;
                case "F2":
                    front_bar_button(null);
                case "F":
                    front_bar_button(null);
                    break;
                case "L2":
                    left_bar_button(null);
                case "L":
                    left_bar_button(null);
                    break;
                case "D2":
                    down_bar_button(null);
                case "D":
                    down_bar_button(null);
                    break;
                case "B2":
                    back_bar_button(null);
                case "B":
                    back_bar_button(null);
                    break;
                case "R'":
                    right_button(null);
                    break;
                case "U'":
                    up_button(null);
                    break;
                case "F'":
                    front_button(null);
                    break;
                case "L'":
                    left_button(null);
                    break;
                case "D'":
                    down_button(null);
                    break;
                case "B'":
                    back_button(null);
                    break;
            }
        }
    }

    public void clear(View view) {
        turn_of_code.setText("");
        turn_code = "";
    }

    public void inputColor(View view) {
        String colors = "YYYYYYYYYOOOOOOOOOGGGGGGGGGWWWWWWWWWRRRRRRRRRBBBBBBBBB";
        setColor(colors);
    }

    private void set_color_put_into_block(String colors){
        char[] colorsArray = colors.toCharArray();
        int[] orders = {33, 34, 35, 30, 31, 32, 27, 28, 29, 42, 43, 44, 39, 40, 41, 36, 37, 38, 24, 25, 26, 21, 22, 23, 18, 19, 20, 15, 16, 17, 12, 13, 14, 9, 10, 11, 51, 52, 53, 48, 49, 50, 45, 46, 47, 6, 7, 8, 3, 4, 5, 0, 1, 2};
        char colorUp = colorsArray[4],colorRight = colorsArray[13],colorFront = colorsArray[22],colorDown = colorsArray[31],colorLeft = colorsArray[40],colorBack = colorsArray[49];
        Log.d("wnilnay",colorUp + "");Log.d("wnilnay",colorRight + "");Log.d("wnilnay",colorFront + "");Log.d("wnilnay",colorDown + "");Log.d("wnilnay",colorLeft + "");Log.d("wnilnay",colorBack + "");
        for(int i = 0;i<colorsArray.length;i++){
            if(colorsArray[orders[i]] == colorUp){
                color_put_into_block[i] = 5;
            }
            else if(colorsArray[orders[i]] == colorRight){
                color_put_into_block[i] = 3;
            }
            else if(colorsArray[orders[i]] == colorFront){
                color_put_into_block[i] = 2;
            }
            else if(colorsArray[orders[i]] == colorDown){
                color_put_into_block[i] = 0;
            }
            else if(colorsArray[orders[i]] == colorLeft){
                color_put_into_block[i] = 1;
            }
            else if(colorsArray[orders[i]] == colorBack){
                color_put_into_block[i] = 4;
            }
        }
        String color = "";
        for (int i : color_put_into_block){
            color += i + "";
        }
        Log.d("wnilnay",color);
    }

    private void setColor(String colors){
        char[] colorsArray = colors.toCharArray();
        int[] positionOrder = {6,7,8,3,4,5,0,1,2};
        int position = 0;
        int[] colorOrder = {5,3,2,0,1,4};
        int colorPosition = 0;
        for(char color : colorsArray){
            switch (color){
                case 'Y':
                    views[colorOrder[colorPosition]][positionOrder[position++]].setBackground(getDrawable(R.drawable.rectangle_yellow));
                    if(isChangePosition(position)) {position = 0;colorPosition++;}
                    break;
                case 'O':
                    views[colorOrder[colorPosition]][positionOrder[position++]].setBackground(getDrawable(R.drawable.rectangle_orange));
                    if(isChangePosition(position)) {position = 0;colorPosition++;}
                    break;
                case 'G':
                    views[colorOrder[colorPosition]][positionOrder[position++]].setBackground(getDrawable(R.drawable.rectangle_green));
                    if(isChangePosition(position)) {position = 0;colorPosition++;}
                    break;
                case 'W':
                    views[colorOrder[colorPosition]][positionOrder[position++]].setBackground(getDrawable(R.drawable.rectangle_white));
                    if(isChangePosition(position)) {position = 0;colorPosition++;}
                    break;
                case 'R':
                    views[colorOrder[colorPosition]][positionOrder[position++]].setBackground(getDrawable(R.drawable.rectangle_red));
                    if(isChangePosition(position)) {position = 0;colorPosition++;}
                    break;
                case 'B':
                    views[colorOrder[colorPosition]][positionOrder[position++]].setBackground(getDrawable(R.drawable.rectangle_blue));
                    if(isChangePosition(position)) {position = 0;colorPosition++;}
                    break;
                default:
                    break;
            }
        }
    }
    private boolean isChangePosition(int position){
        if(position == 9){
            return true;
        }
        return false;
    }

    public void newSolve(View view) {
        up_button(null);
        down_button(null);
        right_button(null);
        down_button(null);
        left_button(null);
        front_bar_button(null);
        left_button(null);

        sendString(solve(null));
    }

    public void OkButton(View view) {
        isOk = true;
        sendString("OK");
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String cubeColor = getString();
                if(cubeColor.contains("cube color")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                           String color = cubeColor.replace("cube color:","");
                           Log.d("wnilnay",color);
                           setColor(color);
                           set_color_put_into_block(color);
                           sendString(solve(null));

                           Timer timer1 = new Timer();
                           timer1.schedule(new TimerTask() {
                               @Override
                               public void run() {
                                   String nextString = getString();
                                   if(nextString.contains("next")){
                                       runOnUiThread(new Runnable() {
                                           @Override
                                           public void run() {
                                               next(null);
                                           }
                                       });
                                   }
                                   else if(nextString.contains("end")){
                                       runOnUiThread(new Runnable() {
                                           @Override
                                           public void run() {
                                               button_lest.setVisibility(View.VISIBLE);
                                               button_next.setVisibility(View.VISIBLE);
                                               Toast.makeText(getBaseContext(),"完成!",Toast.LENGTH_SHORT).show();
                                           }
                                       });

                                       timer1.cancel();
                                   }
                               }
                           },0,1000);
                        }
                    });
                    timer.cancel();
                }
            }
        },0,1000);
    }



//    public void BlueToothTest(View view) {
//        Intent intent = new Intent(this, BlueToothActivity.class);
//        startActivity(intent);
//    }
}