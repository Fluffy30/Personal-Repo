import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class InventorySystem extends JFrame {

    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=InventoryDB;encrypt=false";
    private static final String DB_USER = "sa";              // your SQL login
    private static final String DB_PASSWORD = "12345678"; // your password

    // Product management fields
    private JTextField productIdField;
    private JTextField productNameField;
    private JTextField priceField;
    private JTextField quantityField;

    // Sales management fields
    private JTextField saleProductIdField;
    private JTextField quantitySoldField;
    private JTable table;
    private DefaultTableModel model;

    public InventorySystem() {
        initComponents();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventorySystem frame = new InventorySystem();
            frame.setVisible(true);
        });
    }

    private void initComponents() {
        setTitle("Sales Inventory Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // ===== Product Management Panel =====
        JPanel productPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        productPanel.setBorder(BorderFactory.createTitledBorder("Product Management"));

        productIdField = new JTextField();
        productNameField = new JTextField();
        priceField = new JTextField();
        quantityField = new JTextField();

        JButton addProductButton = new JButton("Add Product");
        JButton updateStockButton = new JButton("Update Stock");

        productPanel.add(new JLabel("Product ID:"));
        productPanel.add(productIdField);
        productPanel.add(new JLabel("Product Name:"));
        productPanel.add(productNameField);
        productPanel.add(new JLabel("Price per Unit:"));
        productPanel.add(priceField);
        productPanel.add(new JLabel("Quantity in Stock:"));
        productPanel.add(quantityField);
        productPanel.add(addProductButton);
        productPanel.add(updateStockButton);

        // ===== Sales Management Panel =====
        JPanel salesPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        salesPanel.setBorder(BorderFactory.createTitledBorder("Sales Management"));

        saleProductIdField = new JTextField();
        quantitySoldField = new JTextField();
        JButton recordSaleButton = new JButton("Record Sale");

        salesPanel.add(new JLabel("Product ID:"));
        salesPanel.add(saleProductIdField);
        salesPanel.add(new JLabel("Quantity Sold:"));
        salesPanel.add(quantitySoldField);
        salesPanel.add(recordSaleButton);

        // ===== Top container panel =====
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        topPanel.add(productPanel);
        topPanel.add(salesPanel);

        add(topPanel, BorderLayout.NORTH);

        // ===== Inventory table =====
        model = new DefaultTableModel(
                new String[]{"Product ID", "Product Name", "Price/Unit", "Quantity in Stock"}, 0
        );
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton viewInventoryButton = new JButton("View Inventory");
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(viewInventoryButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // ===== Button actions =====

        // Add Product
        addProductButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAddProduct();
            }
        });

        // Update Stock
        updateStockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleUpdateStock();
            }
        });

        // Record Sale
        recordSaleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRecordSale();
            }
        });

        // View Inventory
        viewInventoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleViewInventory();
            }
        });
    }

    // ====== Add Product ======
    private void handleAddProduct() {
        String productId = productIdField.getText().trim();
        String name = productNameField.getText().trim();
        String priceText = priceField.getText().trim();
        String quantityText = quantityField.getText().trim();

        if (productId.isEmpty() || name.isEmpty() || priceText.isEmpty() || quantityText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "All product fields must be filled.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        double price;
        int quantity;
        try {
            price = Double.parseDouble(priceText);
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Price must be a number and Quantity must be an integer.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "INSERT INTO Products (product_id, product_name, price_per_unit, quantity_in_stock) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = connectToDatabase();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, productId);
            ps.setString(2, name);
            ps.setDouble(3, price);
            ps.setInt(4, quantity);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this,
                        "Product added successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                // Clear fields
                productIdField.setText("");
                productNameField.setText("");
                priceField.setText("");
                quantityField.setText("");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to add product.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database connection not available.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ====== Update Stock ======
    private void handleUpdateStock() {
        String productId = productIdField.getText().trim();
        String quantityText = quantityField.getText().trim();

        if (productId.isEmpty() || quantityText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Product ID and Quantity in Stock must be filled to update stock.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Quantity must be a valid integer.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "UPDATE Products SET quantity_in_stock = ? WHERE product_id = ?";

        try (Connection conn = connectToDatabase();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setString(2, productId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this,
                        "Stock updated successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No product found with given Product ID.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database connection not available.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ====== Record Sale ======
    private void handleRecordSale() {
        String productId = saleProductIdField.getText().trim();
        String quantitySoldText = quantitySoldField.getText().trim();

        if (productId.isEmpty() || quantitySoldText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Product ID and Quantity Sold must be filled.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quantitySold;
        try {
            quantitySold = Integer.parseInt(quantitySoldText);
            if (quantitySold <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Quantity Sold must be a positive integer.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Quantity Sold must be a valid integer.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Connection conn = null;
        PreparedStatement selectPs = null;
        PreparedStatement updatePs = null;
        PreparedStatement insertSalePs = null;
        ResultSet rs = null;

        try {
            conn = connectToDatabase();
            if (conn == null) {
                throw new SQLException("Connection is null.");
            }

            conn.setAutoCommit(false); // transaction

            // 1. Check current stock
            String selectSql = "SELECT quantity_in_stock FROM Products WHERE product_id = ?";
            selectPs = conn.prepareStatement(selectSql);
            selectPs.setString(1, productId);
            rs = selectPs.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this,
                        "No product found with given Product ID.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                conn.rollback();
                return;
            }

            int currentStock = rs.getInt("quantity_in_stock");

            if (quantitySold > currentStock) {
                JOptionPane.showMessageDialog(this,
                        "Not enough stock available. Current stock: " + currentStock,
                        "Stock Error",
                        JOptionPane.ERROR_MESSAGE);
                conn.rollback();
                return;
            }

            // 2. Update Products stock
            String updateSql = "UPDATE Products SET quantity_in_stock = quantity_in_stock - ? " +
                    "WHERE product_id = ?";
            updatePs = conn.prepareStatement(updateSql);
            updatePs.setInt(1, quantitySold);
            updatePs.setString(2, productId);
            updatePs.executeUpdate();

            // 3. Insert into Sales table (sale_date uses GETDATE() on SQL Server)
            String insertSaleSql = "INSERT INTO Sales (product_id, quantity_sold, sale_date) " +
                    "VALUES (?, ?, GETDATE())";
            insertSalePs = conn.prepareStatement(insertSaleSql);
            insertSalePs.setString(1, productId);
            insertSalePs.setInt(2, quantitySold);
            insertSalePs.executeUpdate();

            conn.commit();

            JOptionPane.showMessageDialog(this,
                    "Sale recorded successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            saleProductIdField.setText("");
            quantitySoldField.setText("");

        } catch (SQLException ex) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ignore) { }
            JOptionPane.showMessageDialog(this,
                    "Database error: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database connection not available.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (selectPs != null) selectPs.close(); } catch (SQLException ignored) {}
            try { if (updatePs != null) updatePs.close(); } catch (SQLException ignored) {}
            try { if (insertSalePs != null) insertSalePs.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    // ====== View Inventory ======
    private void handleViewInventory() {
        model.setRowCount(0);

        String sql = "SELECT product_id, product_name, price_per_unit, quantity_in_stock FROM Products";

        try (Connection conn = connectToDatabase();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String productId = rs.getString("product_id");
                String name = rs.getString("product_name");
                double price = rs.getDouble("price_per_unit");
                int quantity = rs.getInt("quantity_in_stock");

                model.addRow(new Object[]{productId, name, price, quantity});
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error fetching inventory: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (NullPointerException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database connection not available.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Connection connectToDatabase() {
        try {
            // Ensure SQL Server JDBC driver is on the classpath in IntelliJ [web:80][web:87][web:100]
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Database connection failed: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}