package com.example.roadhazardmap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.roadhazardmap.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton    btnLogin;
    private TextView          tvSignup, tvForgot;
    private ProgressBar       progressBar;
    private FirebaseAuth      mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth       = FirebaseAuth.getInstance();
        etEmail     = findViewById(R.id.et_email);
        etPassword  = findViewById(R.id.et_password);
        btnLogin    = findViewById(R.id.btn_login);
        tvSignup    = findViewById(R.id.tv_signup);
        tvForgot    = findViewById(R.id.tv_forgot);
        progressBar = findViewById(R.id.progress_bar);

        // Highlight "Sign Up" portion of the link text
        String signupText = "Don't have an account? Sign Up";
        SpannableString ss = new SpannableString(signupText);
        ss.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(this, R.color.colorAccent)),
                signupText.indexOf("Sign Up"), signupText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvSignup.setText(ss);

        btnLogin.setOnClickListener(v -> loginUser());

        tvSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class)));

        tvForgot.setOnClickListener(v -> {
            String email = etEmail.getText() != null
                    ? etEmail.getText().toString().trim() : "";
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter your email first");
                return;
            }
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Reset email sent!", Toast.LENGTH_SHORT).show();
                } else if (task.getException() != null) {
                    Toast.makeText(this,
                            "Error: " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loginUser() {
        String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email))    { etEmail.setError("Email required");    return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password required"); return; }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";
                        Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
