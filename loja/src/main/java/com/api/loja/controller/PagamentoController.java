package com.api.loja.controller;

import com.api.loja.dto.PagamentoPixRequestDTO;
import com.api.loja.dto.PagamentoPixResponseDTO;
import com.api.loja.service.PagamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    // Injeção de dependência via construtor
    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/pix")
    public ResponseEntity<PagamentoPixResponseDTO> criarCobrancaPix(@RequestBody PagamentoPixRequestDTO request) {
        PagamentoPixResponseDTO response = pagamentoService.criarCobrancaPix(
                request.valor(),
                request.descricao(),
                request.emailCliente()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}