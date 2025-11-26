package com.example.smartair;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class ClickForHelpFeature extends AppCompatActivity {
    /* Video id extracted from Youtube URL
       https://www.youtube.com/watch?v=Br9irulpbsc&t=77s
    */
    private final String videoId = "Br9irulpbsc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_click_for_help_feature);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI elements
        ImageButton imageButtonBackClickForHelp = findViewById(R.id.imageButtonBackClickForHelp);
        YouTubePlayerView youtubePlayerView = findViewById(R.id.youtube_player_view);

        // Add youtubePlayerView as a lifecycle observer of its parent activity
        getLifecycle().addObserver(youtubePlayerView);

        // Set the video to play
        youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(videoId, 0);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state){
                super.onStateChange(youTubePlayer, state);
            }
        });

        // Set click listener for the back button
        imageButtonBackClickForHelp.setOnClickListener(v -> finish());
    }
}