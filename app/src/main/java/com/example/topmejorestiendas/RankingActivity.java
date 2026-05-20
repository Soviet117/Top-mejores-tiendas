package com.example.topmejorestiendas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.topmejorestiendas.adapter.NegocioAdapter;
import com.example.topmejorestiendas.database.AppDatabase;
import com.example.topmejorestiendas.databinding.ActivityRankingBinding;
import com.example.topmejorestiendas.model.Negocio;
import java.util.List;
import java.util.concurrent.Executors;

public class RankingActivity extends AppCompatActivity {
    private ActivityRankingBinding binding;
    private AppDatabase db;
    private String rubro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRankingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        rubro = getIntent().getStringExtra("RUBRO");

        binding.tvRubroTitle.setText(rubro != null ? rubro : "Ranking");
        binding.rvRanking.setLayoutManager(new LinearLayoutManager(this));

        cargarNegocios();
    }

    private void cargarNegocios() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Negocio> negocios = db.negocioDao().obtenerPorRubro(rubro);
            runOnUiThread(() -> {
                if (negocios.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    binding.rvRanking.setVisibility(View.GONE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                    binding.rvRanking.setVisibility(View.VISIBLE);
                    NegocioAdapter adapter = new NegocioAdapter(negocios, n -> {
                        Intent intent = new Intent(this, DetalleLocalActivity.class);
                        intent.putExtra("NEGOCIO_ID", n.id);
                        startActivity(intent);
                    });
                    binding.rvRanking.setAdapter(adapter);
                }
            });
        });
    }
}