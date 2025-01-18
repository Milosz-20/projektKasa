package org.example.console;

import java.util.*;

import org.example.Product;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.example.util.HibernateUtil;

import javax.persistence.NoResultException;

public class ConsoleApp {
    private final Scanner scanner;
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    String input;
    private final Map<String, Integer> tempQuantities = new HashMap<>();
    private final Map<Product, Integer> scannedProducts = new HashMap<>();

    public ConsoleApp() {
        scanner = new Scanner(System.in);
    }

    public void run(){
        System.out.println("Start scanning products...");

        while(true){
            input = scanner.nextLine();
            if(input.equals("print")) {
                printScannedProducts();
            } else {
                handleInput(input);
            }
        }
    }

    public void handleInput(String input){
        if(isDataBarcode(input)) {
            Session session = sessionFactory.openSession();
            Transaction transaction = session.beginTransaction();

            try {
                Product product = session.createQuery("from Product where barcode = :barcode", Product.class)
                        .setParameter("barcode", input)
                        .getSingleResult();
                System.out.println("Product: " + product.getName());

                int tempQuantity = tempQuantities.getOrDefault(input, product.getAvailable_quantity());

                if(tempQuantity > 0) {
                    tempQuantities.put(input, tempQuantity - 1);
                    System.out.println("Product: " + product.getName() + ", Remaining scans: " + tempQuantities.get(input));
                    scannedProducts.put(product, scannedProducts.getOrDefault(product, 0) + 1);
                } else {
                    System.out.println("Product: " + product.getName() + " is out of stock!");
                    tempQuantities.remove(input); // Reset temp quantity if out of stock
                }


            } catch (NoResultException e) {
                System.out.println("Product with barcode: " + input + " does not exist.");
            }

            transaction.commit();
            session.close();
        } else {
            System.out.println("Input is not a barcode!");
        }
    }

    public boolean isDataBarcode(String input){
        if(input == null || input.isEmpty()) {
            return false;
        }
        return input.matches("\\d+");
    }

    public void printScannedProducts(){
        System.out.println("----- Scanned Products -----");
        for (Map.Entry<Product, Integer> entry : scannedProducts.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            double totalPrice = product.getPrice() * quantity;
            System.out.println(product.getName() + "  " +  String.format("%.2f", product.getPrice()) + " x " + quantity + " = " + String.format("%.2f",totalPrice));
        }
        System.out.println("---------------------------");
    }
}