package com.example.projectdonasi.Donatur;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.projectdonasi.MainActivity;
import com.example.projectdonasi.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.widget.GridView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class DonaturHomeFragment extends Fragment {

    private CircleImageView profileUser;
    private TextView namaUserTextView, tglSignUpTextView, saldoUserTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageButton buttonTopUp, buttonDonasi, buttonOut, buttonSeeAll;
    private GridView gridViewDonasi;
    private DonasiAdapter donasiAdapter;
    private List<Map<String, String>> donasiList;


    public DonaturHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_donatur_home, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        namaUserTextView = view.findViewById(R.id.namaUser);
        tglSignUpTextView = view.findViewById(R.id.tglSignUp);
        saldoUserTextView = view.findViewById(R.id.saldoUser);
        profileUser = view.findViewById(R.id.imageProfile);
        gridViewDonasi = view.findViewById(R.id.gridViewDonasi);

        buttonTopUp = view.findViewById(R.id.topupSaldo);
        buttonDonasi = view.findViewById(R.id.donasiUser);
        buttonSeeAll = view.findViewById(R.id.btnSeeAll);
        buttonOut = view.findViewById(R.id.signOutUser);

        buttonTopUp.setOnClickListener(v -> {
            topUpDialog();
        });

        buttonDonasi.setOnClickListener(v -> {
            Intent champaign = new Intent(getActivity(), ChampaignDonasiActivity.class);
            startActivity(champaign);
        });

        buttonSeeAll.setOnClickListener(v -> {
            Intent champaign = new Intent(getActivity(), ChampaignDonasiActivity.class);
            startActivity(champaign);
        });

        buttonOut.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Signout Account")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        logout();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        });

        donasiList = new ArrayList<>();
        donasiAdapter = new DonasiAdapter(getContext(), donasiList);
        gridViewDonasi.setAdapter(donasiAdapter);

        loadData();

        db.collection("Donasi")
                .limit(4)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    donasiList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "Data donasi kosong", Toast.LENGTH_SHORT).show();
                    }
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Map<String, String> donasiItem = new HashMap<>();
                        donasiItem.put("docId", document.getId());
                        donasiItem.put("judul", document.getString("judul"));
                        donasiItem.put("nominal_terkumpul", document.getString("nominal_terkumpul"));
                        donasiItem.put("nominal_target", document.getString("nominal_target"));
                        donasiItem.put("sisa_hari", document.getString("sisa_hari"));
                        donasiItem.put("penerimaId", document.getString("penerimaId"));

                        String gambar = document.getString("gambar");
                        if (gambar != null && gambar.startsWith("http")) {
                            donasiItem.put("gambar", gambar);
                        } else {
                            donasiItem.put("gambar", "");
                        }

                        donasiList.add(donasiItem);
                    }
                    donasiAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal mengambil data donasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                            String saldo = documentSnapshot.getString("saldo");
                            if (saldo != null) {
                                try {
                                    double saldoDouble = Double.parseDouble(saldo);
                                    NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                                    String saldoFormatted = formatRupiah.format(saldoDouble);
                                    saldoUserTextView.setText(saldoFormatted.replace("Rp", "Rp ").trim());
                                } catch (NumberFormatException e) {
                                    saldoUserTextView.setText("Format Saldo Tidak Valid");
                                }
                            }

                            String gambar = documentSnapshot.getString("gambar");
                            if (gambar != null && !gambar.isEmpty()) {
                                loadProfileImage(gambar);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Gagal mengambil data pengguna", Toast.LENGTH_SHORT).show();
                        namaUserTextView.setText("Gagal ambil data");
                        saldoUserTextView.setText("Gagal ambil data");
                    });

            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            String tanggalBergabung = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"))
                    .format(new Date(creationTimestamp));
            tglSignUpTextView.setText("Bergabung " + tanggalBergabung);
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

    private void topUpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Top Up Saldo");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Masukkan jumlah saldo");
        builder.setView(input);

        input.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_box));
        input.setPadding(40, 30, 40, 30);
        input.setTextSize(16);
        input.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        input.setHintTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));

        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Tambah", (dialog, which) -> {
            String inputText = input.getText().toString().trim();

            if (!inputText.isEmpty()) {
                long jumlahTopUp = Long.parseLong(inputText);

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    String uid = user.getUid();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    DocumentReference userRef = db.collection("User").document(uid);
                    userRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String saldoStr = documentSnapshot.getString("saldo");
                            long saldoSaatIni = 0L;
                            try {
                                if (saldoStr != null) {
                                    saldoSaatIni = Long.parseLong(saldoStr);
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(requireContext(), "Saldo saat ini tidak valid", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            long saldoBaru = saldoSaatIni + jumlahTopUp;

                            userRef.update("saldo", String.valueOf(saldoBaru))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(requireContext(), "Saldo berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                                        loadData();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Gagal memperbarui saldo", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Gagal mengambil data pengguna", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                Toast.makeText(requireContext(), "Jumlah saldo tidak boleh kosong", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }
}
