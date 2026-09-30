package es.uclm.esibuy.categories.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("categorias")
public class Category {

    @Id
    private String id;

    private String nombre;

    protected Category() {
    }

    public Category(String nombre) {
        this.nombre = nombre;
    }

    public Category(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
