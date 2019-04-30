// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AssetValidator {

    public void validate(final AssetDTO assetDTO) {
        if (assetDTO.getId() != null) {
            throw new BadRequestAlertException(
                    "Asset transactions are immutable",
                    Asset.ENTITY_NAME,
                    "assetTransactionImmutable");
        }

        if (assetDTO.getDocumentDate().isAfter(assetDTO.getValueDate())) {
            throw new BadRequestAlertException(
                    "Document date may not be after value date",
                    Asset.ENTITY_NAME,
                    "documentDateMayNotBeAfterValueDate");
        }

        if ((assetDTO.getAction() == AssetAction.PAYMENT) && (assetDTO.getAmount().compareTo(BigDecimal.valueOf(0)) <= 0)) {
            throw new BadRequestAlertException(
                    "Asset payments require a positive amount",
                    Asset.ENTITY_NAME,
                    "assetPaymentsPositiveAmount");
        }
        if ((assetDTO.getAction() == AssetAction.ADOPTION) && (assetDTO.getAmount().compareTo(BigDecimal.valueOf(0)) <= 0)) {
            throw new BadRequestAlertException(
                    "Asset adoptions require a positive amount",
                    Asset.ENTITY_NAME,
                    "assetAdoptionsPositiveAmount");
        }

        if ((assetDTO.getAction() == AssetAction.PAYBACK) && (assetDTO.getAmount().compareTo(BigDecimal.valueOf(0)) >= 0)) {
            throw new BadRequestAlertException(
                    "Asset paybacks require a negative amount",
                    Asset.ENTITY_NAME,
                    "assetPaybacksNegativeAmount");
        }
        if ((assetDTO.getAction() == AssetAction.HANDOVER) && (assetDTO.getAmount().compareTo(BigDecimal.valueOf(0)) >= 0)) {
            throw new BadRequestAlertException(
                    "Asset handovers require a negative amount",
                    Asset.ENTITY_NAME,
                    "assetHandoversNegativeAmount");
        }
        if ((assetDTO.getAction() == AssetAction.LOSS) && (assetDTO.getAmount().compareTo(BigDecimal.valueOf(0)) >= 0)) {
            throw new BadRequestAlertException(
                    "Asset losses require a negative amount",
                    Asset.ENTITY_NAME,
                    "assetLossesNegativeAmount");
        }
        if ((assetDTO.getAction() == AssetAction.CLEARING) && (assetDTO.getAmount().compareTo(BigDecimal.valueOf(0)) >= 0)) {
            throw new BadRequestAlertException(
                    "Asset clearings require a negative amount",
                    Asset.ENTITY_NAME,
                    "assetClearingsNegativeAmount");
        }

    }
}
