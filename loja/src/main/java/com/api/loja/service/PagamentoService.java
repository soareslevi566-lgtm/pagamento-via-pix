package com.api.loja.service;

import com.api.loja.dto.PagamentoPixResponseDTO;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PagamentoService {

    public PagamentoPixResponseDTO criarCobrancaPix(BigDecimal valor, String descricao,
                                                    String emailComprador) {
        PaymentClient client = new PaymentClient();

        PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                .transactionAmount(valor)
                .description(descricao)
                .paymentMethodId("Pix")
                .payer(PaymentPayerRequest.builder()
                        .email(emailComprador)
                        .build())
                .build();
        try {
            Payment payment = client.create(createRequest);

            String qrCode = payment.getPointOfInteraction()
                    .getTransactionData()
                    .getQrCode();

            String qrCodeBase64 = payment.getPointOfInteraction()
                    .getTransactionData()
                    .getQrCodeBase64();

            return new PagamentoPixResponseDTO(
                    payment.getId(),
                    payment.getStatus(),
                    qrCode,
                    qrCodeBase64
            );

        } catch (MPApiException apiException) {
            throw new RuntimeException("Erro retornado pelo mercado pago" + apiException.getApiResponse().getContent());
        } catch (MPException exception) {
            throw new RuntimeException("Falha de conexão com Mercado Pago", exception);
        }

    }

}
