-- Esquema inicial do FinanceControl com contas de usuario.
-- Cada categoria e cada movimentacao pertence a um usuario.

CREATE TABLE usuarios (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    nome              VARCHAR(100) NOT NULL,
    email             VARCHAR(160) NOT NULL,
    senha_hash        VARCHAR(100) NULL,
    provedor          ENUM('LOCAL', 'GOOGLE') NOT NULL,
    email_verificado  BIT(1)       NOT NULL,
    tentativas_falhas INT          NOT NULL,
    bloqueado_ate     DATETIME(6)  NULL,
    criado_em         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Tokens de uso unico enviados por e-mail (confirmar conta e redefinir senha).
-- Guardamos apenas o hash (SHA-256): quem ler o banco nao consegue usar os links.
CREATE TABLE tokens_usuario (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT      NOT NULL,
    tipo       ENUM('VERIFICACAO_EMAIL', 'REDEFINICAO_SENHA') NOT NULL,
    token_hash CHAR(64)    NOT NULL,
    expira_em  DATETIME(6) NOT NULL,
    usado_em   DATETIME(6) NULL,
    criado_em  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tokens_hash (token_hash),
    KEY idx_tokens_usuario_tipo (usuario_id, tipo),
    CONSTRAINT fk_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE categorias (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT      NOT NULL,
    nome       VARCHAR(60) NOT NULL,
    tipo       ENUM('RECEITA', 'DESPESA') NOT NULL,
    PRIMARY KEY (id),
    KEY idx_categorias_usuario (usuario_id),
    CONSTRAINT fk_categorias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE transacoes (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id   BIGINT        NOT NULL,
    categoria_id BIGINT        NOT NULL,
    descricao    VARCHAR(120)  NOT NULL,
    valor        DECIMAL(12,2) NOT NULL,
    data         DATE          NOT NULL,
    tipo         ENUM('RECEITA', 'DESPESA') NOT NULL,
    PRIMARY KEY (id),
    KEY idx_transacoes_usuario_data (usuario_id, data),
    KEY idx_transacoes_categoria (categoria_id),
    CONSTRAINT fk_transacoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_transacoes_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
