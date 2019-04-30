// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.service.UserRoleAssignmentQueryService;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.dto.UserRoleAssignmentCriteria;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.hostsharing.hsadminng.web.rest.util.HeaderUtil;
import org.hostsharing.hsadminng.web.rest.util.PaginationUtil;

import io.github.jhipster.web.util.ResponseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

/**
 * REST controller for managing UserRoleAssignment.
 */
@RestController
@RequestMapping("/api")
public class UserRoleAssignmentResource {

    private final Logger log = LoggerFactory.getLogger(UserRoleAssignmentResource.class);

    private static final String ENTITY_NAME = "userRoleAssignment";

    private final UserRoleAssignmentService userRoleAssignmentService;

    private final UserRoleAssignmentQueryService userRoleAssignmentQueryService;

    public UserRoleAssignmentResource(
            UserRoleAssignmentService userRoleAssignmentService,
            UserRoleAssignmentQueryService userRoleAssignmentQueryService) {
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.userRoleAssignmentQueryService = userRoleAssignmentQueryService;
    }

    /**
     * POST /user-role-assignments : Create a new userRoleAssignment.
     *
     * @param userRoleAssignment the userRoleAssignment to create
     * @return the ResponseEntity with status 201 (Created) and with body the new userRoleAssignment, or with status 400 (Bad
     *         Request) if the userRoleAssignment has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/user-role-assignments")
    public ResponseEntity<UserRoleAssignment> createUserRoleAssignment(
            @Valid @RequestBody UserRoleAssignment userRoleAssignment) throws URISyntaxException {
        log.debug("REST request to save UserRoleAssignment : {}", userRoleAssignment);
        if (userRoleAssignment.getId() != null) {
            throw new BadRequestAlertException("A new userRoleAssignment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        UserRoleAssignment result = userRoleAssignmentService.save(userRoleAssignment);
        return ResponseEntity.created(new URI("/api/user-role-assignments/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
                .body(result);
    }

    /**
     * PUT /user-role-assignments : Updates an existing userRoleAssignment.
     *
     * @param userRoleAssignment the userRoleAssignment to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated userRoleAssignment,
     *         or with status 400 (Bad Request) if the userRoleAssignment is not valid,
     *         or with status 500 (Internal Server Error) if the userRoleAssignment couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/user-role-assignments")
    public ResponseEntity<UserRoleAssignment> updateUserRoleAssignment(
            @Valid @RequestBody UserRoleAssignment userRoleAssignment) throws URISyntaxException {
        log.debug("REST request to update UserRoleAssignment : {}", userRoleAssignment);
        if (userRoleAssignment.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        UserRoleAssignment result = userRoleAssignmentService.save(userRoleAssignment);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, userRoleAssignment.getId().toString()))
                .body(result);
    }

    /**
     * GET /user-role-assignments : get all the userRoleAssignments.
     *
     * @param pageable the pagination information
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the list of userRoleAssignments in body
     */
    @GetMapping("/user-role-assignments")
    public ResponseEntity<List<UserRoleAssignment>> getAllUserRoleAssignments(
            UserRoleAssignmentCriteria criteria,
            Pageable pageable) {
        log.debug("REST request to get UserRoleAssignments by criteria: {}", criteria);
        Page<UserRoleAssignment> page = userRoleAssignmentQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/user-role-assignments");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /user-role-assignments/count : count all the userRoleAssignments.
     *
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the count in body
     */
    @GetMapping("/user-role-assignments/count")
    public ResponseEntity<Long> countUserRoleAssignments(UserRoleAssignmentCriteria criteria) {
        log.debug("REST request to count UserRoleAssignments by criteria: {}", criteria);
        return ResponseEntity.ok().body(userRoleAssignmentQueryService.countByCriteria(criteria));
    }

    /**
     * GET /user-role-assignments/:id : get the "id" userRoleAssignment.
     *
     * @param id the id of the userRoleAssignment to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the userRoleAssignment, or with status 404 (Not Found)
     */
    @GetMapping("/user-role-assignments/{id}")
    public ResponseEntity<UserRoleAssignment> getUserRoleAssignment(@PathVariable Long id) {
        log.debug("REST request to get UserRoleAssignment : {}", id);
        Optional<UserRoleAssignment> userRoleAssignment = userRoleAssignmentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(userRoleAssignment);
    }

    /**
     * DELETE /user-role-assignments/:id : delete the "id" userRoleAssignment.
     *
     * @param id the id of the userRoleAssignment to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/user-role-assignments/{id}")
    public ResponseEntity<Void> deleteUserRoleAssignment(@PathVariable Long id) {
        log.debug("REST request to delete UserRoleAssignment : {}", id);
        userRoleAssignmentService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
