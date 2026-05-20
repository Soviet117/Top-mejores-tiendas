package com.example.topmejorestiendas.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.topmejorestiendas.R;
import com.example.topmejorestiendas.model.Resena;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResenaAdapter extends RecyclerView.Adapter<ResenaAdapter.ViewHolder> {
    private List<Resena> resenas;

    public ResenaAdapter(List<Resena> resenas) {
        this.resenas = resenas;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resena, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Resena r = resenas.get(position);
        holder.tvComentario.setText(r.comentario);
        holder.tvCalificacion.setText("⭐ " + r.calificacion + "/5");
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.tvFecha.setText(sdf.format(new Date(r.fecha)));
    }

    @Override
    public int getItemCount() {
        return resenas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvComentario, tvCalificacion, tvFecha;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvComentario = itemView.findViewById(R.id.tvComentario);
            tvCalificacion = itemView.findViewById(R.id.tvCalificacion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
        }
    }
}