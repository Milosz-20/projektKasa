-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Feb 28, 2025 at 02:11 PM
-- Wersja serwera: 10.4.32-MariaDB
-- Wersja PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `shop`
--

-- --------------------------------------------------------

--
-- Struktura tabeli dla tabeli `products`
--

CREATE TABLE `products` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `price` double DEFAULT NULL,
  `barcode` varchar(255) DEFAULT NULL,
  `available_quantity` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `products`
--

INSERT INTO `products` (`id`, `name`, `price`, `barcode`, `available_quantity`) VALUES
(1, 'Jabłka Golden', 2.5, '590000000001', 9),
(2, 'Mleko 3.2% Karton', 3.2, '590000000002', 6),
(3, 'Chleb Pszenny', 4, '590000000003', 3),
(4, 'Woda Mineralna 1.5L', 2, '590000000004', 4),
(5, 'Banan', 5.5, '590000000005', 1),
(6, 'Ser Gouda Plasterki', 12, '590000000006', 6),
(7, 'Masło Ekstra', 7.8, '590000000007', 4),
(8, 'Kawa Mielona Arabica', 25, '590000000008', 8),
(9, 'Herbata Czarna Ekspresowa', 8.5, '590000000009', 8),
(10, 'Czekolada Mleczna', 4.5, '590000000010', 5),
(11, 'Bułka Kajzerka', 0.8, '590000000011', 0),
(12, 'Sok Pomarańczowy 1L', 6, '590000000012', 0);

--
-- Indeksy dla zrzutów tabel
--

--
-- Indeksy dla tabeli `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `products`
--
ALTER TABLE `products`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
