package com.example.roadhazardmap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roadhazardmap.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private MaterialButton    btnSignup;
    private TextView          tvLogin;
    private ProgressBar       progressBar;
    private FirebaseAuth      mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth       = FirebaseAuth.getInstance();
        etName      = findViewById(R.id.et_name);
        etEmail     = findViewById(R.id.et_email);
        etPassword  = findViewById(R.id.et_password);
        etConfirm   = findViewById(R.id.et_confirm_password);
        btnSignup   = findViewById(R.id.btn_signup);
        tvLogin     = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);

        btnSignup.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        TextView tvBack = findViewById(R.id.tvBack);
        if (tvBack != null) tvBack.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name     = etName.getText()     != null ? etName.getText().toString().trim()     : "";
        String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirm  = etConfirm.getText()  != null ? etConfirm.getText().toString().trim()  : "";

        if (TextUtils.isEmpty(name))     { etName.setError("Name required");             return; }
        if (TextUtils.isEmpty(email))    { etEmail.setError("Email required");           return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password required");     return; }
        if (password.length() < 6)       { etPassword.setError("Minimum 6 characters"); return; }
        if (!password.equals(confirm))   { etConfirm.setError("Passwords don't match"); return; }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        // Store display name in Firebase Auth profile
                        if (mAuth.getCurrentUser() != null) {
                            UserProfileChangeRequest profileReq =
                                    new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build();
                            mAuth.getCurrentUser().updateProfile(profileReq);
                        }
                        Toast.makeText(this, "Account created! Welcome 🎉",
                                Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Sign up failed: " + msg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!loading);
    }
}
