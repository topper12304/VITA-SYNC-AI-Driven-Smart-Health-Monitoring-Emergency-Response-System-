package com.vitasync.model;

/**
 * Represents a single medicine/supply item in the hospital inventory.
 * Used by InventoryManager (TreeMap-based BST equivalent).
 */
public class InventoryItem {

    private final int    itemId;
    private       String name;
    private       int    quantity;
    private       String category; // e.g., "Medicine", "Equipment", "Consumable"

    public InventoryItem(int itemId, String name, int quantity, String category) {
        this.itemId    = itemId;
        this.name      = name;
        this.quantity  = quantity;
        this.category  = category;
    }

    public InventoryItem(int itemId, String name, int quantity) {
        this(itemId, name, quantity, "General");
    }

    // ---- Getters ----
    public int    getItemId()   { return itemId; }
    public String getName()     { return name; }
    public int    getQuantity() { return quantity; }
    public String getCategory() { return category; }

    // ---- Setters ----
    public void setName(String name)         { this.name = name; }
    public void setQuantity(int quantity)    { this.quantity = quantity; }
    public void setCategory(String category) { this.category = category; }

    /** Returns true if stock is critically low (≤ 5 units). */
    public boolean isLowStock() { return quantity <= 5; }

    @Override
    public String toString() {
        return String.format("InventoryItem{id=%d, name='%s', qty=%d, category='%s'}",
                itemId, name, quantity, category);
    }
}
