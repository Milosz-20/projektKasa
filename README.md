# Self-Checkout Simulation: Checkout System

This repository is part of a project to simulate a shop self-checkout system. It contains the code for the checkout terminal, which allows users to scan products, view their cart, and complete payments.

---

## Key Features

- **Product Scanning**: Products can be added to the cart using barcodes.
- **Payment Options**: Supports credit/debit card, BLIK mobile payments, and gift card transactions.
- **Database Integration**: Fetches product details from a PostgreSQL database using Hibernate.
- **Logging**: Provides file and console logging for debugging and user interaction.

---

## How to Run

### Prerequisites
1. **Java**: Install JDK 18 or higher.
2. **Database**: Set up a PostgreSQL database.
3. **Build Tool**: Ensure Maven is installed.

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/Milosz-20/projektKasa.git
   cd projektKasa
   ```

2. Configure the database:
   - Use the `shop.sql` script to create the `products` table and insert sample products.

3. Update database details:
   - Edit `hibernate.cfg.xml` to match your database credentials.

4. Build the project:
   ```bash
   mvn clean install
   ```

5. Run the application:
   ```bash
   java -Dfile.encoding=UTF-8 -jar target/kasa.jar
   ```

---

## How It Works

1. **Main Screen**: Start the app to see instructions.
2. **Scan Products**: Enter barcodes to add products to the cart.
3. **Checkout**:
   - Review items and select a payment method.
   - Enter payment details to complete the transaction.
4. **Log Outputs**: Logs are saved in `application.log` for debugging.

---

## Related Repository

For payment processing, see the bank server at https://github.com/Milosz-20/projektBank

---
