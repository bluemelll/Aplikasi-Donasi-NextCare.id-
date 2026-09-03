package com.example.projectdonasi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;

public class ResgiterActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput, namaInput, noHpInput;
    private Button RegisButton;
    private TextView backLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private Spinner spinnerRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resgiter);

        emailInput = findViewById(R.id.Email);
        passwordInput = findViewById(R.id.Password);
        namaInput = findViewById(R.id.Nama);
        noHpInput = findViewById(R.id.noHp);
        RegisButton = findViewById(R.id.Register);
        backLogin = findViewById(R.id.Login);

        mAuth = FirebaseAuth.getInstance();

        firestore = FirebaseFirestore.getInstance();

        spinnerRole = findViewById(R.id.spinnerRole);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        //BUtton menjalankan proses regis
        RegisButton.setOnClickListener(v -> registerUser());

        //Button back to login
        backLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent (ResgiterActivity.this, LoginActivity.class);
                startActivity(back);
            }
        });
    }

    //Main Fungsi add akun ke auth dan firestore
    private void registerUser() {
        //ambil input
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String nama = namaInput.getText().toString().trim();
        String noHp = noHpInput.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        // Validasi form input
        if (email.isEmpty()) {
            emailInput.setError("Email tidak boleh kosong.");
            emailInput.requestFocus();
            return;
        } else if (!email.contains("@") ||
                (!email.endsWith("@gmail.com") &&
                        !email.matches(".*@.+\\..+"))) {
            emailInput.setError("Gunakan email yang valid!");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password tidak boleh kosong.");
            passwordInput.requestFocus();
            return;
        } else if (password.length() < 6) {
            passwordInput.setError("Password minimal 6 karakter");
            passwordInput.requestFocus();
            return;
        }

        if (nama.isEmpty()) {
            namaInput.setError("Nama tidak boleh kosong.");
            namaInput.requestFocus();
            return;
        }

        if (noHp.isEmpty()) {
            noHpInput.setError("Nomor HP tidak boleh kosong.");
            noHpInput.requestFocus();
            return;
        } else if (noHp.length() < 9 || noHp.length() > 13) {
            noHpInput.setError("Nomor harus terdiri dari 9 hingga 13 digit.");
            noHpInput.requestFocus();
            return;
        } else if (noHp.startsWith("08")) {
            noHp = "+62" + noHp.substring(1);
        }
        String finalNoHp = noHp;

        //proses creat akun auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            //akun ke firestore
                            HashMap<String, Object> userData = new HashMap<>();
                            userData.put("uid", firebaseUser.getUid());
                            userData.put("email", email);
                            userData.put("nama", nama);
                            userData.put("noHp", finalNoHp);
                            userData.put("role", role);


                            firestore.collection("User")
                                    .document(firebaseUser.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(ResgiterActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Gagal menyimpan data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Gagal registrasi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }

}
