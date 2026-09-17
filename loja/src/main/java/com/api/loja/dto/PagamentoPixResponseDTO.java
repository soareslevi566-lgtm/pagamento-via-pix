package com.api.loja.dto;

public record PagamentoPixResponseDTO(Long idPagamento,
                                      String status,
                                      String qrCodeCopiaECola,
                                      String qrCodeBase64) {
}
