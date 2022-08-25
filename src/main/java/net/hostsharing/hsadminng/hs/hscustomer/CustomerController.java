package net.hostsharing.hsadminng.hs.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.CustomersApi;
import net.hostsharing.hsadminng.generated.api.v1.model.CustomerResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

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
    @Transactional(readOnly = true)
    public ResponseEntity<List<CustomerResource>> listCustomers(
            String userName,
            String assumedRoles,
            String prefix
    ) {
        context.setCurrentUser(userName);
        if (!StringUtils.isBlank(assumedRoles)) {
            context.assumeRoles(assumedRoles);
        }

        final var result = customerRepository.findCustomerByOptionalPrefixLike(prefix);

        return ResponseEntity.ok(mapList(result, CustomerResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<CustomerResource> addCustomer(
            final String currentUser,
            final String assumedRoles,
            final CustomerResource customer) {

        context.setCurrentTask("create new customer: #" + customer.getReference() + " / " + customer.getPrefix());
        context.setCurrentUser(currentUser);
        if (!StringUtils.isBlank(assumedRoles)) {
            context.assumeRoles(assumedRoles);
        }
        if (customer.getUuid() == null) {
            customer.setUuid(UUID.randomUUID());
        }

        final var saved = customerRepository.save(map(customer, CustomerEntity.class));

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/customers/{id}")
                        .buildAndExpand(customer.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(map(saved, CustomerResource.class));
    }

}
