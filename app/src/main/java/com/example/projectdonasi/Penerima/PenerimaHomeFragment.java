package com.example.projectdonasi.Penerima;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.projectdonasi.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class PenerimaHomeFragment extends Fragment {
    private CircleImageView profileUser;
    private TextView tvDonasi, tvTransaksi, namaUserTextView, tglSignUpTextView, saldoUserTextView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    public PenerimaHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_penerima_home, container, false);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        profileUser = view.findViewById(R.id.imageProfile);
        namaUserTextView = view.findViewById(R.id.namaUser);
        tglSignUpTextView = view.findViewById(R.id.tglSignUp);
        saldoUserTextView = view.findViewById(R.id.saldoUser);

        tvDonasi = view.findViewById(R.id.champaign);
        tvTransaksi = view.findViewById(R.id.transaksi);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return view;
        }

        loadData();
        loadSaldoFromDonasi();
        getTransaksiCount();
        getDonasiCount();
//        getPengajuanCount();

        return view;
    }

    private void loadData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("User").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nama = documentSnapshot.getString("nama");
                            namaUserTextView.setText(nama != null ? nama : "Nama Tidak Ditemukan");

                            String gambar = documentSnapshot.getString("gambar");
                            if (gambar != null && !gambar.isEmpty()) {
                                loadProfileImage(gambar);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        namaUserTextView.setText("Gagal ambil data");
                    });

            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            String tanggalBergabung = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"))
                    .format(new Date(creationTimestamp));
            tglSignUpTextView.setText("Bergabung " + tanggalBergabung);
        }
    }

    private void loadSaldoFromDonasi() {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "User ID not available for saldo.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Donasi")
                .whereEqualTo("penerimaId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalNominalTerkumpul = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String nominalTerkumpulStr = document.getString("nominal_terkumpul");
                        if (nominalTerkumpulStr != null) {
                            try {
                                totalNominalTerkumpul += Long.parseLong(nominalTerkumpulStr);
                            } catch (NumberFormatException e) {
                                System.err.println("Error parsing nominal_terkumpul: " + nominalTerkumpulStr + " - " + e.getMessage());
                            }
                        }
                    }
                    updateSaldoTextView(totalNominalTerkumpul);
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Gagal mengambil saldo donasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    saldoUserTextView.setText("Gagal ambil saldo");
                });
    }

    private void updateSaldoTextView(long totalNominal) {
        if (saldoUserTextView != null) {
            NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            String saldoFormatted = formatRupiah.format(totalNominal);
            saldoUserTextView.setText(saldoFormatted);
        }
    }

    private void loadProfileImage(String url) {
        if (isAdded() && getActivity() != null) {
            Glide.with(requireContext())
                    .load(url)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .into(profileUser);
        }
    }

    private void getTransaksiCount() {
        db.collection("Transaksi")
                .whereEqualTo("penerimaId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvTransaksi.setText(String.valueOf(count));
                });
    }
    private void getDonasiCount() {
        db.collection("Donasi")
                .whereEqualTo("penerimaId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvDonasi.setText(String.valueOf(count));
                });
    }
//    private void getPengajuanCount(){
//        db.collection("Pengajuan")
//                .whereEqualTo("penerimaId", currentUserId)
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    int count = queryDocumentSnapshots.size();
//                    tvPengajuan.setText(String.valueOf(count));
//                });
//    }
}