package com.example.projectdonasi.Admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.projectdonasi.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminHomeFragment extends Fragment {

    private TextView tvPenerima, tvDonatur, tvDonasi, tvTransaksi;
    private FirebaseFirestore db;

    public AdminHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        db = FirebaseFirestore.getInstance();
        tvPenerima = view.findViewById(R.id.penerimaDonasi);
        tvDonatur = view.findViewById(R.id.donatur);
        tvDonasi = view.findViewById(R.id.champaign);
        tvTransaksi = view.findViewById(R.id.transaksiDonatur);

        getPenerimaCount();
        getDonaturCount();
        getDonasiCount();
        getTransaksiCount();

        return view;
    }

    private void getPenerimaCount() {
        db.collection("User")
                .whereEqualTo("role", "Penerima")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvPenerima.setText(String.valueOf(count));
                });
    }

    private void getDonaturCount() {
        db.collection("User")
                .whereEqualTo("role", "Donatur")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvDonatur.setText(String.valueOf(count));
                });
    }

    private void getDonasiCount() {
        db.collection("Donasi")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvDonasi.setText(String.valueOf(count));
                });
    }

    private void getTransaksiCount() {
        db.collection("Transaksi")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvTransaksi.setText(String.valueOf(count));
                });
    }
}
