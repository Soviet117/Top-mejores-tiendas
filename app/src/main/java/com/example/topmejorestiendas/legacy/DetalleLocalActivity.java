package com.example.topmejorestiendas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.example.topmejorestiendas.adapter.ResenaAdapter;
import com.example.topmejorestiendas.database.AppDatabase;
import com.example.topmejorestiendas.databinding.ActivityDetalleLocalBinding;
import com.example.topmejorestiendas.model.Negocio;
import com.example.topmejorestiendas.model.Resena;
import com.example.topmejorestiendas.utils.SessionManager;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

public class DetalleLocalActivity extends AppCompatActivity {
    private ActivityDetalleLocalBinding binding;
    private AppDatabase db;
    private SessionManager session;
    private int negocioId;
    private Negocio negocio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetalleLocalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        session = new SessionManager(this);
        negocioId = getIntent().getIntExtra("NEGOCIO_ID", -1);

        binding.rvResenas.setLayoutManager(new LinearLayoutManager(this));
        
        cargarDetalle();
        
        binding.btnVerMas.setOnClickListener(v -> {
            binding.layoutDetallesExtra.setVisibility(View.VISIBLE);
            binding.btnVerMas.setVisibility(View.GONE);
        });

        binding.btnCalificar.setOnClickListener(v -> mostrarDialogoCalificar());
        binding.btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistroLocalActivity.class);
            intent.putExtra("NEGOCIO_ID", negocioId);
            startActivity(intent);
        });
        binding.btnVolverRanking.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDetalle();
    }

    private void cargarDetalle() {
        Executors.newSingleThreadExecutor().execute(() -> {
            negocio = db.negocioDao().obtenerPorId(negocioId);
            List<Resena> resenas = db.resenaDao().obtenerPorNegocio(negocioId);
            
            runOnUiThread(() -> {
                if (negocio != null) {
                    binding.tvNombreNegocio.setText(negocio.nombreNegocio);
                    binding.tvCalificacionGeneral.setText("⭐ " + negocio.calificacionPromedio);
                    binding.tvDireccion.setText("Dirección: " + negocio.direccion);
                    binding.tvHorario.setText("Horario: " + negocio.horario);
                    binding.tvDescripcion.setText(negocio.descripcion != null ? negocio.descripcion : "Sin descripción.");
                    
                    if (negocio.fotoNegocio != null) {
                        Glide.with(this).load(new File(negocio.fotoNegocio)).into(binding.ivFotoLocal);
                    }

                    ResenaAdapter adapter = new ResenaAdapter(resenas);
                    binding.rvResenas.setAdapter(adapter);

                    // Lógica Dueño vs Cliente
                    // El dueño PUEDE calificar otros negocios, pero no el suyo propio.
                    if (session.getUserId() == negocio.idDuenio) {
                        binding.btnCalificar.setVisibility(View.GONE);
                        binding.btnEditar.setVisibility(View.VISIBLE);
                    } else {
                        binding.btnCalificar.setVisibility(View.VISIBLE);
                        binding.btnEditar.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private void mostrarDialogoCalificar() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calificar, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etComentario = dialogView.findViewById(R.id.etComentario);

        new AlertDialog.Builder(this)
                .setTitle("Calificar este local")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    int calif = (int) ratingBar.getRating();
                    String com = etComentario.getText().toString();
                    if (calif > 0) {
                        guardarResena(calif, com);
                    } else {
                        Toast.makeText(this, "Selecciona una calificación", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarResena(int calif, String com) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Resena r = new Resena(session.getUserId(), negocioId, calif, calif, calif, calif, com, System.currentTimeMillis());
            db.resenaDao().insertar(r);
            
            float nuevoPromedio = db.resenaDao().obtenerPromedio(negocioId);
            negocio.calificacionPromedio = nuevoPromedio;
            db.negocioDao().actualizar(negocio);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "Gracias por tu reseña", Toast.LENGTH_SHORT).show();
                cargarDetalle();
            });
        });
    }
}