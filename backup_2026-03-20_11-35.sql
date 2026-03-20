-- MySQL dump 10.13  Distrib 9.4.0, for macos15 (arm64)
--
-- Host: localhost    Database: linea_cano
-- ------------------------------------------------------
-- Server version	9.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cliente`
--

DROP TABLE IF EXISTS `cliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cliente` (
  `id_dni` varchar(20) NOT NULL,
  `nombre` varchar(100) DEFAULT NULL,
  `apellido1` varchar(100) DEFAULT NULL,
  `apellido2` varchar(100) DEFAULT NULL,
  `correo` varchar(100) DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  PRIMARY KEY (`id_dni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cliente`
--

LOCK TABLES `cliente` WRITE;
/*!40000 ALTER TABLE `cliente` DISABLE KEYS */;
INSERT INTO `cliente` VALUES ('00000000T','Sofía','Vázquez',NULL,'sofia@email.com',NULL,NULL,NULL),('10101010J','Sara','Jiménez',NULL,'sara@email.com',NULL,NULL,NULL),('11111111A','Pedro','Gomez',NULL,'pedro@email.com',NULL,NULL,NULL),('11111111K','Diego','Blanco',NULL,'diego@email.com',NULL,NULL,NULL),('22222222B','Ana','Ruiz',NULL,'ana@email.com',NULL,NULL,NULL),('22222222L','Rosa','Prieto',NULL,'rosa@email.com',NULL,NULL,NULL),('33333333C','Carlos','Vila',NULL,'carlos@email.com',NULL,NULL,NULL),('33333333M','Luis','Sánchez',NULL,'luis@email.com',NULL,NULL,NULL),('44444444D','Maria','Sanz',NULL,'maria@email.com',NULL,NULL,NULL),('44444444N','Eva','Morales',NULL,'eva@email.com',NULL,NULL,NULL),('55555555E','Javier','López',NULL,'javier@email.com',NULL,NULL,NULL),('55555555O','Iván','Castro',NULL,'ivan@email.com',NULL,NULL,NULL),('66666666F','Elena','García',NULL,'elena@email.com',NULL,NULL,NULL),('66666666P','Marta','Rubio',NULL,'marta@email.com',NULL,NULL,NULL),('77777777G','Pablo','Marín',NULL,'pablo@email.com',NULL,NULL,NULL),('77777777Q','Raúl','Gil',NULL,'raul@email.com',NULL,NULL,NULL),('88888888H','Lucía','Fernández',NULL,'lucia@email.com',NULL,NULL,NULL),('88888888R','Alba','Ramos',NULL,'alba@email.com',NULL,NULL,NULL),('99999999I','Hugo','Torres',NULL,'hugo@email.com',NULL,NULL,NULL),('99999999S','Manu','Heredia',NULL,'manu@email.com',NULL,NULL,NULL);
/*!40000 ALTER TABLE `cliente` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `habitacion`
--

DROP TABLE IF EXISTS `habitacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `habitacion` (
  `num_habitacion` int NOT NULL,
  `precio_noche` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`num_habitacion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `habitacion`
--

LOCK TABLES `habitacion` WRITE;
/*!40000 ALTER TABLE `habitacion` DISABLE KEYS */;
INSERT INTO `habitacion` VALUES (101,150.00),(102,150.00),(103,150.00),(104,150.00),(105,180.00),(201,200.00),(202,200.00),(203,200.00),(204,200.00),(205,250.00);
/*!40000 ALTER TABLE `habitacion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reserva`
--

DROP TABLE IF EXISTS `reserva`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reserva` (
  `id_reserva` int NOT NULL AUTO_INCREMENT,
  `id_dni` varchar(20) DEFAULT NULL,
  `fecha_entrada` date DEFAULT NULL,
  `fecha_salida` date DEFAULT NULL,
  `estado_reserva` varchar(50) DEFAULT NULL,
  `importe_total` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id_reserva`),
  KEY `id_dni` (`id_dni`),
  CONSTRAINT `reserva_ibfk_1` FOREIGN KEY (`id_dni`) REFERENCES `cliente` (`id_dni`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserva`
--

LOCK TABLES `reserva` WRITE;
/*!40000 ALTER TABLE `reserva` DISABLE KEYS */;
INSERT INTO `reserva` VALUES (1,'11111111A','2026-03-01','2026-03-05','Confirmada',NULL),(2,'22222222B','2026-03-10','2026-03-12','Confirmada',NULL),(3,'33333333C','2026-04-15','2026-04-20','Confirmada',NULL),(4,'44444444D','2026-05-01','2026-05-02','Confirmada',NULL),(5,'55555555E','2026-06-01','2026-06-10','Confirmada',NULL),(6,'66666666F','2026-06-15','2026-06-18','Confirmada',NULL),(7,'77777777G','2026-07-01','2026-07-05','Confirmada',NULL),(8,'88888888H','2026-08-10','2026-08-12','Confirmada',NULL),(9,'99999999I','2026-09-01','2026-09-05','Confirmada',NULL),(10,'10101010J','2026-10-10','2026-10-15','Confirmada',NULL),(11,'11111111K','2026-11-01','2026-11-05','Confirmada',NULL),(12,'22222222L','2026-12-01','2026-12-05','Confirmada',NULL),(13,'33333333M','2027-01-10','2027-01-15','Confirmada',NULL),(14,'44444444N','2027-02-01','2027-02-05','Confirmada',NULL),(15,'55555555O','2027-03-01','2027-03-05','Confirmada',NULL),(16,'66666666P','2027-04-10','2027-04-15','Confirmada',NULL),(17,'77777777Q','2027-05-01','2027-05-05','Confirmada',NULL),(18,'88888888R','2027-06-10','2027-06-15','Confirmada',NULL),(19,'99999999S','2027-07-01','2027-07-05','Confirmada',NULL),(20,'00000000T','2027-08-10','2027-08-15','Confirmada',NULL);
/*!40000 ALTER TABLE `reserva` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reserva_habitacion`
--

DROP TABLE IF EXISTS `reserva_habitacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reserva_habitacion` (
  `id_reserva` int NOT NULL,
  `num_habitacion` int NOT NULL,
  PRIMARY KEY (`id_reserva`,`num_habitacion`),
  KEY `num_habitacion` (`num_habitacion`),
  CONSTRAINT `reserva_habitacion_ibfk_1` FOREIGN KEY (`id_reserva`) REFERENCES `reserva` (`id_reserva`),
  CONSTRAINT `reserva_habitacion_ibfk_2` FOREIGN KEY (`num_habitacion`) REFERENCES `habitacion` (`num_habitacion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserva_habitacion`
--

LOCK TABLES `reserva_habitacion` WRITE;
/*!40000 ALTER TABLE `reserva_habitacion` DISABLE KEYS */;
INSERT INTO `reserva_habitacion` VALUES (1,101),(11,101),(2,102),(12,102),(6,103),(16,103),(7,104),(17,104),(10,105),(20,105),(3,201),(13,201),(4,202),(14,202),(5,203),(15,203),(8,204),(18,204),(9,205),(19,205);
/*!40000 ALTER TABLE `reserva_habitacion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reserva_salon`
--

DROP TABLE IF EXISTS `reserva_salon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reserva_salon` (
  `id_reserva` int NOT NULL,
  `id_salon` int NOT NULL,
  PRIMARY KEY (`id_reserva`,`id_salon`),
  KEY `id_salon` (`id_salon`),
  CONSTRAINT `reserva_salon_ibfk_1` FOREIGN KEY (`id_reserva`) REFERENCES `reserva` (`id_reserva`),
  CONSTRAINT `reserva_salon_ibfk_2` FOREIGN KEY (`id_salon`) REFERENCES `salon` (`id_salon`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserva_salon`
--

LOCK TABLES `reserva_salon` WRITE;
/*!40000 ALTER TABLE `reserva_salon` DISABLE KEYS */;
INSERT INTO `reserva_salon` VALUES (1,1),(2,2),(9,3);
/*!40000 ALTER TABLE `reserva_salon` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `salon`
--

DROP TABLE IF EXISTS `salon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `salon` (
  `id_salon` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) DEFAULT NULL,
  `capacidad_max` int DEFAULT NULL,
  `tipo_evento` varchar(50) DEFAULT NULL,
  `precio_evento` decimal(10,2) DEFAULT NULL,
  `montaje` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_salon`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `salon`
--

LOCK TABLES `salon` WRITE;
/*!40000 ALTER TABLE `salon` DISABLE KEYS */;
INSERT INTO `salon` VALUES (1,'Salón Dehesa',350,'Gala',1500.00,'Imperial'),(2,'Salón Dehesa',150,'Empresa',800.00,'En U'),(3,'Salón Dehesa',200,'Charla',600.00,'Teatro');
/*!40000 ALTER TABLE `salon` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-20 11:35:21
