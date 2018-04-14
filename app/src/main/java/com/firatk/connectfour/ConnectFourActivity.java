package com.firatk.connectfour;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Random;
import java.util.Timer;

enum CellType{
    PLAYER1,PLAYER2,CPU,SPACE
}
enum GameType{
    PVP,PVC
}

public class ConnectFourActivity extends AppCompatActivity {
    private Cell[][] gameCells;
    private GridLayout layout;
    private ImageView imagePlayer1,imagePlayer2;
    private ImageView timeView;
    private ImageButton undoButton;
    private TextView textPlayer1,textPlayer2;
    private TextView textTime;
    int time;int setTime;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private Dialog finishDialog;
    private ImageButton noAgain,onceAgain;
    private int _width,_height;
    private int finalRow,finalCol;
    private CellType _turn;
    private GameType _type;
    int lastIndex;
    int[] oldCol;
    private final String TAG="connectfourtag";
    private AlphaAnimation slideAnim;
    int click = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_four);

        _turn = CellType.PLAYER1;
        layout = (GridLayout) findViewById(R.id.gridLayout);
        _width = _height = getIntent().getIntExtra(HomeActivity.EXTRA_SIZE,5);
        //put these into a func
        int type = getIntent().getIntExtra(HomeActivity.EXTRA_TYPE, 0);
        setTime = getIntent().getIntExtra(HomeActivity.EXTRA_TIME,3);
        textPlayer1 = (TextView) findViewById(R.id.textPlayer1);
        textPlayer2 = (TextView) findViewById(R.id.textPlayer2);

        textPlayer1.setText("P1");
        if(type == 0) {
            _type = GameType.PVC;
            textPlayer2.setText("CPU");
        }
        else {
            _type = GameType.PVP;
            textPlayer2.setText("P2");
        }

        finalRow = 0;finalCol = 0;

        lastIndex = 0;
        oldCol = new int[_width * _height];

        //undo button
        undoButton = (ImageButton) findViewById(R.id.buttonUndo);
        undoButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(lastIndex != 0 && removeLast(oldCol[lastIndex - 1]))
                {
                    changeTurn();
                    changeLabels();
                    updateImages();
                    time = setTime;
                    if (_turn == CellType.CPU)
                        play(false);
                }
            }
        });

        //time things
        textTime = (TextView) findViewById(R.id.timeText);
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                textTime.setText(String.format("%d",time));
                time--;
                timerHandler.postDelayed(this,1000);
                if(time == -1){
                    time = setTime;
                    Random rand  = new Random();
                    int col;
                    do{
                        col = rand.nextInt(_width);
                    }while(!isColumnLegal(col));
                    putMove(col);
                    animateCells(col,0,_turn);
                }
            }
        };
        timeView = (ImageView) findViewById(R.id.imageTimeView);
        if(setTime == 0)
            timeView.setBackgroundResource(R.drawable.ic_alarm_off);
        else
            timeView.setBackgroundResource(R.drawable.ic_timelapse);

        //images of players(stating turn) at top
        imagePlayer1 = (ImageView) findViewById(R.id.imagePlayer1);
        imagePlayer2 = (ImageView) findViewById(R.id.imagePlayer2);
        imagePlayer1.setBackgroundResource(R.drawable.p1withback);
        imagePlayer2.setBackgroundResource(R.drawable.p2space);

        layout.setColumnCount(_width);
        layout.setRowCount(_height);
        CellListener cellListener = new CellListener();
        gameCells = new Cell[_width][_height];
        for(int i = 0 ; i < _width ; ++i){
            for(int j = 0 ; j < _height ; ++j) {
                gameCells[i][j] = new Cell(ConnectFourActivity.this, i, j);
                gameCells[i][j].setOnClickListener(cellListener);
            }
        }
        for(int i = 0 ; i < _width ; ++i) {
            for (int j = 0; j < _height; ++j)
                layout.addView(gameCells[i][j]);
        }
    }

    /**
     * Listens when a cell is clicked
     */
    private class CellListener implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            click++;
            Cell tmp = (Cell) view;
            int col = tmp.getCol();
            if(isColumnLegal(col)) {
                final int row = rowIndex(col);
                gameCells[row][col].setType(_turn);
                oldCol[lastIndex] = col;
                lastIndex++;
                lockCells();
                int startRow = 0;
                if(_width > 20)
                    startRow = _width / 2;
                animateCells(col, startRow, _turn);
                if (setTime != 0) {
                    time = setTime;
                    timerHandler.removeCallbacks(timerRunnable);
                    timerHandler.postDelayed(timerRunnable, 0);
                }
            }
        }
    }

    /**
     * Runnable class does some required staff after game played by user or computer
     */
    private class PlayOneTurn implements Runnable {

        @Override
        public void run() {
            if(checkForWin()){
                handleFinish(false,true);
            }
            else if(isOver()) {
                handleFinish(true,false);
            }
            else {
                changeTurn();
                changeLabels();
                if(_turn == CellType.CPU)
                    play();
                    //updateImages();
                }
            }
    }

    /**
     *
     * @param col start column
     * @param row start row
     * @param type which type of cell to animate
     */
    private void animateCells(final int col, final int row,final CellType type){
        if(!isLocationSpace(row,col)) {
            gameCells[row][col].updateImage();
            unlockCells();
            final Handler handler = new Handler();
            handler.postDelayed(new PlayOneTurn(),0);
        }
        else{
            slideAnim = new AlphaAnimation(1.0f, 1.0f);
            slideAnim.setDuration(_width < 15 ? 50 : 25);
            gameCells[row][col].setType(type);
            gameCells[row][col].updateImage();
            gameCells[row][col].startAnimation(slideAnim);
            slideAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    gameCells[row][col].setType(CellType.SPACE);
                    gameCells[row][col].updateImage();
                    animateCells(col,row+1,type);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });}
    }

    /**
     * @param isDraw whether game is draw
     * @param playAnimation whether play the animation or not
     */
    private void handleFinish(boolean isDraw,boolean playAnimation){
        timerHandler.removeCallbacks(timerRunnable);
        if(playAnimation){
            lockCells();
            MediaPlayer mp = MediaPlayer.create(this,R.raw.finish);
            mp.start();
            finishAnim(isDraw);
        }else
            showFinish(isDraw);
    }
    /**
     * @param isDraw whether game is draw
     * starts the finish animation(fading in and out cells)
     */
    private void finishAnim(final boolean isDraw){
        if(slideAnim.hasEnded())
            updateImages();
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(1000);
        alphaAnimation.setRepeatCount(3);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        int r = 0,c = 0;
        for(int i = 0 ; i < _width ; ++i){
            for(int j = 0 ; j < _height ; ++j){
                if(gameCells[i][j].isFinal()) {
                    gameCells[i][j].startAnimation(alphaAnimation);
                    r = i;c = j;
                }
            }
        }
        final Cell tmpCell = gameCells[r][c];
        tmpCell.postDelayed(new Runnable() {
            @Override
            public void run() {
                showFinish(isDraw);
            }
        },3500);
    }
    /**
     * @param isDraw whether game is draw
     *  shows the finish dialog asking again or not
     */
    private void showFinish(boolean isDraw){
        finishDialog = new Dialog(ConnectFourActivity.this);
        finishDialog.setContentView(R.layout.wondraw);
        ImageView playerView = (ImageView) finishDialog.findViewById(R.id.wonPlayerId);
        TextView text = (TextView) finishDialog.findViewById(R.id.toastTextView);
        if(isDraw) {
            text.setText("Draw !");
            playerView.setVisibility(View.GONE);
        }else {
            text.setText("won!");
            if(_turn == CellType.PLAYER1)
                playerView.setImageResource(R.drawable.player1);
            else
                playerView.setImageResource(R.drawable.player2);
        }
        noAgain = (ImageButton) finishDialog.findViewById(R.id.noAgainButton);
        onceAgain = (ImageButton) finishDialog.findViewById(R.id.onceAgainButton);

        AgainButtonListener listener = new AgainButtonListener();
        noAgain.setOnClickListener(listener);
        onceAgain.setOnClickListener(listener);

        finishDialog.setCanceledOnTouchOutside(false);
        finishDialog.setCancelable(false);
        finishDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if(!ConnectFourActivity.this.isFinishing())
            finishDialog.show();
    }
    public class AgainButtonListener implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            if(view.getId() == noAgain.getId())
                finish();
            else {
                restart();
                finishDialog.dismiss();
            }

        }
    }

    /**
     * locks cells and buttons
     */
    private void lockCells(){
        undoButton.setClickable(false);
        for(int i = 0 ; i < _width ; ++i) {
            for (int j = 0; j < _height; ++j) {
                gameCells[i][j].setClickable(false);
            }
        }
    }

    /**
     * unlocks cells and buttons
     */
    private void unlockCells(){
        undoButton.setClickable(true);
        for(int i = 0 ; i < _width ; ++i) {
            for (int j = 0; j < _height; ++j) {
                gameCells[i][j].setClickable(true);
            }
        }
    }

    /**
     * resets the variables and makes cells space
     */
    private void restart(){
        finalRow = 0;
        finalCol = 0;
        lastIndex = 0;
        time = 0;
        unlockCells();
        if(getGameType() == GameType.PVP || (getGameType() == GameType.PVC && _turn == CellType.CPU)){
            changeTurn();
            changeLabels();
        }
        for(int i = 0 ; i < _width ; ++i) {
            for (int j = 0; j < _height; ++j) {
                gameCells[i][j].setIsFinal(false);
                gameCells[i][j].setType(CellType.SPACE);
                gameCells[i][j].updateImage();
            }
        }
    }

    /**
     * @return game type which can be pvp or pvc
     */
    private GameType getGameType(){
        return _type;
    }
    public void onBackPressed(){
        AlertDialog.Builder builder = new AlertDialog.Builder(ConnectFourActivity.this);
        builder.setTitle("").setMessage("Are you sure ? Moves will be deleted").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        }).setNegativeButton("No",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        }).show();
    }

    /**
     * play the game for cpu for one turn
     * @return column index to bo played
     */
    private int play(){
        return play(true);
    }
    private int play(boolean isHard){
        int col;
        if(isHard && _width < 10)
            col = getHardCpuMove();
        else
            col = getCpuMove();
        putMove(col);
        lockCells();
        int startRow = 0;
        if(_width > 20)
            startRow = _width / 2;
        animateCells(col,startRow,CellType.CPU);
        return col;
    }

    /**
     * @param col column in which last cell to be deleted
     * @return true if last cell removed succesfully,false otherwise
     */
    public boolean removeLast(int col){
        if(isBoardEmpty())
            return false;
        for(int i = 0 ; i < _width ; ++i){
            if(gameCells[i][col].getType() != CellType.SPACE){
                gameCells[i][col].setType(CellType.SPACE);
                lastIndex--;
                break;
            }
        }
        return true;
    }

    /**
     * @return true if there is at least one space cell
     */
    private boolean isBoardEmpty(){
        for(int i = 0 ; i < _width ; ++i)
        {
            for(int j = 0 ; j < _height ; ++j)
            {
                if(gameCells[i][j].getType() != CellType.SPACE)
                    return false;
            }
        }
        return true;
    }
    /**
     * @param col column to be move inserted
     * @return return the row index move inserted
     */
    public int putMove(int col){
        return putMove(col,_turn);
    }
    public int putMove(int col,CellType p){
        int index = rowIndex(col);
        if(index != -1) {
            gameCells[index][col].setType(p);
            oldCol[lastIndex] = col;
            lastIndex++;
        }
        return index;
    }

    /**
     * @return column index of the cpu move
     */
    public int getCpuMove(){
        int retCol;
        int row;
        for(int i = 0 ; i < _width ; ++i){
            if(CellType.SPACE == gameCells[0][i].getType()){
                row = putMove(i,CellType.CPU);
                if(checkHorizontal(CellType.CPU,true) ||
                        checkVertical(CellType.CPU,true) ||
                        checkDiagonal(CellType.CPU,true) ||
                        checkReverseDiagonal(CellType.CPU,true))
                {
                    removeLast(i);
                    return i;
                }
                removeLast(i);
            }
        }
        for(int i = 0 ; i < _width ; ++i){
            if(CellType.SPACE == gameCells[0][i].getType()){
                row = putMove(i,CellType.PLAYER1);
                if(checkHorizontal(CellType.PLAYER1,true) ||
                        checkVertical(CellType.PLAYER1,true) ||
                        checkDiagonal(CellType.PLAYER1,true) ||
                        checkReverseDiagonal(CellType.PLAYER1,true) ||
                        lookHorizontal(row, i, 3) == 1 ||
                        lookVertical(row, i, 3) == 1 ||
                        lookDiagonal(row, i, 3) == 1  )
                {
                    removeLast(i);
                    return i;
                }
                removeLast(i);
            }
        }
        Random rand  = new Random();
        int col;
        do{
            col = rand.nextInt(_width);
        }while(!isColumnLegal(col));
        return col;
    }

    /**
     * @return true if it is draw,false otherwise
     */
    public boolean isOver(){
        for(int i = 0 ; i < _width ; ++i){
            for(int j = 0 ; j < _width ; ++j)
            {
                if(gameCells[i][j].getType() == CellType.SPACE)
                    return false;
            }
        }
        return true;
    }
    /**
     * @return true if the player in turn wins false otherwise
     */
    public boolean checkForWin(){
        boolean ret = checkHorizontal();
        if(ret)
            makeFinalSpace(0,finalRow,finalCol);
        if(!ret){
            ret = checkVertical();
            if(ret)
                makeFinalSpace(1,finalRow,finalCol);
        }
        if(!ret){
            ret = checkDiagonal();
            if(ret)
                makeFinalSpace(2,finalRow,finalCol);
        }
        if(!ret) {
            ret = checkReverseDiagonal();
            if(ret)
                makeFinalSpace(3,finalRow,finalCol);
        }
        return ret;
    }

    /**
     * @param checkType type to be checked whether horizontally,vertically or diagonally
     * @param eX end of column
     * @param eY end of row
     */
    private void makeFinalSpace(int checkType,int eX,int eY){
        int i;
        switch(checkType)
        {
            //horizontal
            case 0:{
                for(i = 0 ; i < 4 ; ++i)
                    gameCells[eX][eY - i].setIsFinal(true);
                break;
            }
            //vertical
            case 1:{
                for(i = 0 ; i < 4 ; ++i)
                    gameCells[eX + i][eY].setIsFinal(true);
                break;
            }
            //diagonal
            case 2:{
                for(i = 0 ; i < 4 ; ++i){
                    gameCells[eX - i][eY - i].setIsFinal(true);
                }
                break;
            }
            //reverse diagonal
            case 3:{
                for(i = 0 ; i < 4 ; ++i)
                    gameCells[eX + i][eY - i].setIsFinal(true);
                break;}
        }
    }

    /**
     * @param row row to be checked
     * @param col column to be checked
     * @return  true if the cell is space,false otherwise
     */
    private boolean isLocationLegal(int row,int col){
        return ((row >= 0 && row < _width) && (col >= 0 && col < _height));
    }
    private boolean isLocationSpace(int row,int col){
        if(isLocationLegal(row,col))
            return gameCells[row][col].getType() == CellType.SPACE;
        return false;
    }
    /**
     * @return checks board through reverse diagonal indexes true if player in turn wins false otherwise
     */
    private boolean checkReverseDiagonal(){
        return checkReverseDiagonal(_turn,false);
    }
    private boolean checkReverseDiagonal(CellType p,boolean callingFromCpu){
        for(int i = _width - 1 ; i >= 3; --i){
            for(int j = 0 ; j < _width - 3; ++j){
                if(gameCells[i][j].getType() == p &&
                        gameCells[i-1][j+1].getType() == p &&
                        gameCells[i-2][j+2].getType() == p &&
                        gameCells[i-3][j+3].getType() == p) {
                    if(!callingFromCpu) {
                        finalRow = i - 3;
                        finalCol = j + 3;
                    }
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * @return checks board through diagonal indexes true if player in turn wins false otherwise
     */
    private boolean checkDiagonal(){
        return checkDiagonal(_turn,false);
    }
    private boolean checkDiagonal(CellType p,boolean callingFromCpu){
        for(int i = 0 ; i < _width - 3; ++i){
            for(int j = 0 ; j < _width - 3; ++j){
                if(gameCells[i][j].getType() == p &&
                        gameCells[i+1][j+1].getType() == p &&
                        gameCells[i+2][j+2].getType() == p &&
                        gameCells[i+3][j+3].getType() == p) {
                    if(!callingFromCpu) {
                        finalRow = i + 3;
                        finalCol = j + 3;
                    }
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * @return checks board through horizontal indexes true if player in turn wins false otherwise
     */
    private boolean checkHorizontal(){
        return checkHorizontal(_turn,false);
    }
    private  boolean checkHorizontal(CellType p,boolean callingFromCpu){
        for(int i = 0 ; i < _width ; ++i){
            for(int j = 0 ; j < _width - 3; ++j){
                if(gameCells[i][j].getType() == p &&
                        gameCells[i][j+1].getType() == p &&
                        gameCells[i][j+2].getType() == p &&
                        gameCells[i][j+3].getType() == p) {
                    if(!callingFromCpu) {
                        finalRow = i;
                        finalCol = j + 3;
                    }
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * @return checks board through vertical indexes true if player in turn wins false otherwise
     */
    private boolean checkVertical(){
        return checkVertical(_turn,false);
    }
    private boolean checkVertical(CellType p,boolean callingFromCpu){
        for(int i = 0 ; i < _width ; ++i){
            for(int j = _width - 1 ; j >= 3; --j){
                if(gameCells[j][i].getType() == p &&
                        gameCells[j-1][i].getType() == p &&
                        gameCells[j-2][i].getType() == p &&
                        gameCells[j-3][i].getType() == p) {
                    if(!callingFromCpu) {
                        finalRow = j - 3;
                        finalCol = i;
                    }
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * @param x starting col position
     * @param y starting row position
     * @param lookAhead how many cells to be looked for
     * @return 1 if given elements are identical within given x and y
     */
    public int lookHorizontal(int x,int y,int lookAhead){
        int c = 0;
        if(y + lookAhead - 1 < _width)
        {
            for(int i = 0 ; i < lookAhead ; ++i)
            {
                if(gameCells[x][y].getType() == gameCells[x][y + i].getType())
                    c +=1;
                else break;
            }
        }
        if(c >= lookAhead)
            return 1;
        else
            return 0;
    }
    /**
     *@param x starting col position
     * @param y starting row position
     * @param lookAhead how many cells to be looked for
     * @return 1 if given elements are identical within given x and y
     */
    public int lookVertical(int x,int y,int lookAhead){
        int c = 0;
        if(x - lookAhead + 1 >= 0 )
        {
            for(int i = 0 ; i < lookAhead ; ++i)
            {
                if(gameCells[x][y].getType() == gameCells[x - i][y].getType())
                    c +=1;
                else break;
            }
        }

        if(c >= lookAhead)
            return 1;
        else
            return 0;
    }
    /**
     * @param x starting col position
     * @param y starting row position
     * @param lookAhead how many cells to be looked for
     * @return 1 if given elements are identical within given x and y
     */
    public int lookDiagonal(int x,int y,int lookAhead)
    {
        int total = 0;
        int c = 0;
        if(x + lookAhead - 1 < _width && y + lookAhead - 1 < _width)
        {
            for(int i = 0 ; i < lookAhead ; ++i)
            {
                if(gameCells[x][y].getType() == gameCells[x + i][y + i].getType())
                    c +=1;
                else break;
            }
        }

        if(c >= lookAhead)
            total += 1;

        c = 0;
        if(x - lookAhead + 1 >= 0 && y + lookAhead - 1 < _width)
        {
            for(int i = 0 ; i < lookAhead ; ++i)
            {
                if(gameCells[x][y].getType() == gameCells[x - i][y + i].getType())
                    c +=1;
                else break;
            }
        }

        if(c >= lookAhead)
            total +=1;

        return total;
    }

    /**
     * @param player player type to be looked for within lookAhead
     * @param lookAhead how many cells to be looked for
     * @return sum of the lookHorizontal,vertical and diagonal values
     */
    private int findWays(CellType player,int lookAhead){
        int i,j;
        int val = 0;
        for(i = 0 ; i < _height ; ++i)
        {
            for(j = 0 ; j < _width ; ++j)
            {
                if(gameCells[i][j].getType() == player)
                {
                    val += lookVertical(i,j,lookAhead);
                    val += lookHorizontal(i,j,lookAhead);
                    val += lookDiagonal(i,j,lookAhead);
                }
            }
        }
        return val;
    }

    /**
     * @param depth depth to ai dives into
     * @return value for the cpu to determine the current value of the board
     */
    private int evaluateHeuristic(int depth){
        int lookFour = findWays(CellType.CPU,4);
        int lookThree = findWays(CellType.CPU,3);
        int lookTwo = findWays(CellType.CPU,2);
        int lookPlayerFour = findWays(CellType.PLAYER1,4);

        if(lookPlayerFour > 0)
            return -100000 + depth;
        else
            return (lookFour * 100000 + lookThree * 100 + lookTwo * 10 - depth);
    }

    /**
     * @param depth depth to ai dives into
     * @param turn whose turn it is
     * @param highestDepth highest depth to dive into
     * @param alpha max value of the board
     * @param beta min value of the board
     * @return a value for cpu to determine which column to be played
     */
    private int minmax(int depth,CellType turn,int highestDepth,int alpha,int beta){
        if(isOver())
            return 0;
        if(depth > highestDepth)
            return evaluateHeuristic(depth);

        int v;
        if(turn == CellType.CPU){
            int best = -100000;
            for(int i = 0 ; i < _width ; ++i){
                if(isColumnLegal(i)){
                    putMove(i,CellType.CPU);
                    v = minmax(depth + 1,CellType.PLAYER1,highestDepth,alpha,beta);
                    removeLast(i);
                    if(v > best)
                    {
                        best = v;
                        if(best > alpha)
                            alpha = best;
                    }
                    if(beta <= alpha)
                        break;
                }
            }
            return best;
        }else{
            int best = 100000;
            for(int i = 0 ; i < _width ; ++i){
                if(isColumnLegal(i)){
                    putMove(i,CellType.PLAYER1);
                    v = minmax(depth + 1,CellType.CPU,highestDepth,alpha,beta);
                    removeLast(i);
                    if(v < best)
                    {
                        best = v;
                        if(best < beta)
                            beta = best;
                    }
                    if(beta <= alpha)
                        break;
                }
            }
            return best;
        }
    }

    /**
     * @return column index for the cpu
     */
    private int getHardCpuMove(){
        int bestVal = -1000000;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int highestDepth = 4;
        int moveVal;int col = -1;
        for(int i = 0 ; i < _width ; ++i){
            if(isColumnLegal(i)){
                putMove(i,CellType.CPU);
                moveVal = minmax(0,CellType.PLAYER1,highestDepth,alpha,beta);
                removeLast(i);
                if(moveVal > bestVal){
                    col = i;
                    bestVal = moveVal;
                }
            }
        }
        return col;
    }

    /**
     * @param col column index to be looked for legality
     * @return true if the cell valid,false otherwise
     */
    private boolean isColumnLegal(int col){
        return gameCells[0][col].getType() == CellType.SPACE;
    }

    /**
     * @param col column to be looked for the row index
     * @return index of the appropriate row in the given column
     */
    private int rowIndex(int col){
        int index = -1;
        for(int i = 0 ; i < _height ; ++i){
            if(gameCells[i][col].getType() == CellType.SPACE)
                index = i;
        }
        return index;
    }

    /**
     * changes the turn with respect to current turn and game type
     */
    private void changeTurn(){
        if(_type == GameType.PVP){
            if(_turn == CellType.PLAYER1)
                setTurn(CellType.PLAYER2);
            else
                setTurn(CellType.PLAYER1);
        }else{
            if(_turn == CellType.PLAYER1)
                setTurn(CellType.CPU);
            else
                setTurn(CellType.PLAYER1);
        }
        //updateImages();
    }

    /**
     * updates every cell's image with respect to its type
     */
    private void updateImages() {
        for(int i = 0 ; i < _height ; ++i){
            for(int j = 0 ; j < _width ; ++j)
                gameCells[i][j].updateImage();
        }
    }

    /**
     * changes player labels with respect to turn
     */
    private void changeLabels(){
        switch (_turn){
            case PLAYER1:{
                imagePlayer1.setBackgroundResource(R.drawable.p1withback);
                imagePlayer2.setBackgroundResource(R.drawable.p2space);
                break;
            }
            case PLAYER2:
            case CPU:{
                imagePlayer1.setBackgroundResource(R.drawable.p1space);
                imagePlayer2.setBackgroundResource(R.drawable.p2withback);
                break;
            }
        }
    }

    /**
     * @param type sets the given turn
     */
    private void setTurn(CellType type){
        _turn = type;
    }

    //Cell class which is also an ImageButton
    private class Cell extends android.support.v7.widget.AppCompatImageButton{
        private int _row,_col;
        private CellType _type;
        private boolean _isFinal;

        /**
         * @param context context of the activity
         * @param row row of the cell
         * @param col column of the cell
         */
        public Cell(Context context,int row,int col){
            this(context,row,col,CellType.SPACE);
        }
        public Cell(Context context,int row,int col,CellType type){
            super(context);
            _row = row;_col = col;
            _type = type;
            _isFinal = false;
            updateImage();
        }

        /**
         * @param isFinal stating whether this cell is one of the fading cells which will be animated
         */
        public void setIsFinal(boolean isFinal){
            _isFinal = isFinal;
        }

        /**
         * @return true if the cell is final
         */
        public boolean isFinal(){
            return _isFinal;
        }

        /**
         * @return row index
         */
        public int getRow(){
            return _row;
        }

        /**
         * @return column index
         */
        public int getCol(){
            return _col;
        }

        /**
         * @param type cell type to be set
         */
        public void setType(CellType type){
            _type = type;
        }

        /**
         * @return type of the cell
         */
        public CellType getType(){
            return _type;
        }
        //update the image of the cell with respect to CellType
        public void updateImage(){
            switch (this._type){
                case PLAYER1: {
                    setBackgroundResource(R.drawable.p1t);
                    break;
                }
                case PLAYER2:
                case CPU:{
                    setBackgroundResource(R.drawable.p2t);
                    break;
                }
                case SPACE:{
                    setBackgroundResource(R.drawable.spacet);
                    break;
                }
            }
        }
    }
}
