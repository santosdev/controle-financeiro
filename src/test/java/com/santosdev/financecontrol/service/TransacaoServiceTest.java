package com.santosdev.financecontrol.service;

import com.santosdev.financecontrol.dto.TransacaoRequest;
import com.santosdev.financecontrol.dto.TransacaoResponse;
import com.santosdev.financecontrol.exception.RecursoNaoEncontradoException;
import com.santosdev.financecontrol.exception.RegraDeNegocioException;
import com.santosdev.financecontrol.model.Categoria;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.model.Transacao;
import com.santosdev.financecontrol.repository.CategoriaRepository;
import com.santosdev.financecontrol.repository.TransacaoRepository;
import com.santosdev.financecontrol.repository.UsuarioRepository;
import com.santosdev.financecontrol.security.UsuarioAtual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    private static final Long USUARIO_ID = 7L;

    @Mock
    private TransacaoRepository transacoes;
    @Mock
    private CategoriaRepository categorias;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private UsuarioAtual usuarioAtual;

    @InjectMocks
    private TransacaoService service;

    @BeforeEach
    void usuarioLogado() {
        lenient().when(usuarioAtual.id()).thenReturn(USUARIO_ID);
    }

    private static Categoria categoria(String nome, TipoTransacao tipo) {
        return new Categoria(null, nome, tipo);
    }

    private static TransacaoRequest pedido(String descricao, String valor, TipoTransacao tipo) {
        return new TransacaoRequest(descricao, new BigDecimal(valor), LocalDate.of(2026, 11, 10), tipo, 3L);
    }

    @Test
    void criarSalvaComOsDadosDoPedidoEDescricaoSemEspacosNasPontas() {
        when(categorias.findByIdAndUsuarioId(3L, USUARIO_ID)).thenReturn(Optional.of(categoria("Salário", TipoTransacao.RECEITA)));
        when(transacoes.save(any(Transacao.class))).thenAnswer(i -> i.getArgument(0));

        TransacaoResponse resposta = service.criar(pedido("  Salário de novembro  ", "3500.00", TipoTransacao.RECEITA));

        ArgumentCaptor<Transacao> salva = ArgumentCaptor.forClass(Transacao.class);
        verify(transacoes).save(salva.capture());
        assertThat(salva.getValue().getDescricao()).isEqualTo("Salário de novembro");
        assertThat(salva.getValue().getValor()).isEqualByComparingTo("3500.00");
        assertThat(salva.getValue().getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(resposta.categoria().nome()).isEqualTo("Salário");
    }

    @Test
    void criarRecusaCategoriaDeOutroTipo() {
        when(categorias.findByIdAndUsuarioId(3L, USUARIO_ID)).thenReturn(Optional.of(categoria("Salário", TipoTransacao.RECEITA)));

        assertThatThrownBy(() -> service.criar(pedido("Mercado", "50.00", TipoTransacao.DESPESA)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Salário");

        verify(transacoes, never()).save(any());
    }

    @Test
    void criarRecusaCategoriaDeOutroUsuario() {
        // A busca e feita pelo dono: categoria alheia volta vazia, como se nao existisse.
        when(categorias.findByIdAndUsuarioId(3L, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(pedido("Mercado", "50.00", TipoTransacao.DESPESA)))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(transacoes, never()).save(any());
    }

    @Test
    void buscarMovimentacaoDeOutroUsuarioDaNaoEncontrado() {
        when(transacoes.buscarDoUsuario(99L, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void excluirMovimentacaoDeOutroUsuarioNaoApagaNada() {
        when(transacoes.buscarDoUsuario(99L, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(99L)).isInstanceOf(RecursoNaoEncontradoException.class);

        verify(transacoes, never()).delete(any());
    }

    @Test
    void excluirApagaAMovimentacaoDoUsuario() {
        Transacao existente = new Transacao();
        when(transacoes.buscarDoUsuario(5L, USUARIO_ID)).thenReturn(Optional.of(existente));

        service.excluir(5L);

        verify(transacoes).delete(existente);
    }

    @Test
    void atualizarAlteraOsCamposDaMovimentacaoExistente() {
        Transacao existente = new Transacao();
        when(transacoes.buscarDoUsuario(5L, USUARIO_ID)).thenReturn(Optional.of(existente));
        when(categorias.findByIdAndUsuarioId(3L, USUARIO_ID)).thenReturn(Optional.of(categoria("Mercado", TipoTransacao.DESPESA)));

        service.atualizar(5L, pedido("Compras do mês", "320.45", TipoTransacao.DESPESA));

        assertThat(existente.getDescricao()).isEqualTo("Compras do mês");
        assertThat(existente.getValor()).isEqualByComparingTo("320.45");
        assertThat(existente.getTipo()).isEqualTo(TipoTransacao.DESPESA);
    }

    @Test
    void listarRecusaPeriodoComInicioDepoisDoFim() {
        assertThatThrownBy(() -> service.listar(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1), null, null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("data inicial");

        verifyNoInteractions(transacoes);
    }

    @Test
    void listarFiltraSempreComOIdDoUsuarioLogado() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 30);
        when(transacoes.filtrar(USUARIO_ID, inicio, fim, 3L, TipoTransacao.DESPESA)).thenReturn(List.of());

        assertThat(service.listar(inicio, fim, 3L, TipoTransacao.DESPESA)).isEmpty();

        verify(transacoes).filtrar(USUARIO_ID, inicio, fim, 3L, TipoTransacao.DESPESA);
    }
}
