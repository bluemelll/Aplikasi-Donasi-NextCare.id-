package com.example.projectdonasi.Admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView; // Import SearchView
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.projectdonasi.Connecter.CloudinaryHelper;
import com.example.projectdonasi.NavigationVisibilityListener;
import com.example.projectdonasi.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Import Query
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminDonationFragment extends Fragment {

    private ListView listViewDonasi;
    private FloatingActionButton fabAddDonasi;
    private FrameLayout containerForm;
    private View formView;
    private NavigationVisibilityListener navigationVisibilityListener;
    private LinearLayout layoutLogo;

    private ArrayList<Map<String, String>> dataList = new ArrayList<>();
    private ArrayList<String> docIdList = new ArrayList<>();
    private FirebaseFirestore db;

    private EditText etJudul, etDeskripsi, etTarget, etTerkumpul, etStatus;
    private Button btnTanggalBuka, btnWaktuBuka, btnTanggalTutup, btnWaktuTutup, btnSave, btnCancel;
    private TextView tvNamaGambar;
    private Button btnChooseFile;
    private Spinner spinnerPenerima;
    private SearchView searchViewDonasi;

    private ArrayList<String> penerimaList = new ArrayList<>();
    private ArrayList<String> penerimaIdList = new ArrayList<>();

    private String currentDocId = null;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri gambarDonasiUri = null;
    private String currentImageUrl = null;


    public AdminDonationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationVisibilityListener) {
            navigationVisibilityListener = (NavigationVisibilityListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement NavigationVisibilityListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_donation, container, false);

        db = FirebaseFirestore.getInstance();
        listViewDonasi = view.findViewById(R.id.listViewDonasi);
        fabAddDonasi = view.findViewById(R.id.AddDonasi);
        containerForm = view.findViewById(R.id.containerForm);
        searchViewDonasi = view.findViewById(R.id.searchViewDonasi);
        layoutLogo = view.findViewById(R.id.layoutlogo);

        loadData(null);

        fabAddDonasi.setOnClickListener(v -> {
            currentDocId = null;
            showForm(null);
        });

        listViewDonasi.setOnItemClickListener((parent, view1, position, id) -> {
            currentDocId = docIdList.get(position);
            db.collection("Donasi").document(currentDocId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> firebaseData = documentSnapshot.getData();
                            Map<String, String> itemData = new HashMap<>();
                            if (firebaseData != null) {
                                for (Map.Entry<String, Object> entry : firebaseData.entrySet()) {
                                    itemData.put(entry.getKey(), String.valueOf(entry.getValue()));
                                }
                            }
                            showForm(itemData);
                        } else {
                            Toast.makeText(getContext(), "Data tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal memuat data donasi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });


        listViewDonasi.setOnItemLongClickListener((parent, view1, position, id) -> {
            String docIdToDelete = docIdList.get(position);
            new AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Data")
                    .setMessage("Apakah Anda yakin ingin menghapus data donasi ini?")
                    .setPositiveButton("Hapus", (dialog, which) -> {
                        db.collection("Donasi").document(docIdToDelete)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Data berhasil dihapus", Toast.LENGTH_SHORT).show();
                                    loadData(null); // Reload data after deletion
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal menghapus data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Batal", null)
                    .show();
            return true;
        });

        searchViewDonasi.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadData(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadData(null);
                } else {
                    loadData(newText);
                }
                return false;
            }
        });

        return view;
    }

    private void showForm(@Nullable Map<String, String> data) {
        containerForm.setVisibility(View.VISIBLE);
        listViewDonasi.setVisibility(View.GONE);
        fabAddDonasi.setVisibility(View.GONE);
        layoutLogo.setVisibility(View.GONE);
        searchViewDonasi.setVisibility(View.GONE);
        if (navigationVisibilityListener != null) {
            navigationVisibilityListener.setNavigationBarVisibility(View.GONE);
        }

        if (formView == null) {
            formView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_donasi_form, containerForm, false);
            containerForm.addView(formView);

            etJudul = formView.findViewById(R.id.etJudul);
            etDeskripsi = formView.findViewById(R.id.etDeskripsi);
            etTarget = formView.findViewById(R.id.etNominalTarget);
            etTerkumpul = formView.findViewById(R.id.etNominalTerkumpul);
            etStatus = formView.findViewById(R.id.etStatus);

            btnTanggalBuka = formView.findViewById(R.id.btnTanggalBuka);
            btnWaktuBuka = formView.findViewById(R.id.btnWaktuBuka);
            btnTanggalTutup = formView.findViewById(R.id.btnTanggalTutup);
            btnWaktuTutup = formView.findViewById(R.id.btnWaktuTutup);

            btnSave = formView.findViewById(R.id.btnSave);
            btnCancel = formView.findViewById(R.id.btnCancel);
            btnChooseFile = formView.findViewById(R.id.btnChooseFile);
            tvNamaGambar = formView.findViewById(R.id.tvNamaGambar);

            spinnerPenerima = formView.findViewById(R.id.spinnerRoleUser);
            loadPenerimaToSpinner();

            btnCancel.setOnClickListener(v -> hideForm());
            btnSave.setOnClickListener(v -> saveDonasi());

            btnTanggalBuka.setOnClickListener(v -> showDatePicker(btnTanggalBuka));
            btnWaktuBuka.setOnClickListener(v -> showTimePicker(btnWaktuBuka));
            btnTanggalTutup.setOnClickListener(v -> showDatePicker(btnTanggalTutup));
            btnWaktuTutup.setOnClickListener(v -> showTimePicker(btnWaktuTutup));
            btnChooseFile.setOnClickListener(v -> openImageChooser());
        }

        if (data != null) {
            etJudul.setText(data.get("judul"));
            etDeskripsi.setText(data.get("deskripsi"));
            etTarget.setText(data.get("nominal_target"));
            etTerkumpul.setText(data.get("nominal_terkumpul"));
            etStatus.setText(data.get("status"));
            btnTanggalBuka.setText(data.get("tanggal_buka") != null && !data.get("tanggal_buka").equals("null") ? data.get("tanggal_buka") : "Pilih tanggal buka");
            btnWaktuBuka.setText(data.get("waktu_buka") != null && !data.get("waktu_buka").equals("null") ? data.get("waktu_buka") : "Pilih waktu buka");
            btnTanggalTutup.setText(data.get("tanggal_tutup") != null && !data.get("tanggal_tutup").equals("null") ? data.get("tanggal_tutup") : "Pilih tanggal tutup");
            btnWaktuTutup.setText(data.get("waktu_tutup") != null && !data.get("waktu_tutup").equals("null") ? data.get("waktu_tutup") : "Pilih waktu tutup");

            currentImageUrl = data.get("gambar");
            if (currentImageUrl != null && !currentImageUrl.isEmpty() && !currentImageUrl.equals("null")) {
                tvNamaGambar.setText("Gambar terlampir (ubah jika ingin ganti)");
            } else {
                tvNamaGambar.setText("Belum ada gambar dipilih");
            }
            gambarDonasiUri = null;

            String penerimaId = data.get("penerimaId");
            if (penerimaId != null) {
                int pos = penerimaIdList.indexOf(penerimaId);
                if (pos >= 0) {
                    spinnerPenerima.setSelection(pos);
                } else {
                    if (spinnerPenerima.getAdapter() != null && spinnerPenerima.getAdapter().getCount() > 0) {
                        spinnerPenerima.setSelection(0);
                    }
                }
            } else {
                if (spinnerPenerima.getAdapter() != null && spinnerPenerima.getAdapter().getCount() > 0) {
                    spinnerPenerima.setSelection(0);
                }
            }
        } else {
            etJudul.setText("");
            etDeskripsi.setText("");
            etTarget.setText("");
            etTerkumpul.setText("");
            etStatus.setText("");
            btnTanggalBuka.setText("Pilih tanggal buka");
            btnWaktuBuka.setText("Pilih waktu buka");
            btnTanggalTutup.setText("Pilih tanggal tutup");
            btnWaktuTutup.setText("Pilih waktu tutup");
            tvNamaGambar.setText("Belum ada gambar dipilih");
            gambarDonasiUri = null;
            currentImageUrl = null;
            if (spinnerPenerima.getAdapter() != null && spinnerPenerima.getAdapter().getCount() > 0) {
                spinnerPenerima.setSelection(0);
            }
        }
    }

    private void hideForm() {
        containerForm.setVisibility(View.GONE);
        layoutLogo.setVisibility(View.VISIBLE); // Tampilkan logo
        listViewDonasi.setVisibility(View.VISIBLE);
        fabAddDonasi.setVisibility(View.VISIBLE);
        searchViewDonasi.setVisibility(View.VISIBLE);
        if (navigationVisibilityListener != null) {
            navigationVisibilityListener.setNavigationBarVisibility(View.VISIBLE);
        }
        currentDocId = null;
    }

    private void saveDonasi() {
        int selectedPos = spinnerPenerima.getSelectedItemPosition();
        if (selectedPos <= 0 || selectedPos >= penerimaIdList.size()) {
            Toast.makeText(getContext(), "Pilih penerima dulu!", Toast.LENGTH_SHORT).show();
            return;
        }
        String penerimaId = penerimaIdList.get(selectedPos);
        String judul = etJudul.getText().toString().trim();
        String deskripsi = etDeskripsi.getText().toString().trim();
        String target = etTarget.getText().toString().trim();
        String terkumpul = etTerkumpul.getText().toString().trim();
        String status = etStatus.getText().toString().trim();
        String tanggalBuka = btnTanggalBuka.getText().toString().trim();
        String waktuBuka = btnWaktuBuka.getText().toString().trim();
        String tanggalTutup = btnTanggalTutup.getText().toString().trim();
        String waktuTutup = btnWaktuTutup.getText().toString().trim();

        if (judul.isEmpty() || deskripsi.isEmpty() || target.isEmpty() || terkumpul.isEmpty() || status.isEmpty() ||
                tanggalBuka.equals("Pilih tanggal buka") || tanggalTutup.equals("Pilih tanggal tutup") ||
                waktuBuka.equals("Pilih waktu buka") || waktuTutup.equals("Pilih waktu tutup")) {
            Toast.makeText(getContext(), "Semua field harus diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Long.parseLong(target);
            Long.parseLong(terkumpul);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Nominal Target dan Nominal Terkumpul harus angka!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gambarDonasiUri != null) {
            Toast.makeText(getContext(), "Mengunggah gambar...", Toast.LENGTH_SHORT).show();
            CloudinaryHelper.uploadImage(requireContext(), gambarDonasiUri, new CloudinaryHelper.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    saveDonasiData(judul, deskripsi, target, terkumpul, status, tanggalBuka, tanggalTutup, waktuBuka, waktuTutup, penerimaId, imageUrl);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getContext(), "Gagal mengunggah gambar: " + errorMessage, Toast.LENGTH_LONG).show();
                    saveDonasiData(judul, deskripsi, target, terkumpul, status, tanggalBuka, tanggalTutup, waktuBuka, waktuTutup, penerimaId, currentImageUrl);
                }
            });
        } else {
            saveDonasiData(judul, deskripsi, target, terkumpul, status, tanggalBuka, tanggalTutup, waktuBuka, waktuTutup, penerimaId, currentImageUrl);
        }
    }

    private void saveDonasiData(String judul, String deskripsi, String target, String terkumpul, String status,
                                String tglBuka, String tglTutup, String waktuBuka, String waktuTutup, String penerimaId, @Nullable String imageUrl) {
        Map<String, Object> donasi = new HashMap<>();
        donasi.put("judul", judul);
        donasi.put("deskripsi", deskripsi);
        donasi.put("nominal_target", target);
        donasi.put("nominal_terkumpul", terkumpul);
        donasi.put("status", status);
        donasi.put("tanggal_buka", tglBuka);
        donasi.put("waktu_buka", waktuBuka);
        donasi.put("tanggal_tutup", tglTutup);
        donasi.put("waktu_tutup", waktuTutup);
        donasi.put("penerimaId", penerimaId);
        donasi.put("gambar", imageUrl != null ? imageUrl : "");

        if (currentDocId == null) {
            db.collection("Donasi").add(donasi).addOnSuccessListener(doc -> {
                Toast.makeText(getContext(), "Donasi ditambahkan", Toast.LENGTH_SHORT).show();
                hideForm();
                loadData(null); // Reload all data after adding
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal menambahkan data donasi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            db.collection("Donasi").document(currentDocId).update(donasi).addOnSuccessListener(unused -> {
                Toast.makeText(getContext(), "Donasi diperbarui", Toast.LENGTH_SHORT).show();
                hideForm();
                loadData(null); // Reload all data after updating
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal mengupdate data donasi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }


    private void loadData(@Nullable String searchQuery) {
        Query donasiQuery = db.collection("Donasi");

        if (searchQuery != null && !searchQuery.isEmpty()) {
            donasiQuery.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        dataList.clear();
                        docIdList.clear();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String judul = doc.getString("judul");
                            String deskripsi = doc.getString("deskripsi");
                            String nominal_target = doc.getString("nominal_target");
                            String nominal_terkumpul= doc.getString("nominal_terkumpul");

                            if (judul != null && judul.toLowerCase().contains(searchQuery.toLowerCase())) {
                                if (deskripsi != null && nominal_target != null && nominal_terkumpul != null) {
                                    Map<String, String> item = new HashMap<>();
                                    item.put("judul", judul);
                                    item.put("deskripsi", deskripsi);
                                    item.put("nominal_terkumpul", nominal_terkumpul);
                                    item.put("nominal_target", nominal_target);

                                    try {
                                        long targetVal = Long.parseLong(nominal_target);
                                        long terkumpulVal = Long.parseLong(nominal_terkumpul);

                                        int progress = (targetVal > 0) ? (int) ((terkumpulVal * 100) / targetVal) : 0;
                                        if (progress > 100) progress = 100;
                                        item.put("progress", String.valueOf(progress));
                                    } catch (NumberFormatException e) {
                                        item.put("progress", "0");
                                    }
                                    dataList.add(item);
                                    docIdList.add(doc.getId());

                                }
                            }
                        }
                        updateListView();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal mengambil data donasi: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } else {
            donasiQuery.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        dataList.clear();
                        docIdList.clear();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String judul = doc.getString("judul");
                            String deskripsi = doc.getString("deskripsi");
                            String nominal_target = doc.getString("nominal_target");
                            String nominal_terkumpul= doc.getString("nominal_terkumpul");

                            if (judul != null && deskripsi != null && nominal_target != null && nominal_terkumpul != null) {
                                Map<String, String> item = new HashMap<>();
                                item.put("judul", judul);
                                item.put("deskripsi", deskripsi);
                                item.put("nominal_terkumpul", nominal_terkumpul);
                                item.put("nominal_target", nominal_target);

                                try {
                                    long targetVal = Long.parseLong(nominal_target);
                                    long terkumpulVal = Long.parseLong(nominal_terkumpul);

                                    int progress = (targetVal > 0) ? (int) ((terkumpulVal * 100) / targetVal) : 0;
                                    if (progress > 100) progress = 100;
                                    item.put("progress", String.valueOf(progress));
                                } catch (NumberFormatException e) {
                                    item.put("progress", "0");
                                }
                                dataList.add(item);
                                docIdList.add(doc.getId());
                            }
                        }
                        updateListView();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal mengambil data donasi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void updateListView() {
        SimpleAdapter adapter = new SimpleAdapter(
                requireContext(),
                dataList,
                R.layout.list_item_donasi,
                new String[]{"judul", "deskripsi", "progress", "progress", "nominal_terkumpul", "nominal_target"},
                new int[]{R.id.judul, R.id.deskripsi, R.id.progressBar, R.id.progressText, R.id.nominalTerkumpul, R.id.nominalTarget}
        );

        adapter.setViewBinder((view, data, textRepresentation) -> {
            if (view.getId() == R.id.progressBar && view instanceof ProgressBar) {
                int progress = Integer.parseInt(String.valueOf(data));
                ((ProgressBar) view).setProgress(progress);
                return true;
            } else if (view.getId() == R.id.progressText && view instanceof TextView) {
                ((TextView) view).setText(String.valueOf(data) + "%");
                return true;
            } else if (view.getId() == R.id.nominalTerkumpul && view instanceof TextView) {
                String formatted = formatRupiah(String.valueOf(data));
                ((TextView) view).setText(formatted);
                return true;
            } else if (view.getId() == R.id.nominalTarget && view instanceof TextView) {
                String formatted = formatRupiah(String.valueOf(data));
                ((TextView) view).setText(formatted);
                return true;
            }
            return false;
        });

        listViewDonasi.setAdapter(adapter);

        if (dataList.isEmpty()) {
            Toast.makeText(getContext(), "Tidak ada data donasi", Toast.LENGTH_SHORT).show();
        }
    }


    private String formatRupiah(String nominal) {
        try {
            long nominalLong = Long.parseLong(nominal);
            NumberFormat formatter = NumberFormat.getInstance(new Locale("id", "ID"));
            return "Rp " + formatter.format(nominalLong);
        } catch (NumberFormatException e) {
            return "Rp 0";
        }
    }

    private void showDatePicker(Button targetButton) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            targetButton.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(Button targetButton) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            String time = String.format("%02d:%02d", hourOfDay, minute);
            targetButton.setText(time);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            gambarDonasiUri = data.getData();
            String fileName = getFileName(gambarDonasiUri);
            tvNamaGambar.setText(fileName);
            Toast.makeText(getContext(), "File dipilih: " + fileName, Toast.LENGTH_SHORT).show();
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_CANCELED) {
            if (currentDocId != null && currentImageUrl != null && !currentImageUrl.isEmpty() && !currentImageUrl.equals("null")) {
                tvNamaGambar.setText("Gambar terlampir (ubah jika ingin ganti)");
            } else {
                tvNamaGambar.setText("Belum ada gambar dipilih");
            }
            gambarDonasiUri = null;
            Toast.makeText(getContext(), "Pemilihan file dibatalkan", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private void loadPenerimaToSpinner() {
        db.collection("User")
                .whereEqualTo("role", "Penerima")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    penerimaList.clear();
                    penerimaIdList.clear();

                    penerimaList.add("-- Pilih Penerima --");
                    penerimaIdList.add("");

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String nama = doc.getString("nama");
                        String id = doc.getId();
                        if (nama != null) {
                            penerimaList.add(nama);
                            penerimaIdList.add(id);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            penerimaList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerPenerima.setAdapter(adapter);

                    if (penerimaList.size() <= 1) {
                        Toast.makeText(getContext(), "Tidak ada data penerima yang tersedia.", Toast.LENGTH_SHORT).show();
                        spinnerPenerima.setEnabled(false);
                    } else {
                        spinnerPenerima.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal memuat data penerima: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    ArrayAdapter<String> errorAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new String[]{"Error memuat penerima"});
                    spinnerPenerima.setAdapter(errorAdapter);
                    spinnerPenerima.setEnabled(false);
                });
    }
}