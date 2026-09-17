package com.api.loja.dto;

import java.math.BigDecimal;

public record VendaRequestDTO(
        BigDecimal valorTotal,
        String emailCliente,
        String descricao
) {

}