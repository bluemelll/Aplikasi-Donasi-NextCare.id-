package com.example.projectdonasi.Donatur;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.projectdonasi.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class FormDonasiActivity extends AppCompatActivity {

    private String donasiId, uid, penerimaId;
    private EditText editNominalLainnya;
    private MaterialButton btnTransferBank, btnVirtual, btnQRIS, btnGopay, btnDANA, btnOVO, btnSaldo;
    private MaterialButton btn10000, btn25000, btn50000, btnLainnya;
    private Button btnSubmitDonasi, cancelButton;
    private ImageButton btnBack;
    private TextView textJudul;
    private String selectedMetode = "";
    private long selectedNominal = 0;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private MaterialButton[] nominalButtons;
    private MaterialButton[] metodeButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_donasi);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        donasiId = getIntent().getStringExtra("donasiId");
        penerimaId = getIntent().getStringExtra("penerimaId");

        textJudul = findViewById(R.id.judul);
        editNominalLainnya = findViewById(R.id.editNominalLainnya);
        btn10000 = findViewById(R.id.btn10000);
        btn25000 = findViewById(R.id.btn25000);
        btn50000 = findViewById(R.id.btn50000);
        btnLainnya = findViewById(R.id.btnLainnya);

        btnTransferBank = findViewById(R.id.btnTransferBank);
        btnVirtual = findViewById(R.id.btnTransferVirtual);
        btnQRIS = findViewById(R.id.btnQRIS);
        btnGopay = findViewById(R.id.btnGopay);
        btnDANA = findViewById(R.id.btnDANA);
        btnOVO = findViewById(R.id.btnOVO);
        btnSaldo = findViewById(R.id.btnSaldo);

        btnBack = findViewById(R.id.btnBack);
        cancelButton = findViewById(R.id.btnCancel);
        btnSubmitDonasi = findViewById(R.id.btnSubmitDonasi);

        if (donasiId == null || uid.isEmpty() || penerimaId == null) {
            Toast.makeText(this, "Data donasi tidak lengkap", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("Donasi").document(donasiId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String judul = documentSnapshot.getString("judul");
                        textJudul.setText(judul != null ? judul : "Judul Donasi");
                        String sisaHariStr = documentSnapshot.getString("sisa_hari");
                    } else {
                        Toast.makeText(this, "Data donasi tidak ditemukan", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal mengambil judul: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        nominalButtons = new MaterialButton[]{btn10000, btn25000, btn50000, btnLainnya};
        metodeButtons = new MaterialButton[]{btnTransferBank, btnVirtual, btnQRIS, btnGopay, btnDANA, btnOVO, btnSaldo};

        setupNominalButtons();
        setupMetodeButtons();

        btnBack.setOnClickListener(v -> {
            finish();
        });
        cancelButton.setOnClickListener(v -> finish());
        btnSubmitDonasi.setOnClickListener(v -> processDonation());
    }

    private void setupNominalButtons() {
        View.OnClickListener nominalClick = v -> {
            editNominalLainnya.setVisibility(View.GONE);

            for (MaterialButton btn : nominalButtons) {
                btn.setBackgroundColor(getResources().getColor(R.color.white));
                btn.setTextColor(getResources().getColor(R.color.lilac));
            }

            MaterialButton clicked = (MaterialButton) v;
            clicked.setBackgroundColor(getResources().getColor(R.color.lilac));
            clicked.setTextColor(getResources().getColor(R.color.white));

            if (clicked == btn10000) {
                selectedNominal = 10000;
            } else if (clicked == btn25000) {
                selectedNominal = 25000;
            } else if (clicked == btn50000) {
                selectedNominal = 50000;
            } else if (clicked == btnLainnya) {
                selectedNominal = 0;
                editNominalLainnya.setVisibility(View.VISIBLE);
            }
        };

        for (MaterialButton btn : nominalButtons) {
            btn.setOnClickListener(nominalClick);
        }
    }


    private void setupMetodeButtons() {
        View.OnClickListener metodeClick = v -> {
            for (MaterialButton btn : metodeButtons) {
                btn.setBackgroundColor(getResources().getColor(R.color.white));
                btn.setTextColor(getResources().getColor(R.color.black));
            }
            MaterialButton clicked = (MaterialButton) v;
            clicked.setBackgroundColor(getResources().getColor(R.color.lilac));
            clicked.setTextColor(getResources().getColor(R.color.white));
            selectedMetode = clicked.getText().toString();
        };
        for (MaterialButton btn : metodeButtons) {
            btn.setOnClickListener(metodeClick);
        }
    }

    private void processDonation() {
        long inputNominal = selectedNominal;
        if (inputNominal == 0) {
            String txt = editNominalLainnya.getText().toString().trim();
            if (!txt.isEmpty()) {
                try {
                    inputNominal = Long.parseLong(txt);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Nominal tidak valid", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (inputNominal <= 0) {
            Toast.makeText(this, "Pilih nominal donasi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedMetode.isEmpty()) {
            Toast.makeText(this, "Pilih metode pembayaran", Toast.LENGTH_SHORT).show();
            return;
        }
        final long nominalToProcess = inputNominal;

        Calendar c = Calendar.getInstance();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime());
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(c.getTime());
        runPostDonasiTransaction(nominalToProcess, selectedMetode);
    }

    private void runPostDonasiTransaction(long nominalDonasiInput, String metode) {
        DocumentReference donasiRef = firestore.collection("Donasi").document(donasiId);
        DocumentReference userRef = firestore.collection("User").document(uid);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot donasiSnapshot;
            DocumentSnapshot userSnapshot = null;

            try {
                donasiSnapshot = transaction.get(donasiRef);
                if (metode.equalsIgnoreCase("Saldo")) {
                    userSnapshot = transaction.get(userRef);
                }
            } catch (Exception e) {
                throw new FirebaseFirestoreException("Gagal membaca data transaksi",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            String terStr = donasiSnapshot.getString("nominal_terkumpul");
            String targetStr = donasiSnapshot.getString("nominal_target");

            Long currentTer = 0L;
            if (terStr != null) {
                try {
                    currentTer = Long.parseLong(terStr);
                } catch (NumberFormatException e) {
                    currentTer = 0L;
                }
            }

            Long nominalTarget = 0L;
            if (targetStr != null) {
                try {
                    nominalTarget = Long.parseLong(targetStr);
                } catch (NumberFormatException e) {
                    nominalTarget = 0L;
                }
            }

            long actualNominalToApply = nominalDonasiInput;
            if (currentTer + nominalDonasiInput > nominalTarget) {
                actualNominalToApply = nominalTarget - currentTer;
                if (actualNominalToApply < 0) {
                    actualNominalToApply = 0;
                }
            }

            if (actualNominalToApply <= 0) {
                throw new FirebaseFirestoreException("Donasi sudah mencapai target atau nominal donasi terlalu kecil.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(donasiRef, "nominal_terkumpul", String.valueOf(currentTer + actualNominalToApply));

            long finalDebitedAmount = actualNominalToApply;
            if (metode.equalsIgnoreCase("Saldo")) {
                if (userSnapshot == null || !userSnapshot.exists()) {
                    throw new FirebaseFirestoreException("Dokumen pengguna tidak ditemukan",
                            FirebaseFirestoreException.Code.ABORTED);
                }

                String saldoStr = userSnapshot.getString("saldo");
                Long currentSaldo = 0L;
                if (saldoStr != null) {
                    try {
                        currentSaldo = Long.parseLong(saldoStr);
                    } catch (NumberFormatException e) {
                        currentSaldo = 0L;
                    }
                }

                if (currentSaldo < finalDebitedAmount) {
                    throw new FirebaseFirestoreException("Saldo tidak cukup",
                            FirebaseFirestoreException.Code.ABORTED);
                }

                transaction.update(userRef, "saldo", String.valueOf(currentSaldo - finalDebitedAmount));
            }

            Calendar c = Calendar.getInstance();
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime());
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(c.getTime());

            HashMap<String, Object> newTransaksiRecord = new HashMap<>();
            newTransaksiRecord.put("donasiId", donasiId);
            newTransaksiRecord.put("donaturId", uid);
            newTransaksiRecord.put("penerimaId", penerimaId);
            newTransaksiRecord.put("metode_pembayaran", metode);
            newTransaksiRecord.put("nominal", String.valueOf(finalDebitedAmount));
            newTransaksiRecord.put("status", "Berhasil");
            newTransaksiRecord.put("tanggal_donasi", date);
            newTransaksiRecord.put("waktu_donasi", time);
            transaction.set(firestore.collection("Transaksi").document(), newTransaksiRecord);
            return null;

        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Donasi Berhasil", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                if (firestoreException.getCode() == FirebaseFirestoreException.Code.ABORTED) {
                    if (firestoreException.getMessage().contains("Saldo tidak cukup")) {
                        Toast.makeText(this, "Saldo tidak cukup", Toast.LENGTH_SHORT).show();
                    } else if (firestoreException.getMessage().contains("Donasi sudah mencapai target")) {
                        Toast.makeText(this, "Donasi sudah mencapai target", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Transaksi dibatalkan: " + firestoreException.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Gagal update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Terjadi kesalahan: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}