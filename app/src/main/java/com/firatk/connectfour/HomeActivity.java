package com.firatk.connectfour;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class HomeActivity extends AppCompatActivity {
    public static final String EXTRA_SIZE="com.firatk.connectfour.main.EXTRA_SIZE";
    public static final String EXTRA_TYPE="com.firatk.connectfour.main.EXTRA_TYPE";
    public static final String EXTRA_TIME="com.firatk.connectfour.main.EXTRA_TIME";

    private ImageButton playButton;
    private SeekBar sizeSeekBar;
    private TextView sizeTextView;
    private RadioButton multiRadio,singleRadio;

    private ImageButton timeButton;
    private SeekBar timeSeekBar;
    private ViewGroup transtionGroup;
    private boolean timeBarVisible;
    private TextView timeTextView;
    int _time;

    private ImageButton helpButton;
    private Dialog helpDialog;

    private int _size = 5;
    private int _type = 0;//default type pvc
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        playButton = (ImageButton) findViewById(R.id.playImageButton);
        ButtonListener buttonListener = new ButtonListener();
        playButton.setOnTouchListener(buttonListener);

        /*time button and bar*/
        transtionGroup = (ViewGroup) findViewById(R.id.transitionLayout);
        timeButton = (ImageButton) findViewById(R.id.timeView);
        timeButton.setBackgroundResource(R.drawable.ic_time_inactive);
        timeSeekBar = (SeekBar) findViewById(R.id.timeSeekBar);
        timeBarVisible = false;
        _time = 3;
        timeTextView = (TextView) findViewById(R.id.timeTextView);
        timeTextView.setText(_time + "s");
        timeButton.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                TransitionManager.beginDelayedTransition(transtionGroup);
                timeBarVisible = !timeBarVisible;
                timeButton.setBackgroundResource(timeBarVisible ? R.drawable.ic_time : R.drawable.ic_time_inactive);
                timeSeekBar.setVisibility(timeBarVisible ? timeSeekBar.VISIBLE : timeSeekBar.GONE);
                timeTextView.setVisibility(timeBarVisible ? timeSeekBar.VISIBLE : timeSeekBar.GONE);
            }
        });

        /*size bar*/
        sizeSeekBar = (SeekBar) findViewById(R.id.sizeSeekBar);
        sizeTextView = (TextView) findViewById(R.id.seekBarTextView);
        sizeTextView.setText(_size + " X " + _size);
        BarListener seekListener = new BarListener();
        sizeSeekBar.setOnSeekBarChangeListener(seekListener);
        timeSeekBar.setOnSeekBarChangeListener(seekListener);

        /*pvp pvc radio buttons*/
        RadioListener radioListener = new RadioListener();
        singleRadio = (RadioButton) findViewById(R.id.singlePlayerButton);
        multiRadio = (RadioButton) findViewById(R.id.multiPlayerButton);
        singleRadio.setChecked(true);
        singleRadio.setOnClickListener(radioListener);
        multiRadio.setOnClickListener(radioListener);

        /*help button*/
        helpButton = (ImageButton) findViewById(R.id.helpButton);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helpDialog = new Dialog(HomeActivity.this);
                helpDialog.setContentView(R.layout.help);
                helpDialog.setCanceledOnTouchOutside(true);
                helpDialog.setCancelable(true);
                helpDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                if(!HomeActivity.this.isFinishing())
                    helpDialog.show();
            }
        });
    }
    @Override
    public void onRestart() {
        super.onRestart();
        playButton.setBackgroundResource(R.drawable.play_arrow);
        if(!timeBarVisible)
            _time = 0;
    }
    private class RadioListener implements  View.OnClickListener{
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onClick(View view) {
            if(view.getId() == singleRadio.getId()) {
                _type = 0;//pvc
            }
            else if(view.getId() == multiRadio.getId()) {
                _type = 1;//pvp

            }
        }
    }
    private class ButtonListener implements View.OnTouchListener{
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN: {
                    MediaPlayer mp = MediaPlayer.create(HomeActivity.this,R.raw.start);
                    mp.start();
                    view.setBackgroundResource(R.drawable.pressed_play_arrow);
                    Intent intent = new Intent(HomeActivity.this,ConnectFourActivity.class);
                    intent.putExtra(EXTRA_SIZE,_size);
                    intent.putExtra(EXTRA_TYPE,_type);
                    if(!timeBarVisible)
                        _time = 0;
                    intent.putExtra(EXTRA_TIME,_time);
                    startActivity(intent);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    view.setBackgroundResource(R.drawable.play_arrow);
                    break;
                }
            }
            return true;
        }
    }
    private class BarListener implements  SeekBar.OnSeekBarChangeListener{

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if(seekBar.getId() == sizeSeekBar.getId()) {
                double prog = (double) seekBar.getProgress() / 2.5;
                _size = (int) Math.ceil(prog);
                if (seekBar.getProgress() < 13)
                    _size = 5;
                sizeTextView.setText(_size + " X " + _size);
            }else if(seekBar.getId() == timeSeekBar.getId()){
                double prog = (double) seekBar.getProgress();
                if(prog < 34)
                    _time = 3;
                else if(prog < 67)
                    _time = 5;
                else
                    _time = 7;
                timeTextView.setText(_time + "s");
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}
