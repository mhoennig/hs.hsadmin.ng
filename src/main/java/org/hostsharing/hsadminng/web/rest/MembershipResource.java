package org.hostsharing.hsadminng.web.rest;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.hostsharing.hsadminng.web.rest.util.HeaderUtil;
import org.hostsharing.hsadminng.web.rest.util.PaginationUtil;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.service.dto.MembershipCriteria;
import org.hostsharing.hsadminng.service.MembershipQueryService;
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
 * REST controller for managing Membership.
 */
@RestController
@RequestMapping("/api")
public class MembershipResource {

    private final Logger log = LoggerFactory.getLogger(MembershipResource.class);

    private static final String ENTITY_NAME = "membership";

    private final MembershipService membershipService;

    private final MembershipQueryService membershipQueryService;

    public MembershipResource(MembershipService membershipService, MembershipQueryService membershipQueryService) {
        this.membershipService = membershipService;
        this.membershipQueryService = membershipQueryService;
    }

    /**
     * POST  /memberships : Create a new membership.
     *
     * @param membershipDTO the membershipDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new membershipDTO, or with status 400 (Bad Request) if the membership has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/memberships")
    public ResponseEntity<MembershipDTO> createMembership(@Valid @RequestBody MembershipDTO membershipDTO) throws URISyntaxException {
        log.debug("REST request to save Membership : {}", membershipDTO);
        if (membershipDTO.getId() != null) {
            throw new BadRequestAlertException("A new membership cannot already have an ID", ENTITY_NAME, "idexists");
        }
        MembershipDTO result = membershipService.save(membershipDTO);
        return ResponseEntity.created(new URI("/api/memberships/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /memberships : Updates an existing membership.
     *
     * @param membershipDTO the membershipDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated membershipDTO,
     * or with status 400 (Bad Request) if the membershipDTO is not valid,
     * or with status 500 (Internal Server Error) if the membershipDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/memberships")
    public ResponseEntity<MembershipDTO> updateMembership(@Valid @RequestBody MembershipDTO membershipDTO) throws URISyntaxException {
        log.debug("REST request to update Membership : {}", membershipDTO);
        if (membershipDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        MembershipDTO result = membershipService.save(membershipDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, membershipDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /memberships : get all the memberships.
     *
     * @param pageable the pagination information
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the list of memberships in body
     */
    @GetMapping("/memberships")
    public ResponseEntity<List<MembershipDTO>> getAllMemberships(MembershipCriteria criteria, Pageable pageable) {
        log.debug("REST request to get Memberships by criteria: {}", criteria);
        Page<MembershipDTO> page = membershipQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/memberships");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
    * GET  /memberships/count : count all the memberships.
    *
    * @param criteria the criterias which the requested entities should match
    * @return the ResponseEntity with status 200 (OK) and the count in body
    */
    @GetMapping("/memberships/count")
    public ResponseEntity<Long> countMemberships(MembershipCriteria criteria) {
        log.debug("REST request to count Memberships by criteria: {}", criteria);
        return ResponseEntity.ok().body(membershipQueryService.countByCriteria(criteria));
    }

    /**
     * GET  /memberships/:id : get the "id" membership.
     *
     * @param id the id of the membershipDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the membershipDTO, or with status 404 (Not Found)
     */
    @GetMapping("/memberships/{id}")
    public ResponseEntity<MembershipDTO> getMembership(@PathVariable Long id) {
        log.debug("REST request to get Membership : {}", id);
        Optional<MembershipDTO> membershipDTO = membershipService.findOne(id);
        return ResponseUtil.wrapOrNotFound(membershipDTO);
    }

    /**
     * DELETE  /memberships/:id : delete the "id" membership.
     *
     * @param id the id of the membershipDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/memberships/{id}")
    public ResponseEntity<Void> deleteMembership(@PathVariable Long id) {
        log.debug("REST request to delete Membership : {}", id);
        membershipService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
