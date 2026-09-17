package com.api.loja.service;

import com.api.loja.dto.PagamentoPixResponseDTO;
import com.api.loja.dto.VendaRequestDTO;
import com.api.loja.dto.VendaResponseDTO;
import com.api.loja.entity.VendaEntity;
import com.api.loja.repository.VendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendaService {

    private final VendaRepository vendaRepository;
    private final PagamentoService pagamentoService;

    public VendaService(VendaRepository vendaRepository, PagamentoService pagamentoService) {
        this.vendaRepository = vendaRepository;
        this.pagamentoService = pagamentoService;
    }

    @Transactional
    public VendaResponseDTO criarVenda(VendaRequestDTO dto) {
        // 1. Cria a venda inicial no banco como PENDENTE
        VendaEntity venda = new VendaEntity();
        venda.setValorTotal(dto.valorTotal());
        venda.setEmailCliente(dto.emailCliente());
        venda.setStatus("PENDENTE");
        venda = vendaRepository.save(venda);

        // 2. Aciona o Mercado Pago
        String descricao = (dto.descricao() != null && !dto.descricao().isBlank())
                ? dto.descricao()
                : "Venda #" + venda.getId();

        PagamentoPixResponseDTO pixResponse = pagamentoService.criarCobrancaPix(
                venda.getValorTotal(),
                descricao,
                venda.getEmailCliente()
        );

        // 3. Associa o ID retornado pelo Mercado Pago à venda
        venda.setIdPagamentoGateway(pixResponse.idPagamento());
        vendaRepository.save(venda);

        // 4. Retorna para o cliente
        return new VendaResponseDTO(
                venda.getId(),
                venda.getValorTotal(),
                venda.getStatus(),
                pixResponse.idPagamento(),
                pixResponse.qrCodeCopiaECola(),
                pixResponse.qrCodeBase64()
        );
    }

    public VendaEntity buscarPorId(Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venda não encontrada com o ID: " + id));
    }

    @Transactional
    public void atualizarStatusPagamento(Long idPagamentoGateway, String novoStatus) {
        vendaRepository.findByIdPagamentoGateway(idPagamentoGateway).ifPresent(venda -> {
            if ("approved".equalsIgnoreCase(novoStatus)) {
                venda.setStatus("PAGO");
            } else if ("rejected".equalsIgnoreCase(novoStatus) || "cancelled".equalsIgnoreCase(novoStatus)) {
                venda.setStatus("CANCELADO");
            }
            vendaRepository.save(venda);
        });
    }
}