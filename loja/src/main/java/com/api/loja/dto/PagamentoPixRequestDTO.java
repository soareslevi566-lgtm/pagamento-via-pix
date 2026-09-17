package com.api.loja.dto;

import java.math.BigDecimal;

public record PagamentoPixRequestDTO(
        BigDecimal valor,
        String descricao,
        String emailCliente
) {}