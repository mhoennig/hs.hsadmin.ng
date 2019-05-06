// Licensed under Apache-2.0
package org.hostsharing.hsadminng.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.sql.SQLException;
import java.sql.Statement;

public class ReplaceCustomChangeUnitTest {

    private static final String POSTGRES_DATABASE_PRODUCT_NAME = "PostgreSQL";
    private static final String H2_DATABASE_PRODUCT_NAME = "H2";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Database database;

    @Mock
    private JdbcConnection connection;

    @Mock
    private Statement statement;

    @Before
    public void initMocks() throws DatabaseException {
        given(database.getConnection()).willReturn(connection);
        given(connection.createStatement()).willReturn(statement);
    }

    @Test
    public void updatesForPostgres() throws Exception {
        // given
        given(database.getDatabaseProductName()).willReturn(POSTGRES_DATABASE_PRODUCT_NAME);
        final ReplaceCustomChange replaceCustomChange = givenReplaceCustomChange("some_table", "address,remark", "|", "\\n");

        // when
        replaceCustomChange.execute(database);

        // then
        verify(statement).executeUpdate("UPDATE some_table SET address= replace(address, '|', E'\\n')");
        verify(statement).executeUpdate("UPDATE some_table SET remark= replace(remark, '|', E'\\n')");
    }

    @Test
    public void updatesForH2() throws Exception {
        // given
        given(database.getDatabaseProductName()).willReturn(H2_DATABASE_PRODUCT_NAME);
        final ReplaceCustomChange replaceCustomChange = givenReplaceCustomChange("some_table", "address,remark", "|", "\\n");

        // when
        replaceCustomChange.execute(database);

        // then
        verify(statement).executeUpdate("UPDATE some_table SET address= replace(address, '|', STRINGDECODE('\\n'))");
        verify(statement).executeUpdate("UPDATE some_table SET remark= replace(remark, '|', STRINGDECODE('\\n'))");
    }

    @Test
    public void getConfirmationMessage() throws Exception {
        // given
        final ReplaceCustomChange replaceCustomChange = givenReplaceCustomChange("some_table", "address,remark", "|", "\\n");

        // when
        final String actual = replaceCustomChange.getConfirmationMessage();

        // then
        assertThat(actual).isEqualTo("in table some_table / columns address,remark: replaced all '|' to '\\n'");
    }

    @Test
    public void onDatabaseExceptionThrowsCustomChangeException() throws Exception {
        // given
        given(database.getDatabaseProductName()).willReturn(POSTGRES_DATABASE_PRODUCT_NAME);
        final ReplaceCustomChange replaceCustomChange = givenReplaceCustomChange("some_table", "address,remark", "|", "\\n");
        final Exception givenCausingException = new DatabaseException("dummy");
        given(connection.createStatement()).willThrow(givenCausingException);

        // when
        final Throwable actual = catchThrowable(() -> replaceCustomChange.execute(database));

        // then
        assertThat(actual).isInstanceOfSatisfying(
                CustomChangeException.class,
                (cce) -> assertThat(cce.getCause()).isSameAs(givenCausingException));
    }

    @Test
    public void onSQLExceptionThrowsCustomChangeException() throws Exception {
        // given
        given(database.getDatabaseProductName()).willReturn(POSTGRES_DATABASE_PRODUCT_NAME);
        final ReplaceCustomChange replaceCustomChange = givenReplaceCustomChange("some_table", "address,remark", "|", "\\n");
        final Exception givenCausingException = new SQLException("dummy");
        given(statement.executeUpdate(anyString())).willThrow(givenCausingException);

        // when
        final Throwable actual = catchThrowable(() -> replaceCustomChange.execute(database));

        // then
        assertThat(actual).isInstanceOfSatisfying(
                CustomChangeException.class,
                (cce) -> assertThat(cce.getCause()).isSameAs(givenCausingException));
    }

    @Test
    public void setFileOpenerDoesNothing() {
        new ReplaceCustomChange().setFileOpener(null);
    }

    @Test
    public void validateDoesNothing() {
        new ReplaceCustomChange().validate(null);
    }

    // --- only test fixture below ---

    private ReplaceCustomChange givenReplaceCustomChange(
            final String some_table,
            final String columns,
            final String searchFor,
            final String replaceWith) throws SetupException {
        final ReplaceCustomChange replaceCustomChange = new ReplaceCustomChange();
        replaceCustomChange.setUp();
        replaceCustomChange.setTableName(some_table);
        replaceCustomChange.setColumnNames(columns);
        replaceCustomChange.setSearchFor(searchFor);
        replaceCustomChange.setReplaceWith(replaceWith);
        return replaceCustomChange;
    }
}
