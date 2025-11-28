package com.example.midterm;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StudentListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText edtSearch;
    private Spinner spinnerSearchCriteria;
    private Button btnSortName, btnSortId;

    private FirebaseFirestore db;
    private StudentAdapter adapter;
    private List<Student> studentList = new ArrayList<>();
    private List<Student> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        db = FirebaseFirestore.getInstance();

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerViewStudents);
        edtSearch = findViewById(R.id.edtSearch);
        spinnerSearchCriteria = findViewById(R.id.spinnerSearchCriteria);
        btnSortName = findViewById(R.id.btnSortName);
        btnSortId = findViewById(R.id.btnSortId);

        // Setup Recycler
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        // Setup Search Criteria Spinner
        String[] criteria = {"Name", "Student ID", "Class"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, criteria);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchCriteria.setAdapter(spinnerAdapter);

        // Fetch Data
        loadStudents();

        // Search Logic
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Sort Logic
        btnSortName.setOnClickListener(v -> {
            Collections.sort(filteredList, Comparator.comparing(Student::getName));
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Sorted by Name", Toast.LENGTH_SHORT).show();
        });

        btnSortId.setOnClickListener(v -> {
            Collections.sort(filteredList, Comparator.comparing(Student::getStudentId));
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Sorted by ID", Toast.LENGTH_SHORT).show();
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadStudents() {
        db.collection("students").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Create Student object from Firestore data
                        String name = doc.getString("name");
                        String sid = doc.getString("studentId");
                        String sClass = doc.getString("class");

                        if (name != null) {
                            studentList.add(new Student(doc.getId(), name, sid, sClass));
                        }
                    }
                    // Initially, filtered list is same as full list
                    filteredList.clear();
                    filteredList.addAll(studentList);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show());
    }

    private void filter(String text) {
        filteredList.clear();
        String criteria = spinnerSearchCriteria.getSelectedItem().toString();
        String searchText = text.toLowerCase();

        for (Student item : studentList) {
            boolean match = false;

            // Filter based on Spinner Selection
            if (criteria.equals("Name")) {
                if (item.getName().toLowerCase().contains(searchText)) match = true;
            } else if (criteria.equals("Student ID")) {
                if (item.getStudentId().toLowerCase().contains(searchText)) match = true;
            } else if (criteria.equals("Class")) {
                if (item.getSClass().toLowerCase().contains(searchText)) match = true;
            }

            if (match) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- Inner Class: Adapter ---
    public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {
        private List<Student> list;

        public StudentAdapter(List<Student> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Using simple built-in layout for demo, or create a custom row xml
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Student s = list.get(position);
            holder.text1.setText(s.getName());
            holder.text2.setText("ID: " + s.getStudentId() + " | Class: " + s.getSClass());
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
