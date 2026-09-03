package com.example.projectdonasi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.projectdonasi.Connecter.CloudinaryHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;

    Uri gambarUri = null;
    CircleImageView imgProfile;
    TextView name, email, address, phone;
    MaterialButton backButton, editButton;
    View profileFormView;
    FrameLayout profileFormContainer;
    LinearLayout mainLayout;
    ProgressDialog progressDialog;

    EditText edtName, edtEmail, edtAddress, edtPhone;
    TextView txtFileName;
    String currentImageUrl = null;
    FirebaseAuth auth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> finish());

        imgProfile = findViewById(R.id.imgProfile);
        name = findViewById(R.id.txtName);
        email = findViewById(R.id.txtEmail);
        address = findViewById(R.id.txtAddress);
        phone = findViewById(R.id.txtPhone);

        mainLayout = findViewById(R.id.mainLayout);
        profileFormContainer = findViewById(R.id.profileFormContainer);
        editButton = findViewById(R.id.btnEdit);

        editButton.setOnClickListener(v -> showProfileForm());

        loadUserProfile();
    }

    private void loadUserProfile() {
        String uid = auth.getCurrentUser().getUid();
        firestore.collection("User").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        name.setText(doc.getString("nama"));
                        email.setText(doc.getString("email"));
                        address.setText(doc.getString("alamat"));
                        phone.setText(doc.getString("noHp"));
                        String gambar = doc.getString("gambar");

                        currentImageUrl = gambar;

                        if (gambar != null && !gambar.isEmpty()) {
                            Glide.with(this)
                                    .load(gambar)
                                    .placeholder(R.drawable.person_icon)
                                    .error(R.drawable.person_icon)
                                    .into(imgProfile);
                        } else {
                            imgProfile.setImageResource(R.drawable.person_icon);
                        }
                    } else {
                        Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal mengambil data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showProfileForm() {
        if (profileFormView == null) {
            profileFormView = getLayoutInflater().inflate(R.layout.activity_profile_form, profileFormContainer, false);

            edtName = profileFormView.findViewById(R.id.txtName);
            edtEmail = profileFormView.findViewById(R.id.txtEmail);
            edtAddress = profileFormView.findViewById(R.id.txtAddress);
            edtPhone = profileFormView.findViewById(R.id.txtPhone);
            txtFileName = profileFormView.findViewById(R.id.txtFileName);

            edtName.setText(name.getText().toString());
            edtEmail.setText(email.getText().toString());
            edtEmail.setEnabled(false);
            edtAddress.setText(address.getText().toString());
            edtPhone.setText(phone.getText().toString());
            txtFileName.setText(currentImageUrl != null ? currentImageUrl : "No image URL");

            MaterialButton btnBackProfile = profileFormView.findViewById(R.id.btnBackProfile);
            btnBackProfile.setOnClickListener(v -> {
                profileFormContainer.setVisibility(View.GONE);
                mainLayout.setVisibility(View.VISIBLE);
            });

            MaterialButton btnSave = profileFormView.findViewById(R.id.btnSave);
            btnSave.setOnClickListener(v -> saveProfile());

            MaterialButton btnChooseFile = profileFormView.findViewById(R.id.btnChooseFile);
            btnChooseFile.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Pilih Gambar"), PICK_IMAGE_REQUEST);
            });

            profileFormContainer.addView(profileFormView);
        }

        mainLayout.setVisibility(View.GONE);
        profileFormContainer.setVisibility(View.VISIBLE);
    }

    private void saveProfile() {
        String nama = edtName.getText().toString().trim();
        String emailUser = edtEmail.getText().toString().trim();
        String alamat = edtAddress.getText().toString().trim();
        String no_hp = edtPhone.getText().toString().trim();

        if (nama.isEmpty() || emailUser.isEmpty()) {
            Toast.makeText(this, "Nama dan email tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Menyimpan data profil...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (gambarUri != null) {
            CloudinaryHelper.uploadImage(this, gambarUri, new CloudinaryHelper.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    updateUserProfile(uid, nama, emailUser, alamat, no_hp, imageUrl);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(UserProfileActivity.this, "Gagal upload gambar: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            updateUserProfile(uid, nama, emailUser, alamat, no_hp, null);
        }
    }

    private void updateUserProfile(String uid, String namaBaru, String emailBaru, String alamatBaru, String noHpBaru, @Nullable String imageUrl) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("nama", namaBaru);
        updatedData.put("email", emailBaru);
        updatedData.put("alamat", alamatBaru);
        updatedData.put("noHp", noHpBaru);
        if (imageUrl != null) {
            updatedData.put("gambar", imageUrl);
        }

        firestore.collection("User").document(uid)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    name.setText(namaBaru);
                    email.setText(emailBaru);
                    address.setText(alamatBaru);
                    phone.setText(noHpBaru);
                    if (imageUrl != null) {
                        currentImageUrl = imageUrl;
                        Glide.with(imgProfile.getContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.person_icon)
                                .error(R.drawable.person_icon)
                                .into(imgProfile);
                    }

                    gambarUri = null;
                    profileFormContainer.setVisibility(View.GONE);
                    mainLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    loadUserProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memperbarui profil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            gambarUri = data.getData();
            if (gambarUri != null && imgProfile != null) {
                imgProfile.setImageURI(gambarUri);
            }
            if (txtFileName != null) {
                txtFileName.setText(getFileName(gambarUri));
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
