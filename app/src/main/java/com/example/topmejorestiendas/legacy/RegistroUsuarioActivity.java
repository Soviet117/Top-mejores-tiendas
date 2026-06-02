package com.example.topmejorestiendas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.topmejorestiendas.database.AppDatabase;
import com.example.topmejorestiendas.databinding.ActivityRegistroUsuarioBinding;
import com.example.topmejorestiendas.model.Usuario;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;

public class RegistroUsuarioActivity extends AppCompatActivity {
    private ActivityRegistroUsuarioBinding binding;
    private AppDatabase db;
    private Uri imageUri;
    private String savedImagePath = null;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    binding.ivProfileCircle.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroUsuarioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        binding.ivProfileCircle.setOnClickListener(v -> mGetContent.launch("image/*"));
        binding.btnRegistrarUsuario.setOnClickListener(v -> registrar());
        binding.tvBackFromRegister.setOnClickListener(v -> finish());
    }

    private void registrar() {
        String nombre = binding.etNombres.getText().toString();
        String email = binding.etEmail.getText().toString();
        String pass = binding.etPass.getText().toString();
        String telf = binding.etCelular.getText().toString();

        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Campos obligatorios vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            savedImagePath = saveImage(imageUri, email);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            if (db.usuarioDao().obtenerPorEmail(email) != null) {
                runOnUiThread(() -> Toast.makeText(this, "Email ya registrado", Toast.LENGTH_SHORT).show());
                return;
            }
            Usuario user = new Usuario(nombre, email, pass, telf, savedImagePath, false, null);
            db.usuarioDao().registrar(user);
            runOnUiThread(() -> {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private String saveImage(Uri uri, String email) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File directory = new File(getExternalFilesDir(null), "perfiles");
            if (!directory.exists()) directory.mkdirs();
            File file = new File(directory, email + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}