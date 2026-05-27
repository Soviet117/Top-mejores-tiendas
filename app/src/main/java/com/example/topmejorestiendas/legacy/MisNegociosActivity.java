package com.example.topmejorestiendas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.topmejorestiendas.adapter.NegocioAdapter;
import com.example.topmejorestiendas.database.AppDatabase;
import com.example.topmejorestiendas.databinding.ActivityMisNegociosBinding;
import com.example.topmejorestiendas.model.Negocio;
import com.example.topmejorestiendas.utils.SessionManager;
import java.util.List;
import java.util.concurrent.Executors;

public class MisNegociosActivity extends AppCompatActivity {
    private ActivityMisNegociosBinding binding;
    private AppDatabase db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMisNegociosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        session = new SessionManager(this);

        binding.rvMisNegocios.setLayoutManager(new LinearLayoutManager(this));

        cargarMisNegocios();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarMisNegocios();
    }

    private void cargarMisNegocios() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Negocio> negocios = db.negocioDao().obtenerPorDuenio(session.getUserId());
            runOnUiThread(() -> {
                if (negocios.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    binding.rvMisNegocios.setVisibility(View.GONE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                    binding.rvMisNegocios.setVisibility(View.VISIBLE);
                    NegocioAdapter adapter = new NegocioAdapter(negocios, n -> {
                        Intent intent = new Intent(this, RegistroLocalActivity.class);
                        intent.putExtra("NEGOCIO_ID", n.id);
                        startActivity(intent);
                    });
                    binding.rvMisNegocios.setAdapter(adapter);
                }
            });
        });
    }
}