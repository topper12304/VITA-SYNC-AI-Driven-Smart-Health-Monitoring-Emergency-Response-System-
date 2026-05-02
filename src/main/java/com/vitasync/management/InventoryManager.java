package com.vitasync.management;

import com.vitasync.model.InventoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Manages hospital medicine/supply inventory.
 *
 * Data Structure: TreeMap<itemId, InventoryItem> — Java equivalent of the BST from VITAL-CONNECT.
 * TreeMap maintains sorted order by itemId (same as BST inorder traversal).
 * All operations backed by MySQL for persistence.
 */
public class InventoryManager {

    private static final Logger log = LoggerFactory.getLogger(InventoryManager.class);

    // BST equivalent: TreeMap keeps items sorted by ID (O(log n) insert/search)
    private final TreeMap<Integer, InventoryItem> inventory = new TreeMap<>();

    private final HikariDataSource dataSource;

    public InventoryManager(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        loadFromDatabase();
    }

    /** In-memory-only constructor. */
    public InventoryManager() {
        this.dataSource = null;
        seedDefaultItems();
    }

    // -------------------------------------------------------------------------
    // Core Operations (BST equivalent)
    // -------------------------------------------------------------------------

    /**
     * Adds a new item or updates quantity if item already exists.
     * Mirrors VITAL-CONNECT's insertItem() BST logic.
     */
    public void addOrUpdateItem(int id, String name, int quantity, String category) {
        if (inventory.containsKey(id)) {
            InventoryItem existing = inventory.get(id);
            existing.setQuantity(existing.getQuantity() + quantity);
            persistToDb(existing);
            log.info("Updated inventory: {} — new qty={}", name, existing.getQuantity());
        } else {
            InventoryItem item = new InventoryItem(id, name, quantity, category);
            inventory.put(id, item);
            persistToDb(item);
            log.info("Added inventory item: {}", item);
        }
    }

    /**
     * Searches for an item by ID — O(log n) TreeMap lookup.
     * Mirrors VITAL-CONNECT's searchItemByID().
     */
    public Optional<InventoryItem> searchById(int id) {
        return Optional.ofNullable(inventory.get(id));
    }

    /**
     * Returns all items sorted by ID (inorder traversal of BST).
     * Mirrors VITAL-CONNECT's displayInventory().
     */
    public List<InventoryItem> getAllItemsSorted() {
        return new ArrayList<>(inventory.values());
    }

    /**
     * Reduces stock when medicine is used for a patient.
     * Returns false if insufficient stock.
     */
    public boolean useItem(int id, int quantity) {
        InventoryItem item = inventory.get(id);
        if (item == null || item.getQuantity() < quantity) {
            log.warn("Insufficient stock for item ID {}", id);
            return false;
        }
        item.setQuantity(item.getQuantity() - quantity);
        persistToDb(item);
        log.info("Used {} units of '{}'. Remaining: {}", quantity, item.getName(), item.getQuantity());
        return true;
    }

    /** Returns items with critically low stock (≤ 5 units). */
    public List<InventoryItem> getLowStockItems() {
        return inventory.values().stream()
                .filter(InventoryItem::isLowStock)
                .toList();
    }

    public int getTotalItemCount() { return inventory.size(); }

    // -------------------------------------------------------------------------
    // Database operations
    // -------------------------------------------------------------------------

    private void loadFromDatabase() {
        if (dataSource == null) { seedDefaultItems(); return; }
        String sql = "SELECT item_id, name, quantity, category FROM inventory";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InventoryItem item = new InventoryItem(
                        rs.getInt("item_id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getString("category")
                );
                inventory.put(item.getItemId(), item);
            }
            log.info("Loaded {} inventory items from database.", inventory.size());
            if (inventory.isEmpty()) seedDefaultItems();
        } catch (SQLException e) {
            log.warn("Could not load inventory from DB: {}. Using defaults.", e.getMessage());
            seedDefaultItems();
        }
    }

    private void persistToDb(InventoryItem item) {
        if (dataSource == null) return;
        String sql = "INSERT INTO inventory (item_id, name, quantity, category) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE name=VALUES(name), quantity=VALUES(quantity), category=VALUES(category)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getItemId());
            ps.setString(2, item.getName());
            ps.setInt(3, item.getQuantity());
            ps.setString(4, item.getCategory());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to persist inventory item {}: {}", item.getName(), e.getMessage());
        }
    }

    private void seedDefaultItems() {
        if (!inventory.isEmpty()) return;
        addOrUpdateItem(101, "Paracetamol 500mg",    200, "Medicine");
        addOrUpdateItem(102, "Aspirin 75mg",          150, "Medicine");
        addOrUpdateItem(103, "Oxygen Cylinder",        10, "Equipment");
        addOrUpdateItem(104, "IV Saline 500ml",        80, "Consumable");
        addOrUpdateItem(105, "Surgical Gloves (box)",  30, "Consumable");
        addOrUpdateItem(106, "Morphine 10mg",          25, "Medicine");
        addOrUpdateItem(107, "Adrenaline 1mg/ml",      15, "Medicine");
        addOrUpdateItem(108, "Defibrillator Pads",      8, "Equipment");
        addOrUpdateItem(109, "Bandage Roll",           100, "Consumable");
        addOrUpdateItem(110, "Antiseptic Solution",    50, "Medicine");
        log.info("Seeded {} default inventory items.", inventory.size());
    }
}
