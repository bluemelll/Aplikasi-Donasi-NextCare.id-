package com.example.projectdonasi.Donatur;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.projectdonasi.R;
import java.util.List;
import java.util.Map;

public class DonasiAdapter extends BaseAdapter{

    private Context context;
    private List<Map<String, String>> donasiList;

    public DonasiAdapter(Context context, List<Map<String, String>> donasiList) {
        this.context = context;
        this.donasiList = donasiList;
    }

    @Override
    public int getCount() {
        return donasiList.size();
    }

    @Override
    public Object getItem(int position) {
        return donasiList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_champaign, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.gambarDonasi);
        TextView textView = convertView.findViewById(R.id.judulDonasi);

        Map<String, String> item = donasiList.get(position);

        textView.setText(item.get("judul"));
        String gambarUrl = item.get("gambar");

        if (gambarUrl != null && !gambarUrl.isEmpty()) {
            Glide.with(context)
                    .load(gambarUrl)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.person_icon);
        }
        return convertView;
    }
}
