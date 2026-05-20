package com.example.topmejorestiendas.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.topmejorestiendas.model.Negocio;
import java.util.List;

@Dao
public interface NegocioDao {
    @Insert
    long insertar(Negocio negocio);

    @Query("SELECT * FROM negocios WHERE rubro = :rubro ORDER BY calificacionPromedio DESC")
    List<Negocio> obtenerPorRubro(String rubro);

    @Query("SELECT * FROM negocios ORDER BY calificacionPromedio DESC LIMIT 5")
    List<Negocio> obtenerTop5();

    @Query("SELECT * FROM negocios WHERE idDuenio = :idDuenio")
    List<Negocio> obtenerPorDuenio(int idDuenio);

    @Query("SELECT * FROM negocios WHERE id = :id")
    Negocio obtenerPorId(int id);

    @Update
    void actualizar(Negocio negocio);
}