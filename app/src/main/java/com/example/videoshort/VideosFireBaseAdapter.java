package com.example.videoshort;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class VideosFireBaseAdapter extends FirebaseRecyclerAdapter<Video1Model, VideosFireBaseAdapter.MyHolder> {
    private Context context;
    private String currentUserId; // Giả sử bạn có userId của người dùng hiện tại

    public VideosFireBaseAdapter(@NonNull FirebaseRecyclerOptions<Video1Model> options, Context context, String currentUserId) {
        super(options);
        this.context = context;
        this.currentUserId = currentUserId; // Lấy userId từ Firebase Auth hoặc nguồn khác
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_video_row, parent, false);
        return new MyHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull MyHolder holder, int position, @NonNull Video1Model model) {
        holder.textVideoTitle.setText(model.getTitle());
        holder.textVideoDescription.setText(model.getDesc());
        holder.textUploaderName.setText(model.getUsername());
        holder.textLikes.setText(String.valueOf(model.getLikes()));
        holder.textDislikes.setText(String.valueOf(model.getDislikes()));
        holder.videoView.setVideoPath(model.getUrl());

        holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                holder.videoProgressBar.setVisibility(View.GONE);
                mp.start();

                float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
                float screenRatio = holder.videoView.getWidth() / (float) holder.videoView.getHeight();
                float scale = videoRatio / screenRatio;

                if (scale >= 1f) {
                    holder.videoView.setScaleX(scale);
                } else {
                    holder.videoView.setScaleY(1f / scale);
                }
            }
        });

        holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });

        // Kiểm tra trạng thái ban đầu của người dùng với video này
        DatabaseReference videoRef = getRef(position);
        videoRef.child("user_likes").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                holder.isFav = snapshot.exists() && snapshot.getValue(Boolean.class) == true;
                holder.favorites.setImageResource(holder.isFav ? R.drawable.ic_fill_favorites : R.drawable.ic_favorites);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        videoRef.child("user_dislikes").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                holder.isUnfav = snapshot.exists() && snapshot.getValue(Boolean.class) == true;
                holder.unfavorites.setImageResource(holder.isUnfav ? R.drawable.ic_fill_unfavorite : R.drawable.ic_unfavorite);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Xử lý sự kiện nhấn favorites
        holder.favorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;

                DatabaseReference videoRef = getRef(adapterPosition);
                Video1Model currentModel = getItem(adapterPosition);

                if (!holder.isFav) {
                    // Tăng lượt thích và lưu trạng thái người dùng
                    int newLikes = currentModel.getLikes() + 1;
                    videoRef.child("likes").setValue(newLikes);
                    videoRef.child("user_likes").child(currentUserId).setValue(true);
                    holder.textLikes.setText(String.valueOf(newLikes));
                    holder.favorites.setImageResource(R.drawable.ic_fill_favorites);
                    holder.isFav = true;

                    // Nếu đang ở trạng thái không thích, hủy không thích
                    if (holder.isUnfav) {
                        int newDislikes = currentModel.getDislikes() - 1;
                        if (newDislikes >= 0) {
                            videoRef.child("dislikes").setValue(newDislikes);
                            videoRef.child("user_dislikes").child(currentUserId).setValue(false);
                            holder.textDislikes.setText(String.valueOf(newDislikes));
                        }
                        holder.unfavorites.setImageResource(R.drawable.ic_unfavorite);
                        holder.isUnfav = false;
                    }
                } else {
                    // Hủy lượt thích
                    int newLikes = currentModel.getLikes() - 1;
                    if (newLikes >= 0) {
                        videoRef.child("likes").setValue(newLikes);
                        videoRef.child("user_likes").child(currentUserId).setValue(false);
                        holder.textLikes.setText(String.valueOf(newLikes));
                    }
                    holder.favorites.setImageResource(R.drawable.ic_favorites);
                    holder.isFav = false;
                }
            }
        });

        // Xử lý sự kiện nhấn unfavorites
        holder.unfavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;

                DatabaseReference videoRef = getRef(adapterPosition);
                Video1Model currentModel = getItem(adapterPosition);

                if (!holder.isUnfav) {
                    // Tăng lượt không thích và lưu trạng thái người dùng
                    int newDislikes = currentModel.getDislikes() + 1;
                    videoRef.child("dislikes").setValue(newDislikes);
                    videoRef.child("user_dislikes").child(currentUserId).setValue(true);
                    holder.textDislikes.setText(String.valueOf(newDislikes));
                    holder.unfavorites.setImageResource(R.drawable.ic_fill_unfavorite);
                    holder.isUnfav = true;

                    // Nếu đang ở trạng thái thích, hủy thích
                    if (holder.isFav) {
                        int newLikes = currentModel.getLikes() - 1;
                        if (newLikes >= 0) {
                            videoRef.child("likes").setValue(newLikes);
                            videoRef.child("user_likes").child(currentUserId).setValue(false);
                            holder.textLikes.setText(String.valueOf(newLikes));
                        }
                        holder.favorites.setImageResource(R.drawable.ic_favorites);
                        holder.isFav = false;
                    }
                } else {
                    // Hủy lượt không thích
                    int newDislikes = currentModel.getDislikes() - 1;
                    if (newDislikes >= 0) {
                        videoRef.child("dislikes").setValue(newDislikes);
                        videoRef.child("user_dislikes").child(currentUserId).setValue(false);
                        holder.textDislikes.setText(String.valueOf(newDislikes));
                    }
                    holder.unfavorites.setImageResource(R.drawable.ic_unfavorite);
                    holder.isUnfav = false;
                }
            }
        });
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        private VideoView videoView;
        private ProgressBar videoProgressBar;
        private TextView textVideoTitle;
        private TextView textVideoDescription;
        private TextView textUploaderName;
        private TextView textLikes;
        private TextView textDislikes;
        private ImageView imPerson, favorites, unfavorites, imShare, imMore;
        private boolean isFav;
        private boolean isUnfav;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            videoProgressBar = itemView.findViewById(R.id.videoProgressBar);
            textVideoTitle = itemView.findViewById(R.id.textVideoTitle);
            textVideoDescription = itemView.findViewById(R.id.textVideoDescription);
            textUploaderName = itemView.findViewById(R.id.textUploaderName);
            textLikes = itemView.findViewById(R.id.textLikes);
            textDislikes = itemView.findViewById(R.id.textDislikes);
            imPerson = itemView.findViewById(R.id.imPerson);
            favorites = itemView.findViewById(R.id.favorites);
            unfavorites = itemView.findViewById(R.id.unfavorites);
            imShare = itemView.findViewById(R.id.imShare);
            imMore = itemView.findViewById(R.id.imMore);
        }
    }
}