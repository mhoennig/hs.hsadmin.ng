package net.hostsharing.hsadminng.hs.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.CustomersApi;
import net.hostsharing.hsadminng.generated.api.v1.model.CustomerResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.Mapper.map;
import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController

public class CustomerController implements CustomersApi {

    @Autowired
    private Context context;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    @Transactional
    public ResponseEntity<List<CustomerResource>> listCustomers(
        String userName,
        String assumedRoles,
        String prefix
    ) {
        context.setCurrentUser(userName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(
            mapList(
                customerRepository.findCustomerByOptionalPrefixLike(prefix),
                CustomerResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<CustomerResource> addCustomer(
        final String currentUser,
        final String assumedRoles,
        final CustomerResource customer) {
        context.setCurrentUser(currentUser);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        if (customer.getUuid() == null) {
            customer.setUuid(UUID.randomUUID());
        }
        return ResponseEntity.ok(
            map(
                customerRepository.save(map(customer, CustomerEntity.class)),
                CustomerResource.class));
    }

}
