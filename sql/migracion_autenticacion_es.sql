ALTER TABLE `usuario_acceso`
  CHANGE COLUMN `password_hash` `contrasena_hash` varchar(64) NOT NULL;

UPDATE `usuario_acceso`
SET `rol` = CASE
  WHEN LOWER(`rol`) = 'member' THEN 'registrado'
  WHEN LOWER(`rol`) IN ('master', 'comercial') THEN 'maestro'
  ELSE `rol`
END;

CREATE TABLE IF NOT EXISTS `cliente_horeca` (
  `id_dni` varchar(20) NOT NULL,
  `empresa` varchar(150) NOT NULL,
  `cif` varchar(20) NOT NULL,
  `correo_profesional` varchar(100) NOT NULL,
  `telefono_contacto` varchar(20) DEFAULT NULL,
  `fecha_alta` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_dni`),
  UNIQUE KEY `uq_cliente_horeca_cif` (`cif`),
  UNIQUE KEY `uq_cliente_horeca_correo` (`correo_profesional`),
  CONSTRAINT `fk_cliente_horeca_cliente`
    FOREIGN KEY (`id_dni`) REFERENCES `cliente` (`id_dni`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
