package com.example.topmejorestiendas.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.topmejorestiendas.model.Usuario;

@Dao
public interface UsuarioDao {
    @Insert
    long registrar(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE email = :email AND contrasena = :contrasena LIMIT 1")
    Usuario login(String email, String contrasena);

    @Query("SELECT * FROM usuarios WHERE id = :id")
    Usuario obtenerPorId(int id);

    @Query("SELECT * FROM usuarios WHERE email = :email")
    Usuario obtenerPorEmail(String email);

    @Update
    void actualizar(Usuario usuario);
}