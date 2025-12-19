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

/**
 * @class PremioAdapter
 * @brief Adaptador para mostrar la lista de premios canjeables en un RecyclerView.
 *
 * Esta clase se encarga de:
 * - Mostrar los premios disponibles con su información (nombre, descripción, puntos, stock)
 * - Gestionar el estado del botón "Canjear" según los puntos del usuario y disponibilidad
 * - Notificar clicks en los botones mediante un listener
 * - Actualizar dinámicamente la lista de premios y puntos del usuario
 *
 * @author Santiago Aguirre
 * @version 1.0
 */
public class PremioAdapter extends RecyclerView.Adapter<PremioAdapter.PremioViewHolder> {

    private List<Premio> listaPremios;
    private int puntosUsuario;
    private OnPremioClickListener listener;

    // -----------------------------------------------------------------
    // Constructor
    // Descripción: Inicializa el adaptador con los puntos del usuario.
    // Parámetros: puntosUsuario : puntos disponibles del usuario para canjear.
    // Diseño: puntosUsuario -> new PremioAdapter()
    // -----------------------------------------------------------------
    public PremioAdapter(int puntosUsuario) {
        this.listaPremios = new ArrayList<>();
        this.puntosUsuario = puntosUsuario;
    }

    // -----------------------------------------------------------------
    // Interface para manejar clicks en el botón canjear
    // -----------------------------------------------------------------
    public interface OnPremioClickListener {
        void onCanjearClick(Premio premio, int posicion);
    }

    // --------------------------------------------------------------
    // setOnPremioClickListener()
    // Descripción: Establece el listener para recibir eventos de click en los premios.
    // Parámetros: listener : callback que se ejecuta al pulsar "Canjear".
    // Diseño: listener -> setOnPremioClickListener()
    // --------------------------------------------------------------
    public void setOnPremioClickListener(OnPremioClickListener listener) {
        this.listener = listener;
    }

    // --------------------------------------------------------------
    // setPremios()
    // Descripción: Actualiza la lista de premios y notifica al RecyclerView para refrescar la vista.
    // Parámetros: premios : lista de objetos Premio a mostrar.
    // Diseño: List<Premio> -> setPremios() -> notifyDataSetChanged()
    // --------------------------------------------------------------
    public void setPremios(List<Premio> premios) {
        this.listaPremios = premios;
        notifyDataSetChanged();
    }

    // --------------------------------------------------------------
    // setPuntosUsuario()
    // Descripción: Actualiza los puntos del usuario y refresca la vista para actualizar
    //              el estado de los botones de canje.
    // Parámetros: puntos : puntos actuales del usuario.
    // Diseño: puntos -> setPuntosUsuario() -> notifyDataSetChanged()
    // --------------------------------------------------------------
    public void setPuntosUsuario(int puntos) {
        this.puntosUsuario = puntos;
        notifyDataSetChanged();
    }

    // --------------------------------------------------------------
    // onCreateViewHolder()
    // Descripción: Crea una nueva instancia de ViewHolder inflando el layout del item.
    // Parámetros: - parent : contenedor padre
    //             - viewType : tipo de vista (no usado en este caso)
    // Diseño: parent -> onCreateViewHolder() -> PremioViewHolder
    // --------------------------------------------------------------
    @NonNull
    @Override
    public PremioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_premio, parent, false);
        return new PremioViewHolder(view);
    }

    // --------------------------------------------------------------
    // onBindViewHolder()
    // Descripción: Vincula los datos de un premio con las vistas del ViewHolder.
    // Parámetros: - holder : ViewHolder que contiene las vistas
    //             - position : posición del item en la lista
    // Diseño: (holder, position) -> onBindViewHolder() -> bind()
    // --------------------------------------------------------------
    @Override
    public void onBindViewHolder(@NonNull PremioViewHolder holder, int position) {
        Premio premio = listaPremios.get(position);
        holder.bind(premio);
    }

    // --------------------------------------------------------------
    // getItemCount()
    // Descripción: Devuelve el número total de premios en la lista.
    // Diseño: getItemCount() -> int (tamaño de listaPremios)
    // --------------------------------------------------------------
    @Override
    public int getItemCount() {
        return listaPremios.size();
    }

    // -----------------------------------------------------------------
    // @class PremioViewHolder
    // @brief ViewHolder que representa cada item de premio en el RecyclerView.
    //
    // Contiene las referencias a las vistas del layout y la lógica
    // para vincular los datos del premio con dichas vistas.
    // -----------------------------------------------------------------
    class PremioViewHolder extends RecyclerView.ViewHolder {

        private TextView txtNombrePremio;
        private TextView txtDescripcionPremio;
        private TextView txtPuntosRequeridos;
        private TextView txtStock;
        private Button btnCanjear;

        // --------------------------------------------------------------
        // Constructor del ViewHolder
        // Descripción: Inicializa las referencias a las vistas del item.
        // Parámetros: itemView : vista del item inflada.
        // Diseño: itemView -> new PremioViewHolder()
        // --------------------------------------------------------------
        public PremioViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNombrePremio = itemView.findViewById(R.id.txtNombrePremio);
            txtDescripcionPremio = itemView.findViewById(R.id.txtDescripcionPremio);
            txtPuntosRequeridos = itemView.findViewById(R.id.txtPuntosRequeridos);
            txtStock = itemView.findViewById(R.id.txtStock);
            btnCanjear = itemView.findViewById(R.id.btnCanjear);
        }

        // --------------------------------------------------------------
        // bind()
        // Descripción: Vincula los datos de un premio con las vistas del item.
        //              Configura el texto, visibilidad y estado del botón según:
        //              - Los puntos del usuario
        //              - La disponibilidad del premio
        //              - El stock disponible
        // Parámetros: premio : objeto Premio con los datos a mostrar.
        // Diseño: Premio -> bind() -> actualización de vistas
        // --------------------------------------------------------------
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