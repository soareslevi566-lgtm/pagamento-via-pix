# pagamento-via-pix

API REST em Spring Boot que gera cobranças Pix via SDK oficial do Mercado Pago e mantém o registro da venda associada. Nasceu como o backend de um fluxo de checkout simples: o cliente pede para pagar, a API cria a cobrança no Mercado Pago, devolve o QR Code (imagem e copia-e-cola) e guarda o vínculo entre a venda e o pagamento.

Não é um projeto de e-commerce completo — não tem carrinho, catálogo de produtos nem autenticação. É a peça que resolve especificamente "gerar Pix e saber o que aconteceu com ele depois".

## Stack e por que ela é essa

| Item | Escolha | Motivo |
|---|---|---|
| Linguagem | Java 25 | Versão definida no `pom.xml`. Traz `records` maduros e sintaxe mais enxuta, usados de propósito nos DTOs (ver abaixo). |
| Framework | Spring Boot 4.1.1 | Major recente do Spring Boot; repare que a dependência web aqui é `spring-boot-starter-webmvc`, não o clássico `spring-boot-starter-web` — nome que mudou nessa geração do Boot. Se você estiver acostumado com tutoriais antigos, não estranhe. |
| Persistência | Spring Data JPA + MySQL | `JpaRepository` puro, sem query methods complexos — o único método customizado é uma busca por `idPagamentoGateway`, que é exatamente o que o fluxo de conciliação de pagamento precisa. |
| Pagamento | `sdk-java` do Mercado Pago (2.1.28) | SDK oficial em vez de chamar a REST API na mão. Evita reimplementar assinatura de request, serialização e tratamento de erro da API deles. |
| Boilerplate | Lombok (`@Getter`/`@Setter`/`@NoArgsConstructor`) | Usado só na entidade JPA, que precisa de mutabilidade (JPA exige construtor vazio e setters para popular via reflection). Os DTOs **não** usam Lombok — são `record`, que já resolve isso com menos mágica. |

## Arquitetura

Fluxo em três camadas, sem novidade nenhuma — e é assim de propósito, porque esse tipo de integração não pede criatividade arquitetural, pede previsibilidade:

```
Controller → Service → Repository/SDK externo
```

Só que existem **dois pontos de entrada** para pagamento, e isso é intencional, não duplicação por descuido:

- `POST /api/pagamentos/pix` — chama o `PagamentoService` direto, gera a cobrança e devolve o QR Code. Não grava nada no banco. Serve para testar a integração com o Mercado Pago isoladamente, sem depender do fluxo de venda.
- `POST /vendas` — o fluxo de verdade. Cria a `VendaEntity` como `PENDENTE`, só então aciona o `PagamentoService`, associa o `idPagamentoGateway` retornado e devolve tudo num único `VendaResponseDTO`.

Em produção, provavelmente só o segundo endpoint teria uso real; o primeiro fica como uma porta de teste/depuração. Se isso não fizer sentido para o seu caso de uso, é o primeiro candidato a sair ou virar um endpoint interno.

### Sobre o `VendaEntity`

```java
@PrePersist
public void prePersist() {
    this.dataCriacao = LocalDateTime.now();
    if (this.status == null) {
        this.status = "PENDENTE";
    }
}
```

O status inicial é garantido no `@PrePersist`, não no construtor — assim qualquer caminho que crie a entidade (inclusive um teste que só faz `new VendaEntity()`) sai com um estado consistente, sem depender de quem chamou lembrar de setar o status manualmente.

## Endpoints

### Criar cobrança Pix isolada

```
POST /api/pagamentos/pix
Content-Type: application/json

{
  "valor": 49.90,
  "descricao": "Pagamento avulso",
  "emailCliente": "cliente@email.com"
}
```

Resposta (`201 Created`):

```json
{
  "idPagamento": 123456789,
  "status": "pending",
  "qrCodeCopiaECola": "00020126...",
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

### Criar venda (grava no banco + gera Pix)

```
POST /vendas
Content-Type: application/json

{
  "valorTotal": 149.90,
  "emailCliente": "cliente@email.com",
  "descricao": "Pedido #1042"
}
```

### Consultar venda

```
GET /vendas/{id}
```

## Rodando localmente

Pré-requisitos: JDK 25, MySQL rodando localmente (ou em container) e uma conta de teste no Mercado Pago com um Access Token.

O arquivo `src/main/resources/application.properties` está propositalmente no `.gitignore` — é ali que fica o token do Mercado Pago e a senha do banco, e eles não têm razão nenhuma para ir parar no Git. Crie o seu com pelo menos isto:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/loja
spring.datasource.username=root
spring.datasource.password=sua_senha
spring.jpa.hibernate.ddl-auto=update

mercadopago.access-token=TEST-xxxxxxxx-xxxxxx-xxxxxxxxxxxxxxxx
```

O `access-token` é a única propriedade lida explicitamente no código (`MercadoPagoConfigSetup`), então sem ela a aplicação sobe mas qualquer chamada de pagamento falha na hora.

Depois disso:

```bash
./mvnw spring-boot:run
```

## O que ainda não está pronto (e não estou escondendo isso)

Esse projeto está numa fase de "a integração funciona", não de "está pronto para produção". Vale deixar registrado o que falta, porque quem for continuar precisa saber onde pisar com cuidado:

- **Não existe webhook.** O método `VendaService.atualizarStatusPagamento` já sabe transformar `approved`/`rejected`/`cancelled` do Mercado Pago em `PAGO`/`CANCELADO`, mas nada no projeto o chama ainda. Sem um endpoint de notificação (`POST /webhook/mercadopago` ou similar) consumindo o Webhook do Mercado Pago, toda venda fica presa em `PENDENTE` para sempre, independente do que aconteça com o pagamento de verdade.
- **Sem validação de entrada.** Os DTOs de request não têm `@NotNull`, `@Positive` ou qualquer anotação do Bean Validation. Um `valorTotal` nulo ou negativo hoje só vai falhar (ou pior, não falhar) lá na frente, dentro do SDK do Mercado Pago.
- **Tratamento de erro genérico.** `PagamentoService` captura `MPApiException`/`MPException` e relança como `RuntimeException`. Funciona, mas sem um `@ControllerAdvice` isso vira um `500` cru para o cliente da API, com mensagem de stacktrace em vez de um corpo de erro estruturado.
- **Rotas com prefixos inconsistentes.** `/vendas` e `/api/pagamentos/pix` não seguem o mesmo padrão de path. Não afeta o funcionamento, mas é o tipo de detalhe que vale padronizar antes de vazar para documentação de API pública.
- **Sem testes reais.** O único teste presente é o `contextLoads()` gerado pelo Spring Initializr — garante que o contexto sobe, não que a regra de negócio funciona.
- **Sem licença definida.** Se a ideia é abrir o código, vale adicionar uma (MIT é a escolha mais comum para projetos desse porte).

## Próximos passos sugeridos

1. Endpoint de webhook para consumir as notificações do Mercado Pago e efetivamente chamar `atualizarStatusPagamento`.
2. Bean Validation nos DTOs de request.
3. `@ControllerAdvice` com um formato de erro padronizado (código, mensagem, timestamp).
4. Padronizar os prefixos de rota.
5. Testes de unidade para `VendaService` e `PagamentoService` (o segundo dá para testar mockando o `PaymentClient`, sem bater na API real do Mercado Pago).
