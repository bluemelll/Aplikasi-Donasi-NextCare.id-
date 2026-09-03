package com.example.projectdonasi.Donatur;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import android.content.Intent;
import android.util.Log;

import com.example.projectdonasi.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.Locale;

public class DonaturTransaksiFragment extends Fragment {

    private LinearLayout containerTransaksi;
    private FirebaseFirestore db;

    public DonaturTransaksiFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_donatur_transaksi, container, false);

        containerTransaksi = view.findViewById(R.id.containerTransaksi);
        db = FirebaseFirestore.getInstance();

        loadTransaksiData(inflater);

        return view;
    }

    private void loadTransaksiData(LayoutInflater inflater) {
        String userId = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            return;
        }

        db.collection("Transaksi")
                .whereEqualTo("donaturId", userId)
                .orderBy("tanggal_donasi", Query.Direction.DESCENDING)
                .orderBy("waktu_donasi", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        containerTransaksi.removeAllViews();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            View itemView = inflater.inflate(R.layout.list_item_transaksi, containerTransaksi, false);

                            TextView tvTanggal = itemView.findViewById(R.id.tvTanggalDonasi);
                            TextView tvWaktu = itemView.findViewById(R.id.tvWaktuDonasi);
                            TextView tvNominal = itemView.findViewById(R.id.tvNominalDonasi);
                            TextView tvMetode = itemView.findViewById(R.id.tvMetodePembayaran);
                            TextView tvJudul = itemView.findViewById(R.id.tvJudulDonasi);

                            String tanggalDonasi = doc.getString("tanggal_donasi");
                            String waktuDonasi = doc.getString("waktu_donasi");
                            String nominalDonasi = doc.getString("nominal");

                            if (nominalDonasi != null) {
                                try {
                                    double saldoDouble = Double.parseDouble(nominalDonasi);
                                    NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                                    String saldoFormatted = formatRupiah.format(saldoDouble);
                                    tvNominal.setText(saldoFormatted);
                                } catch (NumberFormatException e) {
                                    tvNominal.setText("Format Saldo Tidak Valid");
                                }
                            }

                            String metodePembayaran = doc.getString("metode_pembayaran");
                            tvMetode.setText("Metode: " + (metodePembayaran != null ? metodePembayaran : "Tidak diketahui"));

                            tvTanggal.setText(tanggalDonasi != null ? tanggalDonasi : "-");
                            tvWaktu.setText(waktuDonasi != null ? waktuDonasi : "-");

                            String donasiId = doc.getString("donasiId");
                            if (donasiId != null && !donasiId.isEmpty()) {
                                db.collection("Donasi").document(donasiId).get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                String judulDonasi = documentSnapshot.getString("judul");
                                                tvJudul.setText(judulDonasi != null ? judulDonasi : "Judul tidak tersedia");
                                            } else {
                                                tvJudul.setText("Judul tidak ditemukan");
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            tvJudul.setText("Gagal memuat judul");
                                        });
                            } else {
                                tvJudul.setText("Judul tidak tersedia");
                            }

                            containerTransaksi.addView(itemView);
                        }

                        if (task.getResult().isEmpty()) {
                            TextView empty = new TextView(requireContext());
                            empty.setText("Belum ada Transaksi");
                            empty.setTextSize(16);
                            empty.setGravity(Gravity.CENTER);
                            containerTransaksi.addView(empty);
                        }

                    } else {
                        Log.w("DonaturTransaksi", "Error getting documents.", task.getException());
                    }
                });
    }
}
