package org.example.console;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.example.Product;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.example.util.HibernateUtil;

import jakarta.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleApp {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleApp.class);
    private static final Logger displayLogger = LoggerFactory.getLogger("display");
    private final Scanner scanner;
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    private static final String PRINT_COMMAND = "print";
    private static final String EXIT_COMMAND = "exit";
    private final Map<Product, Integer> scannedProducts = new HashMap<>();

    public ConsoleApp() {
        scanner = new Scanner(System.in);
    }

    public void run() {
        displayLogger.info("Welcome to the Product Scanner App!");
        displayLogger.info("---------------------------------");
        displayLogger.info("Instructions:");
        displayLogger.info("* Scan barcodes to add products to your order.");
        displayLogger.info("* Type '"+ PRINT_COMMAND +"' to view the current order.");
        displayLogger.info("* Type '"+ EXIT_COMMAND +"' to close the application.");
        displayLogger.info("---------------------------------");
        logger.info("Application started.");

        while (true) {
            String barcodeInput = scanner.nextLine();
            if (barcodeInput.equals(EXIT_COMMAND)) {
                break;
            } else if (barcodeInput.equals(PRINT_COMMAND)) {
                printScannedProducts();
            } else {
                handleInput(barcodeInput);
            }
        }

        logger.info("Exiting application.");
        sessionFactory.close();
    }

    public void handleInput(String barcodeInput) {
        if (isDataBarcode(barcodeInput)) {
            try (Session session = sessionFactory.openSession()) {
                processBarcode(session, barcodeInput);
            } catch (HibernateException e) {
                logger.error("Hibernate error occurred: {}", e.getMessage(), e);
            }
        } else {
            displayLogger.info("Input is not a barcode!");
        }
    }

    private void processBarcode(Session session, String barcodeInput) {
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Product product = findProductByBarcode(session, barcodeInput);
            if (product != null) {
                handleProductScan(session, product);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    private Product findProductByBarcode(Session session, String barcodeInput) {
        try {
            return session.createQuery("from Product where barcode = :barcode", Product.class)
                    .setParameter("barcode", barcodeInput)
                    .getSingleResult();
        } catch (NoResultException e) {
            displayLogger.info("Product with barcode: " + barcodeInput + " does not exist.");
            return null;
        }
    }

    private void handleProductScan(Session session, Product product) {
        logger.info("Product scanned: {}", product.getName());
        if (product.getAvailable_quantity() > 0) {
            updateProductQuantity(session, product);
            scannedProducts.put(product, scannedProducts.getOrDefault(product, 0) + 1);
        } else {
            displayLogger.info("Product: {} is currently out of stock. Cannot add to order.", product.getName());
        }
    }

    private void updateProductQuantity(Session session, Product product) {
        product.setAvailable_quantity(product.getAvailable_quantity() - 1);
        session.update(product);
        displayLogger.info("Product: {}, Remaining quantity: {}", product.getName(), product.getAvailable_quantity());
    }

    public boolean isDataBarcode(String barcodeInput) {
        if (barcodeInput == null || barcodeInput.trim().isEmpty()) {
            return false;
        }
        return barcodeInput.matches("\\d+");
    }

    public void printScannedProducts() {
        final int RECEIPT_WIDTH = 60;
        displayLogger.info(repeatChar("-", RECEIPT_WIDTH));
        displayLogger.info(centerText("----- Scanned Products -----", RECEIPT_WIDTH));
        displayLogger.info(repeatChar("-", RECEIPT_WIDTH));

        AtomicReference<Double> totalAmount = new AtomicReference<>(0.0);

        scannedProducts.forEach((product, quantity) -> {
            double totalPrice = product.getPrice() * quantity;
            totalAmount.updateAndGet(v -> v + totalPrice);

            String productName = product.getName();
            String price = String.format("%.2f zł", product.getPrice());
            String quantityStr = "x" + quantity;
            String total = String.format("%.2f zł", totalPrice);

            String line = formatReceiptLine(productName, "", rightAlignText(price + " " + quantityStr + " " + total), RECEIPT_WIDTH);
            displayLogger.info(line);
        });

        displayLogger.info(repeatChar("-", RECEIPT_WIDTH));

        String totalLine = formatReceiptLine("TOTAL:", "", rightAlignText(String.format("%.2f zł", totalAmount.get())), RECEIPT_WIDTH);
        displayLogger.info(totalLine);
        displayLogger.info(repeatChar("-", RECEIPT_WIDTH));
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length() - 2) / 2;
        return "|" + padSides(" ", padding) + text + padSides(" ", width - text.length() - padding - 2) + "|";
    }

    private String formatReceiptLine(String left, String middle, String right, int width) {
        int availableWidth = width - 2;
        String leftPart = left;
        String middlePart = middle;
        String rightPart = right;

        int maxMiddlePartWidth = 10;
        int maxLeftPartWidth = availableWidth - maxMiddlePartWidth - right.length();

        if (leftPart.length() > maxLeftPartWidth) {
            leftPart = leftPart.substring(0, maxLeftPartWidth - 1) + ".";
        }
        if (middlePart.length() > maxMiddlePartWidth) {
            middlePart = middlePart.substring(0, maxMiddlePartWidth - 1) + ".";
        }

        String leftPadded = padSides(leftPart, maxLeftPartWidth);
        String middlePadded = padSides(middlePart, maxMiddlePartWidth);

        return "|" + leftPadded + middlePadded + right + "|";
    }

    private String rightAlignText(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10 - text.length(); i++) {
            sb.append(" ");
        }
        sb.append(text);
        return sb.toString();
    }

    private String padSides(String text, int totalWidth) {
        int padding = Math.max(0, totalWidth - text.length());
        StringBuilder sb = new StringBuilder();
        sb.append(text);
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private String repeatChar(String character, int count) {
        return "|" + character.repeat(count - 2) + "|";
    }
}