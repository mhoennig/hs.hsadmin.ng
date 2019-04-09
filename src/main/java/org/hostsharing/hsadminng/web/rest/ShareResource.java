package org.hostsharing.hsadminng.web.rest;

import io.github.jhipster.web.util.ResponseUtil;
import org.hostsharing.hsadminng.service.ShareQueryService;
import org.hostsharing.hsadminng.service.ShareService;
import org.hostsharing.hsadminng.service.dto.ShareCriteria;
import org.hostsharing.hsadminng.service.dto.ShareDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.hostsharing.hsadminng.web.rest.util.HeaderUtil;
import org.hostsharing.hsadminng.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Share.
 */
@RestController
@RequestMapping("/api")
public class ShareResource {

    private final Logger log = LoggerFactory.getLogger(ShareResource.class);

    private static final String ENTITY_NAME = "share";

    private final ShareService shareService;

    private final ShareQueryService shareQueryService;

    public ShareResource(ShareService shareService, ShareQueryService shareQueryService) {
        this.shareService = shareService;
        this.shareQueryService = shareQueryService;
    }

    /**
     * POST  /shares : Create a new share.
     *
     * @param shareDTO the shareDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shareDTO, or with status 400 (Bad Request) if the share has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/shares")
    public ResponseEntity<ShareDTO> createShare(@Valid @RequestBody ShareDTO shareDTO) throws URISyntaxException {
        log.debug("REST request to save Share : {}", shareDTO);
        if (shareDTO.getId() != null) {
            throw new BadRequestAlertException("A new share cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ShareDTO result = shareService.save(shareDTO);
        return ResponseEntity.created(new URI("/api/shares/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /shares : Updates an existing share.
     *
     * @param shareDTO the shareDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated shareDTO,
     * or with status 400 (Bad Request) if the shareDTO is not valid,
     * or with status 500 (Internal Server Error) if the shareDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/shares")
    public ResponseEntity<ShareDTO> updateShare(@Valid @RequestBody ShareDTO shareDTO) throws URISyntaxException {
        log.debug("REST request to update Share : {}", shareDTO);
        // TODO mhoennig: Rather completely remove the endpoint?
        throw new BadRequestAlertException("Shares are immutable", ENTITY_NAME, "shareTransactionImmutable");
    }

    /**
     * GET  /shares : get all the shares.
     *
     * @param pageable the pagination information
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the list of shares in body
     */
    @GetMapping("/shares")
    public ResponseEntity<List<ShareDTO>> getAllShares(ShareCriteria criteria, Pageable pageable) {
        log.debug("REST request to get Shares by criteria: {}", criteria);
        Page<ShareDTO> page = shareQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shares");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
    * GET  /shares/count : count all the shares.
    *
    * @param criteria the criterias which the requested entities should match
    * @return the ResponseEntity with status 200 (OK) and the count in body
    */
    @GetMapping("/shares/count")
    public ResponseEntity<Long> countShares(ShareCriteria criteria) {
        log.debug("REST request to count Shares by criteria: {}", criteria);
        return ResponseEntity.ok().body(shareQueryService.countByCriteria(criteria));
    }

    /**
     * GET  /shares/:id : get the "id" share.
     *
     * @param id the id of the shareDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the shareDTO, or with status 404 (Not Found)
     */
    @GetMapping("/shares/{id}")
    public ResponseEntity<ShareDTO> getShare(@PathVariable Long id) {
        log.debug("REST request to get Share : {}", id);
        Optional<ShareDTO> shareDTO = shareService.findOne(id);
        return ResponseUtil.wrapOrNotFound(shareDTO);
    }

    /**
     * DELETE  /shares/:id : delete the "id" share.
     *
     * @param id the id of the shareDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/shares/{id}")
    public ResponseEntity<Void> deleteShare(@PathVariable Long id) {
        log.debug("REST request to delete Share : {}", id);
        // TODO mhoennig: Rather completely remove the endpoint?
        throw new BadRequestAlertException("Shares are immutable", ENTITY_NAME, "shareTransactionImmutable");
    }
}
