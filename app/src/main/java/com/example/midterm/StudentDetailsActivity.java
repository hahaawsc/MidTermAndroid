package com.example.midterm;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvId, tvClass;
    private Button btnAddCert;
    private RecyclerView recyclerCertificates;
    private FirebaseFirestore db;

    private String studentId, role;
    private List<DocumentSnapshot> certificateList;
    private CertificateAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_details);

        db = FirebaseFirestore.getInstance();
        certificateList = new ArrayList<>();

        // Get data passed from Dashboard
        studentId = getIntent().getStringExtra("studentId");
        role = getIntent().getStringExtra("role");
        String name = getIntent().getStringExtra("name");
        String sClass = getIntent().getStringExtra("class");

        // Get data passed from Dashboard
        String rawStudentId = getIntent().getStringExtra("studentId");
        studentId = rawStudentId != null ? rawStudentId.trim() : ""; // <--- Safety trim

        // UI Binding
        tvName = findViewById(R.id.tvDetailName);
        tvId = findViewById(R.id.tvDetailId);
        tvClass = findViewById(R.id.tvDetailClass);
        btnAddCert = findViewById(R.id.btnAddCertificate);
        recyclerCertificates = findViewById(R.id.recyclerCertificates);

        // Set Text
        tvName.setText("Name: " + name);
        tvId.setText("ID: " + studentId);
        tvClass.setText("Class: " + sClass);

        // Setup Recycler
        recyclerCertificates.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CertificateAdapter();
        recyclerCertificates.setAdapter(adapter);

        // Load Data
        loadCertificates();

        // Check role to hide/show the "Add" button
        if ("employee".equals(role)) {
            btnAddCert.setVisibility(View.GONE);
        } else {
            btnAddCert.setVisibility(View.VISIBLE);
            btnAddCert.setOnClickListener(v -> showCertificateDialog(null, null, null));
        }

        // Add Button Logic
        btnAddCert.setOnClickListener(v -> showCertificateDialog(null, null, null));
    }

    private void loadCertificates() {
        // DEBUG LOGS
        System.out.println("DEBUG: APP is looking for studentId: [" + studentId + "]");

        // Let's see what is actually in the database for testing
        db.collection("certificates").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot d : queryDocumentSnapshots) {
                System.out.println("DEBUG: DB contains cert for ID: [" + d.getString("studentId") + "]");
            }
        });

        db.collection("certificates")
                .whereEqualTo("studentId", studentId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        certificateList.clear();
                        certificateList.addAll(value.getDocuments());
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // Dialog for Adding OR Editing
    private void showCertificateDialog(String docId, String oldName, String oldDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(docId == null ? "Add Certificate" : "Update Certificate");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("Certificate Name");
        if (oldName != null) inputName.setText(oldName);
        layout.addView(inputName);

        final EditText inputDate = new EditText(this);
        inputDate.setHint("Date (YYYY-MM-DD)");
        if (oldDate != null) inputDate.setText(oldDate);
        layout.addView(inputDate);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String cName = inputName.getText().toString().trim();
            String cDate = inputDate.getText().toString().trim();

            if (!cName.isEmpty() && !cDate.isEmpty()) {
                if (docId == null) {
                    saveNewCertificate(cName, cDate);
                } else {
                    updateCertificate(docId, cName, cDate);
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveNewCertificate(String cName, String cDate) {
        Map<String, Object> cert = new HashMap<>();
        cert.put("studentId", studentId); // Link to student
        cert.put("certificateName", cName);
        cert.put("date", cDate);

        db.collection("certificates").add(cert)
                .addOnSuccessListener(v -> Toast.makeText(this, "Certificate Added", Toast.LENGTH_SHORT).show());
    }

    private void updateCertificate(String docId, String cName, String cDate) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("certificateName", cName);
        updates.put("date", cDate);

        db.collection("certificates").document(docId).update(updates)
                .addOnSuccessListener(v -> Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show());
    }

    private void deleteCertificate(String docId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Certificate")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.collection("certificates").document(docId).delete();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // --- INNER ADAPTER CLASS ---
    class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertViewHolder> {

        @NonNull
        @Override
        public CertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_certificate, parent, false);
            return new CertViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CertViewHolder holder, int position) {
            DocumentSnapshot doc = certificateList.get(position);
            String name = doc.getString("certificateName");
            String date = doc.getString("date");

            holder.tvName.setText(name);
            holder.tvDate.setText(date);

            if ("employee".equals(role)) {
                // Employees can only VIEW, not Edit or Delete
                holder.btnEdit.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);
            } else {
                // Managers/Admins see buttons
                holder.btnEdit.setVisibility(View.VISIBLE);
                holder.btnDelete.setVisibility(View.VISIBLE);

                holder.btnEdit.setOnClickListener(v -> showCertificateDialog(doc.getId(), name, date));
                holder.btnDelete.setOnClickListener(v -> deleteCertificate(doc.getId()));
            }
        }

        @Override
        public int getItemCount() {
            return certificateList.size();
        }

        class CertViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate;
            ImageButton btnEdit, btnDelete;

            public CertViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCertName);
                tvDate = itemView.findViewById(R.id.tvCertDate);
                btnEdit = itemView.findViewById(R.id.btnEditCert);
                btnDelete = itemView.findViewById(R.id.btnDeleteCert);
            }
        }
    }
}
