package com.example.exodemo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Placeholder;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
//import com.krishworks.udeyo.R;
//import com.krishworks.udeyo.core.UDEOApp;
//import com.krishworks.udeyo.providers.Placeholder;
//import com.krishworks.udeyo.providers.medias.Medias;
//import com.krishworks.udeyo.providers.medias.MediasJson;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;


public class VideoPlayerActivity extends AppCompatActivity {

    String mediaPath;

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private PlaybackStateListener playbackStateListener;

    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    private String downloadUrl;
    private boolean mExoPlayerFullscreen=false;

    private Dialog mFullScreenDialog;
    private ImageView mFullScreenIcon;

    private boolean isYoutubeLink=true;
    private String youtubeLink="https://youtu.be/0gosur3db5I";

    private ListView listView;
    private ArrayList<String> youtubeLinks;
     private static int i;
     private ConcatenatingMediaSource concatenatingMediaSource;
     private ArrayList<String> youtubeThumbs,youtubeTitles;
     private RelativeLayout preLayout;

    public static String YT_THUMB_URL_PREFIX="http://img.youtube.com/vi/";
    public static String YT_THUMB_URL_SUFFIX="/default.jpg";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
       setTitle("Playing Video");

        playerView = findViewById(R.id.video_view);
        playbackStateListener = new PlaybackStateListener();
        listView=findViewById(R.id.list_view);

