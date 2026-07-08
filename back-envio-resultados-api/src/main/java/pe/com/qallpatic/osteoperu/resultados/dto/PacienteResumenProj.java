package pe.com.qallpatic.osteoperu.resultados.dto;

import java.time.LocalDateTime;

/** Proyeccion: un paciente (DNI) con el resumen de sus resultados cargados. */
public interface PacienteResumenProj {

	String getDni();

	String getNombre();

	Long getTotalResultados();

	LocalDateTime getUltimaFecha();
}
