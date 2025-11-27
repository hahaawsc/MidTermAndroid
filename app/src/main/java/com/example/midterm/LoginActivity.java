package com.example.midterm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private FirebaseAuth mAuth; // Use the new helper
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming your layout file is login_ui.xml
        // If not, change R.layout.login_ui to R.layout.activity_main
        setContentView(R.layout.login_ui); // Use your actual login layout

        // Initialize the helper by passing 'this' activity as the context
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEditText = findViewById(R.id.editTextText);
        passwordEditText = findViewById(R.id.editTextTextPassword);
        loginButton = findViewById(R.id.button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = nameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email and password.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Call the signIn method from the helper
                performLogin(email, password);
            }
        });

        // Boilerplate insets code
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void performLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login Success - Now fetch the Role
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchRoleAndRedirect(user.getUid());
                        }
                    } else {
                        // Login Failed
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchRoleAndRedirect(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");

                        // Default to employee if role is missing
                        if (role == null) role = "employee";

                        // --- REDIRECT TO DASHBOARD ---
                        Intent intent = new Intent(LoginActivity.this, HomeDashBoard.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("role", role);

                        logLoginHistory(userId, mAuth.getCurrentUser().getEmail());
                        startActivity(intent);
                        finish(); // Prevent going back to login
                    } else {
                        Toast.makeText(LoginActivity.this, "User data not found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Error fetching user role", Toast.LENGTH_SHORT).show();
                });
    }

    private void logLoginHistory(String userId, String email) {
        Map<String, Object> log = new HashMap<>();log.put("userId", userId);
        log.put("email", email);
        log.put("timestamp", System.currentTimeMillis());
        log.put("status", "Success");

        db.collection("login_history").add(log);
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchRoleAndRedirect(currentUser.getUid());
        }
    }
}
