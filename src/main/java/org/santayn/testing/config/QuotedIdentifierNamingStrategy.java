package org.santayn.testing.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class QuotedIdentifierNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return preserveQuoted(name, super.toPhysicalCatalogName(name, jdbcEnvironment));
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return preserveQuoted(name, super.toPhysicalSchemaName(name, jdbcEnvironment));
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return preserveQuoted(name, super.toPhysicalTableName(name, jdbcEnvironment));
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return preserveQuoted(name, super.toPhysicalSequenceName(name, jdbcEnvironment));
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return preserveQuoted(name, super.toPhysicalColumnName(name, jdbcEnvironment));
    }

    private Identifier preserveQuoted(Identifier original, Identifier transformed) {
        if (original != null && original.isQuoted()) {
            return original;
        }

        return transformed;
    }
}
