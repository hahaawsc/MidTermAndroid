package com.example.midterm;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class UserProfile extends AppCompatActivity {

    private ImageView imgProfile;
    private EditText edtName, edtAge, edtPhone;
    private Button btnChangePhoto, btnSave, btnLogout;

    private String userId;
    private String role;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();

    private ActivityResultLauncher<String> imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        imgProfile = findViewById(R.id.imgProfile);
        edtName = findViewById(R.id.edtName);
        edtAge = findViewById(R.id.edtAge);
        edtPhone = findViewById(R.id.edtPhone);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSave = findViewById(R.id.btnSave);
        btnLogout = findViewById(R.id.btnLogout);

        // In onCreate()
        userId = getIntent().getStringExtra("userId");
        role = getIntent().getStringExtra("role");

        // Check if critical data is missing
        if (userId == null || role == null) {
            Toast.makeText(this, "Error: User data not found.", Toast.LENGTH_LONG).show();
            finish(); // Close the activity if data is missing
            return;   // Stop executing the rest of onCreate()
        }

        setupImagePicker();
        loadUserData();
        setupRolePermissions();


        btnChangePhoto.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSave.setOnClickListener(v -> updateUserData());
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // Signs out of Firebase

            // Go back to Login Activity
            Intent intent = new Intent(UserProfile.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close UserProfile so they can't go back
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

    }

    private void setupRolePermissions() {

        Toast.makeText(this, "Logged in as: " + role, Toast.LENGTH_SHORT).show();

        if (role.equals("employee")) {     // Employees can't edit info, but CAN change photo (handled by btnChangePhoto remaining active)
            edtName.setEnabled(false);
            edtAge.setEnabled(false);
            edtPhone.setEnabled(false);
            btnSave.setVisibility(View.GONE);
        }

        if (role.equals("manager") || role.equals("admin")) {
            // Managers and Admins can edit their own profile info
            edtName.setEnabled(true);
            edtAge.setEnabled(true);
            edtPhone.setEnabled(true);
            btnSave.setVisibility(View.VISIBLE); // Explicitly show the save button
        }
    }


    private void setupImagePicker() {
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) uploadProfilePicture(uri);
                }
        );
    }

    private void loadUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        edtName.setText(doc.getString("name"));
                        edtAge.setText(doc.get("age") + "");
                        edtPhone.setText(doc.getString("phone"));

                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl != null)
                            Picasso.get().load(photoUrl).into(imgProfile);
                    }
                });
    }

    private void updateUserData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", edtName.getText().toString());
        data.put("age", Integer.parseInt(edtAge.getText().toString()));
        data.put("phone", edtPhone.getText().toString());

        db.collection("users").document(userId).update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadProfilePicture(Uri imageUri) {
        StorageReference ref = storage.getReference("users/" + userId + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(url -> {
                    db.collection("users").document(userId)
                            .update("photoUrl", url.toString());

                    Picasso.get().load(url).into(imgProfile);
                }));
    }
}
