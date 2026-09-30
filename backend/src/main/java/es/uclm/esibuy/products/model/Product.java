package es.uclm.esibuy.products.model;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("productos")
public class Product {

    @Id
    private String id;

    private String nombre;
    private List<String> categoriaIds;
    private int stock;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal descuento;
    private BigDecimal descuentoPremium;
    private String imagen;
    private boolean disponible;
    private String vendedorId;

    protected Product() {
    }

    public Product(
            String nombre,
            List<String> categoriaIds,
            int stock,
            String descripcion,
            BigDecimal precio,
            BigDecimal descuento,
            BigDecimal descuentoPremium,
            String imagen,
            boolean disponible,
            String vendedorId) {
        this.nombre = nombre;
        this.categoriaIds = List.copyOf(categoriaIds);
        this.stock = stock;
        this.descripcion = descripcion;
        this.precio = precio;
        this.descuento = descuento;
        this.descuentoPremium = descuentoPremium;
        this.imagen = imagen;
        this.disponible = disponible;
        this.vendedorId = vendedorId;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getCategoriaIds() {
        return categoriaIds;
    }

    public int getStock() {
        return stock;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public BigDecimal getDescuentoPremium() {
        return descuentoPremium;
    }

    public String getImagen() {
        return imagen;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public String getVendedorId() {
        return vendedorId;
    }
}
