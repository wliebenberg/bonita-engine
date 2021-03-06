CREATE TABLE category (
  tenantid BIGINT NOT NULL,
  id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  creator BIGINT,
  description LONGVARCHAR,
  creationDate BIGINT NOT NULL,
  lastUpdateDate BIGINT NOT NULL,
  UNIQUE (tenantid, name),
  PRIMARY KEY (tenantid, id)
);

CREATE TABLE processcategorymapping (
  tenantid BIGINT NOT NULL,
  id BIGINT NOT NULL,
  categoryid BIGINT NOT NULL,
  processid BIGINT NOT NULL,
  UNIQUE (tenantid, categoryid, processid),
  PRIMARY KEY (tenantid, id)
);

ALTER TABLE processcategorymapping ADD CONSTRAINT fk_catmapping_catid FOREIGN KEY (tenantid, categoryid) REFERENCES category(tenantid, id) ON DELETE CASCADE;