package com.truiton.rtmpplayer;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements IVLCVout.Callback {
    public final static String TAG = "MainActivity";
    private String mFilePath;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFilePath = "rtmp://rrbalancer.broadcast.tneg.de:1935/pw/ruk/ruk";

        Log.d(TAG, "Playing: " + mFilePath);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onResume() {
        super.onResume();
        createPlayer(mFilePath);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }


    /**
     * Used to set size for SurfaceView
     *
     * @param width
     * @param height
     */
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if (holder == null || mSurface == null)
            return;

        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        holder.setFixedSize(mVideoWidth, mVideoHeight);
        LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /**
     * Creates MediaPlayer and plays video
     *
     * @param media
     */
    private void createPlayer(String media) {
        releasePlayer();
        try {
            if (media.length() > 0) {
                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();
            }

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(this, options);
            holder.setKeepScreenOn(true);

            // Creating media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Seting up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, Uri.parse(media));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    /**
     * Registering callbacks
     */
    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }
}
