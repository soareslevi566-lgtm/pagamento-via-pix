package com.api.loja.repository;

import com.api.loja.entity.VendaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<VendaEntity, Long> {
    Optional<VendaEntity> findByIdPagamentoGateway(Long idPagamentoGateway);
}