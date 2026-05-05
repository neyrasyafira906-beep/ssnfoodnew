-- SSN FoodApp Database Schema for MySQL / phpMyAdmin
-- Created for Tender/Auction System

CREATE DATABASE IF NOT EXISTS `ssn_foodapp`;
USE `ssn_foodapp`;

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

-- --------------------------------------------------------
-- Table structure for table `sellers`
-- --------------------------------------------------------

CREATE TABLE `sellers` (
  `id` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `address` text DEFAULT NULL,
  `lat` double DEFAULT NULL,
  `lng` double DEFAULT NULL,
  `video_url` text DEFAULT NULL,
  `rating` double DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `menu_items`
-- --------------------------------------------------------

CREATE TABLE `menu_items` (
  `id` varchar(50) NOT NULL,
  `seller_id` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `price` bigint(20) NOT NULL,
  `stock` int(11) NOT NULL,
  `emoji` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `seller_id` (`seller_id`),
  CONSTRAINT `menu_items_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `sellers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `orders`
-- --------------------------------------------------------

CREATE TABLE `orders` (
  `id` varchar(50) NOT NULL,
  `buyer_name` varchar(100) DEFAULT NULL,
  `buyer_phone` varchar(20) DEFAULT NULL,
  `buyer_address` text DEFAULT NULL,
  `seller_id` varchar(50) NOT NULL,
  `total_price` bigint(20) NOT NULL,
  `payment_method` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `timestamp` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `seller_id` (`seller_id`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `sellers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `chat_messages`
-- --------------------------------------------------------

CREATE TABLE `chat_messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `context_id` varchar(50) NOT NULL,
  `sender_role` varchar(20) NOT NULL,
  `message_text` text NOT NULL,
  `sent_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Table structure for table `reviews`
-- --------------------------------------------------------

CREATE TABLE `reviews` (
  `id` varchar(50) NOT NULL,
  `seller_id` varchar(50) NOT NULL,
  `buyer_name` varchar(100) NOT NULL,
  `rating` int(11) NOT NULL,
  `comment` text DEFAULT NULL,
  `timestamp` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `seller_id` (`seller_id`),
  CONSTRAINT `reviews_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `sellers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

COMMIT;
