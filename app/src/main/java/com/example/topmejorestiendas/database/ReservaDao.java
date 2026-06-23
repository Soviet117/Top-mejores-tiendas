package com.example.topmejorestiendas.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.topmejorestiendas.model.Reserva;

import java.util.List;

@Dao
public interface ReservaDao {

    @Insert
    long insert(Reserva reserva);

    @Query("SELECT * FROM reservas WHERE idNegocio = :idNegocio ORDER BY fechaCreacion DESC")
    List<Reserva> getReservasPorNegocio(int idNegocio);

    @Query("SELECT * FROM reservas WHERE idUsuario = :idUsuario ORDER BY fechaCreacion DESC")
    List<Reserva> getReservasPorUsuario(int idUsuario);

    @Query("SELECT r.* FROM reservas r INNER JOIN negocios n ON r.idNegocio = n.id WHERE n.idDuenio = :idDuenio ORDER BY r.fechaCreacion DESC")
    List<Reserva> getReservasPorDuenio(int idDuenio);

    @Query("SELECT * FROM reservas WHERE id = :idReserva")
    Reserva getReservaPorId(int idReserva);

    @Query("UPDATE reservas SET estado = :nuevoEstado WHERE id = :idReserva")
    void actualizarEstado(int idReserva, String nuevoEstado);
    
    @Update
    void update(Reserva reserva);

    @Query("DELETE FROM reservas WHERE id = :idReserva")
    void deleteReserva(int idReserva);
}
