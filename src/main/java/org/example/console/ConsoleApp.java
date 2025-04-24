package org.example.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Product;
import org.hibernate
        .HibernateException;
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
        clearConsole();
        displayLogger.info("Welcome to the Product Scanner App!");
        displayNiceLine();
        displayLogger.info("Instructions:");
        displayLogger.info("* Scan product barcodes to add items to your shopping cart.");
        displayLogger.info("* Type '"+ PAY_COMMAND +"' to view your cart and proceed to checkout.");
        displayLogger.info("* Type '"+ EXIT_COMMAND +"' to close the application.");
        displayNiceLine();
        logger.info("Application started.");

        while (true) {
            String barcodeInput = scanner.nextLine();

            if (barcodeInput.equals(EXIT_COMMAND)) {
                break;
            } else if (barcodeInput.equals(PAY_COMMAND)) {
                clearConsole();
                printScannedProducts();
                addLineBreak(2);
                double totalAmount = calculateTotalAmount();
                displayLogger.info("Please select your payment method:");
                displayNiceLine();
                displayLogger.info("  Type '1' for Credit/Debit Card payment");
                displayLogger.info("  Type '2' for BLIK mobile payment");
                displayLogger.info("  Type '3' for Gift Card payment");
                displayNiceLine();
                String paymentMethod = scanner.nextLine();

                switch (paymentMethod) {
                    case "1":
                        clearConsoleLines(8);
                        displayLogger.info("Please enter your card number:");
                        String cardNumber = scanner.nextLine();
                        clearConsoleLines(2);
                        displayLogger.info("Please enter the expiration month (1-12):");
                        String expiryMonth = scanner.nextLine();
                        clearConsoleLines(2);
                        displayLogger.info("Please enter the expiration year (YY):");
                        String expiryYear = scanner.nextLine();
                        clearConsoleLines(2);
                        displayLogger.info("Please enter the CVV security code (3 digits):");
                        String cvv = scanner.nextLine();
                        clearConsoleLines(2);

                        try {
                            int year = Integer.parseInt(expiryYear);
                            if (year < 0 || year > 99) {
                                throw new IllegalArgumentException("Invalid year format. Type 'pay' to try payment again.");
                            }
                            year = 2000 + year;
                            int month = Integer.parseInt(expiryMonth);
                            if(month < 1 || month > 12){
                                throw new IllegalArgumentException("Invalid month. Month must be between 1 and 12. Type 'pay' to try payment again.");
                            }
                            displayLogger.info("Processing payment for amount: {} PLN", String.format("%.2f", totalAmount));
                            processCardPayment(cardNumber, month, year, cvv, totalAmount);
                        } catch (NumberFormatException e) {
                            displayLogger.info("Invalid data format. Please enter numeric values only. Type 'pay' to try payment again.");
                        } catch (DateTimeParseException e) {
                            displayLogger.info("Invalid date format: {}. Type 'pay' to try payment again.", e.getMessage());
                        } catch (IllegalArgumentException e) {
                            displayLogger.info(e.getMessage());
                        }
                        break;

                    case "2":
                        clearConsoleLines(8);
                        displayLogger.info("Please enter your 6-digit BLIK code:");
                        String blikCode = scanner.nextLine();
                        try {
                            Integer.parseInt(blikCode);
                            displayLogger.info("Processing BLIK payment for amount: {} PLN", String.format("%.2f", totalAmount));
                            processBlikPayment(blikCode, totalAmount);
                        } catch (NumberFormatException e) {
                            displayLogger.info("Invalid BLIK code. Please enter numbers only. Type 'pay' to try payment again.");
                        }
                        break;

                    case "3":
                        displayLogger.info("Please scan your gift card or enter the gift card code:");
                        break;

                    default:
                        displayLogger.info("Invalid selection. Please type 1, 2, or 3 to select a payment method.");
                        break;
                }

            } else {
                handleInput(barcodeInput);
            }
        }

        logger.info("Exiting application.");
        sessionFactory.close();
    }

    private void addLineBreak(int i) {
        for (int j = 0; j < i; j++) {
            System.out.println();
        }
    }

    private double calculateTotalAmount() {
        double total = 0.0;

        try (Session session = sessionFactory.openSession()) {
            for (Map.Entry<Product, Integer> entry : scannedProducts.entrySet()) {
                Product product = session.get(Product.class, entry.getKey().getId());
                if (product != null) {
                    total += product.getPrice() * entry.getValue();
                }
            }
        }

        return total;
    }

    private void processCardPayment(String cardNumber, int month, int year, String cvv, double amount) {
        try {
            URL url = new URL("http://localhost:8080/api/platnosc");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("cardNumber", cardNumber);
            paymentData.put("expiryMonth", month);
            paymentData.put("expiryYear", year);
            paymentData.put("cvv", cvv);
            paymentData.put("amount", amount);

            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writeValueAsString(paymentData);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            handleSuccessfulResponse(connection, mapper, responseCode);

        } catch (Exception e) {
            logger.error("Error sending payment data: {}", e.getMessage(), e);
            displayLogger.info("An error occurred while processing your card payment. Please try again.");
        }
    }

    private void processBlikPayment(String blikCode, double amount) {
        try {
            URL url = new URL("http://localhost:8080/api/platnosc/blik");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("blikCode", blikCode);
            paymentData.put("amount", amount);

            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writeValueAsString(paymentData);

            logger.debug("Attempting to send BLIK payment data: {}", jsonData);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            handleSuccessfulResponse(connection, mapper, responseCode);

        } catch (Exception e) {
            logger.error("Error sending BLIK payment data: {}", e.getMessage(), e);
            displayLogger.info("An error occurred while processing your BLIK payment. Please try again.");
        }
    }

    private void handleSuccessfulResponse(HttpURLConnection connection, ObjectMapper mapper, int responseCode) throws IOException {
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                try {
                    Map responseMap = mapper.readValue(response.toString(), Map.class);
                    boolean isPaymentSuccessful = !responseMap.containsKey("status") || responseMap.get("status").equals("success");

                    if (isPaymentSuccessful) {
                        try (Session updateSession = sessionFactory.openSession()) {
                            Transaction updateTransaction = updateSession.beginTransaction();
                            updateProductQuantity(updateSession);
                            updateTransaction.commit();
                            scannedProducts.clear();
                        }

                        if (responseMap.containsKey("message")) {
                            clearConsoleLines(2);
                            displayLogger.info("Payment successful: {}", responseMap.get("message"));
                            addLineBreak(1);
                            displayLogger.info("To start a new purchase, scan another product.");
                        } else {
                            clearConsoleLines(2);
                            displayLogger.info("Payment successful: {}", response);
                            addLineBreak(1);
                            displayLogger.info("To start a new purchase, scan another product.");
                        }
                    } else {
                        if (responseMap.containsKey("message") ) {
                            clearConsole();
                            printScannedProducts();
                            addLineBreak(1);
                            displayLogger.info("Payment failed: {}", responseMap.get("message"));
                            addLineBreak(1);
                            displayLogger.info("To try payment again, type 'pay'.");
                        } else {
                            clearConsole();
                            printScannedProducts();
                            addLineBreak(1);
                            displayLogger.info("Payment failed: {}", response);
                            addLineBreak(1);
                            displayLogger.info("To try payment again, type 'pay'.");
                        }
                    }




                } catch (Exception e) {
                    clearConsoleLines(2);
                    displayLogger.info("Server response: {}", response);
                    addLineBreak(2);
                    displayLogger.info("To try payment again, type 'pay'.");
                    logger.error("Error parsing JSON response: {}", e.getMessage());
                }
            }
        }
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
            displayLogger.info("Invalid input! Please scan a valid product barcode.");
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
            displayLogger.info("Product not found. Barcode " + barcodeInput + " is not in our database.");
            return null;
        }
    }

    private void handleProductScan(Session session, Product product) {
        clearConsole();
        Product currentProduct = session.get(Product.class, product.getId());
        if (currentProduct == null) {
            displayLogger.info("Product with ID: " + product.getId() + " is no longer available in our system.");
            return;
        }

        logger.info("Product scanned: {}", product.getName());
        if (canAddProduct(session, currentProduct)) {
            scannedProducts.put(currentProduct, scannedProducts.getOrDefault(currentProduct, 0) + 1);
            printScannedProducts();
            addLineBreak(1);
            displayLogger.info("Product added to cart. Type 'pay' to proceed to checkout.");
        } else {
            printScannedProducts();
            addLineBreak(1);
            displayLogger.info("Sorry, product '{}' is out of stock or has insufficient quantity available. Cannot add to your cart.", currentProduct.getName());
        }
    }

    private void clearConsole() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "chcp", "65001").inheritIO().start().waitFor();
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            logger.error("Error clearing console: {}", e.getMessage(), e);
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    private void clearConsoleLines(int lines) {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                for (int i = 0; i < lines; i++) {
                    System.out.print("\033[1A");
                    System.out.print("\033[2K");
                }
            } else {
                for (int i = 0; i < lines; i++) {
                    System.out.print("\033[1A");
                    System.out.print("\033[2K");
                }
            }
            System.out.flush();
        } catch (Exception e) {
            logger.error("Error clearing console lines: {}", e.getMessage(), e);
            for (int i = 0; i < lines; i++) {
                System.out.println();
            }
        }
    }

    private boolean canAddProduct(Session session, Product product) {
        int scannedQuantity = scannedProducts.getOrDefault(product, 0);

        if (product.getAvailable_quantity() <= 0 || product.getAvailable_quantity() <= scannedQuantity) {
            return false;
        }

        for (Map.Entry<Product, Integer> entry : scannedProducts.entrySet()) {
            Product p = session.get(Product.class, entry.getKey().getId());
            if (!p.getId().equals(product.getId()) && p.getAvailable_quantity() < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
    

    private void updateProductQuantity(Session session) {
        scannedProducts.forEach((product, quantity) -> {
            if (product.getAvailable_quantity() >= quantity) {
                product.setAvailable_quantity(product.getAvailable_quantity() - quantity);
                session.merge(product);
                logger.info("Updated inventory: {} - new quantity: {}", product.getName(), product.getAvailable_quantity());
            } else {
                displayLogger.info("Inventory update failed: '{}' is out of stock. Please contact store staff for assistance.", product.getName());
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

        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            displayLogger.info(repeatChar("-"));
            displayLogger.info(centerText());
            displayLogger.info(repeatChar("-"));

            AtomicReference<Double> totalAmount = new AtomicReference<>(0.0);

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
                    String price = String.format("%.2f PLN", currentProduct.getPrice());
                    String quantityStr = "x" + quantity;
                    String total = String.format("%.2f PLN", totalPrice);

                    String line = formatReceiptLine(productName, rightAlignText(price + " " + quantityStr + " = " + total));
                    displayLogger.info(line);
                }
            }

            displayLogger.info(repeatChar("-"));

            String totalLine = formatReceiptLine("TOTAL AMOUNT:", rightAlignText(String.format("%.2f PLN", totalAmount.get())));
            displayLogger.info(totalLine);
            displayLogger.info(repeatChar("="));

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error during printing products: {}", e.getMessage(), e);
        }
    }

    private String centerText() {
        int padding = (60 - "===== YOUR SHOPPING CART =====".length() - 2) / 2;
        return "|" + padSides(" ", padding) + "===== YOUR SHOPPING CART =====" + padSides(" ", 60 - "===== YOUR SHOPPING CART =====".length() - padding - 2) + "|";
    }

    private String formatReceiptLine(String left, String right) {
        int availableWidth = 60 - 2;
        String leftPart = left;
        String middlePart = "";

        int maxMiddlePartWidth = 10;
        int maxLeftPartWidth = availableWidth - maxMiddlePartWidth - right.length();

        if (leftPart.length() > maxLeftPartWidth) {
            leftPart = leftPart.substring(0, maxLeftPartWidth - 1) + ".";
        }

        String leftPadded = padSides(leftPart, maxLeftPartWidth);
        String middlePadded = padSides(middlePart, maxMiddlePartWidth);

        return "|" + leftPadded + middlePadded + right + "|";
    }

    private String rightAlignText(String text) {
        return " ".repeat(Math.max(0, 10 - text.length())) +
                text;
    }

    private String padSides(String text, int totalWidth) {
        int padding = Math.max(0, totalWidth - text.length());
        return text +
                " ".repeat(padding);
    }

    private String repeatChar(String character) {
        return "|" + character.repeat(60 - 2) + "|";
    }
}
