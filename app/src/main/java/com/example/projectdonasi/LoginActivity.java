package com.example.projectdonasi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projectdonasi.Admin.AdminMainActivity;
import com.example.projectdonasi.Donatur.DonaturHomeActivity;
import com.example.projectdonasi.Penerima.PenerimaHomeActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private TextInputEditText editEmail, editPassword;
    private Button buttonLogin, googleB;
    private TextView creatAccount, forgotPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        editEmail = findViewById(R.id.Email);
        editPassword = findViewById(R.id.Password);
        buttonLogin = findViewById(R.id.Login);
        googleB = findViewById(R.id.googleButton);
        creatAccount = findViewById(R.id.regis);
        forgotPass = findViewById(R.id.forgotPassword);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // dari Firebase
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Google Sign-In Button
        googleB.setOnClickListener(v -> loginAkunGoogle());

        // Register & Forgot Password
        creatAccount.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ResgiterActivity.class)));
        forgotPass.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        // Email/Password Login
        buttonLogin.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (email.isEmpty()) {
                editEmail.setError("Email harus diisi");
                editEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                editPassword.setError("Password harus diisi");
                editPassword.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        handleLoginSuccess(user);
                    }
                } else {
                    Toast.makeText(this, "Email atau Password salah!", Toast.LENGTH_SHORT).show();
                }
            });
        });

    }

    private void loginAkunGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference userRef = db.collection("User").document(user.getUid());

                    userRef.get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            DocumentSnapshot document = task1.getResult();
                            if (document != null && document.exists()) {
                                // User exists, direct based on role
                                directUserByRole(document.getString("role"));
                            } else {
                                // New user, assign default role
                                Map<String, Object> newUser = new HashMap<>();
                                newUser.put("email", user.getEmail());
                                newUser.put("name", user.getDisplayName());
                                newUser.put("role", "Donatur");

                                userRef.set(newUser).addOnSuccessListener(aVoid -> {
                                    startActivity(new Intent(this, DonaturHomeActivity.class));
                                    finish();
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(this, "Gagal menyimpan data pengguna.", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            Toast.makeText(this, "Gagal mengambil data pengguna.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(this, "Autentikasi Gagal.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLoginSuccess(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("User").document(user.getUid());

        userRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                directUserByRole(document.getString("role"));
            } else {
                Toast.makeText(this, "Data Anda tidak ditemukan.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void directUserByRole(String role) {
        Intent intent;
        switch (role) {
            case "Donatur":
                intent = new Intent(this, DonaturHomeActivity.class);
                break;
            case "Admin":
                intent = new Intent(this, AdminMainActivity.class);
                break;
            case "Penerima":
                intent = new Intent(this, PenerimaHomeActivity.class);
                break;
            default:
                Toast.makeText(this, "Peran pengguna tidak dikenali.", Toast.LENGTH_SHORT).show();
                return;
        }
        startActivity(intent);
        finish();
    }
}
