package com.example.projectdonasi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;


public class ForgotPasswordActivity extends AppCompatActivity {
    private TextInputEditText emailInput;
    private Button resetB;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        emailInput = findViewById(R.id.email);
        resetB = findViewById(R.id.resetButton);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        resetB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(ForgotPasswordActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ForgotPasswordActivity.this, "Reset email sent! Redirecting to login...", Toast.LENGTH_LONG).show();
                                new android.os.Handler().postDelayed(() -> {
                                    Intent login = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                                    startActivity(login);
                                    finish();
                                }, 3000); // Delay 3 detik agar user baca dulu pesannya

                            } else {
                                Toast.makeText(ForgotPasswordActivity.this, "Failed to send reset email", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }
}
