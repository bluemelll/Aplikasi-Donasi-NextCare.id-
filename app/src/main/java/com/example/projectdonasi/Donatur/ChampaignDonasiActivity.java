package com.example.projectdonasi.Donatur;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.projectdonasi.R;
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

public class ChampaignDonasiActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ArrayList<Map<String, String>> dataList = new ArrayList<>();
    private ArrayList<String> docIdList = new ArrayList<>();
    private ListView listViewDonasi;
    private ImageButton buttonBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_champaign_donasi);

        db = FirebaseFirestore.getInstance();
        listViewDonasi = findViewById(R.id.listViewDonasi);
        buttonBack = findViewById(R.id.btnBack);

        loadData();

        listViewDonasi.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, String> selectedItem = dataList.get(position);
            String nominalTerkumpulStr = selectedItem.get("nominal_terkumpul");
            String nominalTargetStr = selectedItem.get("nominal_target");
            String sisaHariStr = selectedItem.get("sisaHari");
            String selectedDocId = docIdList.get(position);

            try {
                long nominalTerkumpul = Long.parseLong(nominalTerkumpulStr);
                long nominalTarget = Long.parseLong(nominalTargetStr);

                // Konversi sisaHariStr ke angka
                long sisaHari = 0;
                if (sisaHariStr != null) {
                    if (sisaHariStr.equalsIgnoreCase("Berakhir")) {
                        sisaHari = 0;
                    } else if (sisaHariStr.contains("Hari")) {
                        sisaHari = Long.parseLong(sisaHariStr.replace(" Hari", "").trim());
                    }
                }

                if (sisaHari <= 0) {
                    Toast.makeText(ChampaignDonasiActivity.this, "Donasi ini sudah berakhir!", Toast.LENGTH_SHORT).show();
                } else if (nominalTerkumpul >= nominalTarget) {
                    Toast.makeText(ChampaignDonasiActivity.this, "Donasi ini sudah mencapai target!", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(ChampaignDonasiActivity.this, FormDonasiActivity.class);
                    intent.putExtra("donasiId", selectedDocId);
                    intent.putExtra("penerimaId", selectedDocId);
                    startActivity(intent);
                }
            } catch (Exception e) {
                Toast.makeText(ChampaignDonasiActivity.this, "Error saat memproses data donasi.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonBack.setOnClickListener(v -> {
            finish();
        });

    }

    private void loadData() {
        db.collection("Donasi")
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
                        Toast.makeText(this, "Tidak ada data donasi", Toast.LENGTH_SHORT).show();
                    }

                    SimpleAdapter adapter = new SimpleAdapter(
                            this,
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
                        Toast.makeText(this, "Gagal mengambil data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
