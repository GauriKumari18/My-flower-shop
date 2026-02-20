package com.flowershop.repository;

import com.flowershop.model.Flower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * FLOWER REPOSITORY
 * -----------------
 * This interface connects the app to the "flowers" table in PostgreSQL.
 *
 * By extending JpaRepository<Flower, Long>, Spring automatically provides:
 *
 * save(flower) → INSERT or UPDATE a flower
 * findById(id) → SELECT flower WHERE id = ?
 * findAll() → SELECT * FROM flowers
 * deleteById(id) → DELETE FROM flowers WHERE id = ?
 * existsById(id) → check if a flower exists
 *
 * No SQL or implementation code is needed — Spring generates it all.
 * Custom queries can be added below when needed.
 */
@Repository
public interface FlowerRepository extends JpaRepository<Flower, Long> {

    /**
     * Find all flowers that match a given name (case-insensitive).
     * Spring generates: SELECT * FROM flowers WHERE LOWER(name) LIKE LOWER(?)
     *
     * Used later for search functionality.
     */
    List<Flower> findByNameContainingIgnoreCase(String name);

    /**
     * Find all flowers that currently have stock > 0.
     * Used to show only available flowers to customers.
     */
    List<Flower> findByStockGreaterThan(Integer minStock);
}
