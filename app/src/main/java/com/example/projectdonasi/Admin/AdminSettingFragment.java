package com.example.projectdonasi.Admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.projectdonasi.LoginActivity;
import com.example.projectdonasi.R;
import com.example.projectdonasi.UserProfileActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.projectdonasi.NavigationVisibilityListener;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;

public class AdminSettingFragment extends Fragment {
    private RelativeLayout profileUser, transaksi, aboutUs, signout;
    private CircleImageView profileIcon;
    private TextView namaUser;
    private FrameLayout aboutUsContainer;
    private View aboutUsView;
    private LinearLayout mainLayout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private NavigationVisibilityListener navigationVisibilityListener;

    public AdminSettingFragment() {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_setting, container, false);
        profileUser = view.findViewById(R.id.profileUser);
        profileUser.setOnClickListener(v -> {
            Intent profile = new Intent(getActivity(), UserProfileActivity.class);
            startActivity(profile);
        });

        namaUser = view.findViewById(R.id.namaUser);

        profileIcon = view.findViewById(R.id.profile_icon);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfileImage();

        transaksi = view.findViewById(R.id.transaksiUser);
        transaksi.setOnClickListener(v -> {
            Intent trans = new Intent(getActivity(), AdminTransaksiActivity.class);
            startActivity(trans);
        });


        mainLayout = view.findViewById(R.id.mainLayout);
        aboutUsContainer = view.findViewById(R.id.aboutUsContainer);

        aboutUs = view.findViewById(R.id.aboutus);
        aboutUs.setOnClickListener(v ->  {
            showAboutUs();
        });

        signout = view.findViewById(R.id.signoutUser);
        signout.setOnClickListener(v -> {
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
        return view;
    }

    private void loadUserProfileImage() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("User").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nama = documentSnapshot.getString("nama");
                            String imageUrl = documentSnapshot.getString("gambar");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.person_icon)
                                        .error(R.drawable.person_icon)
                                        .into(profileIcon);
                            }

                            if (nama != null && !nama.isEmpty()) {
                                namaUser.setText(nama);
                            }
                        }
                    });
        }
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void showAboutUs() {
        if (aboutUsView == null) {
            aboutUsView = getLayoutInflater().inflate(R.layout.activity_about_us, aboutUsContainer, false);
            MaterialButton backButton = aboutUsView.findViewById(R.id.backButton);
            backButton.setOnClickListener(v -> {
                aboutUsContainer.setVisibility(View.GONE);
                mainLayout.setVisibility(View.VISIBLE);
                if (navigationVisibilityListener != null) {
                    navigationVisibilityListener.setNavigationBarVisibility(View.VISIBLE);
                }
            });
            aboutUsContainer.addView(aboutUsView);
        }
        mainLayout.setVisibility(View.GONE);
        aboutUsContainer.setVisibility(View.VISIBLE);
        if (navigationVisibilityListener != null) {
            navigationVisibilityListener.setNavigationBarVisibility(View.GONE);
        }
    }
}