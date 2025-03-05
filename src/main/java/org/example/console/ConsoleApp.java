package org.example.console;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final String PAY_COMMAND = "pay";
    private static final String EXIT_COMMAND = "exit";
    private final Map<Product, Integer> scannedProducts = new HashMap<>();

    public ConsoleApp() {
        scanner = new Scanner(System.in);
    }

    public void run() {
        displayLogger.info("Welcome to the Product Scanner App!");
        displayNiceLine();
        displayLogger.info("Instructions:");
        displayLogger.info("* Scan barcodes to add products to your order.");
        displayLogger.info("* Type '"+ PAY_COMMAND +"' to view the current order.");
        displayLogger.info("* Type '"+ EXIT_COMMAND +"' to close the application.");
        displayNiceLine();
        logger.info("Application started.");

        while (true) {
            String barcodeInput = scanner.nextLine();

            if (barcodeInput.equals(EXIT_COMMAND)) {
                break;
            } else if (barcodeInput.equals(PAY_COMMAND)) {
                printScannedProducts();
                displayLogger.info("Select payment method");
                displayNiceLine();
                displayLogger.info("  Type '1' to pay by card");
                displayLogger.info("  Type '2' to pay by scanning gift card");
                displayNiceLine();
                String paymentMethod = scanner.nextLine();

                switch (paymentMethod) {
                    case "1":
                        displayLogger.info("enter Card number");
                        String cardNumber = scanner.nextLine();
                        displayLogger.info("enter Card expiry month");
                        String expiryMonth = scanner.nextLine();
                        displayLogger.info("enter Card expiry year");
                        String expiryYear = scanner.nextLine();

                        try {
                            // Przyjmujemy, że rok jest podawany w formacie RR (np. 20), a nie YYYY (np. 2020)
                            // Najpierw przekształcamy rok do formatu YYYY, zakładając, że '20' oznacza '2020'
                            int year = Integer.parseInt(expiryYear);
                            if (year < 100) {
                                year = 2000 + year;
                            }

                            // Format MM/YYYY - potrzebny do parsowania stringów
                            String formattedMonth = String.format("%02d", Integer.parseInt(expiryMonth)); // Formatujemy miesiąc z zerem wiodącym
                            String expiryDateString = String.format("%s/%d", formattedMonth, year); // np. "03/2020"

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

                            LocalDate expiryDate = LocalDate.parse(expiryDateString, formatter);

                            // W tym miejscu masz LocalDate obiekt, który reprezentuje datę 1 dnia danego miesiąca w roku
                            // Możesz go teraz zapisać do bazy danych, używając np. prepared statement:
                            // preparedStatement.setDate(1, java.sql.Date.valueOf(expiryDate));

                            displayLogger.info("Parsed Date: " + expiryDate); // Możesz wyświetlić datę w odpowiednim formacie
                            displayLogger.info("Saving Date: " + java.sql.Date.valueOf(expiryDate.withDayOfMonth(1))); // Data dla bazy danych
                        } catch (NumberFormatException e) {
                            displayLogger.info("Invalid year format: " + expiryYear);
                        }
                        catch (DateTimeParseException e) {
                            displayLogger.info("Invalid date format. Please enter MM/YY: " + expiryMonth + "/" + expiryYear);
                        }

                        break;
                    case "2":
                        displayLogger.info("Gift card");

                        break;
                    default:
                        displayLogger.info("Unknown payment method");
                        break;
                }

            } else {
                handleInput(barcodeInput);
            }
        }

        logger.info("Exiting application.");
        sessionFactory.close();
    }

    public void displayNiceLine() {
        displayLogger.info("---------------------------------");
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
        Product currentProduct = session.get(Product.class, product.getId());
        if (currentProduct == null) {
            displayLogger.info("Product with ID: " + product.getId() + " does not exist.");
            return;
        }

        logger.info("Product scanned: {}", product.getName());
        if (canAddProduct(session, currentProduct)) {
            scannedProducts.put(currentProduct, scannedProducts.getOrDefault(currentProduct, 0) + 1);
            displayLogger.info("Product scanned: " + currentProduct.getName());
        } else {
            displayLogger.info("Product: {} is currently out of stock (or insufficient quantity). Cannot add to order.", currentProduct.getName());
        }
    }

    private boolean canAddProduct(Session session, Product product) {
        int scannedQuantity = scannedProducts.getOrDefault(product, 0);

        for (Map.Entry<Product, Integer> entry : scannedProducts.entrySet()) {
            Product p = session.get(Product.class, entry.getKey().getId());
            if (p.getId().equals(product.getId())) {
                if (p.getAvailable_quantity() < scannedQuantity + 1) {
                    return false;
                }
            } else {
                if (p.getAvailable_quantity() < entry.getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateProductQuantity(Session session) {
        scannedProducts.forEach((product, quantity) -> {
            if (product.getAvailable_quantity() >= quantity) {
                product.setAvailable_quantity(product.getAvailable_quantity() - quantity);
                session.merge(product);
                logger.info("Updated product: {} to quantity: {}", product.getName(), product.getAvailable_quantity());
            } else {
                displayLogger.info("Product: {} is out of stock. Cannot add to order.", product.getName());
            }
        });
    }

    public boolean isDataBarcode(String barcodeInput) {
        if (barcodeInput == null || barcodeInput.trim().isEmpty()) {
            return false;
        }
        return barcodeInput.matches("\\d+");
    }

    public void printScannedProducts() {
        Session session = null;
        Transaction transaction = null;

        try {
            session = sessionFactory.openSession();
            transaction = session.beginTransaction();

            final int RECEIPT_WIDTH = 60;
            displayLogger.info(repeatChar("-", RECEIPT_WIDTH));
            displayLogger.info(centerText("----- Scanned Products -----", RECEIPT_WIDTH));
            displayLogger.info(repeatChar("-", RECEIPT_WIDTH));

            AtomicReference<Double> totalAmount = new AtomicReference<>(0.0);
            Map<Product, Integer> productsToUpdate = new HashMap<>();

            for (Map.Entry<Product, Integer> entry : scannedProducts.entrySet()) {
                Product product = entry.getKey();
                Integer quantity = entry.getValue();

                Product currentProduct = session.get(Product.class, product.getId());
                if (currentProduct == null) {
                    displayLogger.info("Product with ID: {} does not exist.", product.getId());
                    continue;
                }

                if (currentProduct.getAvailable_quantity() >= quantity) {
                    double totalPrice = currentProduct.getPrice() * quantity;
                    totalAmount.updateAndGet(v -> v + totalPrice);

                    String productName = currentProduct.getName();
                    String price = String.format("%.2f zl", currentProduct.getPrice());
                    String quantityStr = "x" + quantity;
                    String total = String.format("%.2f zl", totalPrice);

                    String line = formatReceiptLine(productName, "", rightAlignText(price + " " + quantityStr + " " + total), RECEIPT_WIDTH);
                    displayLogger.info(line);

                    productsToUpdate.put(currentProduct, quantity);
                } else {
                    displayLogger.info("Insufficient quantity for product: {}. Available: {}, Requested: {}",
                            currentProduct.getName(), currentProduct.getAvailable_quantity(), quantity);
                }
            }

            displayLogger.info(repeatChar("-", RECEIPT_WIDTH));

            String totalLine = formatReceiptLine("TOTAL:", "", rightAlignText(String.format("%.2f zl", totalAmount.get())), RECEIPT_WIDTH);
            displayLogger.info(totalLine);
            displayLogger.info(repeatChar("-", RECEIPT_WIDTH));

            updateProductQuantity(session);
            transaction.commit();
            scannedProducts.clear();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error during printing or updating products: {}", e.getMessage(), e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length() - 2) / 2;
        return "|" + padSides(" ", padding) + text + padSides(" ", width - text.length() - padding - 2) + "|";
    }

    private String formatReceiptLine(String left, String middle, String right, int width) {
        int availableWidth = width - 2;
        String leftPart = left;
        String middlePart = middle;

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