package com.api.loja.controller;

import com.api.loja.dto.VendaRequestDTO;
import com.api.loja.dto.VendaResponseDTO;
import com.api.loja.entity.VendaEntity;
import com.api.loja.service.VendaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vendas")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @PostMapping
    public ResponseEntity<VendaResponseDTO> criarVenda(@RequestBody VendaRequestDTO request) {
        VendaResponseDTO response = vendaService.criarVenda(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendaEntity> buscarVenda(@PathVariable Long id) {
        VendaEntity venda = vendaService.buscarPorId(id);
        return ResponseEntity.ok(venda);
    }
}