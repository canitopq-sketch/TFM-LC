ALTER TABLE `usuario_acceso`
  CHANGE COLUMN `password_hash` `contrasena_hash` varchar(64) NOT NULL;

UPDATE `usuario_acceso`
SET `rol` = CASE
  WHEN LOWER(`rol`) = 'member' THEN 'registrado'
  WHEN LOWER(`rol`) IN ('master', 'comercial') THEN 'maestro'
  ELSE `rol`
END;
