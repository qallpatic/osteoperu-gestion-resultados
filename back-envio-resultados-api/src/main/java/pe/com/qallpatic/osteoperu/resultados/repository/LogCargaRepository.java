package pe.com.qallpatic.osteoperu.resultados.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.com.qallpatic.osteoperu.resultados.entity.LogCarga;

public interface LogCargaRepository
		extends JpaRepository<LogCarga, Long>, JpaSpecificationExecutor<LogCarga> {
}
