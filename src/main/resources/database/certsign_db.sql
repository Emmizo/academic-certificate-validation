-- MySQL dump 10.13  Distrib 8.0.40, for macos14 (arm64)
--
-- Host: localhost    Database: certsign_db
-- ------------------------------------------------------
-- Server version	8.4.3

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `certificates`
--

DROP TABLE IF EXISTS `certificates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `certificate_id` varchar(50) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `degree` varchar(200) NOT NULL,
  `digital_signature` text NOT NULL,
  `document_hash` varchar(512) NOT NULL,
  `institution` varchar(200) NOT NULL,
  `issue_date` date NOT NULL,
  `student_id` varchar(50) NOT NULL,
  `student_name` varchar(200) NOT NULL,
  `issued_by` bigint NOT NULL,
  `key_pair_id` bigint NOT NULL,
  `student_ref_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_certificates_certificate_id` (`certificate_id`),
  KEY `FKad5wjs5x0lgjfymaj5nr1pp6w` (`issued_by`),
  KEY `FKi8v5al5l2o36nytn6c091y8qy` (`key_pair_id`),
  KEY `FKomkrifr4mmmm4mjvmij4icmh8` (`student_ref_id`),
  CONSTRAINT `FKad5wjs5x0lgjfymaj5nr1pp6w` FOREIGN KEY (`issued_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKi8v5al5l2o36nytn6c091y8qy` FOREIGN KEY (`key_pair_id`) REFERENCES `key_pairs` (`id`),
  CONSTRAINT `FKomkrifr4mmmm4mjvmij4icmh8` FOREIGN KEY (`student_ref_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `certificates`
--

