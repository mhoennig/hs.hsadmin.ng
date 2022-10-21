package net.hostsharing.hsadminng.test.cust;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.test.generated.api.v1.api.TestCustomersApi;
import net.hostsharing.hsadminng.test.generated.api.v1.model.TestCustomerResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.mapper.Mapper.map;
import static net.hostsharing.hsadminng.mapper.Mapper.mapList;

@RestController

public class TestCustomerController implements TestCustomersApi {

    @Autowired
    private Context context;

    @Autowired
    private TestCustomerRepository testCustomerRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<TestCustomerResource>> listCustomers(
            String currentUser,
            String assumedRoles,
            String prefix
    ) {
        context.define(currentUser, assumedRoles);

        final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(prefix);

        return ResponseEntity.ok(mapList(result, TestCustomerResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<TestCustomerResource> addCustomer(
            final String currentUser,
            final String assumedRoles,
            final TestCustomerResource customer) {

        context.define(currentUser, assumedRoles);

        if (customer.getUuid() == null) {
            customer.setUuid(UUID.randomUUID());
        }

        final var saved = testCustomerRepository.save(map(customer, TestCustomerEntity.class));

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/test/customers/{id}")
                        .buildAndExpand(customer.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(map(saved, TestCustomerResource.class));
    }

}
