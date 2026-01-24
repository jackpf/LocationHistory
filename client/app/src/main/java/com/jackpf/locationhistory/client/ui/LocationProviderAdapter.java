package com.jackpf.locationhistory.client.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.jackpf.locationhistory.client.R;

import java.util.Collections;
import java.util.List;

import lombok.Setter;

public class LocationProviderAdapter extends RecyclerView.Adapter<LocationProviderAdapter.ViewHolder> {

    private final List<LocationProviderItem> providers;
    @Setter
    private ItemTouchHelper itemTouchHelper;

    public LocationProviderAdapter(List<LocationProviderItem> providers) {
        this.providers = providers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_provider, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationProviderItem item = providers.get(position);

        holder.providerName.setText(item.getProviderName());
        holder.providerCheckbox.setChecked(item.isEnabled());

        holder.providerCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setEnabled(isChecked);
        });

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && itemTouchHelper != null) {
                itemTouchHelper.startDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    public List<LocationProviderItem> getProviders() {
        return providers;
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(providers, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(providers, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView dragHandle;
        final CheckBox providerCheckbox;
        final TextView providerName;

        ViewHolder(View itemView) {
            super(itemView);
            dragHandle = itemView.findViewById(R.id.dragHandle);
            providerCheckbox = itemView.findViewById(R.id.providerCheckbox);
            providerName = itemView.findViewById(R.id.providerName);
        }
    }

    public static class DragCallback extends ItemTouchHelper.Callback {
        private final LocationProviderAdapter adapter;

        public DragCallback(LocationProviderAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false; // We use the drag handle instead
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // Not used
        }
    }
}
