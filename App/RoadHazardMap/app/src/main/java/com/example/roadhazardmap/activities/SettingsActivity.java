package com.example.roadhazardmap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roadhazardmap.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();

        // Show current user info
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            TextView tvEmail = findViewById(R.id.tv_user_email);
            TextView tvName  = findViewById(R.id.tvUserName);
            if (tvEmail != null) tvEmail.setText(user.getEmail());
            if (tvName  != null) {
                String name = user.getDisplayName();
                tvName.setText(name != null && !name.isEmpty() ? name : "Rider");
            }
        }

        // Reset Password card
        MaterialCardView cardReset = findViewById(R.id.btn_reset_password);
        if (cardReset != null) cardReset.setOnClickListener(v -> showResetPasswordDialog());

        // Logout button
        MaterialButton btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log Out", (d, w) -> {
                        mAuth.signOut();
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }
    }

    private void showResetPasswordDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        new AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Send a password reset email to:\n" + user.getEmail() + "?")
            .setPositiveButton("Send", (dialog, which) ->
                mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reset email sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        }
                    }))
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
