package com.example.topmejorestiendas.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios", indices = {@Index(value = {"email"}, unique = true)})
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String nombreCompleto;
    public String email;
    public String contrasena;
    public String telefono;
    public String fotoPerfil;
    public boolean esDuenio;
    public String ruc;
    public boolean emailVerificado;
    public String razonSocial;

    public Usuario(String nombreCompleto, String email, String contrasena, String telefono, String fotoPerfil, boolean esDuenio, String ruc) {
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.contrasena = contrasena;
        this.telefono = telefono;
        this.fotoPerfil = fotoPerfil;
        this.esDuenio = esDuenio;
        this.ruc = ruc;
        this.emailVerificado = false;
        this.razonSocial = null;
    }
}