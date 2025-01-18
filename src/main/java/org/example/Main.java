package org.example;

import org.example.console.ConsoleApp;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.example.util.HibernateUtil;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        ConsoleApp consoleApp = new ConsoleApp();
        consoleApp.run();
    }
}

//SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
//Session session = sessionFactory.openSession();
//Transaction transaction = session.beginTransaction();
//
//List<Product> result = session.createQuery("from Product", Product.class).list();
//        result.forEach(product -> {
//        System.out.println(product.getName());
//        });
//
//        transaction.commit();
//        session.close();