LOCK TABLES `certificates` WRITE;
/*!40000 ALTER TABLE `certificates` DISABLE KEYS */;
INSERT INTO `certificates` VALUES (1,'CERT-YXSMD63Q','2026-03-04 10:36:54.075182','Software Engineering','EngvqOVLGHVMlYy49Sw+YTpXXnFgPKwUnQgcETyMZFShM1O4nfPGhw4aMBg2MmWcjiu7LG4d561HN1Jtr7yaht0DE1zIC81AsXmVbUcYresv05S8wDgAe4HTyFbYeAu84gel7h4VJzEp23pE6PxxHfEXXZSYHbQjgvdo+D1O475ufWcTGSlYSSgQsHIvjEqpkc/KXK7DsJMbn25JpmseUU9ZB5T0Q37cR8fs4zsHY8UO5BA2n1DqalMWqNHzQWUY/qZOhVTD7GgH+Dylr+Vz1+xw2TDRneL5sldqOX6pm53P2BfuTO/6ZGlG6nm18E9wggBqB7zoz13Zxzkgk3HXBA==','44c5a90a79a5fee03be9b2b8b4a2df6f3b1709a5668dc59e95f751129c3af65c','INES Ruhengeri','2026-03-04','MSWE02424','Kwizera emmanuel',1,1,1),(2,'CERT-M17IJKPG','2026-03-04 10:42:08.097449','Original Program Name','Ol79ioSTrSq5mcMktYiswHSH4S/EbQSvpooS8mj6bFZL0tfedK1l2C1xMTMbnB6zQWkH57H9DYWgm0l8cT7/56QJZwgpjhcyAUbGl99dxIQQQaMJ+z2bNLAa/ODUNaZw96Y/Nthn7kNGJVwcDwghRtDZO1LPVBDs7gPLoagIdo+B9vXfmUL0Bvr98nq9krCvPHl7CErX6N9hcOMsLUBe9+msgOVuKD027Pt7WzaUH9+2Vtf6jL89Dmi7z4Ao6oxklavE6uTb36ZQKlr8IQiZdbM9zKotCw08cjyZDkIyjI6PeQQYOuOpJq6ZmoFfh6zAaDHL2zSNC69EAnllUxlpbw==','7b05a64c34e2b30dfaf41d2a737223cb3324646da47e7ff7f594bdcd3e27e999','INES Ruhengeri','2026-03-04','MSWE02423','Migisha Fabrice',1,1,2),(3,'CERT-HDNGTIR1','2026-03-04 11:51:09.853070','Computer science','ALWPcGPA1Y7eNI0exfg1G9zyS7H7gn/JRjd+4dl5+cq29anIemTb2X4AtRUxadiR2fNRLkFHHMaaYYWOeq8hmk6HMSFIZYx9TU0noDr6mtogUFnEoayUhlvM+Zv/XwiUnw+JxBUxyeWkHwzTtdlUks7gxDAQPVkZuFiGDywcdz0tn+OVQGCvDFwlRm+mINKoY7Rk05wDbOwhouCy0uZ8Pa04nAHhggVJKTefqdnL3lKuNATfSCmMAJRCUAWmgj8jNnf/23Ul+BbDnDgJjZkJ+hwl3sIWLsehErYZBPVu76T51HRI2cSQMlIzMq8ONDFIHD/NawjZ0wl7USsIgWqPrg==','004f554f5d07ba73978608323271e24b6bf7b4e3e2e6890475d4d514aa38b977','INES Ruhengeri','2026-03-04','MSWE02424','Kwizera Emmanuel',1,1,1),(4,'CERT-LUQUOA0W','2026-03-04 12:41:55.958029','PM','jVwsHESEjJzOcEUeEUA7i73hAu6t0v4wTuAlL7BC67UoXzLlgYFsrRjSXajj4IelUxgZOCjtMPEUO1qrMkskTY744NMFxTKKarOfG1oWljXcakKDwvO40PLmbxH/qQH1fI2vMAdwPaLe1zqUp8EexUuZ2iYWCq7s9V3kCzGr5LVe5Ma3X6GEnQdB3wHhug0Lk7iELaJJ/Ze/5knA/5Q66HnMHeXQAciP1GGekjedXVx0Ef6aSMLItRTo3DFDdXpca5E2vWF4AkCJj2WO0qck33w3lWmnp1QiaXtHMqXySK85w+KSqeSA/N6KFSdkDxmqPpaKBBEZ3UFQeAcWrWL6gw==','3663a0e19ad5ccb108f17dcf7a231012310cbe5c443da718da1bc01df88619c2','CISCO','2026-03-04','MSWE02423','Eric',1,1,2);
/*!40000 ALTER TABLE `certificates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `key_pairs`
--

DROP TABLE IF EXISTS `key_pairs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `key_pairs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `private_key_encrypted` text NOT NULL,
  `public_key` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `key_pairs`
--

LOCK TABLES `key_pairs` WRITE;
/*!40000 ALTER TABLE `key_pairs` DISABLE KEYS */;
INSERT INTO `key_pairs` VALUES (1,_binary '\0','2026-03-04 10:19:13.720832','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'),(2,_binary '','2026-03-04 15:51:37.465688','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');
/*!40000 ALTER TABLE `key_pairs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `date_of_birth` date DEFAULT NULL,
  `email` varchar(200) DEFAULT NULL,
  `full_name` varchar(200) NOT NULL,
  `national_id` varchar(100) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `student_number` varchar(50) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKh7gboo6v79gig1eo7lt1fubew` (`student_number`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (1,'2026-03-04 11:50:01.560740','2000-02-04','mswe02424@ines.ac.rw','Kwizera Emmanuel','11991928828822','ACTIVE','MSWE02424','2026-03-04 11:50:01.560740'),(2,'2026-03-04 11:50:40.255468',NULL,'mswe02423@ines.ac.rw','Eric','11991928828822','ACTIVE','MSWE02423','2026-03-04 14:59:14.485952');
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('ADMIN','SIGNER','VERIFIER') NOT NULL,
  `username` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-03-04 09:59:54.472404','$2a$12$a0dBEGU/83C83kafZKgM6ex2W0AuTwRs/YFYmuUkWcU8e4xuqK6d2','ADMIN','admin'),(2,'2026-03-05 11:36:27.577093','$2a$12$Ms7KpuVKp3zrYNb36k5sC.jrlvbt6TTj6aRGL6xnQ8WJhKTzp9CNi','SIGNER','signer'),(3,'2026-03-05 11:36:27.908780','$2a$12$5yTEX96MY5xnIgtIFWWE3Oa1nmXA/8XnmIU62o3an5PXvw9FHe83O','VERIFIER','verifier');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `verification_logs`
--

DROP TABLE IF EXISTS `verification_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `verification_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `certificate_id` varchar(50) NOT NULL,
  `failure_reason` varchar(255) DEFAULT NULL,
  `result` bit(1) NOT NULL,
  `verified_at` datetime(6) NOT NULL,
  `verifier_ip` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `verification_logs`
--

LOCK TABLES `verification_logs` WRITE;
/*!40000 ALTER TABLE `verification_logs` DISABLE KEYS */;
INSERT INTO `verification_logs` VALUES (1,'77737373','Certificate not found',_binary '\0','2026-03-04 10:04:23.904239','0:0:0:0:0:0:0:1'),(2,'77737373','Certificate not found',_binary '\0','2026-03-04 10:04:27.589469','0:0:0:0:0:0:0:1'),(3,'CERT-YXSMD63Q',NULL,_binary '','2026-03-04 10:41:07.037199','0:0:0:0:0:0:0:1'),(4,'CERT-M17IJKPG',NULL,_binary '','2026-03-04 10:42:16.898193','0:0:0:0:0:0:0:1'),(5,'CERT-M17IJKP2','Certificate not found',_binary '\0','2026-03-04 10:43:11.306438','0:0:0:0:0:0:0:1'),(6,'CERT-M17IJKPG',NULL,_binary '','2026-03-04 10:43:19.401641','0:0:0:0:0:0:0:1'),(7,'CERT-HDNGTIR1',NULL,_binary '','2026-03-04 12:00:54.901805','0:0:0:0:0:0:0:1'),(8,'CERT-HDNGTIR1',NULL,_binary '','2026-03-04 12:01:12.009608','0:0:0:0:0:0:0:1'),(9,'CERT-HDNGTIR18','Certificate not found',_binary '\0','2026-03-04 12:01:38.699919','0:0:0:0:0:0:0:1'),(10,'CERT-HDNGTIR18','Certificate not found',_binary '\0','2026-03-04 12:01:40.120679','0:0:0:0:0:0:0:1'),(11,'CERT-HDNGTIR1',NULL,_binary '','2026-03-04 12:01:43.141622','0:0:0:0:0:0:0:1'),(12,'CERT-M17IJKPG',NULL,_binary '','2026-03-04 14:30:55.827325','0:0:0:0:0:0:0:1'),(13,'CERT-LUQUOA0W',NULL,_binary '','2026-03-04 14:31:33.310943','0:0:0:0:0:0:0:1'),(14,'CERT-M17IJKPG',NULL,_binary '','2026-03-04 15:48:15.542768','0:0:0:0:0:0:0:1'),(15,'CERT-M17IJKPG',NULL,_binary '','2026-03-04 15:51:45.470242','0:0:0:0:0:0:0:1'),(16,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 15:56:07.253645','0:0:0:0:0:0:0:1'),(17,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 15:56:55.170760','0:0:0:0:0:0:0:1'),(18,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 15:56:56.049839','0:0:0:0:0:0:0:1'),(19,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 15:56:56.304128','0:0:0:0:0:0:0:1'),(20,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 15:56:56.486418','0:0:0:0:0:0:0:1'),(21,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 15:56:56.654838','0:0:0:0:0:0:0:1'),(22,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 15:56:56.853762','0:0:0:0:0:0:0:1'),(23,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 15:56:57.073917','0:0:0:0:0:0:0:1'),(24,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:00:54.934974','0:0:0:0:0:0:0:1'),(25,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:00:55.560210','0:0:0:0:0:0:0:1'),(26,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:00:55.808392','0:0:0:0:0:0:0:1'),(27,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:00:56.008680','0:0:0:0:0:0:0:1'),(28,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:00:56.225226','0:0:0:0:0:0:0:1'),(29,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:00:56.424841','0:0:0:0:0:0:0:1'),(30,'CERT-M17IJKPG5','Certificate not found',_binary '\0','2026-03-04 16:01:00.814844','0:0:0:0:0:0:0:1'),(31,'CERT-M17IJKPG5','Certificate not found',_binary '\0','2026-03-04 16:01:01.483328','0:0:0:0:0:0:0:1'),(32,'CERT-M17IJKPG5','Certificate not found',_binary '\0','2026-03-04 16:01:01.844524','0:0:0:0:0:0:0:1'),(33,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:01:06.146144','0:0:0:0:0:0:0:1'),(34,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:01:06.882284','0:0:0:0:0:0:0:1'),(35,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:01:07.125104','0:0:0:0:0:0:0:1'),(36,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:01:07.324673','0:0:0:0:0:0:0:1'),(37,'CERT-LUQUOA0W',NULL,_binary '','2026-03-04 16:18:37.341169','0:0:0:0:0:0:0:1'),(38,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:36:43.002550','0:0:0:0:0:0:0:1'),(39,'CERT-M17IJKPG5','Certificate not found',_binary '\0','2026-03-04 16:36:46.710336','0:0:0:0:0:0:0:1'),(40,'CERT-LUQUOA0W',NULL,_binary '','2026-03-04 16:36:53.123665','0:0:0:0:0:0:0:1'),(41,'CERT-LUQUOA0W',NULL,_binary '','2026-03-04 16:38:04.257394','0:0:0:0:0:0:0:1'),(42,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:38:19.503336','0:0:0:0:0:0:0:1'),(43,'CERT-M17IJKPG','Document tampered',_binary '\0','2026-03-04 16:39:31.895457','0:0:0:0:0:0:0:1'),(44,'CERT-HDNGTIR1',NULL,_binary '','2026-03-04 16:39:45.120284','0:0:0:0:0:0:0:1'),(45,'CERT-HDNGTIR1',NULL,_binary '','2026-03-04 16:40:07.574723','0:0:0:0:0:0:0:1');
/*!40000 ALTER TABLE `verification_logs` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-05 13:59:54
