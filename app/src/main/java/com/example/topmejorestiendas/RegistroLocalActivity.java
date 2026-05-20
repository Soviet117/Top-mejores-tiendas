package com.example.topmejorestiendas;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.topmejorestiendas.database.AppDatabase;
import com.example.topmejorestiendas.databinding.ActivityRegistroLocalBinding;
import com.example.topmejorestiendas.model.Negocio;
import com.example.topmejorestiendas.utils.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;

public class RegistroLocalActivity extends AppCompatActivity {
    private ActivityRegistroLocalBinding binding;
    private AppDatabase db;
    private SessionManager session;
    private Uri imageUri;
    private String savedImagePath = null;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    binding.ivNegocio.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroLocalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        session = new SessionManager(this);

        String[] rubros = {"Restaurantes", "Ferreterías", "Canchas Deportivas", "Farmacias", "Supermercados", "Tiendas de Ropa"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rubros);
        binding.spRubro.setAdapter(adapter);

        binding.ivNegocio.setOnClickListener(v -> mGetContent.launch("image/*"));
        binding.btnRegistrarLocal.setOnClickListener(v -> registrarLocal());
        binding.tvBackFromLocal.setOnClickListener(v -> finish());
    }

    private void registrarLocal() {
        String nombre = binding.etNombreNegocio.getText().toString();
        String rubro = binding.spRubro.getSelectedItem().toString();
        String direccion = binding.etDireccion.getText().toString();
        String horario = binding.etHorario.getText().toString();

        if (nombre.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(this, "Nombre y dirección son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            savedImagePath = saveImage(imageUri, nombre);
        }

        int idDuenio = session.getUserId();

        Executors.newSingleThreadExecutor().execute(() -> {
            Negocio negocio = new Negocio(nombre, rubro, direccion, horario, savedImagePath, idDuenio);
            db.negocioDao().insertar(negocio);
            runOnUiThread(() -> {
                Toast.makeText(this, "Negocio registrado", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private String saveImage(Uri uri, String nombre) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File directory = new File(getExternalFilesDir(null), "negocios");
            if (!directory.exists()) directory.mkdirs();
            File file = new File(directory, nombre + "_" + System.currentTimeMillis() + ".jpg");
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