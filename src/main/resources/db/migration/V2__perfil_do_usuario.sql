-- Perfil do usuario: apelido e foto.
-- A foto fica em tabela propria para nao ser carregada junto com o usuario a cada login.

ALTER TABLE usuarios ADD COLUMN apelido VARCHAR(40) NULL;

CREATE TABLE fotos_usuario (
    usuario_id    BIGINT      NOT NULL PRIMARY KEY,
    dados         BYTEA       NOT NULL,
    atualizada_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_fotos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);
