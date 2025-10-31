package vn.haui.android_project.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import vn.haui.android_project.R;

public class AddressAdapter extends ArrayAdapter<String> {

    private final LayoutInflater inflater;
    private final List<String> addresses;

    public AddressAdapter(Context context, List<String> addresses) {
        super(context, 0, addresses);
        this.inflater = LayoutInflater.from(context);
        this.addresses = addresses;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_address, parent, false);
        }

        TextView tvAddress = convertView.findViewById(R.id.tvAddress);
        tvAddress.setText(addresses.get(position));

        return convertView;
    }
}

