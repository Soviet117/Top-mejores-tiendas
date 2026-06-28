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
import com.example.topmejorestiendas.data.remote.RetrofitClient;
import com.example.topmejorestiendas.data.remote.dto.AuthResponse;
import com.example.topmejorestiendas.data.remote.dto.RegisterRequest;
import com.example.topmejorestiendas.databinding.ActivityRegistroUsuarioBinding;
import com.example.topmejorestiendas.utils.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de registro simple (flujo Java legacy).
 * Ahora se conecta al backend API en lugar de Room.
 */
public class RegistroUsuarioActivity extends AppCompatActivity {
    private ActivityRegistroUsuarioBinding binding;
    private SessionManager sessionManager;
    private Uri imageUri;

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

        sessionManager = new SessionManager(this);

        binding.ivProfileCircle.setOnClickListener(v -> mGetContent.launch("image/*"));
        binding.btnRegistrarUsuario.setOnClickListener(v -> registrar());
        binding.tvBackFromRegister.setOnClickListener(v -> finish());
    }

    private void registrar() {
        String nombre = binding.etNombres.getText().toString().trim();
        String email  = binding.etEmail.getText().toString().trim();
        String pass   = binding.etPass.getText().toString().trim();
        String telf   = binding.etCelular.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Campos obligatorios vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnRegistrarUsuario.setEnabled(false);

        // Llamada al backend via Retrofit
        RegisterRequest request = new RegisterRequest(nombre, email, pass, telf.isEmpty() ? null : telf, false, null, null, null);

        RetrofitClient.INSTANCE.getApiService().registerCall(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                binding.btnRegistrarUsuario.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse body = response.body();
                    // Guardar sesión JWT
                    sessionManager.saveSession(
                            body.getToken(),
                            body.getUser().getId(),
                            body.getUser().getEsDuenio(),
                            body.getUser().getNombreCompleto(),
                            body.getUser().getEmail(),
                            body.getUser().getFotoPerfil() != null ? body.getUser().getFotoPerfil() : ""
                    );
                    Toast.makeText(RegistroUsuarioActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    int code = response.code();
                    String msg = code == 409 ? "El correo ya está registrado" : "Error al registrar (" + code + ")";
                    Toast.makeText(RegistroUsuarioActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                binding.btnRegistrarUsuario.setEnabled(true);
                Toast.makeText(RegistroUsuarioActivity.this, "Sin conexión al servidor: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}