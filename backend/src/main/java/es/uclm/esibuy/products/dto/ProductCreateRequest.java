package es.uclm.esibuy.products.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ProductCreateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotEmpty(message = "Debe seleccionar al menos una categoría")
    private List<@NotBlank(message = "La referencia de categoría no puede estar vacía") String> categoriaIds;

    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer stock;

    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que 0")
    private BigDecimal precio;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser menor que 0")
    @DecimalMax(value = "100.0", message = "El descuento no puede superar el 100")
    private BigDecimal descuento;

    @DecimalMin(value = "0.0", message = "El descuento premium no puede ser menor que 0")
    @DecimalMax(value = "100.0", message = "El descuento premium no puede superar el 100")
    private BigDecimal descuentoPremium;

    @Size(max = 2000, message = "La imagen no puede superar los 2000 caracteres")
    private String imagen;

    @NotNull(message = "Debe indicar si el producto está disponible")
    private Boolean disponible;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<String> getCategoriaIds() {
        return categoriaIds;
    }

    public void setCategoriaIds(List<String> categoriaIds) {
        this.categoriaIds = categoriaIds;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getDescuentoPremium() {
        return descuentoPremium;
    }

    public void setDescuentoPremium(BigDecimal descuentoPremium) {
        this.descuentoPremium = descuentoPremium;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }
}
