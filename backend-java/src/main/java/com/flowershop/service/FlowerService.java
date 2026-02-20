package com.flowershop.service;

import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.model.Flower;
import com.flowershop.repository.FlowerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * FLOWER SERVICE
 * --------------
 * Business logic for the flower catalogue.
 *
 * Exceptions thrown (caught by GlobalExceptionHandler):
 * ResourceNotFoundException → 404 (flower not found by ID)
 * IllegalArgumentException → 400 (blank name, invalid price/stock)
 */
@Service
public class FlowerService {

    private final FlowerRepository flowerRepository;

    public FlowerService(FlowerRepository flowerRepository) {
        this.flowerRepository = flowerRepository;
    }

    // ─── GET ALL ──────────────────────────────────────────────────────────────

    public List<Flower> getAllFlowers() {
        return flowerRepository.findAll();
    }

    // ─── GET BY ID ────────────────────────────────────────────────────────────

    /**
     * Returns a flower or throws ResourceNotFoundException (→ 404).
     */
    public Flower getFlowerById(Long id) {
        return flowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flower not found with id: " + id));
    }

    // ─── ADD FLOWER (ADMIN) ───────────────────────────────────────────────────

    /**
     * Validates and saves a new flower.
     * Business rules:
     * ✗ Name must not be blank
     * ✗ Price must be > 0
     * ✗ Stock cannot be negative
     */
    public Flower addFlower(Flower flower) {

        if (flower.getName() == null || flower.getName().isBlank()) {
            throw new IllegalArgumentException("Flower name cannot be blank.");
        }
        if (flower.getPrice() == null || flower.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Flower price must be greater than 0.");
        }
        if (flower.getStock() == null || flower.getStock() < 0) {
            throw new IllegalArgumentException("Flower stock cannot be negative.");
        }

        return flowerRepository.save(flower);
    }

    // ─── UPDATE FLOWER (ADMIN) ────────────────────────────────────────────────

    /**
     * Partially updates an existing flower.
     * Only provided (non-null) fields are applied.
     */
    public Flower updateFlower(Long id, Flower updatedFlower) {

        Flower existing = getFlowerById(id); // throws ResourceNotFoundException if missing

        if (updatedFlower.getPrice() != null
                && updatedFlower.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Flower price must be greater than 0.");
        }
        if (updatedFlower.getStock() != null && updatedFlower.getStock() < 0) {
            throw new IllegalArgumentException("Flower stock cannot be negative.");
        }

        if (updatedFlower.getName() != null && !updatedFlower.getName().isBlank()) {
            existing.setName(updatedFlower.getName());
        }
        if (updatedFlower.getPrice() != null) {
            existing.setPrice(updatedFlower.getPrice());
        }
        if (updatedFlower.getStock() != null) {
            existing.setStock(updatedFlower.getStock());
        }
        if (updatedFlower.getImageUrl() != null) {
            existing.setImageUrl(updatedFlower.getImageUrl());
        }

        return flowerRepository.save(existing);
    }

    // ─── DELETE FLOWER (ADMIN) ────────────────────────────────────────────────

    /**
     * Deletes a flower by ID. Throws ResourceNotFoundException if not found.
     */
    public void deleteFlower(Long id) {
        if (!flowerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Flower not found with id: " + id);
        }
        flowerRepository.deleteById(id);
    }

    // ─── SEARCH BY NAME ───────────────────────────────────────────────────────

    public List<Flower> searchByName(String name) {
        return flowerRepository.findByNameContainingIgnoreCase(name);
    }
}
