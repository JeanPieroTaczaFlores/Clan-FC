package com.tienda.controller;

import com.tienda.dto.ImpuestosPreviewRequest;
import com.tienda.dto.TotalesImpuestosResponse;
import com.tienda.model.EmpresaCliente;
import com.tienda.model.Pais;
import com.tienda.model.Usuario;
import com.tienda.repository.EmpresaClienteRepository;
import com.tienda.repository.UsuarioRepository;
import com.tienda.service.TaxCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * CALCULADORA DE IMPUESTOS en vivo (IVA por país).
 * El frontend (carrito/POS) llama a /api/impuestos/preview para pintar el
 * desglose subtotal-IVA-total con la tasa del país/régimen del cliente.
 * Para consumidor final se usa el PAÍS DEL USUARIO autenticado.
 */
@RestController
@RequestMapping("/api/impuestos")
@RequiredArgsConstructor
public class ImpuestosController {

    private final TaxCalculationService taxService;
    private final EmpresaClienteRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/preview")
    public TotalesImpuestosResponse previsualizar(@Valid @RequestBody ImpuestosPreviewRequest request,
                                                  Authentication authentication) {
        EmpresaCliente empresa = null;
        if (request.empresaClienteId() != null) {
            empresa = empresaRepository.findById(request.empresaClienteId()).orElse(null);
        }

        // País fiscal del usuario logueado (null en llamadas anónimas).
        Pais paisUsuario = null;
        if (authentication != null) {
            paisUsuario = usuarioRepository.findByUsernameIgnoreCase(authentication.getName())
                    .map(Usuario::getPais)
                    .orElse(null);
        }
        return taxService.calcularTotales(request.subtotalBase(), empresa, paisUsuario);
    }
}
