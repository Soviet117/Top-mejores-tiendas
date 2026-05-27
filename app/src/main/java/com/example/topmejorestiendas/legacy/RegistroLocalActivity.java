package com.example.topmejorestiendas;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.topmejorestiendas.database.AppDatabase;
import com.example.topmejorestiendas.databinding.ActivityRegistroLocalBinding;
import com.example.topmejorestiendas.model.Negocio;
import com.example.topmejorestiendas.model.Usuario;
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
    private int negocioId = -1;
    private Negocio negocioExistente;

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

        negocioId = getIntent().getIntExtra("NEGOCIO_ID", -1);

        String[] rubros = {"Restaurantes", "Ferreterías", "Canchas Deportivas", "Farmacias", "Supermercados", "Tiendas de Ropa"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rubros);
        binding.spRubro.setAdapter(adapter);

        if (negocioId != -1) {
            cargarDatosNegocio();
            binding.btnRegistrarLocal.setText("Actualizar Local");
            
            // Ocultar campos de cuenta de usuario si estamos editando un negocio existente
            binding.tvDatosDuenioHeader.setVisibility(View.GONE);
            binding.tilNombreDuenio.setVisibility(View.GONE);
            binding.tilEmailDuenio.setVisibility(View.GONE);
            binding.tilPasswordDuenio.setVisibility(View.GONE);
            binding.tilTelefonoDuenio.setVisibility(View.GONE);
        }

        binding.ivNegocio.setOnClickListener(v -> mGetContent.launch("image/*"));
        binding.btnRegistrarLocal.setOnClickListener(v -> {
            if (negocioId == -1) {
                registrarLocal();
            } else {
                actualizarLocal();
            }
        });
        binding.tvBackFromLocal.setOnClickListener(v -> finish());
    }

    private void cargarDatosNegocio() {
        Executors.newSingleThreadExecutor().execute(() -> {
            negocioExistente = db.negocioDao().obtenerPorId(negocioId);
            runOnUiThread(() -> {
                if (negocioExistente != null) {
                    binding.etNombreNegocio.setText(negocioExistente.nombreNegocio);
                    binding.etDireccion.setText(negocioExistente.direccion);
                    binding.etHorario.setText(negocioExistente.horario);
                    binding.etDescripcion.setText(negocioExistente.descripcion);
                    if (negocioExistente.fotoNegocio != null) {
                        binding.ivNegocio.setImageURI(Uri.fromFile(new File(negocioExistente.fotoNegocio)));
                    }
                    // Seleccionar rubro en spinner
                    for (int i = 0; i < binding.spRubro.getAdapter().getCount(); i++) {
                        if (binding.spRubro.getAdapter().getItem(i).toString().equals(negocioExistente.rubro)) {
                            binding.spRubro.setSelection(i);
                            break;
                        }
                    }
                }
            });
        });
    }

    private void actualizarLocal() {
        String nombre = binding.etNombreNegocio.getText().toString();
        String rubro = binding.spRubro.getSelectedItem().toString();
        String direccion = binding.etDireccion.getText().toString();
        String horario = binding.etHorario.getText().toString();
        String descripcion = binding.etDescripcion.getText().toString();

        if (nombre.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(this, "Nombre y dirección son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            savedImagePath = saveImage(imageUri, nombre);
        } else {
            savedImagePath = negocioExistente.fotoNegocio;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            negocioExistente.nombreNegocio = nombre;
            negocioExistente.rubro = rubro;
            negocioExistente.direccion = direccion;
            negocioExistente.horario = horario;
            negocioExistente.fotoNegocio = savedImagePath;
            negocioExistente.descripcion = descripcion;

            db.negocioDao().actualizar(negocioExistente);
            runOnUiThread(() -> {
                Toast.makeText(this, "Negocio actualizado", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void registrarLocal() {
        // Datos del Usuario
        String nombreDuenio = binding.etNombreDuenio.getText().toString().trim();
        String emailDuenio = binding.etEmailDuenio.getText().toString().trim();
        String passwordDuenio = binding.etPasswordDuenio.getText().toString().trim();
        String telefonoDuenio = binding.etTelefonoDuenio.getText().toString().trim();

        // Datos del Negocio
        String nombreNegocio = binding.etNombreNegocio.getText().toString().trim();
        String rubro = binding.spRubro.getSelectedItem().toString();
        String direccion = binding.etDireccion.getText().toString().trim();
        String horario = binding.etHorario.getText().toString().trim();
        String descripcion = binding.etDescripcion.getText().toString().trim();

        if (nombreDuenio.isEmpty() || emailDuenio.isEmpty() || passwordDuenio.isEmpty() || 
            nombreNegocio.isEmpty() || direccion.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            savedImagePath = saveImage(imageUri, nombreNegocio);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Verificar si el email ya existe
            Usuario existingUser = db.usuarioDao().obtenerPorEmail(emailDuenio);
            if (existingUser != null) {
                runOnUiThread(() -> Toast.makeText(this, "El email ya está registrado", Toast.LENGTH_SHORT).show());
                return;
            }

            // 2. Crear la cuenta de Usuario (Dueño)
            Usuario nuevoDuenio = new Usuario(nombreDuenio, emailDuenio, passwordDuenio, telefonoDuenio, null, true);
            long idDuenio = db.usuarioDao().registrar(nuevoDuenio);

            // 3. Crear el Negocio asociado al nuevo idDuenio
            Negocio negocio = new Negocio(nombreNegocio, rubro, direccion, horario, savedImagePath, descripcion, (int) idDuenio);
            db.negocioDao().insertar(negocio);

            runOnUiThread(() -> {
                Toast.makeText(this, "Registro exitoso. Ahora puedes iniciar sesión.", Toast.LENGTH_SHORT).show();
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