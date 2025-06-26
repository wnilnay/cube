package com.example.test;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Arrays;

public class ManualFragment extends Fragment {
    private RubiksCube3DView mGLView;
    private RubiksCubeRenderer renderer;
    private View view;
    private Button white_color_button, blue_color_button, red_color_button,
            green_color_button, yellow_color_button, orange_color_button;
    private Button ok_button, solve_button, lest_button, next_button, up_button, down_button,
            left_button, right_button, front_button, back_button, up_bar_button, down_bar_button,
            left_bar_button, right_bar_button, front_bar_button, back_bar_button, clear_button;
    private TextView textView_solution, textView_turn_of_code, textView_lock;
    private Button resetViewButton;
    private boolean presetAngleSet = false;

    private String cubeStatus_color = "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN",
            turn_code = "", cubeStatus_direction = "";
    private char opCode = '\0';
    private Boolean isOk = false;
    private final ColorDirectionManager colorDirectionManager = new ColorDirectionManager();
    private int Solution_position = -1;
    private CheckBox animationCheckBox;
    private ScrollView operation_scroll_view;
    private int retryCount = 0;
    private String savedCubeColors = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_manual, container, false);
            init();
        }
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGLView != null && renderer != null) {
            mGLView.onPause();
            renderer.resetInitialization();
            // 保存目前顏色狀態，並將未填色格子補為'N'（灰色）
            savedCubeColors = renderer.getAllColors();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGLView != null && renderer != null) {
            mGLView.onResume();
            mGLView.post(()->{
                if(savedCubeColors != null && savedCubeColors.length() == 54){
                    renderer.setAllColors(savedCubeColors);
                }
                mGLView.requestRender();
            });
        }
    }

    private void init(){
        initializationRubiksCube3DView();
        initializationColorView();
        initializationButton();
        textView_solution = view.findViewById(R.id.TextView_Solve);
        textView_turn_of_code = view.findViewById(R.id.turn_of_code);
        textView_lock = view.findViewById(R.id.lock);
        operation_scroll_view = view.findViewById(R.id.operation_scroll_view);
        animationCheckBox = view.findViewById(R.id.checkbox_animation);

        operation_scroll_view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                String lastTurn_code = "";
                if(!turn_code.isEmpty()){
                    char lastChar = turn_code.charAt(turn_code.length() - 1);
                    if (lastChar == '\'') {
                        char twoChar = turn_code.charAt(turn_code.length() - 2);
                        lastTurn_code = twoChar + "'";
                    }
                    else if (lastChar == '2') {
                        char twoChar = turn_code.charAt(turn_code.length() - 2);
                        lastTurn_code = twoChar + "2";
                    }
                    else {
                        lastTurn_code = String.valueOf(lastChar);
                    }
                }
                clearButtonClick(null);
                turn_code = lastTurn_code;
                textView_turn_of_code.setText(lastTurn_code);
            }
        });

        resetViewButton = view.findViewById(R.id.button_reset_view);
        resetViewButton.setOnClickListener(v -> {
            boolean animate = animationCheckBox != null && animationCheckBox.isChecked();
            renderer.setPresetOrientation(animate);
            resetViewButton.setVisibility(View.GONE);
        });
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
    }
    private void initializationButton(){
        ok_button = view.findViewById(R.id.okButton);
        ok_button.setOnClickListener(this::okButtonClick);
        solve_button = view.findViewById(R.id.solveButton);
        solve_button.setOnClickListener(this::solveButtonClick);
        lest_button = view.findViewById(R.id.button_lest);
        lest_button.setOnClickListener(this::lestButtonClick);
        next_button = view.findViewById(R.id.button_next);
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
    private void initializationRubiksCube3DView() {
        mGLView = view.findViewById(R.id.cube_view);

        // 獲取渲染器引用
        renderer = mGLView.getRenderer();

        mGLView.setOnHighlightListener(new RubiksCube3DView.OnHighlightListener() {
            @Override
            public void onHighlight(int faceIndex, int cellIndex) {
                cubeViewsClick(faceIndex, cellIndex);
            }
        });

        // 儲存預設角度
        final float presetX = 334.56f;
        final float presetY = 144.88f;
        final float presetZ = 345f;

        mGLView.setOnTouchListener((v, event) -> {
            if(event.getAction()==android.view.MotionEvent.ACTION_UP && presetAngleSet){
                float dx = Math.abs(renderer.getAngleX() - presetX);
                float dy = Math.abs(renderer.getAngleY() - presetY);
                float dz = Math.abs(renderer.getAngleZ() - presetZ);
                dx = dx>180?360-dx:dx;
                dy = dy>180?360-dy:dy;
                dz = dz>180?360-dz:dz;
                if((dx>3 || dy>3 || dz>3) && resetViewButton.getVisibility()==View.GONE){
                    resetViewButton.setVisibility(View.VISIBLE);
                }
            }
            return false;
        });
    }

    private void colorButtonClick(View view){
        white_color_button.setAlpha(1);
        yellow_color_button.setAlpha(1);
        green_color_button.setAlpha(1);
        blue_color_button.setAlpha(1);
        red_color_button.setAlpha(1);
        orange_color_button.setAlpha(1);
        view.setAlpha(0.5f);
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

    private void cubeViewsClick(int faceIndex, int cellIndex){
        if (faceIndex == -1 || cellIndex == -1) return;
        if(isOk) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("是否解除鎖定魔術方塊?")
                    .setMessage("若要繼續此操作必須解除鎖定魔術方塊。\n是否要繼續?")
                    .setPositiveButton("是", (dialog, which) -> {
                        isOk = false;
                        presetAngleSet = false;
                        textView_lock.setVisibility(View.INVISIBLE);
                        cubeStatus_color = exchangeDirection2ColorString(cubeStatus_direction);
                    })
                    .setNegativeButton("否", null)
                    .show();
            return;
        }
        int colorInt;
        float[] color;
        switch (opCode){
            case 'W':
                colorInt = ContextCompat.getColor(requireContext(), R.color.cube_white);
                color = colorIntToRgba(colorInt);
                break;
            case 'Y':
                colorInt = ContextCompat.getColor(requireContext(), R.color.cube_yellow);
                color = colorIntToRgba(colorInt);
                break;
            case 'G':
                colorInt = ContextCompat.getColor(requireContext(), R.color.cube_green);
                color = colorIntToRgba(colorInt);
                break;
            case 'B':
                colorInt = ContextCompat.getColor(requireContext(), R.color.cube_blue);
                color = colorIntToRgba(colorInt);
                break;
            case 'R':
                colorInt = ContextCompat.getColor(requireContext(), R.color.cube_red);
                color = colorIntToRgba(colorInt);
                break;
            case 'O':
                colorInt = ContextCompat.getColor(requireContext(), R.color.cube_orange);
                color = colorIntToRgba(colorInt);
                break;
            default:
                Toast.makeText(getContext(), "請選擇顏色", Toast.LENGTH_SHORT).show();
                return;
        }
        int index = faceIndex * 9 + cellIndex;
        StringBuilder stringBuilder = new StringBuilder(cubeStatus_color);
        stringBuilder.setCharAt(index, opCode);
        cubeStatus_color = stringBuilder.toString();
        renderer.setCellColor(faceIndex, cellIndex,color);
    }

    public static float[] colorIntToRgba(int colorInt) {
        float r = ((colorInt >> 16) & 0xFF) / 255f;
        float g = ((colorInt >> 8) & 0xFF) / 255f;
        float b = (colorInt & 0xFF) / 255f;
        float a = ((colorInt >> 24) & 0xFF) / 255f;

        return new float[] { r, g, b, a };
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

    private void clearButtonClick(View view) {
        turn_code = "";
        textView_turn_of_code.setText(turn_code);
    }

    private interface MoveAction { void apply(); }
    private void executeMove(String moveCode, MoveAction action){
        boolean showAnim = animationCheckBox != null && animationCheckBox.isChecked();
        if(showAnim){
            renderer.startSliceAnimation(moveCode, () -> {
                action.apply();
                requireActivity().runOnUiThread(() -> {
                    turn_code += moveCode;
                    textView_turn_of_code.setText(turn_code);
                });
            });
        }else {
            action.apply();
            turn_code += moveCode;
            textView_turn_of_code.setText(turn_code);
        }
    }

    private void upButtonClick(View view) {
        if(isOk){
            executeMove("U", () -> {
                cubeStatus_direction = CubeStatusManager.up(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void downButtonClick(View view) {
        if(isOk){
            executeMove("D", () -> {
                cubeStatus_direction = CubeStatusManager.down(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void leftButtonClick(View view) {
        if(isOk){
            executeMove("L", () -> {
                cubeStatus_direction = CubeStatusManager.left(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void rightButtonClick(View view) {
        if(isOk){
            executeMove("R", () -> {
                cubeStatus_direction = CubeStatusManager.right(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void frontButtonClick(View view) {
        if(isOk){
            executeMove("F", () -> {
                cubeStatus_direction = CubeStatusManager.front(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void backButtonClick(View view) {
        if(isOk){
            executeMove("B", () -> {
                cubeStatus_direction = CubeStatusManager.back(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void up_barButtonClick(View view) {
        if(isOk){
            executeMove("U'", () -> {
                cubeStatus_direction = CubeStatusManager.up_bar(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void down_barButtonClick(View view) {
        if(isOk){
            executeMove("D'", () -> {
                cubeStatus_direction = CubeStatusManager.down_bar(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void left_barButtonClick(View view) {
        if(isOk){
            executeMove("L'", () -> {
                cubeStatus_direction = CubeStatusManager.left_bar(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void right_barButtonClick(View view) {
        if(isOk){
            executeMove("R'", () -> {
                cubeStatus_direction = CubeStatusManager.right_bar(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void front_barButtonClick(View view) {
        if(isOk){
            executeMove("F'", () -> {
                cubeStatus_direction = CubeStatusManager.front_bar(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void back_barButtonClick(View view) {
        if(isOk){
            executeMove("B'", () -> {
                cubeStatus_direction = CubeStatusManager.back_bar(cubeStatus_direction);
                updateCubeStatus(cubeStatus_direction);
            });
        }
    }
    private void nextButtonClick(View view) {
        if(!isOk) return;
        String solution = textView_solution.getText().toString();
        String[] solutions = solution.split(" ");
        Solution_position++;
        if(Solution_position < solutions.length){
            SolveCube(solutions[Solution_position],true);
            SolutionText(Solution_position);
        }
        else {
            Solution_position--;
        }

    }

    private void lestButtonClick(View view) {
        if(!isOk) return;
        String solution = textView_solution.getText().toString();
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
        Toast.makeText(requireContext(), "開始求得解法", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String solution = new Search().solution(cubeStatus_direction,20,1000000,10000,0);
                solution = solution.replaceAll("  "," ");

                if (solution.contains("Error")) solution += " ";
                String finalSolution = solution;
                Solution_position = -1;

                requireActivity().runOnUiThread(() -> {
                    textView_solution.setText(finalSolution);
                    Toast.makeText(requireContext(), "完成", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();

    }

    private void okButtonClick(View view){
        if(!cubeStatus_color.contains("N")){
            isOk = true;
            textView_lock.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "已鎖定魔術方塊", Toast.LENGTH_SHORT).show();
            initDirection(cubeStatus_color);
            cubeStatus_direction = exchangeColor2DirectionString(cubeStatus_color);

            boolean animate = animationCheckBox != null && animationCheckBox.isChecked();
            // 設定預設視角
            renderer.setPresetOrientation(animate);
            presetAngleSet = true;
            resetViewButton.setVisibility(View.GONE);
        }
        else{
            Toast.makeText(getContext(), "請將顏色全數填入完畢", Toast.LENGTH_SHORT).show();
        }
    }
    private void SolveCube(String turn_code,boolean isPositive){
        //Log.d("wnilnay",turn_code);
        if(isPositive){
            switch (turn_code){
                case "R2":
                    executeMove("R2", () -> {
                        cubeStatus_direction = CubeStatusManager.right_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "R":
                    rightButtonClick(null);
                    break;
                case "U2":
                    executeMove("U2", () -> {
                        cubeStatus_direction = CubeStatusManager.up_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "U":
                    upButtonClick(null);
                    break;
                case "F2":
                    executeMove("F2", () -> {
                        cubeStatus_direction = CubeStatusManager.front_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "F":
                    frontButtonClick(null);
                    break;
                case "L2":
                    executeMove("L2", () -> {
                        cubeStatus_direction = CubeStatusManager.left_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "L":
                    leftButtonClick(null);
                    break;
                case "D2":
                    executeMove("D2", () -> {
                        cubeStatus_direction = CubeStatusManager.down_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "D":
                    downButtonClick(null);
                    break;
                case "B2":
                    executeMove("B2", () -> {
                        cubeStatus_direction = CubeStatusManager.back_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
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
                    executeMove("R2", () -> {
                        cubeStatus_direction = CubeStatusManager.right_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "R":
                    right_barButtonClick(null);
                    break;
                case "U2":
                    executeMove("U2", () -> {
                        cubeStatus_direction = CubeStatusManager.up_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "U":
                    up_barButtonClick(null);
                    break;
                case "F2":
                    executeMove("F2", () -> {
                        cubeStatus_direction = CubeStatusManager.front_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "F":
                    front_barButtonClick(null);
                    break;
                case "L2":
                    executeMove("L2", () -> {
                        cubeStatus_direction = CubeStatusManager.left_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "L":
                    left_barButtonClick(null);
                    break;
                case "D2":
                    executeMove("D2", () -> {
                        cubeStatus_direction = CubeStatusManager.down_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
                case "D":
                    down_barButtonClick(null);
                    break;
                case "B2":
                    executeMove("B2", () -> {
                        cubeStatus_direction = CubeStatusManager.back_two(cubeStatus_direction);
                        updateCubeStatus(cubeStatus_direction);
                    });
                    break;
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

    private void updateCubeStatus(String newStatus){
        char[] newStatuses = newStatus.toCharArray();
        int position = 0;
        int[] drawableIDs = new int[6];
        drawableIDs[colorDirectionManager.getYellowDirection().ordinal()] = R.color.cube_yellow;
        drawableIDs[colorDirectionManager.getBlueDirection().ordinal()] = R.color.cube_blue;
        drawableIDs[colorDirectionManager.getRedDirection().ordinal()] = R.color.cube_red;
        drawableIDs[colorDirectionManager.getWhiteDirection().ordinal()] = R.color.cube_white;
        drawableIDs[colorDirectionManager.getOrangeDirection().ordinal()] = R.color.cube_orange;
        drawableIDs[colorDirectionManager.getGreenDirection().ordinal()] = R.color.cube_green;
        for (char status : newStatuses){
            int faceIndex = position / 9;
            int cellIndex = position - (faceIndex * 9);
            switch (status){
                case 'U':
                    renderer.setCellColor(faceIndex, cellIndex, colorIntToRgba(
                            ContextCompat.getColor(requireContext(), drawableIDs[Direction.UP.ordinal()])
                    ));
                    break;
                case 'R':
                    renderer.setCellColor(faceIndex, cellIndex, colorIntToRgba(
                            ContextCompat.getColor(requireContext(), drawableIDs[Direction.RIGHT.ordinal()])
                    ));
                    break;
                case 'F':
                    renderer.setCellColor(faceIndex, cellIndex, colorIntToRgba(
                            ContextCompat.getColor(requireContext(), drawableIDs[Direction.FORWARD.ordinal()])
                    ));
                    break;
                case 'D':
                    renderer.setCellColor(faceIndex, cellIndex, colorIntToRgba(
                            ContextCompat.getColor(requireContext(), drawableIDs[Direction.DOWN.ordinal()])
                    ));
                    break;
                case 'L':
                    renderer.setCellColor(faceIndex, cellIndex, colorIntToRgba(
                            ContextCompat.getColor(requireContext(), drawableIDs[Direction.LEFT.ordinal()])
                    ));
                    break;
                case 'B':
                    renderer.setCellColor(faceIndex, cellIndex, colorIntToRgba(
                            ContextCompat.getColor(requireContext(), drawableIDs[Direction.BACKWARD.ordinal()])
                    ));
                    break;
                default:
                    break;
            }
            position++;
        }
    }

    // 恢復魔方顏色（因GL內容遺失重新創建時）
    private void restoreCubeColors(){
        if(renderer==null) return;
        if(!renderer.isInitialized()){
            if(retryCount < 20){
                retryCount++;
                mGLView.postDelayed(this::restoreCubeColors, 100);
                return;
            }
            return;
        }
        if(savedCubeColors != null && savedCubeColors.length() == 54){
            mGLView.postDelayed(() -> {
                renderer.setAllColors(savedCubeColors);
                mGLView.requestRender();
            }, 150);
        }else{
            mGLView.requestRender();
        }
        retryCount = 0;
    }

    public RubiksCubeRenderer getRenderer() {
        return renderer;
    }
}