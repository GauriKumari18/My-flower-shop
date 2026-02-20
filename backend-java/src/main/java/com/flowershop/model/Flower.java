package com.flowershop.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * FLOWER MODEL
 * ------------
 * This class maps directly to the "flowers" table in PostgreSQL.
 *
 * @Entity → tells JPA this is a database table
 * @Table → specifies the exact table name
 *        Lombok annotations remove boilerplate (getters, setters, constructors)
 */
@Entity
@Table(name = "flowers") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flower {

    /** Primary key — auto-incremented by the database */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name of the flower (e.g. "Red Rose", "White Lily") */
    @Column(nullable = false)
    private String name;

    /**
     * Price of the flower.
     * BigDecimal is used (not double) to avoid floating-point rounding errors
     * when dealing with money.
     */
    @Column(nullable = false)
    private BigDecimal price;

    /**
     * How many units are available in stock.
     * Business rule (enforced in service): cannot be negative.
     */
    @Column(nullable = false)
    private Integer stock;

    /**
     * URL pointing to the flower's product image.
     * Optional field — can be null if no image is provided.
     */
    @Column
    private String imageUrl;
}
