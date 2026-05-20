package com.example.topmejorestiendas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.example.topmejorestiendas.adapter.TopNegociosAdapter;
import com.example.topmejorestiendas.database.AppDatabase;
import com.example.topmejorestiendas.databinding.ActivityMainBinding;
import com.example.topmejorestiendas.model.Negocio;
import com.example.topmejorestiendas.model.Usuario;
import com.example.topmejorestiendas.utils.SessionManager;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AppDatabase db;
    private SessionManager session;
    private Usuario usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupUI();
        cargarDatos();

        binding.swipeRefresh.setOnRefreshListener(this::cargarDatos);
    }

    private void setupUI() {
        binding.cvProfile.setOnClickListener(v -> startActivity(new Intent(this, PerfilUsuarioActivity.class)));
        binding.cvSearch.setOnClickListener(v -> Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show());
        
        binding.rvTopNegocios.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Category clicks
        binding.catRestaurantes.getRoot().setOnClickListener(v -> goToRanking("Restaurantes"));
        binding.catFerreterias.getRoot().setOnClickListener(v -> goToRanking("Ferreterías"));
        binding.catCanchas.getRoot().setOnClickListener(v -> goToRanking("Canchas Deportivas"));
        binding.catFarmacias.getRoot().setOnClickListener(v -> goToRanking("Farmacias"));
        binding.catSupermercados.getRoot().setOnClickListener(v -> goToRanking("Supermercados"));
        binding.catRopa.getRoot().setOnClickListener(v -> goToRanking("Tiendas de Ropa"));

        // Static Category names and icons
        setupCategory(binding.catRestaurantes.getRoot(), "Restaurantes", R.drawable.cat_restaurantes);
        setupCategory(binding.catFerreterias.getRoot(), "Ferreterías", R.drawable.cat_ferreterias);
        setupCategory(binding.catCanchas.getRoot(), "Canchas", R.drawable.cat_canchas);
        setupCategory(binding.catFarmacias.getRoot(), "Farmacias", R.drawable.cat_farmacias);
        setupCategory(binding.catSupermercados.getRoot(), "Supermercados", R.drawable.cat_supermercados);
        setupCategory(binding.catRopa.getRoot(), "Ropa", R.drawable.cat_ropa);

        binding.fabAddNegocio.setOnClickListener(v -> startActivity(new Intent(this, RegistroLocalActivity.class)));

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, PerfilUsuarioActivity.class));
            } else if (id == R.id.nav_favorites) {
                Toast.makeText(this, "Favoritos próximamente", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void setupCategory(View categoryView, String name, int iconRes) {
        TextView tvName = categoryView.findViewById(R.id.tvCatName);
        tvName.setText(name);
        ImageView ivIcon = categoryView.findViewById(R.id.ivCatIcon);
        ivIcon.setImageResource(iconRes);
    }

    private void cargarDatos() {
        binding.swipeRefresh.setRefreshing(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            usuarioActual = db.usuarioDao().obtenerPorId(session.getUserId());
            List<Negocio> topNegocios = db.negocioDao().obtenerTop(3);
            
            // Counts
            int cRest = db.negocioDao().contarPorRubro("Restaurantes");
            int cFerr = db.negocioDao().contarPorRubro("Ferreterías");
            int cCanc = db.negocioDao().contarPorRubro("Canchas Deportivas");
            int cFarm = db.negocioDao().contarPorRubro("Farmacias");
            int cSuper = db.negocioDao().contarPorRubro("Supermercados");
            int cRopa = db.negocioDao().contarPorRubro("Tiendas de Ropa");

            runOnUiThread(() -> {
                if (usuarioActual != null) {
                    binding.tvWelcome.setText("¡Hola, " + usuarioActual.nombreCompleto + "!");
                    if (usuarioActual.fotoPerfil != null) {
                        Glide.with(this).load(new File(usuarioActual.fotoPerfil)).into(binding.ivProfile);
                    }
                    binding.fabAddNegocio.setVisibility(usuarioActual.esDuenio ? View.VISIBLE : View.GONE);
                }

                TopNegociosAdapter adapter = new TopNegociosAdapter(topNegocios, n -> {
                    Intent intent = new Intent(this, DetalleLocalActivity.class);
                    intent.putExtra("NEGOCIO_ID", n.id);
                    startActivity(intent);
                });
                binding.rvTopNegocios.setAdapter(adapter);

                updateCount(binding.catRestaurantes.getRoot(), cRest);
                updateCount(binding.catFerreterias.getRoot(), cFerr);
                updateCount(binding.catCanchas.getRoot(), cCanc);
                updateCount(binding.catFarmacias.getRoot(), cFarm);
                updateCount(binding.catSupermercados.getRoot(), cSuper);
                updateCount(binding.catRopa.getRoot(), cRopa);

                binding.swipeRefresh.setRefreshing(false);
            });
        });
    }

    private void updateCount(View view, int count) {
        TextView tvCount = view.findViewById(R.id.tvCatCount);
        tvNameInInclude(view); // Ensure name is set if not already
        tvCount.setText(count + (count == 1 ? " lugar" : " lugares"));
    }
    
    private void tvNameInInclude(View view) {
        // This is a helper to ensure names stay correct after layout inflation
        TextView tvName = view.findViewById(R.id.tvCatName);
        if (view.getId() == R.id.catRestaurantes) tvName.setText("Restaurantes");
        if (view.getId() == R.id.catFerreterias) tvName.setText("Ferreterías");
        if (view.getId() == R.id.catCanchas) tvName.setText("Canchas");
        if (view.getId() == R.id.catFarmacias) tvName.setText("Farmacias");
        if (view.getId() == R.id.catSupermercados) tvName.setText("Supermercados");
        if (view.getId() == R.id.catRopa) tvName.setText("Ropa");
    }

    private void goToRanking(String rubro) {
        Intent intent = new Intent(this, RankingActivity.class);
        intent.putExtra("RUBRO", rubro);
        startActivity(intent);
    }
}