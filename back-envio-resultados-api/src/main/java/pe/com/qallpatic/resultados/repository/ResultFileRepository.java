package pe.com.qallpatic.resultados.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.qallpatic.resultados.model.ResultFile;

public interface ResultFileRepository extends JpaRepository<ResultFile, Long> {
}
