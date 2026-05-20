package com.example.topmejorestiendas;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.topmejorestiendas.databinding.ActivityRubrosBinding;
import com.example.topmejorestiendas.utils.SessionManager;

public class RubrosActivity extends AppCompatActivity {
    private ActivityRubrosBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRubrosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        binding.cardRestaurantes.setOnClickListener(v -> goToRanking("Restaurantes"));
        binding.cardFerreterias.setOnClickListener(v -> goToRanking("Ferreterías"));
        binding.cardCanchas.setOnClickListener(v -> goToRanking("Canchas Deportivas"));
        binding.cardFarmacias.setOnClickListener(v -> goToRanking("Farmacias"));
        binding.cardSupermercados.setOnClickListener(v -> goToRanking("Supermercados"));
        binding.cardTiendasRopa.setOnClickListener(v -> goToRanking("Tiendas de Ropa"));

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, PerfilUsuarioActivity.class));
                return true;
            }
            return true;
        });
    }

    private void goToRanking(String rubro) {
        Intent intent = new Intent(this, RankingActivity.class);
        intent.putExtra("RUBRO", rubro);
        startActivity(intent);
    }
}