package com.example.test;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MainFragment extends Fragment {
    private RubiksCube3DView mGLView;
    private RubiksCubeRenderer renderer;
    private View view;
    private Button button_ok, button_lest, button_next, resetViewButton;
    private TextView textView_Solve, turn_of_code, lock;
    private boolean presetAngleSet = false;
    private String turn_code = "";
    private int Solution_position = -1;
    private boolean isOk = false;
    private boolean isSolve = false;
    private String cubeStatus = "";
    private String cubeStatus_direction = null;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private ColorDirectionManager colorDirectionManager = new ColorDirectionManager();
    private CheckBox animationCheckBox;
    private ScrollView operation_scroll_view;
    private int retryCount = 0;
    private String savedCubeColors = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_main, container, false);
            init();
        }
        return view;
    }

    private void init() {
        mGLView = view.findViewById(R.id.cube_view);
        renderer = mGLView.getRenderer();
        button_ok = view.findViewById(R.id.button_ok);
        button_lest = view.findViewById(R.id.button_lest);
        button_next = view.findViewById(R.id.button_next);
        resetViewButton = view.findViewById(R.id.button_reset_view);
        textView_Solve = view.findViewById(R.id.TextView_Solve);
        turn_of_code = view.findViewById(R.id.turn_of_code);
        lock = view.findViewById(R.id.lock);
        animationCheckBox = view.findViewById(R.id.checkbox_animation);
        animationCheckBox.setChecked(false);
        operation_scroll_view = view.findViewById(R.id.operation_scroll_view);
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
                clear(null);
                turn_code = lastTurn_code;
                turn_of_code.setText(lastTurn_code);
            }
        });
        lock.setVisibility(View.INVISIBLE);
        socket = BluetoothSocketManager.getSocket();
        button_lest.setVisibility(View.INVISIBLE);
        button_next.setVisibility(View.INVISIBLE);
        button_ok.setOnClickListener(v -> OkButton());
        button_lest.setOnClickListener(v -> lest());
        button_next.setOnClickListener(v -> next());
        resetViewButton.setOnClickListener(v -> {
            boolean animate = animationCheckBox != null && animationCheckBox.isChecked();
            renderer.setPresetOrientation(animate);
            resetViewButton.setVisibility(View.GONE);
        });
        renderer.setPresetOrientation(false);
        presetAngleSet = true;
        resetViewButton.setVisibility(View.GONE);
        mGLView.setOnTouchListener((v, event) -> {
            if(event.getAction()==android.view.MotionEvent.ACTION_UP && presetAngleSet){
                float dx = Math.abs(renderer.getAngleX() - 334.56f);
                float dy = Math.abs(renderer.getAngleY() - 144.88f);
                float dz = Math.abs(renderer.getAngleZ() - 345f);
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

    private void sendString(String sendTitle, String dataToSend){
        String result = BluetoothSocketManager.sendString(sendTitle, dataToSend);
        Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
    }

    public void up_button() {
        if(isOk){
            executeMove("U", () -> {
                cubeStatus = CubeStatusManager.up(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void right_button() {
        if(isOk){
            executeMove("R", () -> {
                cubeStatus = CubeStatusManager.right(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void down_button() {
        if(isOk){
            executeMove("D", () -> {
                cubeStatus = CubeStatusManager.down(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void left_button() {
        if(isOk){
            executeMove("L", () -> {
                cubeStatus = CubeStatusManager.left(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void front_button() {
        if(isOk){
            executeMove("F", () -> {
                cubeStatus = CubeStatusManager.front(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void back_button() {
        if(isOk){
            executeMove("B", () -> {
                cubeStatus = CubeStatusManager.back(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void up_bar_button() {
        if(isOk){
            executeMove("U'", () -> {
                cubeStatus = CubeStatusManager.up_bar(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void down_bar_button() {
        if(isOk){
            executeMove("D'", () -> {
                cubeStatus = CubeStatusManager.down_bar(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void right_bar_button() {
        if(isOk){
            executeMove("R'", () -> {
                cubeStatus = CubeStatusManager.right_bar(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void left_bar_button() {
        if(isOk){
            executeMove("L'", () -> {
                cubeStatus = CubeStatusManager.left_bar(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void front_bar_button() {
        if(isOk){
            executeMove("F'", () -> {
                cubeStatus = CubeStatusManager.front_bar(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public void back_bar_button() {
        if(isOk){
            executeMove("B'", () -> {
                cubeStatus = CubeStatusManager.back_bar(cubeStatus);
                updateCubeStatus(cubeStatus);
            });
        }
    }

    public String solve() {
        if(isOk){
            String solution = new Search().solution(cubeStatus,20,1000000,10000,0);
            solution = solution.replaceAll("  "," ");
            isSolve = true;

            if (solution.contains("Error")) solution += " ";
            String finalSolution = solution;
            Solution_position = -1;

            textView_Solve.setText(finalSolution);

            return solution;
        }
        return null;
    }

    public void lest() {
        if(!isSolve) return;
        String solution = textView_Solve.getText().toString();
        String[] solutions = solution.split(" ");
        if(Solution_position != -1){
            SolveCube(solutions[Solution_position],false);
            Solution_position--;
            SolutionText(Solution_position);
        }
    }

    public void next() {
        if(!isSolve) return;
        String solution = textView_Solve.getText().toString();
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
    private void SolveCube(String turn_code, boolean isPositive) {
        if (isPositive) {
            switch (turn_code) {
                case "R2":
                    executeMove("R2", () -> {
                        cubeStatus = CubeStatusManager.right_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "R":
                    right_button();
                    break;
                case "U2":
                    executeMove("U2", () -> {
                        cubeStatus = CubeStatusManager.up_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "U":
                    up_button();
                    break;
                case "F2":
                    executeMove("F2", () -> {
                        cubeStatus = CubeStatusManager.front_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "F":
                    front_button();
                    break;
                case "L2":
                    executeMove("L2", () -> {
                        cubeStatus = CubeStatusManager.left_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "L":
                    left_button();
                    break;
                case "D2":
                    executeMove("D2", () -> {
                        cubeStatus = CubeStatusManager.down_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "D":
                    down_button();
                    break;
                case "B2":
                    executeMove("B2", () -> {
                        cubeStatus = CubeStatusManager.back_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
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
        } else {
            switch (turn_code) {
                case "R2":
                    executeMove("R2", () -> {
                        cubeStatus = CubeStatusManager.right_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "R":
                    right_bar_button();
                    break;
                case "U2":
                    executeMove("U2", () -> {
                        cubeStatus = CubeStatusManager.up_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "U":
                    up_bar_button();
                    break;
                case "F2":
                    executeMove("F2", () -> {
                        cubeStatus = CubeStatusManager.front_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "F":
                    front_bar_button();
                    break;
                case "L2":
                    executeMove("L2", () -> {
                        cubeStatus = CubeStatusManager.left_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "L":
                    left_bar_button();
                    break;
                case "D2":
                    executeMove("D2", () -> {
                        cubeStatus = CubeStatusManager.down_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
                case "D":
                    down_bar_button();
                    break;
                case "B2":
                    executeMove("B2", () -> {
                        cubeStatus = CubeStatusManager.back_two(cubeStatus);
                        updateCubeStatus(cubeStatus);
                    });
                    break;
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

    private void executeMove(String moveCode, Runnable action) {
        boolean showAnim = animationCheckBox != null && animationCheckBox.isChecked();
        if (showAnim) {
            renderer.startSliceAnimation(moveCode, () -> {
                action.run();
                requireActivity().runOnUiThread(() -> {
                    turn_code += moveCode;
                    turn_of_code.setText(turn_code);
                });
            });
        } else {
            action.run();
            turn_code += moveCode;
            turn_of_code.setText(turn_code);
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
    public static float[] colorIntToRgba(int colorInt) {
        float r = ((colorInt >> 16) & 0xFF) / 255f;
        float g = ((colorInt >> 8) & 0xFF) / 255f;
        float b = (colorInt & 0xFF) / 255f;
        float a = ((colorInt >> 24) & 0xFF) / 255f;

        return new float[] { r, g, b, a };
    }

    public void OkButton() {
        isOk = true;
        sendString("OK", "");
        button_ok.setVisibility(View.INVISIBLE);
        button_lest.setVisibility(View.INVISIBLE);
        button_next.setVisibility(View.INVISIBLE);
        lock.setVisibility(View.VISIBLE);
        textView_Solve.setText("解法顯示區");
        cubeStatus = "";
        animationCheckBox.setChecked(false);
        startColorPolling();
    }

    private void startColorPolling() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String[] cubeColor = BluetoothSocketManager.getDataString();
                if (cubeColor != null && cubeColor[0].contains("Color")) {
                    requireActivity().runOnUiThread(() -> handleColorReceived(cubeColor[1]));
                    timer.cancel();
                }
            }
        }, 0, 100);
    }

    private void handleColorReceived(String color) {
        Log.d("wnilnay color", color);
        setColor(color);
        sendString("SolveStep", solve());
        startSolveStepPolling();
    }

    private void startSolveStepPolling() {
        Timer timer1 = new Timer();
        timer1.schedule(new TimerTask() {
            @Override
            public void run() {
                String[] nextString = BluetoothSocketManager.getDataString();
                if (nextString == null) return;
                requireActivity().runOnUiThread(() -> handleSolveStep(nextString, timer1));
            }
        }, 0, 100);
    }

    private void handleSolveStep(String[] nextString, Timer timer1) {
        if (nextString[0].contains("next")) {
            next();
        } else if (nextString[0].contains("end")) {
            button_lest.setVisibility(View.VISIBLE);
            button_next.setVisibility(View.VISIBLE);
            button_ok.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "完成!", Toast.LENGTH_SHORT).show();
            if (animationCheckBox != null) animationCheckBox.setChecked(true);
            timer1.cancel();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGLView != null && renderer != null) {
            mGLView.onPause();
            renderer.resetInitialization();
            // 保存目前顏色狀態
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
}