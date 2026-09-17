package com.api.loja.dto;

import java.math.BigDecimal;

public record VendaResponseDTO(
        Long idVenda,
        BigDecimal valorTotal,
        String statusVenda,
        Long idPagamentoMercadoPago,
        String qrCodePixCopiaECola,
        String qrCodePixImagemBase64
) {}