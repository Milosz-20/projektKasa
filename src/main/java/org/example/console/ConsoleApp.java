package org.example.console;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
                double totalAmount = calculateTotalAmount();
                displayLogger.info("Select payment method");
                displayNiceLine();
                displayLogger.info("  Type '1' to pay by card");
                displayLogger.info("  Type '2' to pay by scanning gift card");
                displayNiceLine();
                String paymentMethod = scanner.nextLine();

                switch (paymentMethod) {
                    case "1":
                        clearConsoleLines(8);
                        displayLogger.info("Enter Card number");
                        String cardNumber = scanner.nextLine();
                        clearConsoleLines(2);
                        displayLogger.info("Enter Card expiry month");
                        String expiryMonth = scanner.nextLine();
                        clearConsoleLines(2);
                        displayLogger.info("Enter Card expiry year (YY)");
                        String expiryYear = scanner.nextLine();
                        clearConsoleLines(2);

                        try {
                            int year = Integer.parseInt(expiryYear);
                            if (year < 0 || year > 99) {
                                throw new IllegalArgumentException("Nieprawidłowy format roku. Proszę wprowadzić dwucyfrowy rok (YY).");
                            }
                            year = 2000 + year;
                            int month = Integer.parseInt(expiryMonth);
                            if(month < 1 || month > 12){
                                throw new IllegalArgumentException("Nieprawidłowy miesiąc. Proszę wprowadzić wartość od 1 do 12.");
                            }
                            displayLogger.info("Płatność przetwarzana dla kwoty: {} zł", String.format("%.2f", totalAmount));
                            processCardPayment(cardNumber, month, year, totalAmount);
                        } catch (NumberFormatException e) {
                            displayLogger.info("Nieprawidłowe dane: Wprowadź liczby dla miesiąca i roku.");
                        } catch (DateTimeParseException e) {
                            displayLogger.info("Nieprawidłowy format daty: {}", e.getMessage());
                        } catch (IllegalArgumentException e) {
                            displayLogger.info(e.getMessage());
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

    private double calculateTotalAmount() {
        double total = 0.0;
        Session session = sessionFactory.openSession();

        try {
            for (Map.Entry<Product, Integer> entry : scannedProducts.entrySet()) {
                Product product = session.get(Product.class, entry.getKey().getId());
                if (product != null) {
                    total += product.getPrice() * entry.getValue();
                }
            }
        } finally {
            session.close();
        }

        return total;
    }

    private void processCardPayment(String cardNumber, int month, int year, double amount) {
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
            paymentData.put("amount", amount);

            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writeValueAsString(paymentData);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            displayLogger.info("Wysłano dane płatności do serwera, kod odpowiedzi: {}", responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                try (Session updateSession = sessionFactory.openSession()) {
                    Transaction updateTransaction = updateSession.beginTransaction();
                    updateProductQuantity(updateSession);
                    updateTransaction.commit();
                    scannedProducts.clear();
                }
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    // Parsowanie odpowiedzi JSON i wyświetlanie tylko pola "message"
                    try {
                        Map responseMap = mapper.readValue(response.toString(), Map.class);
                        if (responseMap.containsKey("message")) {
                            displayLogger.info("Odpowiedź serwera: {}", responseMap.get("message"));
                        } else {
                            displayLogger.info("Odpowiedź serwera: {}", response);
                        }
                    } catch (Exception e) {
                        displayLogger.info("Odpowiedź serwera: {}", response);
                        logger.error("Błąd parsowania odpowiedzi JSON: {}", e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Błąd podczas wysyłania danych płatności: {}", e.getMessage(), e);
            displayLogger.info("Wystąpił błąd podczas przetwarzania płatności. Spróbuj ponownie.");
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
        clearConsole();
        Product currentProduct = session.get(Product.class, product.getId());
        if (currentProduct == null) {
            displayLogger.info("Product with ID: " + product.getId() + " does not exist.");
            return;
        }

        logger.info("Product scanned: {}", product.getName());
        if (canAddProduct(session, currentProduct)) {
            scannedProducts.put(currentProduct, scannedProducts.getOrDefault(currentProduct, 0) + 1);
            printScannedProducts();
            displayLogger.info("Type 'pay' to complete the order.");
        } else {
            displayLogger.info("Product: {} is currently out of stock (or insufficient quantity). Cannot add to order.", currentProduct.getName());
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
                    System.out.print("\033[1A"); // Move cursor up one line
                    System.out.print("\033[2K"); // Clear the line
                }
            } else {
                for (int i = 0; i < lines; i++) {
                    System.out.print("\033[1A"); // Move cursor up one line
                    System.out.print("\033[2K"); // Clear the line
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
                } else {
                    displayLogger.info("Insufficient quantity for product: {}. Available: {}, Requested: {}",
                            currentProduct.getName(), currentProduct.getAvailable_quantity(), quantity);
                }
            }

            displayLogger.info(repeatChar("-", RECEIPT_WIDTH));

            String totalLine = formatReceiptLine("TOTAL:", "", rightAlignText(String.format("%.2f zl", totalAmount.get())), RECEIPT_WIDTH);
            displayLogger.info(totalLine);
            displayLogger.info(repeatChar("-", RECEIPT_WIDTH));

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error during printing products: {}", e.getMessage(), e);
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