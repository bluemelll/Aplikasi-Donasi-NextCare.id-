package com.example.projectdonasi.Admin;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.projectdonasi.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class AdminPersonFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewGroup container;
    private FirebaseFirestore db;

    private ArrayList<Map<String, Object>> userList = new ArrayList<>();
    private ListView listView;
    private UserArrayAdapter adapter;

    private String currentRole = "Penerima";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerFrag,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_person, containerFrag, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        container = view.findViewById(R.id.container);

        db = FirebaseFirestore.getInstance();

        listView = new ListView(requireContext());
        container.addView(listView);

        adapter = new UserArrayAdapter(requireContext(), userList);
        listView.setAdapter(adapter);

        setupTabs();

        fetchUsersByRole(currentRole);

        return view;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("PENERIMA"));
        tabLayout.addTab(tabLayout.newTab().setText("DONATUR"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentRole = (tab.getPosition() == 0) ? "Penerima" : "Donatur";
                fetchUsersByRole(currentRole);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void fetchUsersByRole(String role) {
        db.collection("User")
                .whereEqualTo("role", role)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        userList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            userList.add(doc.getData());
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private static class UserArrayAdapter extends ArrayAdapter<Map<String, Object>> {
        public UserArrayAdapter(@NonNull Context context, ArrayList<Map<String, Object>> users) {
            super(context, 0, users);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_penerima, parent, false);
            }

            CircleImageView imageProfile = convertView.findViewById(R.id.imageProfile);
            TextView textName = convertView.findViewById(R.id.textName);
            TextView textEmail = convertView.findViewById(R.id.textEmail);
            TextView textAlamat = convertView.findViewById(R.id.textAlamat);

            Map<String, Object> user = getItem(position);

            String nama = user.get("nama") != null ? user.get("nama").toString() : "-";
            String email = user.get("email") != null ? user.get("email").toString() : "-";
            String alamat = user.get("alamat") != null ? user.get("alamat").toString() : "-";
            String gambar = user.get("gambar") != null ? user.get("gambar").toString() : null;

            textName.setText(nama);
            textEmail.setText(email);
            textAlamat.setText(alamat);

            Glide.with(parent.getContext())
                    .load(gambar)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .into(imageProfile);

            return convertView;
        }
    }
}
