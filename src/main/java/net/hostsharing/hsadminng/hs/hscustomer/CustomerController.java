package net.hostsharing.hsadminng.hs.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@RestController

public class CustomerController {

    @Autowired
    private Context context;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping(value = "/api/customers")
    @Transactional
    public List<CustomerEntity> listCustomers(
        @RequestHeader(value = "current-user") String userName,
        @RequestHeader(value = "assumed-roles", required = false) String assumedRoles
    ) {
        context.setCurrentUser(userName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return customerRepository.findAll();
    }

    @PostMapping(value = "/api/customers")
    @ResponseStatus
    @Transactional
    public CustomerEntity addCustomer(
        @RequestHeader(value = "current-user") String userName,
        @RequestHeader(value = "assumed-roles", required = false) String assumedRoles,
        @RequestBody CustomerEntity customer
    ) {
        context.setCurrentUser(userName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        if (customer.getUuid() == null) {
            customer.setUuid(UUID.randomUUID());
        }
        return customerRepository.save(customer);
    }

}