        initializePlayer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer();
        }
    }



    private void loadVideo(Intent videoIntent) {
       /*
        String mediaID = videoIntent.getStringExtra(getString(R.string.udeo_media_id_key));

        Medias.IndivMedias indivMedia = MediasJson.getInstance().getMediaDetailsForID(mediaID);
        if (indivMedia!=null) {

            if (indivMedia.getYoutube().length()>0) {
                isYoutubeLink = true;
                youtubeLink = indivMedia.getYoutube();
            } else {
                String storagePath = UDEOApp.getRootDownloadPath();
                storagePath += Placeholder.CLOUD_MEDIA_ROOT;
                storagePath += "/" + indivMedia.getFile();
                mediaPath = storagePath;
            }
        }

        */
    }

    private void getYoutubeLink(){
        youtubeLinks=new ArrayList<>();
        youtubeThumbs=new ArrayList<>();
        youtubeTitles=new ArrayList<>();
        youtubeLinks.add("https://youtu.be/0gosur3db5I");
        youtubeLinks.add("https://youtu.be/svdq1BWl4r8");
        youtubeLinks.add("https://youtu.be/6P20npkvcb8");
        youtubeLinks.add("https://youtu.be/5MgBikgcWnY");
        youtubeLinks.add("https://youtu.be/d0yGdNEWdn0");

        for(int l=0;l<youtubeLinks.size();l++){
            youtubeThumbs.add(getThumbnailUrlForYoutube(youtubeLinks.get(l)));
            //youtubeTitles.add(getTitleQuietly(youtubeLinks.get(l)));
        }

        ListAdapter listAdapter=new ListAdapter();
        listView.setAdapter(listAdapter);

    }

    public String getThumbnailUrlForYoutube(String youtubeUrl) {
        String videoID="";
        String thumbnailUrl="";

        String regularEx = "http(?:s)?:\\/\\/(?:m.)?(?:www\\.)?youtu(?:\\.be\\/|be\\.com\\/(?:watch\\?(?:feature=youtu.be\\&)?v=|v\\/|embed\\/|user\\/(?:[\\w#]+\\/)+))([^&#?\\n]+)";
        Pattern pattern = Pattern.compile(regularEx,Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(youtubeUrl);
        if(matcher.matches()) {
            videoID = matcher.group(1);
        }

        if(videoID.trim().length()>1) {
            thumbnailUrl = YT_THUMB_URL_PREFIX + videoID + YT_THUMB_URL_SUFFIX;
        }
        return thumbnailUrl;
    }

    private void generatePlaylist(){

        if(concatenatingMediaSource==null) {


            for (int j = i; j < youtubeLinks.size(); j++) {
                if (!youtubeLink.isEmpty()) {
                    // youtubeLink = "https://youtu.be/0gosur3db5I";


                    new YouTubeExtractor(this) {
                        @Override
                        public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                            if (ytFiles != null) {
                                //int itag = 22;
                                // 720, 1080, 480

                                for (int i = 0; i < ytFiles.size(); i++) {
                                    int itag = ytFiles.keyAt(i);
                                    YtFile ytFile = ytFiles.get(itag);

                                    // Just get videos with a decent format => height -1 = audio
                                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                                        downloadUrl = ytFile.getUrl();
                                        break;
                                    }
                                }
//                        List<Integer> iTags = Arrays.asList(22, 137, 18);
//                        for (Integer iTag : iTags) {
//
//                            YtFile ytFile = ytFiles.get(iTag);
//                            if (ytFile != null) {
//                                downloadUrl = ytFile.getUrl();
//                            }
//
//                        }
                                Uri uri = Uri.parse(downloadUrl);
                                MediaSource mediaSource = buildMediaSource(uri);
                                if (concatenatingMediaSource == null) {
                                    concatenatingMediaSource = new ConcatenatingMediaSource(mediaSource);

                                } else {
                                    concatenatingMediaSource.addMediaSource(mediaSource);
                                }

/*
                            player.setPlayWhenReady(playWhenReady);
                            player.seekTo(currentWindow, playbackPosition);
                            player.prepare(concatenatingMediaSource, false, false);
  */
                            } else {
                                Toast.makeText(getApplicationContext(), "Cannot play this video", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        }
                    }.extract(youtubeLinks.get(j), true, true);
                } else {
                    Uri uri = Uri.parse(mediaPath);
                    MediaSource mediaSource = buildMediaSource(uri);
                    player.setPlayWhenReady(playWhenReady);
                    player.seekTo(currentWindow, playbackPosition);
                    player.prepare(mediaSource, false, false);
                }
            }


        }
    }

    private void initializePlayer() {

        getYoutubeLink();
        generatePlaylist();

        player = new SimpleExoPlayer.Builder(getApplicationContext()).build();

        //setup listener
        player.addListener(playbackStateListener);

        //setup view
        playerView.setPlayer(player);




       // if (isYoutubeLink)
/*
        if(!youtubeLink.isEmpty()){
           // youtubeLink = "https://youtu.be/0gosur3db5I";

            new YouTubeExtractor(this) {
                @Override
                public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                    if (ytFiles != null) {
                        //int itag = 22;
                        // 720, 1080, 480

                        for(int i=0;i<ytFiles.size();i++) {
                            int itag = ytFiles.keyAt(i);
                            YtFile ytFile = ytFiles.get(itag);

                            // Just get videos with a decent format => height -1 = audio
                            if(ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight()>=360) {
                                downloadUrl = ytFile.getUrl();
                                break;
                            }
                        }
//                        List<Integer> iTags = Arrays.asList(22, 137, 18);
//                        for (Integer iTag : iTags) {
//
//                            YtFile ytFile = ytFiles.get(iTag);
//                            if (ytFile != null) {
//                                downloadUrl = ytFile.getUrl();
//                            }
//
//                        }
                        Uri uri = Uri.parse(downloadUrl);
                        MediaSource mediaSource = buildMediaSource(uri);
                        if(concatenatingMediaSource==null){
                            concatenatingMediaSource=new ConcatenatingMediaSource(mediaSource);

                        }else{
                            concatenatingMediaSource.addMediaSource(i,mediaSource);
                        }


                        player.setPlayWhenReady(playWhenReady);
                        player.seekTo(currentWindow, playbackPosition);
                        player.prepare(concatenatingMediaSource, false, false);
                    } else {
                        Toast.makeText(getApplicationContext(),"Cannot play this video",Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }.extract(youtubeLink, true, true);
        } else {
            Uri uri = Uri.parse(mediaPath);
            MediaSource mediaSource = buildMediaSource(uri);
            player.setPlayWhenReady(playWhenReady);
            player.seekTo(currentWindow, playbackPosition);
            player.prepare(mediaSource, false, false);
        }

         */


        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.seekToDefaultPosition(i);
        if(concatenatingMediaSource!=null)
            player.prepare(concatenatingMediaSource, false, false);

        initFullscreenButton();
        initFullscreenDialog();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {



            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                VideoPlayerActivity.i=i;
                view.setSelected(true);

                if(preLayout!=null) {

                    preLayout.setBackgroundColor(Color.WHITE);
                }

                 preLayout=view.findViewById(R.id.card_layout);
                preLayout.setBackgroundColor(Color.RED);


                releaseNewPlayer();
                initializePlayer();
            }
        });


    }

    private void releaseNewPlayer() {
        if(player!=null){
            player.removeListener(playbackStateListener);
            player.release();
            player = null;

        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory
                (this, Util.getUserAgent(getApplicationContext(),"Udeo"));
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.removeListener(playbackStateListener);
            player.release();
            player = null;

        }
    }

    private void initFullscreenDialog() {

        mFullScreenDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            public void onBackPressed() {
                if (mExoPlayerFullscreen)
                    closeFullscreenDialog();
                super.onBackPressed();
            }
        };
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void openFullscreenDialog() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mFullScreenIcon.setImageDrawable(getApplicationContext().getDrawable( R.drawable.exo_icon_fullscreen_enter));
        }


      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

      if(getSupportActionBar() != null){
            getSupportActionBar().show();
        }



        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      if(mFullScreenDialog.isShowing())
         mFullScreenDialog.dismiss();

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) playerView.getLayoutParams();
       params.width = params.MATCH_PARENT;
       params.height=675;
        playerView.setLayoutParams(params);


        mExoPlayerFullscreen = false;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void closeFullscreenDialog() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mFullScreenIcon.setImageDrawable(getApplicationContext().getDrawable(R.drawable.exo_icon_fullscreen_exit));
        }

       getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                       |  View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                       |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                       |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                       |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
               );

