package net.hostsharing.hsadminng.hs.hosting.asset.validator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static net.hostsharing.hsadminng.hs.hosting.asset.validator.EnumPropertyValidator.enumerationProperty;
import static net.hostsharing.hsadminng.hs.hosting.asset.validator.HsHostingAssetPropertyValidator.defType;
import static net.hostsharing.hsadminng.hs.hosting.asset.validator.BooleanPropertyValidator.booleanProperty;
import static net.hostsharing.hsadminng.hs.hosting.asset.validator.IntegerPropertyValidator.integerProperty;

public class HsHostingAssetValidator {

    private static final Map<HsHostingAssetType, HsHostingAssetValidator> validators = Map.ofEntries(
            defType(HsHostingAssetType.CLOUD_SERVER, new HsHostingAssetValidator(
                    integerProperty("CPUs").min(1).max(32).required(),
                    integerProperty("RAM").unit("GB").min(1).max(128).required(),
                    integerProperty("SSD").unit("GB").min(25).max(1000).step(25).required(),
                    integerProperty("HDD").unit("GB").min(0).max(4000).step(250).optional(),
                    integerProperty("Traffic").unit("GB").min(250).max(10000).step(250).required(),
                    enumerationProperty("SLA-Infrastructure").values("BASIC", "EXT8H", "EXT4H", "EXT2H").optional())),
            defType(HsHostingAssetType.MANAGED_SERVER, new HsHostingAssetValidator(
                    integerProperty("CPUs").min(1).max(32).required(),
                    integerProperty("RAM").unit("GB").min(1).max(128).required(),
                    integerProperty("SSD").unit("GB").min(25).max(1000).step(25).required(),
                    integerProperty("HDD").unit("GB").min(0).max(4000).step(250).optional(),
                    integerProperty("Traffic").unit("GB").min(250).max(10000).step(250).required(),
                    enumerationProperty("SLA-Platform").values("BASIC", "EXT8H", "EXT4H", "EXT2H").optional(),
                    booleanProperty("SLA-EMail").falseIf("SLA-Platform", "BASIC").optional(),
                    booleanProperty("SLA-Maria").falseIf("SLA-Platform", "BASIC").optional(),
                    booleanProperty("SLA-PgSQL").falseIf("SLA-Platform", "BASIC").optional(),
                    booleanProperty("SLA-Office").falseIf("SLA-Platform", "BASIC").optional(),
                    booleanProperty("SLA-Web").falseIf("SLA-Platform", "BASIC").optional())),
            defType(HsHostingAssetType.MANAGED_WEBSPACE, new HsHostingAssetValidator(
                    integerProperty("SSD").unit("GB").min(1).max(100).step(1).required(),
                    integerProperty("HDD").unit("GB").min(0).max(250).step(10).optional(),
                    integerProperty("Traffic").unit("GB").min(10).max(1000).step(10).required(),
                    enumerationProperty("SLA-Platform").values("BASIC", "EXT24H").optional(),
                    integerProperty("Daemons").min(0).max(10).optional(),
                    booleanProperty("Online Office Server").optional())
            ));
    static {
        validators.entrySet().forEach(typeDef -> {
            stream(typeDef.getValue().propertyValidators).forEach( entry -> {
                entry.verifyConsistency(typeDef);
            });
        });
    }
    private final HsHostingAssetPropertyValidator<?>[] propertyValidators;

    public static HsHostingAssetValidator forType(final HsHostingAssetType type) {
        return validators.get(type);
    }

    HsHostingAssetValidator(final HsHostingAssetPropertyValidator<?>... validators) {
        propertyValidators = validators;
    }

    public static Set<HsHostingAssetType> types() {
        return validators.keySet();
    }

    public List<String> validate(final HsHostingAssetEntity assetEntity) {
        final var result = new ArrayList<String>();
        assetEntity.getConfig().keySet().forEach( givenPropName -> {
            if (stream(propertyValidators).map(pv -> pv.propertyName).noneMatch(propName -> propName.equals(givenPropName))) {
                result.add("'" + givenPropName + "' is not expected but is '" +assetEntity.getConfig().get(givenPropName) + "'");
            }
        });
        stream(propertyValidators).forEach(pv -> {
          result.addAll(pv.validate(assetEntity.getConfig()));
        });
        return result;
    }

    public List<Map<String, Object>> properties() {
        final var mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return Arrays.stream(propertyValidators)
                .map(propertyValidator -> propertyValidator.toMap(mapper))
                .map(HsHostingAssetValidator::asKeyValueMap)
                .toList();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String, Object> asKeyValueMap(final Map map) {
        return (Map<String, Object>) map;
    }

}
