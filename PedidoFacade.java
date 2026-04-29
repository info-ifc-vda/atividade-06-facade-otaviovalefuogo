import modelos.Pedido;
import modelos.ResultadoPedido;
import sistemas.EstoqueService;
import sistemas.FreteService;
import sistemas.NotificacaoService;
import sistemas.PagamentoService;
import modelos.ResultadoPagamento;
import modelos.ResultadoColeta;

/**
 * PedidoFacade fornece uma interface unificada e simplificada
 * para o processo de finalização de pedidos, ocultando a
 * complexidade dos subsistemas de Estoque, Pagamento, Frete
 * e Notificação do código cliente.
 */
public class PedidoFacade {
    // Os subsistemas são instanciados aqui e nunca expostos ao cliente
    private final EstoqueService     estoque     = new EstoqueService();
    private final PagamentoService   pagamento   = new PagamentoService();
    private final FreteService       frete       = new FreteService();
    private final NotificacaoService notificacao = new NotificacaoService();

    public ResultadoPedido finalizarPedido(Pedido pedido) {
        if (!estoque.verificarDisponibilidade(pedido.produtoId, pedido.quantidade)) {
            return new ResultadoPedido(false, "Product out of stock or quantity unavailable.");
        }
        estoque.reservarItens(pedido.produtoId, pedido.quantidade);

        if (!pagamento.validarCartao(pedido.dadosCartao)) {
            return new ResultadoPedido(false, "Invalid card. Please check your details.");
        }
        ResultadoPagamento resPagamento = pagamento.processarCobranca(pedido.valor, pedido.dadosCartao);
        if (!resPagamento.sucesso) {
            return new ResultadoPedido(false, "Card transaction failed.");
        }

        double valorFrete = frete.calcularFrete(pedido.cep, pedido.peso);
        System.out.printf("[Facade] shipping value R$ %.2f\n", valorFrete);
        ResultadoColeta resColeta = frete.agendarColeta(pedido.cep, resPagamento.transacaoId);

        notificacao.enviarEmail(pedido.email, "Payment approved! Transaction: " + resPagamento.transacaoId);
        notificacao.enviarSMS(pedido.telefone, "Your order is being processed. Tracking: " + resColeta.codigo);

        ResultadoPedido resultadoFinal = new ResultadoPedido(true, "Order completed successfully!");
        resultadoFinal.transacaoId = resPagamento.transacaoId;
        resultadoFinal.codigoColeta = resColeta.codigo;
        resultadoFinal.prazoEntrega = resColeta.prazo;
        return resultadoFinal;
    }
    public ResultadoPedido cancelarPedido(String produtoId, int quantidade, String transacaoId) {
        System.out.println("\nCancellation");
        return new ResultadoPedido(true, "Order cancelled and refunded..");
    }
    public String consultarStatus(String transacaoId) {

        return "The transaction " + transacaoId + " is on route.";
    }
    
}
