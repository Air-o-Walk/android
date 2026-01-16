package com.example.air_o_walk_sprint0;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter para mostrar notificaciones de incidencias
 */
public class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private List<NotificationIncidencia> lista;

    public NotificationsAdapter(List<NotificationIncidencia> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationIncidencia notif = lista.get(position);

        holder.tvTitle.setText(notif.title);
        holder.tvStatus.setText(notif.status);

        holder.tvResolution.setText(
                notif.resolution == null || notif.resolution.isEmpty()
                        ? "Sin respuesta"
                        : notif.resolution
        );

        // Color según estado
        if ("resuelta".equalsIgnoreCase(notif.status)) {
            holder.tvStatus.setTextColor(0xFF2E7D32); // verde
        } else if ("rechazada".equalsIgnoreCase(notif.status)) {
            holder.tvStatus.setTextColor(0xFFC62828); // rojo
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvStatus, tvResolution;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvStatus = itemView.findViewById(R.id.tvNotifStatus);
            tvResolution = itemView.findViewById(R.id.tvNotifResolution);
        }
    }
}
