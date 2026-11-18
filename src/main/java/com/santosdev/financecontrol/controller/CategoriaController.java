package com.santosdev.financecontrol.controller;

import com.santosdev.financecontrol.dto.CategoriaDTO;
import com.santosdev.financecontrol.model.TipoTransacao;
import com.santosdev.financecontrol.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoriaDTO> listar(@RequestParam(required = false) TipoTransacao tipo) {
        return service.listar(tipo);
    }

    @GetMapping("/{id}")
    public CategoriaDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaDTO criar(@Valid @RequestBody CategoriaDTO dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public CategoriaDTO atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaDTO dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
