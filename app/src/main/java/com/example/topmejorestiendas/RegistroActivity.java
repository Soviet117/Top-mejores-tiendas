package com.example.topmejorestiendas;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.topmejorestiendas.databinding.ActivityRegistroBinding;

public class RegistroActivity extends AppCompatActivity {
    private ActivityRegistroBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnUsuarioCliente.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistroUsuarioActivity.class));
        });

        binding.btnDuenoNegocio.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistroLocalActivity.class));
        });

        binding.tvBackToLogin.setOnClickListener(v -> finish());
    }
}