package com.example.videoplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaExtractor;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.firebase.firestore.EventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.SimpleTimeZone;


public class VideoPlayerActivity extends AppCompatActivity implements View.OnClickListener{

    PlayerView playerView;
    SimpleExoPlayer player;
    ArrayList<MediaFiles> mVideoFiles = new ArrayList<>();
    ConcatenatingMediaSource concatenatingMediaSource;

    private ControlsMode controlsMode;
    public enum ControlsMode {
        LOCK, FULLSCREEN
    }

    int position;
    String videoTitle;
    TextView title;
    ImageView nextButton, previousButton, videoBack, lock, unlock, scaling, rotate, darkMode, mute;
    RelativeLayout root;
    LinearLayout left, right;
    View nightMode;
    boolean isDark = false;
    boolean isMute = false;

    //Swipe and zoom Variables
    private int deviceHeight, deviceWidth, brightness, mediaVolume;
    boolean isStart = false;
    boolean isLeft, isRight;
    boolean swipeMove = false;
    boolean isSuccess = true;
    private float baseX, baseY;
    private long diffX, diffY;
    public static final int MINIMUM_DISTANCE = 100;
    TextView volText, brtText;
    ProgressBar volProgress, brtProgress;
    LinearLayout volProgressContainer, volTextContainer, brtProgressContainer, brtTextContainer;
    ImageView volIcon, brtIcon;
    AudioManager audioManager;
    private ContentResolver contentResolver;
    private Window window;
    boolean singleTap = false;
    //Swipe and zoom Variables

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_video_player);
        playerView = findViewById(R.id.exoplayer_view);

        //        getSupportActionBar().hide();

        position = getIntent().getIntExtra("position", 1);
        videoTitle = getIntent().getStringExtra("videoTitle");
        mVideoFiles = getIntent().getExtras().getParcelableArrayList("videoArrayList");

        initViews();
        playVideo();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        deviceWidth = displayMetrics.widthPixels;
        deviceHeight = displayMetrics.heightPixels;
        playerView.setOnTouchListener(new OnSwipeTouchListener(this){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
//                        playerView.showController();
                        is Start = true;
                        if (motionEvent.getX() < (deviceWidth / 2)) {
                            isLeft = true;
                            isRight = false;
                        } else  if (motionEvent.getX() > (deviceWidth / 2)) {
                            isLeft = false;
                            isRight = true;
                        }

                        baseX = motionEvent.getX();
                        baseY = motionEvent.getY();
                        break;

                        case MotionEvent.ACTION_MOVE:
                            swipeMove = true;
                            diffX = (long) Math.ceil(motionEvent.getX() - baseX);
                            diffY = (long) Math.ceil(motionEvent.getY() - baseY);
                            double brightnessSpeed = 0.01;
                            if (Math.abs(diffY) > MINIMUM_DISTANCE) {
                                isStart = true;
                                if (Math.abs(diffY) > Math.abs(diffX)) {
                                    boolean value;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        value = Settings.System.canWrite(getApplicationContext());
                                        if (value) {
                                            if (isLeft) {
                                                contentResolver = getContentResolver();
                                                window = getWindow();
                                                try {
                                                    Settings.System.putInt(contentResolver,
                                                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                                                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

                                                        brightness = Settings.System.getInt(contentResolver,
                                                                Settings.System.SCREEN_BRIGHTNESS);
                                                    } catch (Settings.SettingNotFoundException e) {
                                                        throw new RuntimeException(e);
                                                    }

                                                int newBrightness = (int) (brightness - (diffY *  brightnessSpeed));
                                                if (newBrightness > 250) {
                                                    newBrightness = 250;
                                                } else if (newBrightness < 1) {
                                                    newBrightness = 1;
                                                }
                                                double brtPercentage = Math.ceil((((double) newBrightness / (double) 250)  * (double) 100));
                                                brtProgressContainer.setVisibility(View.VISIBLE);
                                                brtTextContainer.setVisibility(View.VISIBLE);
                                                brtProgress.setProgress((int) brtPercentage);

                                                if (brtPercentage < 30) {
                                                    brtIcon.setImageResource(R.drawable.ic_brightness_5);
                                                } else if (brtPercentage > 30 && brtPercentage < 80) {
                                                    brtIcon.setImageResource(R.drawable.ic_brightness_medium);
                                                } else if (brtPercentage > 80) {
                                                    brtIcon.setImageResource(R.drawable.ic_brightness);
                                                }

                                                brtText.setText(" " + (int) brtPercentage + "%");
                                                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS,
                                                        (newBrightness));
                                                WindowManager.LayoutParams layoutParams = window.getAttributes();
                                                layoutParams.screenBrightness = brightness / (float) 255;
                                                window.setAttributes(layoutParams);

//                                                Toast.makeText(getApplicationContext(), "Left Swipe", Toast.LENGTH_SHORT).show();
                                            } else  if (isRight) {
                                                volTextContainer.setVisibility(View.VISIBLE);
                                                mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                                int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                                double cal = (double) diffY * ((double) maxVol / ((double) (deviceHeight * 2) - brightnessSpeed));
                                                int newMediaVolume = mediaVolume - (int) cal;
                                                if (newMediaVolume > maxVol) {
                                                    newMediaVolume = maxVol;
                                                } else if (newMediaVolume < 1) {
                                                    newMediaVolume = 0;
                                                }

                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                                        newMediaVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                                                double volPer = Math.ceil((((double) newMediaVolume / (double) maxVol) * (double) 100));
                                                volText.setText(" " + (int) volPer + "%");
                                                if (volPer < 1) {
                                                     volIcon.setImageResource(R.drawable.ic_volume_off);
                                                     volText.setVisibility(View.VISIBLE);
                                                     volText.setText(" Off");
                                                } else if (volPer >= 1) {
                                                    volIcon.setImageResource(R.drawable.ic_volume_up);
                                                    volText.setVisibility(View.VISIBLE);
                                                }

                                                volProgressContainer.setVisibility(View.VISIBLE);
                                                volProgress.setProgress((int) volPer);

//                                                Toast.makeText(getApplicationContext(), "Right Swipe", Toast.LENGTH_SHORT).show();
                                            }
                                            isSuccess = true;
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Allow Write Settings for Swipe Controls", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                            intent.setData(Uri.parse("package:" + getPackageName()));
                                            startActivityForResult(intent, 111);
                                        }
                                    }
                                }
                            }
                            break;

                    case MotionEvent.ACTION_UP:
                        swipeMove = false;
                        isStart = false;
                        volProgressContainer.setVisibility(View.GONE);
                        brtProgressContainer.setVisibility(View.GONE);
                        volTextContainer.setVisibility(View.GONE);
                        brtTextContainer.setVisibility(View.GONE);
                        break;
                }
                return super.onTouch(view, motionEvent);
            }

            @Override
            public void onDoubleTouch() {
                super.onDoubleTouch();
            }

            @Override
            public void onSingleTouch() {
                super.onSingleTouch();
                if (singleTap) {
//                    playerView.showController();
                    singleTap = false;
                } else {
//                    playerView.hideController();
                    singleTap = true;
                }
            }
        });

    }

    private void initViews() {
        title = findViewById(R.id.video_title);
        nextButton = findViewById(R.id.exo_next);
        previousButton = findViewById(R.id.exo_prev);
        videoBack = findViewById(R.id.video_back);
        lock = findViewById(R.id.lock);
        unlock = findViewById(R.id.unlock);
        scaling = findViewById(R.id.scaling);
        rotate = findViewById(R.id.rotate);
        darkMode = findViewById(R.id.dark_mode);
        mute = findViewById(R.id.mute);
        nightMode = findViewById(R.id.night_mode);
        root = findViewById(R.id.root_layout);
        right = findViewById(R.id.right_icons);
        left = findViewById(R.id.left_icons);
        volText = findViewById(R.id.vol_text);
        brtText = findViewById(R.id.brt_text);
        volProgress = findViewById(R.id.vol_progress);
        brtProgress = findViewById(R.id.brt_progress);
        volProgressContainer = findViewById(R.id.vol_progress_container);
        brtProgressContainer = findViewById(R.id.brt_progress_container);
        volTextContainer = findViewById(R.id.vol_text_container);
        brtTextContainer = findViewById(R.id.brt_text_container);
        volIcon = findViewById(R.id.vol_icon);
        brtIcon = findViewById(R.id.brt_icon);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        title.setText(videoTitle);
        nextButton.setOnClickListener(this);
        previousButton.setOnClickListener(this);
        videoBack.setOnClickListener(this);
        lock.setOnClickListener(this);
        unlock.setOnClickListener(this);
        rotate.setOnClickListener(this);
        mute.setOnClickListener(this);
        darkMode.setOnClickListener(this);
        scaling.setOnClickListener(firstListener);
    }

    private void playVideo() {

        String path = mVideoFiles.get(position).getPath();
        Uri uri = Uri.parse(path);


        player = new SimpleExoPlayer.Builder(this).build();
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "app"));
        concatenatingMediaSource = new ConcatenatingMediaSource();

        for (int i = 0; i < mVideoFiles.size(); i++){
            new File(String.valueOf(mVideoFiles.get(i)));
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(String.valueOf(uri))));

            concatenatingMediaSource.addMediaSource(mediaSource);
        }

        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        player.prepare(concatenatingMediaSource);
        player.seekTo(position, C.TIME_UNSET);
        playerError();

    }

    private void playerError() {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {

                Toast.makeText(VideoPlayerActivity.this, "Video Playing Error", Toast.LENGTH_SHORT).show();
//                Player.Listener.super.onPlayerError(error);
            }
        });

        player.setPlayWhenReady(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (player.isPlaying()){
            player.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.setPlayWhenReady(false);
        player.getPlaybackState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.setPlayWhenReady(true);
        player.getPlaybackState();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        player.setPlayWhenReady(true);
        player.getPlaybackState();
    }

    private void setFullScreen(){
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    @Override
    public void onClick(View view) {

        int id = view.getId();
       if (id == R.id.exo_next) {
           try {
               player.stop();
               position++;
               playVideo();
           } catch (Exception e) {
               Toast.makeText(this, "No Next Video", Toast.LENGTH_SHORT).show();
               finish();
           }

       } else if (id == R.id.exo_prev) {
           try {
               player.stop();
               position--;
               playVideo();
           } catch (Exception e) {
               Toast.makeText(this, "No Previous Video", Toast.LENGTH_SHORT).show();
               finish();
           }

       } else if (id == R.id.video_back) {

           if (player != null) {
               player.release();
           }
           finish();


       } else if (id == R.id.lock) {
           controlsMode = ControlsMode.FULLSCREEN;
           root.setVisibility(View.VISIBLE);
           left.setVisibility(View.VISIBLE);
           right.setVisibility(View.VISIBLE);
           lock.setVisibility(View.INVISIBLE);
           Toast.makeText(this, "unLocked", Toast.LENGTH_SHORT).show();

       } else if (id == R.id.unlock) {
           controlsMode = ControlsMode.LOCK;
           root.setVisibility(View.INVISIBLE);
           left.setVisibility(View.INVISIBLE);
           right.setVisibility(View.INVISIBLE);
           lock.setVisibility(View.VISIBLE);
//           lock.setImageResource(R.drawable.ic_lock);
           Toast.makeText(this, "Locked", Toast.LENGTH_SHORT).show();

       } else if (id == R.id.rotate) {
           if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
               setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//               rotate.notify(); // Doubt
           } else  if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
               setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//               rotate.notify(); // Doubt
           }
       } else if (id == R.id.mute) {
           if (isMute) {
               player.setVolume(1);
               mute.setImageResource(R.drawable.ic_volume_off);
               isMute = false;
           } else {
               player.setVolume(0);
               mute.setImageResource(R.drawable.ic_volume_up );
               isMute = true;
           }

       } else if (id == R.id.dark_mode) {
           if (isDark) {
               nightMode.setVisibility(View.GONE);
               darkMode.setImageResource(R.drawable.ic_night_stay);
               isDark = false;
           } else {
               nightMode.setVisibility(View.VISIBLE);
               darkMode.setImageResource(R.drawable.ic_light_mode);
               isDark = true;
           }

       } else if (id == R.id.mute) {

       }
    }


    View.OnClickListener firstListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.ic_fullscreen);
            Toast.makeText(VideoPlayerActivity.this, "Full Screen", Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(secondListener);
        }
    };

    View.OnClickListener secondListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.ic_zoom);
            Toast.makeText(VideoPlayerActivity.this, "Zoom", Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(thirdListener);
        }
    };

    View.OnClickListener thirdListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.ic_fit);
            Toast.makeText(VideoPlayerActivity.this, "Fit", Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(firstListener);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 111) {
            boolean value;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                value = Settings.System.canWrite(getApplicationContext());
                if (value) {
                    isSuccess = true;
                } else {
                    Toast.makeText(getApplicationContext(), "Not Granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}