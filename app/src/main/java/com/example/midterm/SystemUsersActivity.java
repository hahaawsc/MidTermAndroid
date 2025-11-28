package com.example.midterm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SystemUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> userList;
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_users);

        db = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerSystemUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter();
        recyclerView.setAdapter(adapter);

        loadUsers();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUsers() {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    userList.addAll(queryDocumentSnapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show());
    }

    // --- Inner Adapter Class ---
    class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_system_user, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            DocumentSnapshot doc = userList.get(position);

            String name = doc.getString("name");
            String role = doc.getString("role");
            String phone = doc.getString("phone");
            String photoUrl = doc.getString("photoUrl");

            holder.tvName.setText(name != null ? name : "Unknown Name");
            holder.tvRole.setText("Role: " + (role != null ? role : "N/A"));
            holder.tvPhone.setText(phone != null ? "Phone: " + phone : "");

            // Load Image using Picasso
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Picasso.get().load(photoUrl).placeholder(R.drawable.profile).into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.drawable.profile);
            }
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvPhone;
            ImageView imgProfile;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvUserName);
                tvRole = itemView.findViewById(R.id.tvUserRole);
                tvPhone = itemView.findViewById(R.id.tvUserPhone);
                imgProfile = itemView.findViewById(R.id.imgUserAvatar);
            }
        }
    }
}
