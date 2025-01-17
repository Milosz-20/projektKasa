package org.example;

import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.example.util.HibernateUtil;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        List<Product> result = session.createQuery("from Product", Product.class).list();
        result.forEach(product -> {
            System.out.println(product.getName());
            System.out.println(product.getPrice());
        });

        transaction.commit();
        session.close();
    }
}