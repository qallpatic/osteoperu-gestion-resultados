package pe.com.qallpatic.osteoperu.dashboard.dto;

import java.time.LocalDate;

/**
 * Punto en una serie temporal diaria. Lo consume el front para graficar
 * (barras / lineas) con el eje X = fecha y el valor en Y = total.
 */
public record PuntoSerieDiaria(LocalDate fecha, long total) {
}