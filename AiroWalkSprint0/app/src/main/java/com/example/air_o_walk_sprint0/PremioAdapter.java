package com.example.air_o_walk_sprint0;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.example.air_o_walk_sprint0.R;

public class PremioAdapter extends RecyclerView.Adapter<PremioAdapter.PremioViewHolder> {

    private List<Premio> listaPremios;
    private int puntosUsuario;
    private OnPremioClickListener listener;

    // Constructor
    public PremioAdapter(int puntosUsuario) {
        this.listaPremios = new ArrayList<>();
        this.puntosUsuario = puntosUsuario;
    }

    // Interface para manejar clicks en el botón canjear
    public interface OnPremioClickListener {
        void onCanjearClick(Premio premio, int posicion);
    }

    // Setter para el listener
    public void setOnPremioClickListener(OnPremioClickListener listener) {
        this.listener = listener;
    }

    // Actualizar lista de premios
    public void setPremios(List<Premio> premios) {
        this.listaPremios = premios;
        notifyDataSetChanged();
    }

    // Actualizar puntos del usuario
    public void setPuntosUsuario(int puntos) {
        this.puntosUsuario = puntos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PremioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_premio, parent, false);
        return new PremioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PremioViewHolder holder, int position) {
        Premio premio = listaPremios.get(position);
        holder.bind(premio);
    }

    @Override
    public int getItemCount() {
        return listaPremios.size();
    }

    // ViewHolder
    class PremioViewHolder extends RecyclerView.ViewHolder {

        private TextView txtNombrePremio;
        private TextView txtDescripcionPremio;
        private TextView txtPuntosRequeridos;
        private TextView txtStock;
        private Button btnCanjear;

        public PremioViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombrePremio = itemView.findViewById(R.id.txtNombrePremio);
            txtDescripcionPremio = itemView.findViewById(R.id.txtDescripcionPremio);
            txtPuntosRequeridos = itemView.findViewById(R.id.txtPuntosRequeridos);
            txtStock = itemView.findViewById(R.id.txtStock);
            btnCanjear = itemView.findViewById(R.id.btnCanjear);
        }

        public void bind(Premio premio) {
            // Nombre del premio
            txtNombrePremio.setText(premio.getNombre());

            // Descripción (manejar null)
            if (premio.getDescripcion() != null && !premio.getDescripcion().isEmpty()) {
                txtDescripcionPremio.setVisibility(View.VISIBLE);
                txtDescripcionPremio.setText(premio.getDescripcion());
            } else {
                txtDescripcionPremio.setVisibility(View.GONE);
            }

            // Puntos requeridos
            txtPuntosRequeridos.setText(premio.getPuntosRequeridos() + " pts");

            // Stock
            txtStock.setText("Stock: " + premio.getCantidadDisponible());

            // Determinar si el botón debe estar habilitado
            boolean puedeCanjar = puntosUsuario >= premio.getPuntosRequeridos()
                    && premio.estaDisponible();

            btnCanjear.setEnabled(puedeCanjar);

            // Cambiar apariencia del botón según disponibilidad
            if (puedeCanjar) {
                btnCanjear.setText("Canjear");
                btnCanjear.setBackgroundTintList(
                        itemView.getContext().getColorStateList(android.R.color.holo_green_dark)
                );
            } else if (puntosUsuario < premio.getPuntosRequeridos()) {
                btnCanjear.setText("Insuficiente");
                btnCanjear.setBackgroundTintList(
                        itemView.getContext().getColorStateList(android.R.color.darker_gray)
                );
            } else {
                btnCanjear.setText("No disponible");
                btnCanjear.setBackgroundTintList(
                        itemView.getContext().getColorStateList(android.R.color.darker_gray)
                );
            }

            // Click listener del botón
            btnCanjear.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCanjearClick(premio, getAdapterPosition());
                }
            });
        }
    }
}