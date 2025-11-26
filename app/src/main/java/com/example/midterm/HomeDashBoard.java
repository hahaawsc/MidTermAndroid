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
    private CardView cardAddStudent, cardDeleteStudent, cardUpdateStudent, cardViewStudents;
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
            intent.putExtra("userId", userId);
            intent.putExtra("role", role);
            startActivity(intent2);
        });

        cardAddStudent.setOnClickListener(v -> showAddStudentDialog());
        cardDeleteStudent.setOnClickListener(v -> showDeleteStudentDialog());

        // Add Listener for Update
        cardUpdateStudent.setOnClickListener(v -> showSearchForUpdateDialog());
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
        } else {
            // Managers and Admins see all
            cardAddStudent.setVisibility(View.VISIBLE);
            cardDeleteStudent.setVisibility(View.VISIBLE);
            cardUpdateStudent.setVisibility(View.VISIBLE);
        }
    }

    // ... showAddStudentDialog and saveStudentToFirestore exist here (keeping them same as before) ...

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

    // --- NEW: DELETE LOGIC ---

    private void showDeleteStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Student");
        builder.setMessage("Enter the Student ID to delete:");

        // Simple input field for ID
        final EditText inputId = new EditText(this);
        inputId.setHint("Student ID (e.g., S123)");
        inputId.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(inputId);

        builder.setPositiveButton("Delete", (dialog, which) -> {
            String sid = inputId.getText().toString().trim();
            if (!sid.isEmpty()) {
                confirmDeleteStudent(sid);
            } else {
                Toast.makeText(this, "Please enter an ID", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void confirmDeleteStudent(String studentId) {
        // 1. Query Firestore to find the document with this studentId
        db.collection("students")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            // 2. Found the student, get the document ID (not the studentId field)
                            String docId = snapshot.getDocuments().get(0).getId();

                            // 3. Perform the actual delete
                            deleteStudentDocument(docId);
                        } else {
                            Toast.makeText(HomeDashBoard.this, "Student not found with ID: " + studentId, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(HomeDashBoard.this, "Error finding student", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteStudentDocument(String docId) {
        db.collection("students").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(HomeDashBoard.this, "Student deleted successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HomeDashBoard.this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // --- NEW: UPDATE LOGIC ---

    // Step 1: Ask for the ID to find
    private void showSearchForUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Student");
        builder.setMessage("Enter Student ID to update:");

        final EditText inputId = new EditText(this);
        inputId.setHint("Student ID (e.g. S123)");
        inputId.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(inputId);

        builder.setPositiveButton("Find", (dialog, which) -> {
            String sid = inputId.getText().toString().trim();
            if (!sid.isEmpty()) {
                findStudentForUpdate(sid);
            } else {
                Toast.makeText(this, "Please enter an ID", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Step 2: Find the student in database
    private void findStudentForUpdate(String studentId) {
        db.collection("students")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            // Found it! Get the first match
                            DocumentSnapshot doc = snapshot.getDocuments().get(0);
                            String docId = doc.getId();
                            String currentName = doc.getString("name");
                            String currentClass = doc.getString("class");

                            // Step 3: Show the edit form
                            showEditStudentDialog(docId, currentName, currentClass);
                        } else {
                            Toast.makeText(this, "Student not found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error searching DB", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Step 3: Show dialog with pre-filled data
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

    // Step 4: Save changes to Firestore
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
