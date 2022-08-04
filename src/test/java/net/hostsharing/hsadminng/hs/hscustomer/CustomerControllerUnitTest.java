package net.hostsharing.hsadminng.hs.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerUnitTest {

    @Mock
    Context contextMock;
    @Mock
    CustomerRepository customerRepositoryMock;

    @InjectMocks
    CustomerController customerController;

    @Test
    void apiCustomersWillReturnCustomersFromRepository() throws Exception {

        // given
        when(customerRepositoryMock.findCustomerByOptionalPrefix(null)).thenReturn(asList(TestCustomer.xxx, TestCustomer.yyy));

        // when
        final var pacs = customerController.listCustomers("mike@hostsharing.net", null, null);

        // then
        assertThat(pacs).hasSize(2);
        verify(contextMock).setCurrentUser("mike@hostsharing.net");
        verify(contextMock, never()).assumeRoles(any());
    }

    @Test
    void findAllWithAssumedCustomerAdminRole() throws Exception {

        // given
        when(customerRepositoryMock.findCustomerByOptionalPrefix(null)).thenReturn(singletonList(TestCustomer.yyy));

        // when
        final var pacs = customerController.listCustomers("mike@hostsharing.net", "customer#yyy.admin", null);

        // then
        assertThat(pacs).hasSize(1);
        verify(contextMock).setCurrentUser("mike@hostsharing.net");
        verify(contextMock).assumeRoles("customer#yyy.admin");
    }
}
