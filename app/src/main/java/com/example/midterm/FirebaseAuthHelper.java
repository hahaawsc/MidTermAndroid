package com.example.midterm;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseAuthHelper {

    private static final String TAG = "FirebaseAuthHelper";
    private FirebaseAuth mAuth;
    private Activity activity; // To perform UI actions like Toast and startActivity

    // Constructor that takes the activity that is using it
    public FirebaseAuthHelper(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
    }

    public void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(activity, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    // Checks if a user is already logged in when the app starts
    public void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, go straight to getting their role
            updateUI(currentUser);
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            String userId = user.getUid();

            // Go to Firestore to get the user's role
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the role from the document
                            String role = documentSnapshot.getString("role");

                            if (role != null) {
                                // Create an intent for UserProfile
                                Intent intent = new Intent(activity, UserProfile.class);

                                // Add the userId and role as extras
                                intent.putExtra("userId", userId);
                                intent.putExtra("role", role);

                                activity.startActivity(intent);
                                activity.finish(); // Prevents going back to login screen
                            } else {
                                Toast.makeText(activity, "Error: User has no role assigned.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(activity, "Error: User data not found in Database.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(activity, "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
