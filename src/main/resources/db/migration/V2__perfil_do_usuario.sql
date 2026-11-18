-- Perfil do usuario: apelido e foto.
-- A foto fica em tabela propria para nao ser carregada junto com o usuario a cada login.

ALTER TABLE usuarios ADD COLUMN apelido VARCHAR(40) NULL AFTER nome;

CREATE TABLE fotos_usuario (
    usuario_id    BIGINT      NOT NULL,
    dados         MEDIUMBLOB  NOT NULL,
    atualizada_em DATETIME(6) NOT NULL,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_fotos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
