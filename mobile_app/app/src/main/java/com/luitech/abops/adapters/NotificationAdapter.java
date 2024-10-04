package com.luitech.abops.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.luitech.abops.NotificationModel;
import com.luitech.abops.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationModel> notifications;

    public NotificationAdapter(List<NotificationModel> notifications) {
        this.notifications = notifications;
        setHasStableIds(true); // Enable stable IDs
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notifications.get(position);
        holder.headingTextView.setText(notification.getBoardId()); // Use 'boardId' as heading
        holder.messageTextView.setText(notification.getText()); // Use 'text' as message
        holder.timestampTextView.setText(notification.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    @Override
    public long getItemId(int position) {
        return notifications.get(position).getId(); // Assuming NotificationModel has a unique ID
    }

    // Method to update notifications


    public void updateNotifications(List<NotificationModel> newNotifications) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NotificationDiffCallback(this.notifications, newNotifications));

        this.notifications.clear();
        this.notifications.addAll(newNotifications);
        diffResult.dispatchUpdatesTo(this);
    }


    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView headingTextView;
        TextView messageTextView;
        TextView timestampTextView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            headingTextView = itemView.findViewById(R.id.notificationHeading);
            messageTextView = itemView.findViewById(R.id.notificationMessage);
            timestampTextView = itemView.findViewById(R.id.notificationTimestamp);
        }
    }

    // DiffUtil Callback class
    private static class NotificationDiffCallback extends DiffUtil.Callback {

        private final List<NotificationModel> oldList;
        private final List<NotificationModel> newList;

        public NotificationDiffCallback(List<NotificationModel> oldList, List<NotificationModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId(); // Compare IDs
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition)); // Compare item content
        }
    }
}
