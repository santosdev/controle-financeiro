-- Contas temporarias do "Entrar como visitante": sem senha, apagadas ao sair ou depois de 24 horas.
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_provedor;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_provedor CHECK (provedor IN ('LOCAL', 'GOOGLE', 'VISITANTE'));

-- A limpeza periodica procura visitantes antigos.
CREATE INDEX idx_usuarios_provedor_criado ON usuarios (provedor, criado_em);
