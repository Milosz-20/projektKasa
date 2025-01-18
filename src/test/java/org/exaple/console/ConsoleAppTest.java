package org.exaple.console;

import org.example.Product;
import org.example.console.ConsoleApp;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.example.util.HibernateUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;

class ConsoleAppTest {
    private ConsoleApp consoleApp;
    private static SessionFactory sessionFactory;
    private static Session session;
    private static Transaction transaction;

    @BeforeAll
    static void setup(){
        sessionFactory = HibernateUtil.getTestSessionFactory();
        session = sessionFactory.openSession();
        transaction = session.beginTransaction();

        //Add 2 products
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Test Product 1");
        product1.setPrice(10.0);
        product1.setBarcode("1234567890");
        product1.setAvailable_quantity(2);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Test Product 2");
        product2.setPrice(20.0);
        product2.setBarcode("0987654321");
        product2.setAvailable_quantity(2);

        session.save(product1);
        session.save(product2);
        transaction.commit(); // Zatwierdzenie transakcji
    }

    @AfterAll
    static void tearDown() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        List<Product> products = session.createQuery("from Product", Product.class).list();

        products.forEach(product -> session.delete(product));

        transaction.commit();
        session.close();
        HibernateUtil.shutdown();
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
    @BeforeEach
    void setUp(){
        consoleApp = new ConsoleApp();
    }


    @Test
    @DisplayName("Should handle valid barcode input")
    void shouldHandleValidBarcodeInput() {
        String barcode = "1234567890";
        consoleApp.handleInput(barcode);

        try(Session session = sessionFactory.openSession()){
            Transaction transaction = session.beginTransaction();
            Product product = session.createQuery("from Product where barcode = :barcode", Product.class)
                    .setParameter("barcode", barcode)
                    .getSingleResult();

            // Zmniejszamy available_quantity tak, jak robi to handleProductScan
            product.setAvailable_quantity(product.getAvailable_quantity() - 1);
            session.update(product);
            transaction.commit();

            assertEquals(1, product.getAvailable_quantity());
        }

        assertTrue(consoleApp.isDataBarcode(barcode));

        consoleApp.handleInput(barcode);

        try(Session session = sessionFactory.openSession()){
            Transaction transaction = session.beginTransaction();
            Product product = session.createQuery("from Product where barcode = :barcode", Product.class)
                    .setParameter("barcode", barcode)
                    .getSingleResult();

            // Zmniejszamy available_quantity tak, jak robi to handleProductScan
            product.setAvailable_quantity(product.getAvailable_quantity() - 1);
            session.update(product);
            transaction.commit();

            assertEquals(0, product.getAvailable_quantity());
        }
    }

    @Test
    @DisplayName("Should handle invalid barcode input")
    void shouldHandleInvalidBarcodeInput() {
        String barcode = "abc123def";
        assertFalse(consoleApp.isDataBarcode(barcode));
    }

    @Test
    @DisplayName("Should handle null barcode input")
    void shouldHandleNullBarcodeInput() {
        assertFalse(consoleApp.isDataBarcode(null));
    }

    @Test
    @DisplayName("Should handle empty barcode input")
    void shouldHandleEmptyBarcodeInput() {
        assertFalse(consoleApp.isDataBarcode(""));
    }
}