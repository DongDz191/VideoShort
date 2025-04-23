package com.example.videoshort;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

public class UploadVideoActivity extends AppCompatActivity {
    private EditText titleEditText, descEditText;
    private Button selectVideoButton, uploadVideoButton;
    private ProgressBar uploadProgressBar;
    private TextView videoUrlTextView;
    private Uri videoUri;

    private FirebaseAuth mAuth;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private DatabaseReference userRef;

    private static final int STORAGE_PERMISSION_CODE = 100;

    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                try {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        videoUri = result.getData().getData();
                        if (videoUri != null) {
                            Log.d("VideoPicker", "Selected video URI: " + videoUri);
                            Toast.makeText(this, "Video selected: " + videoUri, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("VideoPicker", "Video URI is null");
                            Toast.makeText(this, "Failed to get video URI", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("VideoPicker", "Failed to select video: Result code = " + result.getResultCode());
                        Toast.makeText(this, "Failed to select video", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("VideoPicker", "Error in video picker callback: " + e.getMessage(), e);
                    Toast.makeText(this, "Error selecting video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);

        mAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference("videos");
        userRef = FirebaseDatabase.getInstance().getReference("users");

        titleEditText = findViewById(R.id.titleEditText);
        descEditText = findViewById(R.id.descEditText);
        selectVideoButton = findViewById(R.id.selectVideoButton);
        uploadVideoButton = findViewById(R.id.uploadVideoButton);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        videoUrlTextView = findViewById(R.id.videoUrlTextView);

        selectVideoButton.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                pickVideo();
            } else {
                requestStoragePermission();
            }
        });

        uploadVideoButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String description = descEditText.getText().toString().trim();
            String userId = mAuth.getCurrentUser().getUid();

            if (TextUtils.isEmpty(title)) {
                titleEditText.setError("Title is required");
                return;
            }

            if (TextUtils.isEmpty(description)) {
                descEditText.setError("Description is required");
                return;
            }

            if (videoUri == null) {
                Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadVideo(title, description, userId);
        });

        videoUrlTextView.setOnClickListener(v -> {
            String url = videoUrlTextView.getText().toString().replace("Video URL: ", "");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        });
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickVideo();
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show();
        }
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        videoPickerLauncher.launch(intent);
    }

    private void uploadVideo(String title, String description, String userId) {
        uploadProgressBar.setVisibility(View.VISIBLE);
        uploadVideoButton.setEnabled(false);

        String videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
        StorageReference videoRef = storageReference.child("videos/" + userId + "/" + videoFileName);

        videoRef.putFile(videoUri)
                .addOnSuccessListener(taskSnapshot -> videoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    userRef.child(userId).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            UserModel user = task.getResult().getValue(UserModel.class);
                            String videoId = databaseReference.push().getKey();

                            Video1Model video = new Video1Model();
                            video.setTitle(title);
                            video.setDesc(description);
                            video.setUrl(uri.toString());
                            video.setUserId(userId);
                            video.setUsername(user.getUsername());

                            databaseReference.child(videoId).setValue(video)
                                    .addOnSuccessListener(aVoid -> {
                                        uploadProgressBar.setVisibility(View.GONE);
                                        uploadVideoButton.setEnabled(true);
                                        Toast.makeText(this, "Video uploaded successfully", Toast.LENGTH_SHORT).show();
                                        videoUrlTextView.setVisibility(View.VISIBLE);
                                        videoUrlTextView.setText("Video URL: " + uri.toString());
                                    })
                                    .addOnFailureListener(e -> {
                                        uploadProgressBar.setVisibility(View.GONE);
                                        uploadVideoButton.setEnabled(true);
                                        Toast.makeText(this, "Failed to save video metadata", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            uploadProgressBar.setVisibility(View.GONE);
                            uploadVideoButton.setEnabled(true);
                            Toast.makeText(this, "Failed to retrieve user info", Toast.LENGTH_SHORT).show();
                        }
                    });
                }))
                .addOnFailureListener(e -> {
                    uploadProgressBar.setVisibility(View.GONE);
                    uploadVideoButton.setEnabled(true);
                    Toast.makeText(this, "Video upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    uploadProgressBar.setProgress((int) progress);
                });
    }
}
