// Licensed under Apache-2.0
package org.hostsharing.hsadminng.liquibase;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Used in Liquibase <customChange/> for database-independent search and replace with special characters.
 */
public class ReplaceCustomChange implements CustomTaskChange {

    private String tableName;
    private String columnNames;
    private String searchFor;
    private String replaceWith;

    @Override
    public void execute(final Database database) throws CustomChangeException {
        final JdbcConnection conn = (JdbcConnection) database.getConnection();
        final boolean isH2 = "H2".equals(database.getDatabaseProductName());
        try {
            conn.setAutoCommit(false);
            final Statement statement = conn.createStatement();
            for (String columnName : columnNames.split(",")) {
                final String sql = "UPDATE " + tableName + " SET " + columnName + "= replace(" + columnName + ", '" + searchFor
                        + "', " +
                        (isH2 ? "STRINGDECODE('" + replaceWith + "')" : "E'" + replaceWith + "'") + ")";
                statement.executeUpdate(sql);
            }
            conn.commit();
        } catch (DatabaseException | SQLException e) {
            throw new CustomChangeException("cannot perform search&replace", e);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "in table " + tableName + " / columns " + columnNames + ": replaced all '" + searchFor + "' to '" + replaceWith
                + "'";
    }

    @Override
    public void setUp() throws SetupException {

    }

    @Override
    public void setFileOpener(final ResourceAccessor resourceAccessor) {

    }

    @Override
    public ValidationErrors validate(final Database database) {
        return null;
    }

    // public String getTableName() {
    // return tableName;
    // }

    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    // public String getColumnNames() {
    // return columnNames;
    // }

    public void setColumnNames(final String columns) {
        this.columnNames = columns;
    }

    // public String getSearchFor() {
    // return searchFor;
    // }

    public void setSearchFor(final String searchFor) {
        this.searchFor = searchFor;
    }

    // public String getReplaceWith() {
    // return replaceWith;
    // }

    public void setReplaceWith(final String replaceWith) {
        this.replaceWith = replaceWith;
    }
}
