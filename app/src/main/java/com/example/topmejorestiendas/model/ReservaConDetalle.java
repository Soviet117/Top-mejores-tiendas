package com.example.topmejorestiendas.model;

import androidx.room.Embedded;

public class ReservaConDetalle {
    @Embedded
    public Reserva reserva;

    public String nombreNegocio;
    public String nombreCliente;
    public String emailCliente;
    public String telefonoCliente;
}
