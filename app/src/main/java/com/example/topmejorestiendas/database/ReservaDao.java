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

    @Query("SELECT r.*, n.nombreNegocio AS nombreNegocio, u.nombreCompleto AS nombreCliente, u.email AS emailCliente, u.telefono AS telefonoCliente FROM reservas r INNER JOIN negocios n ON r.idNegocio = n.id INNER JOIN usuarios u ON r.idUsuario = u.id WHERE r.idNegocio = :idNegocio ORDER BY r.fechaCreacion DESC")
    List<com.example.topmejorestiendas.model.ReservaConDetalle> getReservasPorNegocio(int idNegocio);

    @Query("SELECT r.*, n.nombreNegocio AS nombreNegocio, u.nombreCompleto AS nombreCliente, u.email AS emailCliente, u.telefono AS telefonoCliente FROM reservas r INNER JOIN negocios n ON r.idNegocio = n.id INNER JOIN usuarios u ON r.idUsuario = u.id WHERE r.idUsuario = :idUsuario ORDER BY r.fechaCreacion DESC")
    List<com.example.topmejorestiendas.model.ReservaConDetalle> getReservasPorUsuario(int idUsuario);

    @Query("SELECT r.*, n.nombreNegocio AS nombreNegocio, u.nombreCompleto AS nombreCliente, u.email AS emailCliente, u.telefono AS telefonoCliente FROM reservas r INNER JOIN negocios n ON r.idNegocio = n.id INNER JOIN usuarios u ON r.idUsuario = u.id WHERE n.idDuenio = :idDuenio ORDER BY r.fechaCreacion DESC")
    List<com.example.topmejorestiendas.model.ReservaConDetalle> getReservasPorDuenio(int idDuenio);

    @Query("SELECT * FROM reservas WHERE id = :idReserva")
    Reserva getReservaPorId(int idReserva);

    @Query("UPDATE reservas SET estado = :nuevoEstado WHERE id = :idReserva")
    void actualizarEstado(int idReserva, String nuevoEstado);
    
    @Update
    void update(Reserva reserva);

    @Query("DELETE FROM reservas WHERE id = :idReserva")
    void deleteReserva(int idReserva);
}
