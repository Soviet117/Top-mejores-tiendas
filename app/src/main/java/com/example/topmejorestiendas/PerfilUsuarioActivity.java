package com.example.topmejorestiendas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.topmejorestiendas.database.AppDatabase;
import com.example.topmejorestiendas.databinding.ActivityPerfilUsuarioBinding;
import com.example.topmejorestiendas.model.Usuario;
import com.example.topmejorestiendas.utils.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;

public class PerfilUsuarioActivity extends AppCompatActivity {
    private ActivityPerfilUsuarioBinding binding;
    private AppDatabase db;
    private SessionManager session;
    private Usuario usuario;
    private String newImagePath = null;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    saveAndRefreshImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPerfilUsuarioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        session = new SessionManager(this);

        cargarPerfil();

        binding.btnCambiarFoto.setOnClickListener(v -> mGetContent.launch("image/*"));
        binding.btnActualizarPerfil.setOnClickListener(v -> actualizar());
        binding.btnLogout.setOnClickListener(v -> {
            session.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        binding.btnMisNegocios.setOnClickListener(v -> {
            // Ir a Mis Negocios (podría ser la misma RankingActivity filtrada o una nueva)
            Toast.makeText(this, "Funcionalidad de Mis Negocios próximamente", Toast.LENGTH_SHORT).show();
        });
    }

    private void cargarPerfil() {
        Executors.newSingleThreadExecutor().execute(() -> {
            usuario = db.usuarioDao().obtenerPorId(session.getUserId());
            runOnUiThread(() -> {
                if (usuario != null) {
                    binding.etNombres.setText(usuario.nombreCompleto);
                    binding.etEmail.setText(usuario.email);
                    binding.etCelular.setText(usuario.telefono);
                    
                    if (usuario.fotoPerfil != null) {
                        Glide.with(this).load(new File(usuario.fotoPerfil)).into(binding.ivProfile);
                    }
                    
                    if (usuario.esDuenio) {
                        binding.btnMisNegocios.setVisibility(View.VISIBLE);
                    } else {
                        binding.btnMisNegocios.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private void saveAndRefreshImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File directory = new File(getExternalFilesDir(null), "perfiles");
            if (!directory.exists()) directory.mkdirs();
            File file = new File(directory, usuario.email + "_new.jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            newImagePath = file.getAbsolutePath();
            binding.ivProfile.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void actualizar() {
        usuario.nombreCompleto = binding.etNombres.getText().toString();
        usuario.telefono = binding.etCelular.getText().toString();
        if (newImagePath != null) {
            usuario.fotoPerfil = newImagePath;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            db.usuarioDao().actualizar(usuario);
            runOnUiThread(() -> Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show());
        });
    }
}