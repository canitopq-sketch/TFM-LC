CREATE TABLE IF NOT EXISTS `usuario_acceso` (
  `correo` varchar(100) NOT NULL,
  `contrasena_hash` varchar(64) NOT NULL,
  `rol` varchar(20) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  `id_dni` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`correo`),
  KEY `fk_usuario_acceso_cliente` (`id_dni`),
  CONSTRAINT `fk_usuario_acceso_cliente`
    FOREIGN KEY (`id_dni`) REFERENCES `cliente` (`id_dni`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

INSERT INTO `usuario_acceso` (`correo`, `contrasena_hash`, `rol`, `activo`, `id_dni`)
VALUES
  ('cliente@lineacano.com', SHA2('cliente123', 256), 'registrado', 1, '11111111A'),
  ('comercial@lineacano.com', SHA2('master123', 256), 'maestro', 1, NULL)
ON DUPLICATE KEY UPDATE
  `contrasena_hash` = VALUES(`contrasena_hash`),
  `rol` = VALUES(`rol`),
  `activo` = VALUES(`activo`),
  `id_dni` = VALUES(`id_dni`);
