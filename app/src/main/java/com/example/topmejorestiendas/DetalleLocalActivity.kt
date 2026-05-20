package com.example.topmejorestiendas

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.topmejorestiendas.databinding.ActivityDetalleLocalBinding

class DetalleLocalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetalleLocalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleLocalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVerMas.setOnClickListener {
            binding.layoutDetallesExtra.visibility = View.VISIBLE
            binding.btnVerMas.visibility = View.GONE
        }

        binding.btnVolverRanking.setOnClickListener {
            finish()
        }
    }
}