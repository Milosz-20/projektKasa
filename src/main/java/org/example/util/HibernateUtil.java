package org.example.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.File; // Ten import można usunąć, jeśli nigdzie indziej nie jest używany

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();
    private static SessionFactory testSessionFactory = null;

    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration().configure("/hibernate.cfg.xml").buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static SessionFactory buildTestSessionFactory() {
        try {
            return new Configuration().configure("/hibernate.test.cfg.xml").buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial Test SessionFactory creation failed." + ex);
            throw new RuntimeException("Failed to build test session factory", ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static SessionFactory getTestSessionFactory() {
        if (testSessionFactory == null) {
            synchronized (HibernateUtil.class) {
                if (testSessionFactory == null) {
                    testSessionFactory = buildTestSessionFactory();
                }
            }
        }
        return testSessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && sessionFactory.isOpen()) {
            sessionFactory.close();
        }
        if (testSessionFactory != null && testSessionFactory.isOpen()) {
            testSessionFactory.close();
        }
    }
}