package net.hostsharing.hsadminng.rbac.test.cust;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.test.generated.api.v1.api.TestCustomersApi;
import net.hostsharing.hsadminng.test.generated.api.v1.model.TestCustomerResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@RestController
public class TestCustomerController implements TestCustomersApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private TestCustomerRepository testCustomerRepository;

    @PersistenceContext
    EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<TestCustomerResource>> listCustomers(
            String currentSubject,
            String assumedRoles,
            String prefix
    ) {
        context.define(currentSubject, assumedRoles);

        final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(prefix);

        return ResponseEntity.ok(mapper.mapList(result, TestCustomerResource.class));
    }

    @Override
    @Transactional
    public ResponseEntity<TestCustomerResource> addCustomer(
            final String currentSubject,
            final String assumedRoles,
            final TestCustomerResource customer) {

        context.define(currentSubject, assumedRoles);

        final var saved = testCustomerRepository.save(mapper.map(customer, TestCustomerEntity.class));
        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/test/customers/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        return ResponseEntity.created(uri).body(mapper.map(saved, TestCustomerResource.class));
    }

}
