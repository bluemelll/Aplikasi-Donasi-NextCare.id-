package com.example.projectdonasi.Penerima;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.example.projectdonasi.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PenerimaChampaignFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private ArrayList<Map<String, String>> dataList = new ArrayList<>();
    private ArrayList<String> docIdList = new ArrayList<>();
    private ListView listViewDonasi;

    public PenerimaChampaignFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_penerima_champaign, container, false);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return view;
        }
        listViewDonasi = view.findViewById(R.id.listViewDonasi);

        loadData();
        return view;

    }
    private void loadData() {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "User ID not available. Cannot load data.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("Donasi")
                .whereEqualTo("penerimaId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    dataList.clear();
                    docIdList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String judul = doc.getString("judul");
                        String deskripsi = doc.getString("deskripsi");
                        String nominal_target = doc.getString("nominal_target");
                        String nominal_terkumpul = doc.getString("nominal_terkumpul");
                        String gambar = doc.getString("gambar");

                        if (judul != null && nominal_target != null && nominal_terkumpul != null) {
                            Map<String, String> item = new HashMap<>();
                            item.put("judul", judul);
                            item.put("deskripsi", deskripsi != null ? deskripsi : "");
                            item.put("nominal_terkumpul", nominal_terkumpul);
                            item.put("nominal_target", nominal_target);
                            item.put("gambar", (gambar != null && gambar.startsWith("http")) ? gambar : "");

                            try {
                                long targetVal = Long.parseLong(nominal_target);
                                long terkumpulVal = Long.parseLong(nominal_terkumpul);
                                int progress = (targetVal > 0) ? (int) ((terkumpulVal * 100) / targetVal) : 0;
                                item.put("progress", String.valueOf(Math.min(progress, 100)));
                            } catch (NumberFormatException e) {
                                item.put("progress", "0");
                            }

                            String tanggalTutup = doc.getString("tanggal_tutup");
                            String waktuTutup = doc.getString("waktu_tutup");
                            String sisaHariText = "Tidak diketahui";

                            if (tanggalTutup != null && waktuTutup != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                try {
                                    Date tutupDate = sdf.parse(tanggalTutup + " " + waktuTutup);
                                    long millisDiff = tutupDate.getTime() - new Date().getTime();
                                    long sisaHari = TimeUnit.MILLISECONDS.toDays(millisDiff);
                                    sisaHariText = (sisaHari < 0) ? "Berakhir" : sisaHari + " Hari";
                                } catch (ParseException e) {
                                    sisaHariText = "Tidak diketahui";
                                }
                            }

                            item.put("sisaHari", sisaHariText);
                            dataList.add(item);
                            docIdList.add(doc.getId());
                        }
                    }

                    if (dataList.isEmpty()) {
                        Toast.makeText(getContext(), "Tidak ada data donasi", Toast.LENGTH_SHORT).show();
                    }

                    SimpleAdapter adapter = new SimpleAdapter(
                            getContext(),
                            dataList,
                            R.layout.list_item_alldonasi,
                            new String[]{"judul", "deskripsi", "progress", "progress", "nominal_terkumpul", "nominal_target", "sisaHari", "gambar"},
                            new int[]{R.id.judul, R.id.deskripsi, R.id.progressBar, R.id.progressText, R.id.nominalTerkumpul, R.id.nominalTarget, R.id.sisaHari, R.id.imageDonasi}
                    );

                    adapter.setViewBinder((view, data, textRep) -> {
                        int viewId = view.getId();
                        if (viewId == R.id.progressBar && view instanceof ProgressBar) {
                            ((ProgressBar) view).setProgress(Integer.parseInt(data.toString()));
                            return true;
                        } else if (viewId == R.id.progressText && view instanceof TextView) {
                            ((TextView) view).setText(data + "%");
                            return true;
                        } else if ((viewId == R.id.nominalTerkumpul || viewId == R.id.nominalTarget) && view instanceof TextView) {
                            ((TextView) view).setText(formatRupiah(data.toString()));
                            return true;
                        } else if (viewId == R.id.imageDonasi && view instanceof ImageView) {
                            String url = data.toString();
                            if (!url.isEmpty()) {
                                Glide.with(this).load(url).into((ImageView) view);
                            }
                            return true;
                        }
                        return false;
                    });

                    listViewDonasi.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Gagal mengambil data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String formatRupiah(String nominal) {
        try {
            long value = Long.parseLong(nominal);
            NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return rupiahFormat.format(value);
        } catch (NumberFormatException e) {
            return "Rp 0";
        }
    }
}