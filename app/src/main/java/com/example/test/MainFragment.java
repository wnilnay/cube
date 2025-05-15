package com.example.test;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MainFragment extends Fragment {
    View[] backward_views = new View[9];
    View[] right_views = new View[9];
    View[] down_views = new View[9];
    View[] forward_views = new View[9];
    View[] left_views = new View[9];
    View[] up_views = new View[9];
    int[] bs = {R.id.backward_1,R.id.backward_2,R.id.backward_3,R.id.backward_4,R.id.backward_5,R.id.backward_6,R.id.backward_7,R.id.backward_8,R.id.backward_9};
    int[] rs = {R.id.right_1,R.id.right_2,R.id.right_3,R.id.right_4,R.id.right_5,R.id.right_6,R.id.right_7,R.id.right_8,R.id.right_9};
    int[] ds = {R.id.down_1,R.id.down_2,R.id.down_3,R.id.down_4,R.id.down_5,R.id.down_6,R.id.down_7,R.id.down_8,R.id.down_9};
    int[] fs = {R.id.forward_1,R.id.forward_2,R.id.forward_3,R.id.forward_4,R.id.forward_5,R.id.forward_6,R.id.forward_7,R.id.forward_8,R.id.forward_9};
    int[] ls = {R.id.left_1,R.id.left_2,R.id.left_3,R.id.left_4,R.id.left_5,R.id.left_6,R.id.left_7,R.id.left_8,R.id.left_9};
    int[] us = {R.id.up_1,R.id.up_2,R.id.up_3,R.id.up_4,R.id.up_5,R.id.up_6,R.id.up_7,R.id.up_8,R.id.up_9};
    //String[] type = {"white","red","green","orange","blue"};
    private TextView turn_of_code;
    private int click = -1,location = 0,white = 0,red = 0,green = 0,orange = 0,blue = 0,yellow = 0;
    int[] click_button = new int[48];
    int click_times = -1;
    int[] last = {white,red,green,orange,blue,yellow};
    View[][] views = {down_views,left_views,forward_views,right_views,backward_views,up_views};
    View[][] viewsForNew = {up_views,right_views,forward_views,down_views,left_views,backward_views};
    private boolean isOk = false;
    private boolean isSolve = false;
    private TextView lock;
    int[] color_put_into_block = new int[54];
    private String turn_code = "";
    private Button button_lest, button_next,button_ok;
    private TextView textView_Solve;
    private int Solution_position = -1;
    private String cubeStatus = "";

    private BluetoothSocket socket;
    private OutputStream outputStream;
    private ColorDirectionManager colorDirectionManager = new ColorDirectionManager();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        socket = BluetoothSocketManager.getSocket();

        button_lest = view.findViewById(R.id.button_lest);
        button_next = view.findViewById(R.id.button_next);
        button_ok = view.findViewById(R.id.button_ok);
        textView_Solve = view.findViewById(R.id.TextView_Solve);
        button_next.setText(">");
        button_lest.setText("<");
        for(int i = 0;i<9;i++){
            backward_views[i] = view.findViewById(bs[i]);
            right_views[i] = view.findViewById(rs[i]);
            down_views[i] = view.findViewById(ds[i]);
            forward_views[i] = view.findViewById(fs[i]);
            left_views[i] = view.findViewById(ls[i]);
            up_views[i] = view.findViewById(us[i]);
        }
        lock = view.findViewById(R.id.lock);
        lock.setVisibility(View.INVISIBLE);
        turn_of_code = view.findViewById(R.id.turn_of_code);
        //initCube();

        button_lest.setVisibility(View.INVISIBLE);
        button_next.setVisibility(View.INVISIBLE);
        Toast.makeText(getContext(),"請確認魔術方塊放置於解魔方機上",Toast.LENGTH_LONG).show();

        button_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OkButton();
            }
        });

        button_lest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lest();
            }
        });

        button_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

