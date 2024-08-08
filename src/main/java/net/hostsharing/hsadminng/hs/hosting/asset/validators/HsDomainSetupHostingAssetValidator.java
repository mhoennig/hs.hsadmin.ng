package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.List;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;

class HsDomainSetupHostingAssetValidator extends HostingAssetEntityValidator {

    public static final String FQDN_REGEX = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,12}";

    private final Pattern identifierPattern;

    HsDomainSetupHostingAssetValidator() {
        super(  DOMAIN_SETUP,
                AlarmContact.isOptional(),

                NO_EXTRA_PROPERTIES);
        this.identifierPattern = Pattern.compile(FQDN_REGEX);
    }

    @Override
    public List<String> validateEntity(final HsHostingAsset assetEntity) {
        // TODO.impl: for newly created entities, check the permission of setting up a domain
        //
        // reject, if the domain is any of these:
        //  hostsharing.com|net|org|coop, // just to be on the safe side
        //  [^.}+, // top-level-domain
        //   co.uk, org.uk, gov.uk, ac.uk, sch.uk,
        //   com.au, net.au, org.au, edu.au, gov.au, asn.au, id.au,
        //   co.jp, ne.jp, or.jp, ac.jp, go.jp,
        //   com.cn, net.cn, org.cn, gov.cn, edu.cn, ac.cn,
        //   com.br, net.br, org.br, gov.br, edu.br, mil.br, art.br,
        //   co.in, net.in, org.in, gen.in, firm.in, ind.in,
        //   com.mx, net.mx, org.mx, gob.mx, edu.mx,
        //   gov.it, edu.it,
        //   co.nz, net.nz, org.nz, govt.nz, ac.nz, school.nz, geek.nz, kiwi.nz,
        //   co.kr, ne.kr, or.kr, go.kr, re.kr, pe.kr
        //
        // allow if
        //  - user has Admin/Agent-role for all its sub-domains and the direct parent-Domain which are set up at at Hostsharing
        //  - domain has DNS zone with TXT record approval
        //  - parent-domain has DNS zone with TXT record approval
        //
        // TXT-Record check:
        // new InitialDirContext().getAttributes("dns:_netblocks.google.com", new String[] { "TXT"}).get("TXT").getAll();

        return super.validateEntity(assetEntity);
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        return identifierPattern;
    }
}
