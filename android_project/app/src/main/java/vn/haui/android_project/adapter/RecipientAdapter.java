package vn.haui.android_project.adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserLocationEntity;

public class RecipientAdapter extends RecyclerView.Adapter<RecipientAdapter.RecipientViewHolder> {

    private final List<UserLocationEntity> locationList;
    private final Context context;
    private final OnLocationActionListener listener;

    public interface OnLocationActionListener {
        void onEditClick(UserLocationEntity location);
        void onSelectLocation(UserLocationEntity location);
    }

    public RecipientAdapter(List<UserLocationEntity> locationList, Context context, OnLocationActionListener listener) {
        this.locationList = locationList;
        this.context = context;
        this.listener = listener;
    }
    @NonNull
    @Override
    public RecipientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipient_address, parent, false);
        return new RecipientViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull RecipientViewHolder holder, int position) {
        UserLocationEntity location = locationList.get(position);
        holder.tvLocationType.setText(location.getLocationType());
        holder.tvFullAddress.setText(location.getAddress());
        String recipientInfo = location.getPhoneNumber();
        if (location.getRecipientName() != null && !location.getRecipientName().isEmpty()) {
            recipientInfo = location.getRecipientName() + " | " + location.getPhoneNumber();
        }
        holder.tvRecipientInfo.setText(recipientInfo);
        if (location.isDefaultLocation()) {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_selector_address_card);
            holder.imgEditIcon.setVisibility(View.VISIBLE);
        } else {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_normal_address_card);
            holder.imgEditIcon.setVisibility(View.VISIBLE);
        }
        if ("Home".equals(location.getLocationType())){
            holder.imgIconLocation.setImageResource(R.drawable.ic_marker_home);
        }else if ("Work".equals(location.getLocationType())){
            holder.imgIconLocation.setImageResource(R.drawable.ic_marker_work);
        }else {
            holder.imgIconLocation.setImageResource(R.drawable.ic_marker);
        }
    }
    @Override
    public int getItemCount() {
        return locationList.size();
    }
    public class RecipientViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvLocationType;
        public final TextView tvFullAddress;
        public final LinearLayout cardContainer;
        public final TextView tvRecipientInfo;
        public final ImageView imgEditIcon,imgIconLocation;
        public RecipientViewHolder(View itemView) {
            super(itemView);
            tvLocationType = itemView.findViewById(R.id.tv_location_type);
            tvFullAddress = itemView.findViewById(R.id.tv_full_address);
            cardContainer = itemView.findViewById(R.id.card_container);
            tvRecipientInfo = itemView.findViewById(R.id.tv_recipient_info);
            imgEditIcon = itemView.findViewById(R.id.img_edit_icon);
            imgIconLocation=itemView.findViewById(R.id.img_location_icon);
            imgEditIcon.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEditClick(locationList.get(position));
                }
            });
            cardContainer.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSelectLocation(locationList.get(position));
                }
            });
        }
    }
}