//mFullScreenDialog.dismiss();
        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) playerView.getLayoutParams();
        params.width = params.MATCH_PARENT;
        params.height = params.MATCH_PARENT;
        playerView.setLayoutParams(params);


        mExoPlayerFullscreen = true;
    }

    private void initFullscreenButton() {

        mFullScreenIcon = playerView.findViewById(R.id.exo_fullscreen_icon);
        mFullScreenIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mExoPlayerFullscreen){
                    openFullscreenDialog();}
                else
                    closeFullscreenDialog();
            }
        });
    }

    public void goBack(View view) {
        this.finish();}

        public String getTitleQuietly(String youtubeUrl) {
            try {
                if (youtubeUrl != null) {
                    URL embededURL = new URL("http://www.youtube.com/oembed?url=" +
                            youtubeUrl + "&format=json"
                    );

                    return new JSONObject(IOUtils.toString(embededURL)).getString("title");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


    // playback state
    private class PlaybackStateListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            String stateString;
            if(playbackState==PlaybackStateCompat.STATE_SKIPPING_TO_NEXT){
                if(i<youtubeLinks.size()){
                    i++;}
                Log.i("Value", "onPlayerStateChanged: i =" +i);
                youtubeLink=youtubeLinks.get(i);
                releaseNewPlayer();
                initializePlayer();
            }

            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;
                case ExoPlayer.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    break;
                case ExoPlayer.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";

                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            //Log.d("udio", "changed state to " + stateString + " playWhenReady: " + playWhenReady);

            // screen dim/lock toggle
            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED ||
                    !playWhenReady) {
                playerView.setKeepScreenOn(false);
            } else { // STATE_IDLE, STATE_ENDED
                // This prevents the screen from getting dim/lock
                playerView.setKeepScreenOn(true);
            }
        }
    }

    class ListAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return youtubeLinks.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v= LayoutInflater.from(getApplicationContext()).inflate(R.layout.thumbnail,null);
            TextView nameTv=v.findViewById(R.id.name_tv);
            ImageView thumbImg=v.findViewById(R.id.thumb_img);

            nameTv.setText(youtubeLinks.get(i));
            Glide.with(getApplicationContext()).load(youtubeThumbs.get(i)).into(thumbImg);

            v.setSelected(true);
            return v;
        }
    }


}

