package pe.com.qallpatic.osteoperu.dashboard.service;

import java.time.LocalDate;

/** Cache key para el resumen del dashboard. Identifica un rango [(desde, hasta)]. */
public record CacheKeyDashboard(LocalDate desde, LocalDate hasta) {
}