package com.flowershop.service;

import com.flowershop.exception.EmptyCartException;
import com.flowershop.exception.OutOfStockException;
import com.flowershop.exception.ResourceNotFoundException;
import com.flowershop.model.*;
import com.flowershop.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ORDER SERVICE
 * -------------
 * All order business logic.
 *
 * Logging (SLF4J):
 * INFO – order placed successfully (orderId, userId, total)
 * INFO – order cancelled (orderId, userId)
 * INFO – order status changed by admin (orderId, old → new)
 *
 * Exceptions thrown (→ GlobalExceptionHandler):
 * EmptyCartException → 400 (checkout with empty cart)
 * OutOfStockException → 400 (not enough stock during checkout)
 * ResourceNotFoundException → 404 (order or user not found)
 * IllegalArgumentException → 400 (invalid status, wrong owner, terminal state)
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FlowerRepository flowerRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            FlowerRepository flowerRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.flowerRepository = flowerRepository;
        this.userRepository = userRepository;
    }

    // ─── PLACE ORDER ──────────────────────────────────────────────────────────

    /**
     * Converts the user's cart into a confirmed Order.
     *
     * Steps:
     * 1. Load user and their cart
     * 2. Reject empty cart → EmptyCartException
     * 3. Validate stock per item → OutOfStockException
     * 4. Deduct stock from flower
     * 5. Build OrderItems with price SNAPSHOT (frozen at order time)
     * 6. Calculate total and save Order
     * 7. Clear the cart
     *
     * @Transactional: if ANY step fails, ALL DB changes roll back (no partial
     *                 orders).
     */
    @Transactional
    public Order placeOrder(Long userId, String deliveryAddress) {

        User user = findUserById(userId);

        // ── Cannot checkout empty cart ─────────────────────────────────────────
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new EmptyCartException(
                        "Cannot checkout empty cart. Add flowers before placing an order."));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot checkout empty cart. Add flowers before placing an order.");
        }

        // ── Validate stock and deduct in one pass ─────────────────────────────
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Flower flower = cartItem.getFlower();
            int quantity = cartItem.getQuantity();

            // Cannot order more than available stock
            if (flower.getStock() < quantity) {
                throw new OutOfStockException(
                        "Cannot order more than available stock for '" + flower.getName() +
                                "'. Available: " + flower.getStock() + ", Requested: " + quantity);
            }

            // Deduct stock (inside transaction — rolls back if anything fails)
            flower.setStock(flower.getStock() - quantity);
            flowerRepository.save(flower);

            // Snapshot: freeze price at the exact moment order is placed
            BigDecimal unitPrice = flower.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            orderItems.add(OrderItem.builder()
                    .flowerName(flower.getName()) // name snapshot — safe if flower deleted later
                    .flower(flower)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build());

            total = total.add(subtotal);
        }

        // ── Save Order + OrderItems ────────────────────────────────────────────
        Order order = Order.builder()
                .user(user)
                .totalPrice(total)
                .status(OrderStatus.PENDING)
                .deliveryAddress(deliveryAddress)
                .build();

        Order savedOrder = orderRepository.save(order);
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        savedOrder.getItems().addAll(orderItems);
        orderRepository.save(savedOrder);

        // ── Clear the cart ────────────────────────────────────────────────────
        cartItemRepository.deleteByCart(cart);

        log.info("Order placed: orderId={}, userId={}, items={}, total={}",
                savedOrder.getId(), userId, orderItems.size(), total);

        return savedOrder;
    }

    // ─── CUSTOMER: MY ORDERS ──────────────────────────────────────────────────

    /** Returns all orders placed by this user, newest first. */
    public List<Order> getMyOrders(Long userId) {
        User user = findUserById(userId);
        return orderRepository.findByUserOrderByOrderedAtDesc(user);
    }

    // ─── CUSTOMER: CANCEL ORDER ───────────────────────────────────────────────

    /**
     * Customer cancels their OWN order — only if it is still PENDING.
     * Stock is restored so items go back on sale.
     *
     * Rules:
     * ✗ Only the order's owner can cancel it
     * ✗ Only PENDING orders can be cancelled
     */
    @Transactional
    public Order cancelOrder(Long userId, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own orders.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING orders can be cancelled. This order is: " + order.getStatus());
        }

        // Restore stock for each item
        for (OrderItem item : order.getItems()) {
            if (item.getFlower() != null) {
                Flower flower = item.getFlower();
                flower.setStock(flower.getStock() + item.getQuantity());
                flowerRepository.save(flower);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order cancelled: orderId={}, userId={}", orderId, userId);
        return saved;
    }

    // ─── ADMIN: ALL ORDERS ────────────────────────────────────────────────────

    /**
     * Returns all orders, optionally filtered by status string.
     * e.g. getAllOrders("PENDING") → only pending orders.
     */
    public List<Order> getAllOrders(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                OrderStatus status = OrderStatus.valueOf(statusFilter.toUpperCase());
                return orderRepository.findByStatus(status);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid status: " + statusFilter +
                                ". Valid values: PENDING, CONFIRMED, CANCELLED, DELIVERED");
            }
        }
        return orderRepository.findAll();
    }

    // ─── ADMIN: UPDATE ORDER STATUS ───────────────────────────────────────────

    /**
     * Admin changes the status of any order.
     *
     * Valid transitions:
     * PENDING → CONFIRMED or CANCELLED
     * CONFIRMED → DELIVERED or CANCELLED (restores stock if cancelled)
     * DELIVERED → terminal (no further changes)
     * CANCELLED → terminal (no further changes)
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatusStr) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: " + newStatusStr +
                            ". Valid values: PENDING, CONFIRMED, CANCELLED, DELIVERED");
        }

        OrderStatus current = order.getStatus();

        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot change status of a " + current + " order. It is already in a terminal state.");
        }

        // Restore stock if admin cancels a CONFIRMED order
        if (newStatus == OrderStatus.CANCELLED && current == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                if (item.getFlower() != null) {
                    Flower flower = item.getFlower();
                    flower.setStock(flower.getStock() + item.getQuantity());
                    flowerRepository.save(flower);
                }
            }
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        log.info("Order status updated: orderId={}, {} → {}", orderId, current, newStatus);
        return saved;
    }

    // ─── PRIVATE HELPER ───────────────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
