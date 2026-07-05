package pe.com.qallpatic.resultados.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.com.qallpatic.resultados.model.ViewEvent;

public interface ViewEventRepository extends JpaRepository<ViewEvent, Long> {
}
