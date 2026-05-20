package com.example.topmejorestiendas.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.topmejorestiendas.R;
import com.example.topmejorestiendas.model.Negocio;
import java.io.File;
import java.util.List;

public class TopNegociosAdapter extends RecyclerView.Adapter<TopNegociosAdapter.ViewHolder> {
    private List<Negocio> negocios;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Negocio negocio);
    }

    public TopNegociosAdapter(List<Negocio> negocios, OnItemClickListener listener) {
        this.negocios = negocios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_negocio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Negocio n = negocios.get(position);
        holder.tvNombre.setText(n.nombreNegocio);
        holder.tvCalificacion.setText("⭐ " + n.calificacionPromedio);

        if (n.fotoNegocio != null) {
            Glide.with(holder.itemView.getContext())
                    .load(new File(n.fotoNegocio))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivFoto);
        } else {
            holder.ivFoto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(n));
    }

    @Override
    public int getItemCount() {
        return negocios.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCalificacion;
        ImageView ivFoto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreNegocio);
            tvCalificacion = itemView.findViewById(R.id.tvCalificacion);
            ivFoto = itemView.findViewById(R.id.ivNegocio);
        }
    }
}