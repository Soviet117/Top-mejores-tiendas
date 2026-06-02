package com.example.topmejorestiendas.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.topmejorestiendas.model.Reporte;

import java.util.List;

@Dao
public interface ReporteDao {
    @Insert
    long insertar(Reporte reporte);

    @Query("SELECT * FROM reportes WHERE idNegocio = :idNegocio")
    List<Reporte> obtenerPorNegocio(int idNegocio);

    @Query("DELETE FROM reportes")
    void eliminarTodos();
}
