package com.example.midterm;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.RecyclerView;

public class HomeDashBoard extends AppCompatActivity {

    // Added cardUpdateStudent
    private CardView cardAddStudent, cardDeleteStudent, cardUpdateStudent, cardViewStudents, cardDataExchange, cardStudentDetails;
    private ImageButton btnProfile;
    private TextView txtHeader;

    private FirebaseFirestore db;
    private String role;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_dash_board);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        role = intent.getStringExtra("role");

        cardAddStudent = findViewById(R.id.cardAddStudent);
        cardDeleteStudent = findViewById(R.id.cardDeleteStudent);
        cardUpdateStudent = findViewById(R.id.cardUpdateStudent);
        cardViewStudents = findViewById(R.id.cardViewStudents);
        cardDataExchange = findViewById(R.id.cardDataExchange);
        cardStudentDetails = findViewById(R.id.cardStudentDetails);

        btnProfile = findViewById(R.id.imageButton2);
        txtHeader = findViewById(R.id.textView2);

        setupRolePermissions();

        btnProfile.setOnClickListener(v -> {
            Intent profileIntent = new Intent(HomeDashBoard.this, UserProfile.class);
            profileIntent.putExtra("userId", userId);
            profileIntent.putExtra("role", role);
            startActivity(profileIntent);
        });

        cardViewStudents.setOnClickListener(v -> {
            Intent intent2 = new Intent(HomeDashBoard.this, StudentListActivity.class);
            intent2.putExtra("userId", userId);
            intent2.putExtra("role", role);
            startActivity(intent2);
        });

        cardDataExchange.setOnClickListener(v -> {
            Intent intent3 = new Intent(HomeDashBoard.this, DataExchangeActivity.class);
            intent3.putExtra("role", role);
            startActivity(intent3);
        });

        cardAddStudent.setOnClickListener(v -> showAddStudentDialog());
        cardStudentDetails.setOnClickListener(v -> showSearchForDetailsDialog());

        cardDeleteStudent.setOnClickListener(v -> {
            Intent intent4 = new Intent(HomeDashBoard.this, DeleteStudentActivity.class);
            startActivity(intent4);
        });

        cardUpdateStudent.setOnClickListener(v -> {
            Intent intent4 = new Intent(HomeDashBoard.this, UpdateStudentActivity.class);
            startActivity(intent4);
        });

    }

    private void setupRolePermissions() {
        if (role == null) return;

        txtHeader.setText("Welcome, " + role);

        cardViewStudents.setVisibility(View.VISIBLE);

        if (role.equals("employee")) {
            // Employees cannot Add, Delete, or Update students
            cardAddStudent.setVisibility(View.GONE);
            cardDeleteStudent.setVisibility(View.GONE);
            cardUpdateStudent.setVisibility(View.GONE);
            cardDataExchange.setVisibility(View.GONE);
        } else {
            // Managers and Admins see all
            cardAddStudent.setVisibility(View.VISIBLE);
            cardDeleteStudent.setVisibility(View.VISIBLE);
            cardUpdateStudent.setVisibility(View.VISIBLE);
            cardDataExchange.setVisibility(View.VISIBLE);
            cardStudentDetails.setVisibility(View.VISIBLE);
        }

        cardViewStudents.setVisibility(View.VISIBLE);
    }


    private void showAddStudentDialog() {
        // (Your existing add logic code here)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Student");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Student Name");
        layout.addView(inputName);

        final EditText inputId = new EditText(this);
        inputId.setHint("Student ID");
        layout.addView(inputId);

        final EditText inputClass = new EditText(this);
        inputClass.setHint("Class/Grade");
        layout.addView(inputClass);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String sid = inputId.getText().toString().trim();
            String sClass = inputClass.getText().toString().trim();

            if (name.isEmpty() || sid.isEmpty()) {
                Toast.makeText(HomeDashBoard.this, "Name and ID are required", Toast.LENGTH_SHORT).show();
            } else {
                saveStudentToFirestore(name, sid, sClass);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveStudentToFirestore(String name, String sid, String sClass) {
        // (Your existing save logic code here)
        Map<String, Object> student = new HashMap<>();
        student.put("name", name);
        student.put("studentId", sid);
        student.put("class", sClass);
        student.put("createdBy", userId);
        student.put("timestamp", System.currentTimeMillis());

        db.collection("students").add(student)
                .addOnSuccessListener(documentReference -> Toast.makeText(HomeDashBoard.this, "Student Added", Toast.LENGTH_SHORT).show())
//                .addOnFailureListener(e -> Toast.makeText(HomeDashBoard.this, "Error adding student", Toast.LENGTH_SHORT).show());
                .addOnFailureListener(e -> Toast.makeText(HomeDashBoard.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());

    }



    private void showSearchForDetailsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Student Details");
        builder.setMessage("Enter Student ID to view details & certificates:");

        final EditText inputId = new EditText(this);
        inputId.setHint("Student ID (e.g. S123)");
        inputId.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(inputId);

        builder.setPositiveButton("View", (dialog, which) -> {
            String sid = inputId.getText().toString().trim();
            if (!sid.isEmpty()) {
                findStudentForDetails(sid);
            } else {
                Toast.makeText(this, "Please enter an ID", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void findStudentForDetails(String studentId) {
        db.collection("students")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot doc = task.getResult().getDocuments().get(0);

                        // Pass data to the new Activity
                        Intent intent = new Intent(HomeDashBoard.this, StudentDetailsActivity.class);
                        intent.putExtra("docId", doc.getId()); // Firestore Document ID
                        intent.putExtra("studentId", studentId);
                        intent.putExtra("name", doc.getString("name"));
                        intent.putExtra("class", doc.getString("class"));
                        intent.putExtra("role", role); // Pass role for permissions inside details
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Student not found!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Show dialog with pre-filled data
    private void showEditStudentDialog(String docId, String oldName, String oldClass) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Student Info");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Name");
        inputName.setText(oldName); // Pre-fill existing name
        layout.addView(inputName);

        final EditText inputClass = new EditText(this);
        inputClass.setHint("Class/Grade");
        inputClass.setText(oldClass); // Pre-fill existing class
        layout.addView(inputClass);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            String newClass = inputClass.getText().toString().trim();

            if (!newName.isEmpty() && !newClass.isEmpty()) {
                performUpdate(docId, newName, newClass);
            } else {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    //Save changes to Firestore
    private void performUpdate(String docId, String newName, String newClass) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("class", newClass);

        db.collection("students").document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(HomeDashBoard.this, "Student Info Updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(HomeDashBoard.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


}
