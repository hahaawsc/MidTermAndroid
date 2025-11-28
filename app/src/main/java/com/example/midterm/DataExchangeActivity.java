package com.example.midterm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class DataExchangeActivity extends AppCompatActivity {

    private Button btnImportStudents, btnExportStudents, btnImportCertificates, btnExportCertificates;
    private FirebaseFirestore db;

    // Launchers for file picking
    private ActivityResultLauncher<String> importStudentLauncher;
    private ActivityResultLauncher<String> importCertificateLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_exchange);

        db = FirebaseFirestore.getInstance();

        btnImportStudents = findViewById(R.id.btnImportStudents);
        btnExportStudents = findViewById(R.id.btnExportStudents);
        btnImportCertificates = findViewById(R.id.btnImportCertificates);
        btnExportCertificates = findViewById(R.id.btnExportCertificates);

        setupFilePickers();

        // Listeners
        btnImportStudents.setOnClickListener(v -> importStudentLauncher.launch("text/*")); // Opens file picker
        btnExportStudents.setOnClickListener(v -> exportStudentsToCSV());

        btnImportCertificates.setOnClickListener(v -> importCertificateLauncher.launch("text/*"));
        btnExportCertificates.setOnClickListener(v -> exportCertificatesToCSV());

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupFilePickers() {
        // Picker for Students
        importStudentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) readStudentCSV(uri);
        });

        // Picker for Certificates
        importCertificateLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) readCertificateCSV(uri);
        });
    }

    // --- IMPORT LOGIC ---

    private void readStudentCSV(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Expected Format: Name,ID,Class
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    Map<String, Object> student = new HashMap<>();
                    student.put("name", tokens[0].trim());
                    student.put("studentId", tokens[1].trim());
                    student.put("class", tokens[2].trim());

                    db.collection("students").add(student);
                }
            }
            Toast.makeText(this, "Student Import Started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void readCertificateCSV(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {// CSV Format: StudentID,CertificateName,Date
                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    Map<String, Object> cert = new HashMap<>();

                    // CRITICAL: Ensure strict trimming on the ID
                    String cleanId = tokens[0].trim();

                    cert.put("studentId", cleanId);
                    cert.put("certificateName", tokens[1].trim());
                    cert.put("date", tokens[2].trim());

                    db.collection("certificates").add(cert);
                }
            }
            Toast.makeText(this, "Certificate Import Started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }


    // --- EXPORT LOGIC ---

    private void exportStudentsToCSV() {
        db.collection("students").get().addOnSuccessListener(queryDocumentSnapshots -> {
            try {
                // Create file in Downloads folder
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "students_export.csv");
                FileWriter writer = new FileWriter(file);

                writer.append("Name,Student ID,Class\n"); // Header

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    writer.append(doc.getString("name")).append(",");
                    writer.append(doc.getString("studentId")).append(",");
                    writer.append(doc.getString("class")).append("\n");
                }
                writer.flush();
                writer.close();
                Toast.makeText(this, "Saved to Downloads/students_export.csv", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportCertificatesToCSV() {
        db.collection("certificates").get().addOnSuccessListener(queryDocumentSnapshots -> {
            try {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "certificates_export.csv");
                FileWriter writer = new FileWriter(file);

                writer.append("Student ID,Certificate Name,Date\n");

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    writer.append(doc.getString("studentId")).append(",");
                    writer.append(doc.getString("certificateName")).append(",");
                    writer.append(doc.getString("date")).append("\n");
                }
                writer.flush();
                writer.close();
                Toast.makeText(this, "Saved to Downloads/certificates_export.csv", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
