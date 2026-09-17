CREATE TABLE users (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    username              VARCHAR(100)  NOT NULL,
    password_hash         VARCHAR(255)  NOT NULL,
    display_name          VARCHAR(150)  NOT NULL,
    role                  VARCHAR(20)   NOT NULL,
    enabled               BOOLEAN       NOT NULL DEFAULT TRUE,
    must_change_password  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE = InnoDB;

CREATE TABLE designs (
    id                            BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id                      BIGINT      NOT NULL,
    forked_from_design_id         BIGINT      NULL,
    current_published_version_id  BIGINT      NULL,
    next_version_number           INT         NOT NULL DEFAULT 1,
    created_at                    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_designs_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_designs_forked_from FOREIGN KEY (forked_from_design_id) REFERENCES designs (id)
) ENGINE = InnoDB;

CREATE TABLE design_versions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    design_id           BIGINT        NOT NULL,
    version_number      INT           NOT NULL,
    name                VARCHAR(200)  NOT NULL,
    description         TEXT          NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at        TIMESTAMP     NULL,
    reviewed_at         TIMESTAMP     NULL,
    reviewed_by_id      BIGINT        NULL,
    rejection_comment   TEXT          NULL,
    CONSTRAINT fk_versions_design FOREIGN KEY (design_id) REFERENCES designs (id),
    CONSTRAINT fk_versions_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users (id),
    CONSTRAINT uk_versions_design_number UNIQUE (design_id, version_number)
) ENGINE = InnoDB;

ALTER TABLE designs
    ADD CONSTRAINT fk_designs_current_published_version
        FOREIGN KEY (current_published_version_id) REFERENCES design_versions (id);

CREATE TABLE design_images (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    design_version_id  BIGINT        NOT NULL,
    storage_key        VARCHAR(500)  NOT NULL,
    original_filename  VARCHAR(255)  NOT NULL,
    content_type       VARCHAR(100)  NOT NULL,
    size_bytes         BIGINT        NOT NULL,
    is_preview         BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order         INT           NOT NULL DEFAULT 0,
    uploaded_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_images_version FOREIGN KEY (design_version_id) REFERENCES design_versions (id)
) ENGINE = InnoDB;

CREATE INDEX idx_versions_design_status ON design_versions (design_id, status);
CREATE INDEX idx_versions_status ON design_versions (status);
CREATE INDEX idx_images_version ON design_images (design_version_id);
CREATE INDEX idx_designs_forked_from ON designs (forked_from_design_id);
