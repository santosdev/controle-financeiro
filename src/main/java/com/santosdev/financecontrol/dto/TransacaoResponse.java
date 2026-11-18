package com.santosdev.financecontrol.dto;

import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        TipoTransacao tipo,
        CategoriaDTO categoria) {

    public static TransacaoResponse de(Transacao t) {
        return new TransacaoResponse(t.getId(), t.getDescricao(), t.getValor(), t.getData(),
                t.getTipo(), CategoriaDTO.de(t.getCategoria()));
    }
}
