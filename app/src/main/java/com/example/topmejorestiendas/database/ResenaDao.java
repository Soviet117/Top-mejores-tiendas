package com.example.topmejorestiendas.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.topmejorestiendas.model.Resena;
import java.util.List;

@Dao
public interface ResenaDao {
    @Insert
    void insertar(Resena resena);

    @Query("SELECT * FROM resenas WHERE idNegocio = :idNegocio ORDER BY fecha DESC")
    List<Resena> obtenerPorNegocio(int idNegocio);

    @Query("SELECT AVG(calificacion) FROM resenas WHERE idNegocio = :idNegocio")
    float obtenerPromedio(int idNegocio);

    @Query("SELECT * FROM resenas WHERE idUsuario = :idUsuario AND idNegocio = :idNegocio LIMIT 1")
    Resena obtenerPorUsuarioYNegocio(int idUsuario, int idNegocio);

    @Query("SELECT * FROM resenas WHERE idUsuario = :idUsuario ORDER BY fecha DESC")
    List<Resena> obtenerPorUsuario(int idUsuario);

    @Update
    void actualizar(Resena resena);

    @Query("DELETE FROM resenas")
    void eliminarTodos();
}