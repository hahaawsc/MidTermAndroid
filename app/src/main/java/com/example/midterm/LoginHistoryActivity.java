package com.example.midterm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LoginHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> historyList;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_history);

        db = FirebaseFirestore.getInstance();
        historyList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerLoginHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);

        loadHistory();

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadHistory() {
        // Assumes you have a collection "login_history"
        // Ordered by timestamp descending (newest first)
        db.collection("login_history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();
                    historyList.addAll(queryDocumentSnapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show());
    }

    // --- Inner Adapter Class ---
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_login_history, parent, false);
            return new HistoryViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            DocumentSnapshot doc = historyList.get(position);

            String email = doc.getString("email");
            String status = doc.getString("status"); // e.g., "Success"

            // Timestamp handling
            Long timestamp = doc.getLong("timestamp");
            String dateString = "N/A";
            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                dateString = sdf.format(new Date(timestamp));
            }

            holder.tvEmail.setText(email != null ? email : "Unknown User");
            holder.tvDate.setText(dateString);

            holder.tvStatus.setText(status);
            if ("Success".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(0xFF4CAF50); // Green
            } else {
                holder.tvStatus.setTextColor(0xFFF44336); // Red
            }
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmail, tvStatus, tvDate;

            public HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEmail = itemView.findViewById(R.id.tvHistoryEmail);
                tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
                tvDate = itemView.findViewById(R.id.tvHistoryDate);
            }
        }
    }
}
