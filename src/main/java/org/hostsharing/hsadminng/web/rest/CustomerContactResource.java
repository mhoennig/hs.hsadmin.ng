package org.hostsharing.hsadminng.web.rest;
import org.hostsharing.hsadminng.service.CustomerContactService;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.hostsharing.hsadminng.web.rest.util.HeaderUtil;
import org.hostsharing.hsadminng.web.rest.util.PaginationUtil;
import org.hostsharing.hsadminng.service.dto.CustomerContactDTO;
import org.hostsharing.hsadminng.service.dto.CustomerContactCriteria;
import org.hostsharing.hsadminng.service.CustomerContactQueryService;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing CustomerContact.
 */
@RestController
@RequestMapping("/api")
public class CustomerContactResource {

    private final Logger log = LoggerFactory.getLogger(CustomerContactResource.class);

    private static final String ENTITY_NAME = "customerContact";

    private final CustomerContactService customerContactService;

    private final CustomerContactQueryService customerContactQueryService;

    public CustomerContactResource(CustomerContactService customerContactService, CustomerContactQueryService customerContactQueryService) {
        this.customerContactService = customerContactService;
        this.customerContactQueryService = customerContactQueryService;
    }

    /**
     * POST  /customer-contacts : Create a new customerContact.
     *
     * @param customerContactDTO the customerContactDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new customerContactDTO, or with status 400 (Bad Request) if the customerContact has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/customer-contacts")
    public ResponseEntity<CustomerContactDTO> createCustomerContact(@Valid @RequestBody CustomerContactDTO customerContactDTO) throws URISyntaxException {
        log.debug("REST request to save CustomerContact : {}", customerContactDTO);
        if (customerContactDTO.getId() != null) {
            throw new BadRequestAlertException("A new customerContact cannot already have an ID", ENTITY_NAME, "idexists");
        }
        CustomerContactDTO result = customerContactService.save(customerContactDTO);
        return ResponseEntity.created(new URI("/api/customer-contacts/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /customer-contacts : Updates an existing customerContact.
     *
     * @param customerContactDTO the customerContactDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated customerContactDTO,
     * or with status 400 (Bad Request) if the customerContactDTO is not valid,
     * or with status 500 (Internal Server Error) if the customerContactDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/customer-contacts")
    public ResponseEntity<CustomerContactDTO> updateCustomerContact(@Valid @RequestBody CustomerContactDTO customerContactDTO) throws URISyntaxException {
        log.debug("REST request to update CustomerContact : {}", customerContactDTO);
        if (customerContactDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        CustomerContactDTO result = customerContactService.save(customerContactDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, customerContactDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /customer-contacts : get all the customerContacts.
     *
     * @param pageable the pagination information
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the list of customerContacts in body
     */
    @GetMapping("/customer-contacts")
    public ResponseEntity<List<CustomerContactDTO>> getAllCustomerContacts(CustomerContactCriteria criteria, Pageable pageable) {
        log.debug("REST request to get CustomerContacts by criteria: {}", criteria);
        Page<CustomerContactDTO> page = customerContactQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/customer-contacts");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
    * GET  /customer-contacts/count : count all the customerContacts.
    *
    * @param criteria the criterias which the requested entities should match
    * @return the ResponseEntity with status 200 (OK) and the count in body
    */
    @GetMapping("/customer-contacts/count")
    public ResponseEntity<Long> countCustomerContacts(CustomerContactCriteria criteria) {
        log.debug("REST request to count CustomerContacts by criteria: {}", criteria);
        return ResponseEntity.ok().body(customerContactQueryService.countByCriteria(criteria));
    }

    /**
     * GET  /customer-contacts/:id : get the "id" customerContact.
     *
     * @param id the id of the customerContactDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the customerContactDTO, or with status 404 (Not Found)
     */
    @GetMapping("/customer-contacts/{id}")
    public ResponseEntity<CustomerContactDTO> getCustomerContact(@PathVariable Long id) {
        log.debug("REST request to get CustomerContact : {}", id);
        Optional<CustomerContactDTO> customerContactDTO = customerContactService.findOne(id);
        return ResponseUtil.wrapOrNotFound(customerContactDTO);
    }

    /**
     * DELETE  /customer-contacts/:id : delete the "id" customerContact.
     *
     * @param id the id of the customerContactDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/customer-contacts/{id}")
    public ResponseEntity<Void> deleteCustomerContact(@PathVariable Long id) {
        log.debug("REST request to delete CustomerContact : {}", id);
        customerContactService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