//        requireActivity().getOnBackPressedDispatcher().addCallback(
//                getViewLifecycleOwner(),
//                new OnBackPressedCallback(true) {
//                    @Override
//                    public void handleOnBackPressed() {
//                        if(((MainActivity)requireActivity()).getCurrentBottomFragment() ==
//                                ((MainActivity)requireActivity()).getMainFragment() &&
//                                ((MainActivity) requireActivity()).getCurrentSubFragment() ==
//                        ((MainActivity)requireActivity()).getMainFragment()){
//                            Log.v("wnilnay OnBackPress","YES");
//
//                            ((MainActivity)requireActivity()).hideAllFragment();
//                            requireActivity().getSupportFragmentManager().beginTransaction()
//                                    .show(((MainActivity)requireActivity()).getBlueToothFragment())
//                                    .commit();
//                            ((MainActivity)requireActivity())
//                                    .updateCurrentSub((((MainActivity) requireActivity())
//                                            .getBlueToothFragment()));
//
//                            try {
//                                BluetoothSocketManager.getSocket().close();
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                        else {
//                            setEnabled(false);
//                            requireActivity().onBackPressed();
//                        }
//                    }
//                });


        return view;
    }
    private void sendString(String dataToSend){
        if (socket != null && socket.isConnected()) {
            try {
                outputStream = socket.getOutputStream();

                outputStream.write(dataToSend.getBytes("utf-8"));

                Toast.makeText(getContext(), "已傳送字串", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                //Log.d("BActivity", "IOException: " + e.getMessage());
                Toast.makeText(getContext(), "傳送失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "藍牙未連線", Toast.LENGTH_SHORT).show();
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

    public void up_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.up(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="U";
            turn_of_code.setText(turn_code);
        }
    }

    public void right_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.right(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="R";
            turn_of_code.setText(turn_code);
        }
    }

    public void down_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.down(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="D";
            turn_of_code.setText(turn_code);
        }
    }

    public void left_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.left(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="L";
            turn_of_code.setText(turn_code);
        }
    }

    public void front_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.front(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="F";
            turn_of_code.setText(turn_code);
        }
    }

    public void back_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.back(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="B";
            turn_of_code.setText(turn_code);
        }
    }

    public void up_bar_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.up_bar(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="u";
            turn_of_code.setText(turn_code);
        }
    }

    public void down_bar_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.down_bar(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="d";
            turn_of_code.setText(turn_code);
        }
    }

    public void right_bar_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.right_bar(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="r";
            turn_of_code.setText(turn_code);
        }
    }

    public void left_bar_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.left_bar(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="l";
            turn_of_code.setText(turn_code);
        }
    }

    public void front_bar_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.front_bar(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="f";
            turn_of_code.setText(turn_code);
        }
    }

    public void back_bar_button() {
        if(isOk){
            cubeStatus = CubeStatusManager.back_bar(cubeStatus);
            updateCubeStatus(cubeStatus);
            turn_code+="b";
            turn_of_code.setText(turn_code);
        }
    }

    public String solve() {
        if(isOk){
//            int[] cube_position = new int[]{51,52,53,48,49,50,45,46,47,33,34,35,30,31,32,27,28,29,24,25,26,21,22,23,18,19,20,6,7,8,3,4,5,0,1,2,15,16,17,12,13,14,9,10,11,42,43,44,39,40,41,36,37,38};
//            String cubeStatus = "";
//            for(int i = 0;i<cube_position.length;i++){
//                switch (color_put_into_block[cube_position[i]]){
//                    case 0:
//                        cubeStatus += "D";
//                        break;
//                    case 1:
//                        cubeStatus += "L";
//                        break;
//                    case 2:
//                        cubeStatus += "F";
//                        break;
//                    case 3:
//                        cubeStatus += "R";
//                        break;
//                    case 4:
//                        cubeStatus += "B";
//                        break;
//                    case 5:
//                        cubeStatus += "U";
//                        break;
//                    default:
//                        break;
//                }
//            }
            //cubeStatus = "FBFBUDBFUBBUURLURBDDRRFFURBRDLLDURFDDLRBLFFDFLRLUBULLD";
//          Log.d("wnilnay", cubeStatus);
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

    public void lest() {
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

    public void next() {
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
                    right_button();
                case "R":
                    right_button();
                    break;
                case "U2":
                    up_button();
                case "U":
                    up_button();
                    break;
                case "F2":
                    front_button();
                case "F":
                    front_button();
                    break;
                case "L2":
                    left_button();
                case "L":
                    left_button();
                    break;
                case "D2":
                    down_button();
                case "D":
                    down_button();
                    break;
                case "B2":
                    back_button();
                case "B":
                    back_button();
                    break;
                case "R'":
                    right_bar_button();
                    break;
                case "U'":
                    up_bar_button();
                    break;
                case "F'":
                    front_bar_button();
                    break;
                case "L'":
                    left_bar_button();
                    break;
                case "D'":
                    down_bar_button();
                    break;
                case "B'":
                    back_bar_button();
                    break;
            }
        }
        else {
            switch (turn_code){
                case "R2":
                    right_bar_button();
                case "R":
                    right_bar_button();
                    break;
                case "U2":
                    up_bar_button();
                case "U":
                    up_bar_button();
                    break;
                case "F2":
                    front_bar_button();
                case "F":
                    front_bar_button();
                    break;
                case "L2":
                    left_bar_button();
                case "L":
                    left_bar_button();
                    break;
                case "D2":
                    down_bar_button();
                case "D":
                    down_bar_button();
                    break;
                case "B2":
                    back_bar_button();
                case "B":
                    back_bar_button();
                    break;
                case "R'":
                    right_button();
                    break;
                case "U'":
                    up_button();
                    break;
                case "F'":
                    front_button();
                    break;
                case "L'":
                    left_button();
                    break;
                case "D'":
                    down_button();
                    break;
                case "B'":
                    back_button();
                    break;
            }
        }
    }

    public void clear(View view) {
        turn_of_code.setText("");
        turn_code = "";
    }

    public void inputColor() {
        String colors = "YYYYYYYYYOOOOOOOOOGGGGGGGGGWWWWWWWWWRRRRRRRRRBBBBBBBBB";
        setColor(colors);
    }

    private void setColor(String colors){
        initDirection(colors);
        cubeStatus = exchangeColor2DirectionString(colors);
        updateCubeStatus(cubeStatus);
    }
    private void initDirection(String colors){
        char[] colorsCharArray = colors.toCharArray();
        int[] colorDirectionPosition = {4,13,22,31,40,49};

        for (int i : colorDirectionPosition){
            switch (colorsCharArray[i]){
                case 'Y':
                    colorDirectionManager.setYellowDirection
                            (Direction.getEnumFromOrdinal(Arrays.binarySearch(colorDirectionPosition,i)));
                    break;
                case 'O':
                    colorDirectionManager.setOrangeDirection
                            (Direction.getEnumFromOrdinal(Arrays.binarySearch(colorDirectionPosition,i)));
                    break;
                case 'G':
                    colorDirectionManager.setGreenDirection
                            (Direction.getEnumFromOrdinal(Arrays.binarySearch(colorDirectionPosition,i)));
                    break;
                case 'W':
                    colorDirectionManager.setWhiteDirection
                            (Direction.getEnumFromOrdinal(Arrays.binarySearch(colorDirectionPosition,i)));
                    break;
                case 'R':
                    colorDirectionManager.setRedDirection
                            (Direction.getEnumFromOrdinal(Arrays.binarySearch(colorDirectionPosition,i)));
                    break;
                case 'B':
                    colorDirectionManager.setBlueDirection
                            (Direction.getEnumFromOrdinal(Arrays.binarySearch(colorDirectionPosition,i)));
                    break;
                default:
                    break;
            }
        }
    }
    private String exchangeColor2DirectionString(String colors){
        char[] colorsCharArray = colors.toCharArray();
        char[] directionCharArray = new char[54];
        char[] directionPosition = {'U','R','F','D','L','B'};
        for (int i = 0;i<colorsCharArray.length;i++){
            switch (colorsCharArray[i]){
                case 'Y':
                    directionCharArray[i] = directionPosition
                            [colorDirectionManager.getYellowDirection().ordinal()];
                    break;
                case 'O':
                    directionCharArray[i] = directionPosition
                            [colorDirectionManager.getOrangeDirection().ordinal()];
                    break;
                case 'G':
                    directionCharArray[i] = directionPosition
                            [colorDirectionManager.getGreenDirection().ordinal()];
                    break;
                case 'W':
                    directionCharArray[i] = directionPosition
                            [colorDirectionManager.getWhiteDirection().ordinal()];
                    break;
                case 'R':
                    directionCharArray[i] = directionPosition
                            [colorDirectionManager.getRedDirection().ordinal()];
                    break;
                case 'B':
                    directionCharArray[i] = directionPosition
                            [colorDirectionManager.getBlueDirection().ordinal()];
                    break;
                default:
                    break;
            }
        }
        return new String(directionCharArray);
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateCubeStatus(String newStatus){
        char[] newStatuses = newStatus.toCharArray();
        int position = 0;
        int[] drawableIDs = new int[6];
        drawableIDs[colorDirectionManager.getYellowDirection().ordinal()] = R.drawable.rectangle_yellow;
        drawableIDs[colorDirectionManager.getBlueDirection().ordinal()] = R.drawable.rectangle_blue;
        drawableIDs[colorDirectionManager.getRedDirection().ordinal()] = R.drawable.rectangle_red;
        drawableIDs[colorDirectionManager.getWhiteDirection().ordinal()] = R.drawable.rectangle_white;
        drawableIDs[colorDirectionManager.getOrangeDirection().ordinal()] = R.drawable.rectangle_orange;
        drawableIDs[colorDirectionManager.getGreenDirection().ordinal()] = R.drawable.rectangle_green;
        for (char status : newStatuses){
            switch (status){
                case 'U':
                    viewsForNew[position / 9][position - ((position / 9) * 9)]
                            .setBackground(requireContext().getDrawable(drawableIDs[Direction.UP.ordinal()]));
                    break;
                case 'R':
                    viewsForNew[position / 9][position - ((position / 9) * 9)]
                            .setBackground(requireContext().getDrawable(drawableIDs[Direction.RIGHT.ordinal()]));
                    break;
                case 'F':
                    viewsForNew[position / 9][position - ((position / 9) * 9)]
                            .setBackground(requireContext().getDrawable(drawableIDs[Direction.FORWARD.ordinal()]));
                    break;
                case 'D':
                    viewsForNew[position / 9][position - ((position / 9) * 9)]
                            .setBackground(requireContext().getDrawable(drawableIDs[Direction.DOWN.ordinal()]));
                    break;
                case 'L':
                    viewsForNew[position / 9][position - ((position / 9) * 9)]
                            .setBackground(requireContext().getDrawable(drawableIDs[Direction.LEFT.ordinal()]));
                    break;
                case 'B':
                    viewsForNew[position / 9][position - ((position / 9) * 9)]
                            .setBackground(requireContext().getDrawable(drawableIDs[Direction.BACKWARD.ordinal()]));
                    break;
                default:
                    break;
            }
            position++;
        }
    }

    public void OkButton() {
        isOk = true;
        //inputColor();
        sendString("OK");
        button_ok.setVisibility(View.INVISIBLE);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String cubeColor = getString();
                if(cubeColor.contains("cube color")){
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String color = cubeColor.replace("cube color:","");
                            Log.d("wnilnay",color);
                            setColor(color);
                            sendString(solve());

                            Timer timer1 = new Timer();
                            timer1.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    String nextString = getString();
                                    if(nextString.contains("next")){
                                        requireActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                next();
                                            }
                                        });
                                    }
                                    else if(nextString.contains("end")){
                                        requireActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                button_lest.setVisibility(View.VISIBLE);
                                                button_next.setVisibility(View.VISIBLE);
                                                button_ok.setVisibility(View.VISIBLE);
                                                Toast.makeText(getContext(),"完成!",Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                        timer1.cancel();
                                    }
                                }
                            },0,100);
                        }
                    });
                    timer.cancel();
                }
            }
        },0,100);
    }






//    public void BlueToothTest(View view) {
//        Intent intent = new Intent(getContext(), BlueToothActivity.class);
//        startActivity(intent);
//    }
}