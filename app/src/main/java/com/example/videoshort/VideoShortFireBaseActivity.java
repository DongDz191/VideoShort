package com.example.videoshort;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.firebase.ui.database.FirebaseRecyclerOptions;

public class VideoShortFireBaseActivity extends AppCompatActivity {
    private ViewPager2 viewPager2;
    private VideosFireBaseAdapter videosAdapter;
    private FirebaseAuth mAuth;
    private ImageView uploadFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Hide title bar and set full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.main_activity); // Use correct layout
        viewPager2 = findViewById(R.id.vpager); // ID of ViewPager2
        uploadFab = findViewById(R.id.uploadFab); // ID of ImageView

        // Set click listener for upload button
        uploadFab.setOnClickListener(v -> {
            Intent intent = new Intent(VideoShortFireBaseActivity.this, UploadVideoActivity.class);
            startActivity(intent);
        });

        // Get videos from Firebase
        getVideos();
    }

    private void getVideos() {
        DatabaseReference mDataBase = FirebaseDatabase.getInstance().getReference("videos");
        FirebaseRecyclerOptions<Video1Model> options = new FirebaseRecyclerOptions.Builder<Video1Model>()
                .setQuery(mDataBase, Video1Model.class).build();

        videosAdapter = new VideosFireBaseAdapter(options, this);
        viewPager2.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        viewPager2.setAdapter(videosAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (videosAdapter != null) {
            videosAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videosAdapter != null) {
            videosAdapter.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videosAdapter != null) {
            videosAdapter.notifyDataSetChanged();
        }
    }
}