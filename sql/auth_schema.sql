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

CREATE TABLE IF NOT EXISTS `sesion_acceso` (
  `token` varchar(80) NOT NULL,
  `correo` varchar(100) NOT NULL,
  `fecha_creacion` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token`),
  KEY `fk_sesion_acceso_usuario` (`correo`),
  CONSTRAINT `fk_sesion_acceso_usuario`
    FOREIGN KEY (`correo`) REFERENCES `usuario_acceso` (`correo`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `pedido_producto` (
  `id_pedido` int NOT NULL AUTO_INCREMENT,
  `id_dni` varchar(20) NOT NULL,
  `canal` varchar(20) NOT NULL,
  `estado` varchar(40) NOT NULL DEFAULT 'Solicitud recibida',
  `importe_total` decimal(10,2) NOT NULL DEFAULT 0,
  `fecha_creacion` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_pedido`),
  KEY `fk_pedido_producto_cliente` (`id_dni`),
  CONSTRAINT `fk_pedido_producto_cliente`
    FOREIGN KEY (`id_dni`) REFERENCES `cliente` (`id_dni`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `pedido_producto_linea` (
  `id_linea` int NOT NULL AUTO_INCREMENT,
  `id_pedido` int NOT NULL,
  `producto_codigo` varchar(40) NOT NULL,
  `producto_nombre` varchar(120) NOT NULL,
  `categoria` varchar(60) NOT NULL,
  `cantidad` int NOT NULL,
  `precio_unitario` decimal(10,2) NOT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id_linea`),
  KEY `fk_pedido_producto_linea_pedido` (`id_pedido`),
  CONSTRAINT `fk_pedido_producto_linea_pedido`
    FOREIGN KEY (`id_pedido`) REFERENCES `pedido_producto` (`id_pedido`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `cliente` (`id_dni`, `nombre`, `apellido1`, `apellido2`, `correo`, `telefono`, `direccion`, `fecha_nacimiento`)
VALUES
  ('H1234567A', 'Laura', 'Horeca', NULL, 'horeca@lineacano.com', '+34 600 000 100', NULL, NULL)
ON DUPLICATE KEY UPDATE
  `nombre` = VALUES(`nombre`),
  `apellido1` = VALUES(`apellido1`),
  `correo` = VALUES(`correo`),
  `telefono` = VALUES(`telefono`);

INSERT INTO `usuario_acceso` (`correo`, `contrasena_hash`, `rol`, `activo`, `id_dni`)
VALUES
  ('cliente@lineacano.com', SHA2('cliente123', 256), 'registrado', 1, '11111111A'),
  ('horeca@lineacano.com', SHA2('horeca123', 256), 'horeca', 1, 'H1234567A'),
  ('comercial@lineacano.com', SHA2('master123', 256), 'maestro', 1, NULL)
ON DUPLICATE KEY UPDATE
  `contrasena_hash` = VALUES(`contrasena_hash`),
  `rol` = VALUES(`rol`),
  `activo` = VALUES(`activo`),
  `id_dni` = VALUES(`id_dni`);

INSERT INTO `cliente_horeca` (`id_dni`, `empresa`, `cif`, `correo_profesional`, `telefono_contacto`)
VALUES
  ('H1234567A', 'Linea Cano Demo HORECA', 'B12345678', 'horeca@lineacano.com', '+34 600 000 100')
ON DUPLICATE KEY UPDATE
  `empresa` = VALUES(`empresa`),
  `cif` = VALUES(`cif`),
  `correo_profesional` = VALUES(`correo_profesional`),
  `telefono_contacto` = VALUES(`telefono_contacto`);
