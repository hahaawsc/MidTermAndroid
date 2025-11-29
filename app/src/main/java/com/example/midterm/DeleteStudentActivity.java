package com.example.midterm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class DeleteStudentActivity extends AppCompatActivity {

    private EditText edtSearchId;
    private Button btnSearch, btnBackHome;
    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> studentList = new ArrayList<>();
    private StudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_student);

        edtSearchId = findViewById(R.id.edtSearchId);
        btnSearch = findViewById(R.id.btnSearchStudent);
        btnBackHome = findViewById(R.id.btnBackHome);
        recyclerView = findViewById(R.id.recyclerDeleteStudents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        adapter = new StudentAdapter(studentList);
        recyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> searchStudent());
        btnBackHome.setOnClickListener(v -> goHome());
    }

    private void searchStudent() {
        String studentId = edtSearchId.getText().toString().trim();
        if (studentId.isEmpty()) {
            Toast.makeText(this, "Please enter Student ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("students")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(this::showStudents)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showStudents(QuerySnapshot snapshots) {
        studentList.clear();
        studentList.addAll(snapshots.getDocuments());
        adapter.notifyDataSetChanged();
        if (studentList.isEmpty()) {
            Toast.makeText(this, "No student found!", Toast.LENGTH_SHORT).show();
        }
    }

    private class StudentAdapter extends RecyclerView.Adapter<StudentViewHolder> {
        private List<DocumentSnapshot> list;

        StudentAdapter(List<DocumentSnapshot> list) {
            this.list = list;
        }

        @Override
        public StudentViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new StudentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(StudentViewHolder holder, int position) {
            DocumentSnapshot doc = list.get(position);
            holder.text1.setText(doc.getString("name"));
            holder.text2.setText("ID: " + doc.getString("studentId") + " | Class: " + doc.getString("class"));

            holder.itemView.setOnClickListener(v -> confirmDelete(doc.getId(), doc.getString("name")));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private static class StudentViewHolder extends RecyclerView.ViewHolder {
        android.widget.TextView text1, text2;
        public StudentViewHolder(android.view.View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }

    private void confirmDelete(String docId, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Student")
                .setMessage("Are you sure you want to delete " + name + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.collection("students").document(docId).delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeDashBoard.class);
        startActivity(intent);
        finish();
    }
}
