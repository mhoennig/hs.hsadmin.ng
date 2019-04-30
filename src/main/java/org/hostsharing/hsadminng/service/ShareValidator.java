// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.service.dto.ShareDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import org.springframework.stereotype.Service;

@Service
public class ShareValidator {

    public void validate(final ShareDTO shareDTO) {
        if (shareDTO.getId() != null) {
            throw new BadRequestAlertException(
                    "Share transactions are immutable",
                    Share.ENTITY_NAME,
                    "shareTransactionImmutable");
        }

        if (shareDTO.getDocumentDate().isAfter(shareDTO.getValueDate())) {
            throw new BadRequestAlertException(
                    "Document date may not be after value date",
                    Share.ENTITY_NAME,
                    "documentDateMayNotBeAfterValueDate");
        }

        if ((shareDTO.getAction() == ShareAction.SUBSCRIPTION) && (shareDTO.getQuantity() <= 0)) {
            throw new BadRequestAlertException(
                    "Share subscriptions require a positive quantity",
                    Share.ENTITY_NAME,
                    "shareSubscriptionPositiveQuantity");
        }

        if ((shareDTO.getAction() == ShareAction.CANCELLATION) && (shareDTO.getQuantity() >= 0)) {
            throw new BadRequestAlertException(
                    "Share cancellations require a negative quantity",
                    Share.ENTITY_NAME,
                    "shareCancellationNegativeQuantity");
        }

    }
}
