package com.tienda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Categoría del catálogo (Electrónica, Hogar, etc.). */
@Entity
@Table(name = "categorias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    @NotBlank
    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activa = Boolean.TRUE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();

    /**
     * Productos de la categoría. LAZY + helper addProducto para mantener
     * ambos lados sincronizados al asignar la relación.
     */
    @OneToMany(mappedBy = "categoria", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Producto> productos = new ArrayList<>();

    public void addProducto(Producto producto) {
        productos.add(producto);
        producto.setCategoria(this);
    }
}
