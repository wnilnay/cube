package com.example.test;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;

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

import java.util.Arrays;

public class ManualFragment extends Fragment {
    private View view;
    private int[] up_ids = {R.id.up_1, R.id.up_2, R.id.up_3, R.id.up_4, R.id.up_5, R.id.up_6, R.id.up_7, R.id.up_8, R.id.up_9};
    private int[] right_ids = {R.id.right_1, R.id.right_2, R.id.right_3, R.id.right_4, R.id.right_5, R.id.right_6, R.id.right_7, R.id.right_8, R.id.right_9};
    private int[] forward_ids = {R.id.forward_1, R.id.forward_2, R.id.forward_3, R.id.forward_4, R.id.forward_5, R.id.forward_6, R.id.forward_7, R.id.forward_8, R.id.forward_9};
    private int[] down_ids = {R.id.down_1, R.id.down_2, R.id.down_3, R.id.down_4, R.id.down_5, R.id.down_6, R.id.down_7, R.id.down_8, R.id.down_9};
    private int[] left_ids = {R.id.left_1, R.id.left_2, R.id.left_3, R.id.left_4, R.id.left_5, R.id.left_6, R.id.left_7, R.id.left_8, R.id.left_9};
    private int[] backward_ids = {R.id.backward_1, R.id.backward_2, R.id.backward_3, R.id.backward_4, R.id.backward_5, R.id.backward_6, R.id.backward_7, R.id.backward_8, R.id.backward_9};
    private View[] cubeViews = new View[54];
    private Button white_color_button, blue_color_button, red_color_button,
            green_color_button, yellow_color_button, orange_color_button;
    private String cubeStatus_color = "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN",
            turn_code = "", cubeStatus_direction = "";
    private char opCode = '\0';
    private Button ok_button, solve_button, lest_button, next_button, up_button, down_button,
            left_button, right_button, front_button, back_button, up_bar_button, down_bar_button,
            left_bar_button, right_bar_button, front_bar_button, back_bar_button, clear_button;
    private Boolean isOk = false;
    private ColorDirectionManager colorDirectionManager = new ColorDirectionManager();
    private TextView textView_solution, textView_turn_of_code, textView_lock;
    private int Solution_position = -1;
    View[][] viewsForNew = new View[6][9];

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view == null){
            view = inflater.inflate(R.layout.fragment_manual, container, false);
            init();
            textView_solution = view.findViewById(R.id.TextView_Solve);
            textView_turn_of_code = view.findViewById(R.id.turn_of_code);
            textView_lock = view.findViewById(R.id.lock);
        }
        return view;
    }

    private void colorButtonClick(View view){
        if(view.getId() == R.id.white_color_button){
            opCode = 'W';
        }
        else if(view.getId() == R.id.yellow_color_button){
            opCode = 'Y';
        }
        else if(view.getId() == R.id.green_color_button){
            opCode = 'G';
        }
        else if(view.getId() == R.id.blue_color_button){
            opCode = 'B';
        }
        else if(view.getId() == R.id.red_color_button){
            opCode = 'R';
        }
        else if(view.getId() == R.id.orange_color_button){
            opCode = 'O';
        }
    }
    private void cubeViewsClick(View view){
        if(isOk) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("是否解除鎖定魔術方塊?")
                    .setMessage("若要繼續此操作必須解除鎖定魔術方塊。\n是否要繼續?")
                    .setPositiveButton("是", (dialog, which) -> {
                        isOk = false;
                        textView_lock.setVisibility(View.INVISIBLE);
                        cubeStatus_color = exchangeDirection2ColorString(cubeStatus_direction);
                    })
                    .setNegativeButton("否", null)
                    .show();
            return;
        }
        switch (opCode){
            case 'W':
                view.setBackground(requireContext().getDrawable(R.drawable.rectangle_white));
                break;
            case 'Y':
                view.setBackground(requireContext().getDrawable(R.drawable.rectangle_yellow));
                break;
            case 'G':
                view.setBackground(requireContext().getDrawable(R.drawable.rectangle_green));
                break;
            case 'B':
                view.setBackground(requireContext().getDrawable(R.drawable.rectangle_blue));
                break;
            case 'R':
                view.setBackground(requireContext().getDrawable(R.drawable.rectangle_red));
                break;
            case 'O':
                view.setBackground(requireContext().getDrawable(R.drawable.rectangle_orange));
                break;
            default:
                Toast.makeText(getContext(), "請選擇顏色", Toast.LENGTH_SHORT).show();
                return;
        }
        int index = Arrays.asList(cubeViews).indexOf(view);
        StringBuilder stringBuilder = new StringBuilder(cubeStatus_color);
        stringBuilder.setCharAt(index, opCode);
        cubeStatus_color = stringBuilder.toString();
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
    private String exchangeDirection2ColorString(String direction){
        char[] directionCharArray = direction.toCharArray();
        char[] colorCharArray = new char[54];
        for (int i = 0; i< directionCharArray.length; i++){
            char color;
            switch (directionCharArray[i]){
                case 'U':
                    color = colorDirectionManager.getColorFromDirection(Direction.UP);
                    break;
                case 'R':
                    color = colorDirectionManager.getColorFromDirection(Direction.RIGHT);
                    break;
                case 'F':
                    color = colorDirectionManager.getColorFromDirection(Direction.FORWARD);
                    break;
                case 'D':
                    color = colorDirectionManager.getColorFromDirection(Direction.DOWN);
                    break;
                case 'L':
                    color = colorDirectionManager.getColorFromDirection(Direction.LEFT);
                    break;
                case 'B':
                    color = colorDirectionManager.getColorFromDirection(Direction.BACKWARD);
                    break;
                default:
                    color = '\0';
                    break;
            }
            colorCharArray[i] = color;
        }
        return new String(colorCharArray);
    }

    private void init(){
        initializationColorView();
        initializationButton();
    }
    private void initializationColorView(){
        white_color_button = view.findViewById(R.id.white_color_button);
        white_color_button.setOnClickListener(this::colorButtonClick);
        yellow_color_button = view.findViewById(R.id.yellow_color_button);
        yellow_color_button.setOnClickListener(this::colorButtonClick);
        green_color_button = view.findViewById(R.id.green_color_button);
        green_color_button.setOnClickListener(this::colorButtonClick);
        blue_color_button = view.findViewById(R.id.blue_color_button);
        blue_color_button.setOnClickListener(this::colorButtonClick);
        red_color_button = view.findViewById(R.id.red_color_button);
        red_color_button.setOnClickListener(this::colorButtonClick);
        orange_color_button = view.findViewById(R.id.orange_color_button);
        orange_color_button.setOnClickListener(this::colorButtonClick);

        for(int i = 0; i < 9; i++){
            cubeViews[i] = view.findViewById(up_ids[i]);
            viewsForNew[0][i] = cubeViews[i];
        }
        for(int i = 9; i < 18; i++){
            cubeViews[i] = view.findViewById(right_ids[i - 9]);
            viewsForNew[1][i - 9] = cubeViews[i];
        }
        for(int i = 18; i < 27; i++){
            cubeViews[i] = view.findViewById(forward_ids[i - 18]);
            viewsForNew[2][i - 18] = cubeViews[i];
        }
        for(int i = 27; i < 36; i++){
            cubeViews[i] = view.findViewById(down_ids[i - 27]);
            viewsForNew[3][i - 27] = cubeViews[i];
        }
        for(int i = 36; i < 45; i++){
            cubeViews[i] = view.findViewById(left_ids[i - 36]);
            viewsForNew[4][i - 36] = cubeViews[i];
        }
        for(int i = 45; i < 54; i++){
            cubeViews[i] = view.findViewById(backward_ids[i - 45]);
            viewsForNew[5][i - 45] = cubeViews[i];
        }
        for(int i = 0; i < 54; i++){
            cubeViews[i].setOnClickListener(this::cubeViewsClick);
        }
    }
    private void initializationButton(){
        ok_button = view.findViewById(R.id.okButton);
        ok_button.setOnClickListener(this::okButtonClick);
        solve_button = view.findViewById(R.id.solveButton);
        solve_button.setOnClickListener(this::solveButtonClick);
        lest_button = view.findViewById(R.id.button_lest);
        lest_button.setText("<");
        lest_button.setOnClickListener(this::lestButtonClick);
        next_button = view.findViewById(R.id.button_next);
        next_button.setText(">");
        next_button.setOnClickListener(this::nextButtonClick);
        up_button = view.findViewById(R.id.up_button);
        up_button.setOnClickListener(this::upButtonClick);
        down_button = view.findViewById(R.id.down_button);
        down_button.setOnClickListener(this::downButtonClick);
        left_button = view.findViewById(R.id.left_button);
        left_button.setOnClickListener(this::leftButtonClick);
        right_button = view.findViewById(R.id.right_button);
        right_button.setOnClickListener(this::rightButtonClick);
        front_button = view.findViewById(R.id.front_button);
        front_button.setOnClickListener(this::frontButtonClick);
        back_button = view.findViewById(R.id.back_button);
        back_button.setOnClickListener(this::backButtonClick);
        up_bar_button = view.findViewById(R.id.up_bar_button);
        up_bar_button.setOnClickListener(this::up_barButtonClick);
        down_bar_button = view.findViewById(R.id.down_bar_button);
        down_bar_button.setOnClickListener(this::down_barButtonClick);
        left_bar_button = view.findViewById(R.id.left_bar_button);
        left_bar_button.setOnClickListener(this::left_barButtonClick);
        right_bar_button = view.findViewById(R.id.right_bar_button);
        right_bar_button.setOnClickListener(this::right_barButtonClick);
        front_bar_button = view.findViewById(R.id.front_bar_button);
        front_bar_button.setOnClickListener(this::front_barButtonClick);
        back_bar_button = view.findViewById(R.id.back_bar_button);
        back_bar_button.setOnClickListener(this::back_barButtonClick);
        clear_button = view.findViewById(R.id.clearButton);
        clear_button.setOnClickListener(this::clearButtonClick);
    }

    private void clearButtonClick(View view) {
        turn_code = "";
        textView_turn_of_code.setText(turn_code);
    }

    private void upButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.up(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="U";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void downButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.down(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="D";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void leftButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.left(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="L";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void rightButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.right(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="R";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void frontButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.front(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="F";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void backButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.back(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="B";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void up_barButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.up_bar(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="u";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void down_barButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.down_bar(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="d";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void left_barButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.left_bar(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="l";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void right_barButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.right_bar(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="r";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void front_barButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.front_bar(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="f";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void back_barButtonClick(View view) {
        if(isOk){
            cubeStatus_direction = CubeStatusManager.back_bar(cubeStatus_direction);
            updateCubeStatus(cubeStatus_direction);
            turn_code+="b";
            textView_turn_of_code.setText(turn_code);
        }
    }
    private void nextButtonClick(View view) {
        if(!isOk) return;
        String solution = textView_solution.getText().toString().replaceAll("\n","");
        String[] solutions = solution.split(" ");
        Solution_position++;
        if(Solution_position < solutions.length){
            SolveCube(solutions[Solution_position],true);
        }
        SolutionText(Solution_position);
    }

    private void lestButtonClick(View view) {
        if(!isOk) return;
        String solution = textView_solution.getText().toString().replaceAll("\n","");
        String[] solutions = solution.split(" ");
        if(Solution_position != -1){
            SolveCube(solutions[Solution_position],false);
            Solution_position--;
            SolutionText(Solution_position);
        }
    }
    private void SolutionText(int solution_position){
        String solution = textView_solution.getText().toString();
        if(solution.isEmpty()) return;
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
        if(char_position > 0){
            Log.d("wnilnay",char_position+"");
            spannableString.setSpan(new ForegroundColorSpan(Color.GREEN),0,char_position,0);
        }
        textView_solution.setText(spannableString);
    }

    private void solveButtonClick(View view) {
        if(!isOk) return;
        String solution = new Search().solution(cubeStatus_direction,20,1000000,10000,0);
        solution = solution.replaceAll("  "," ");

        String[] solutions = solution.split(" ");
        String newSolution = "";
        for(int i = 0;i<solutions.length;i++){
            if(i%9 == 8){
                newSolution += "\n";
            }
            newSolution += solutions[i];
            newSolution += " ";
        }
        textView_solution.setText(newSolution);
        Solution_position = -1;
    }

    private void okButtonClick(View view){
        if(!cubeStatus_color.contains("N")){
            isOk = true;
            textView_lock.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "已鎖定魔術方塊", Toast.LENGTH_SHORT).show();
            initDirection(cubeStatus_color);
            cubeStatus_direction = exchangeColor2DirectionString(cubeStatus_color);
        }
        else{
            Toast.makeText(getContext(), "請將顏色全數填入完畢", Toast.LENGTH_SHORT).show();
        }
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
    private void SolveCube(String turn_code,boolean isPositive){
        //Log.d("wnilnay",turn_code);
        if(isPositive){
            switch (turn_code){
                case "R2":
                    rightButtonClick(null);
                case "R":
                    rightButtonClick(null);
                    break;
                case "U2":
                    upButtonClick(null);
                case "U":
                    upButtonClick(null);
                    break;
                case "F2":
                    frontButtonClick(null);
                case "F":
                    frontButtonClick(null);
                    break;
                case "L2":
                    leftButtonClick(null);
                case "L":
                    leftButtonClick(null);
                    break;
                case "D2":
                    downButtonClick(null);
                case "D":
                    downButtonClick(null);
                    break;
                case "B2":
                    backButtonClick(null);
                case "B":
                    backButtonClick(null);
                    break;
                case "R'":
                    right_barButtonClick(null);
                    break;
                case "U'":
                    up_barButtonClick(null);
                    break;
                case "F'":
                    front_barButtonClick(null);
                    break;
                case "L'":
                    left_barButtonClick(null);
                    break;
                case "D'":
                    down_barButtonClick(null);
                    break;
                case "B'":
                    back_barButtonClick(null);
                    break;
            }
        }
        else {
            switch (turn_code){
                case "R2":
                    right_barButtonClick(null);
                case "R":
                    right_barButtonClick(null);
                    break;
                case "U2":
                    up_barButtonClick(null);
                case "U":
                    up_barButtonClick(null);
                    break;
                case "F2":
                    front_barButtonClick(null);
                case "F":
                    front_barButtonClick(null);
                    break;
                case "L2":
                    left_barButtonClick(null);
                case "L":
                    left_barButtonClick(null);
                    break;
                case "D2":
                    down_barButtonClick(null);
                case "D":
                    down_barButtonClick(null);
                    break;
                case "B2":
                    back_barButtonClick(null);
                case "B":
                    back_barButtonClick(null);
                    break;
                case "R'":
                    rightButtonClick(null);
                    break;
                case "U'":
                    upButtonClick(null);
                    break;
                case "F'":
                    frontButtonClick(null);
                    break;
                case "L'":
                    leftButtonClick(null);
                    break;
                case "D'":
                    downButtonClick(null);
                    break;
                case "B'":
                    backButtonClick(null);
                    break;
            }
        }
    }

}