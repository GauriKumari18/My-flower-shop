package com.flowershop.controller;

import com.flowershop.model.Flower;
import com.flowershop.service.FlowerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FLOWER CONTROLLER
 * -----------------
 * REST endpoints for the flower catalogue.
 *
 * ── PUBLIC (no token required) ────────────────────────────────────────────
 * GET /api/flowers → list all flowers
 * GET /api/flowers/{id} → get one flower
 * GET /api/flowers/search?name → search by name
 *
 * ── ADMIN ONLY (requires JWT with role=ADMIN) ──────────────────────────────
 * POST /api/flowers → add a new flower
 * PUT /api/flowers/{id} → update a flower
 * DELETE /api/flowers/{id} → delete a flower
 *
 * Role enforcement is handled by SecurityConfig (no manual checks needed here).
 * Error handling is handled by GlobalExceptionHandler (no try-catch needed
 * here).
 */
@RestController
@RequestMapping("/api/flowers")
@CrossOrigin(origins = "*")
public class FlowerController {

    private final FlowerService flowerService;

    public FlowerController(FlowerService flowerService) {
        this.flowerService = flowerService;
    }

    // ─── GET ALL FLOWERS (public) ─────────────────────────────────────────────

    /**
     * GET /api/flowers
     * Response: 200 + JSON array of all flowers
     */
    @GetMapping
    public ResponseEntity<List<Flower>> getAllFlowers() {
        return ResponseEntity.ok(flowerService.getAllFlowers());
    }

    // ─── GET BY ID (public) ───────────────────────────────────────────────────

    /**
     * GET /api/flowers/{id}
     * Response: 200 + flower JSON
     * 404 if flower not found → ResourceNotFoundException → GlobalExceptionHandler
     */
    @GetMapping("/{id}")
    public ResponseEntity<Flower> getFlowerById(@PathVariable Long id) {
        return ResponseEntity.ok(flowerService.getFlowerById(id));
    }

    // ─── SEARCH BY NAME (public) ──────────────────────────────────────────────

    /**
     * GET /api/flowers/search?name=rose
     * Response: 200 + JSON array (may be empty)
     */
    @GetMapping("/search")
    public ResponseEntity<List<Flower>> searchFlowers(@RequestParam String name) {
        return ResponseEntity.ok(flowerService.searchByName(name));
    }

    // ─── ADD FLOWER (ADMIN only) ──────────────────────────────────────────────

    /**
     * POST /api/flowers
     * Requires: Authorization: Bearer <ADMIN token>
     *
     * Request body example:
     * {
     * "name": "Red Rose",
     * "price": 4.99,
     * "stock": 100,
     * "imageUrl": "https://example.com/rose.jpg"
     * }
     *
     * Response: 201 Created + saved flower
     * 403 Forbidden if caller is not ADMIN
     * 400 Bad Request if validation fails
     */
    @PostMapping
    public ResponseEntity<Flower> addFlower(@RequestBody Flower flower) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flowerService.addFlower(flower));
    }

    // ─── UPDATE FLOWER (ADMIN only) ───────────────────────────────────────────

    /**
     * PUT /api/flowers/{id}
     * Requires: Authorization: Bearer <ADMIN token>
     * Response: 200 + updated flower | 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Flower> updateFlower(@PathVariable Long id,
            @RequestBody Flower updatedFlower) {
        return ResponseEntity.ok(flowerService.updateFlower(id, updatedFlower));
    }

    // ─── DELETE FLOWER (ADMIN only) ───────────────────────────────────────────

    /**
     * DELETE /api/flowers/{id}
     * Requires: Authorization: Bearer <ADMIN token>
     * Response: 204 No Content | 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlower(@PathVariable Long id) {
        flowerService.deleteFlower(id);
        return ResponseEntity.noContent().build();
    }
}
