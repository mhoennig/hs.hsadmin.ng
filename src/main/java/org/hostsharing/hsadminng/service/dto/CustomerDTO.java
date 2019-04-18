package org.hostsharing.hsadminng.service.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.NotImplementedException;
import org.hostsharing.hsadminng.security.SecurityUtils;
import org.springframework.boot.jackson.JsonComponent;

import javax.validation.constraints.*;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * A DTO for the Customer entity.
 */
public class CustomerDTO implements Serializable {

    @AccessFor(read = Role.ANY_CUSTOMER_USER)
    private Long id;

    @NotNull
    @Min(value = 10000)
    @Max(value = 99999)
    @AccessFor(init = Role.ADMIN, read = Role.ANY_CUSTOMER_USER)
    private Integer reference;

    @NotNull
    @Size(max = 3)
    @Pattern(regexp = "[a-z][a-z0-9]+")
    @AccessFor(init = Role.ADMIN, read = Role.ANY_CUSTOMER_USER)
    private String prefix;

    @NotNull
    @Size(max = 80)
    @AccessFor(init = Role.ADMIN, read = Role.ANY_CUSTOMER_USER)
    private String name;

    @Size(max = 80)
    @AccessFor(init = Role.ADMIN, update = Role.CONTRACTUAL_CONTACT, read = Role.ANY_CUSTOMER_CONTACT)
    private String contractualSalutation;

    @NotNull
    @Size(max = 400)
    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = Role.CONTRACTUAL_CONTACT)
    private String contractualAddress;

    @Size(max = 80)
    @AccessFor(init = Role.ADMIN, update = Role.CONTRACTUAL_CONTACT, read = Role.ANY_CUSTOMER_CONTACT)
    private String contractualSalutation;

    @Size(max = 80)
    @AccessFor(init = Role.ADMIN, update = {Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT}, read = Role.CONTRACTUAL_CONTACT)
    private String billingSalutation;

    @Size(max = 400)
    @AccessFor(init = Role.ADMIN, update = Role.ADMIN, read = Role.CONTRACTUAL_CONTACT)
    private String billingAddress;

    @Size(max = 160)
    @AccessFor(init = Role.ADMIN, update = Role.SUPPORTER, read = Role.SUPPORTER)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getReference() {
        return reference;
    }

    public void setReference(Integer reference) {
        this.reference = reference;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContractualSalutation() {
        return contractualSalutation;
    }

    public void setContractualSalutation(String contractualSalutation) {
        this.contractualSalutation = contractualSalutation;
    }

    public String getContractualAddress() {
        return contractualAddress;
    }

    public void setContractualAddress(String contractualAddress) {
        this.contractualAddress = contractualAddress;
    }

    public String getBillingSalutation() {
        return billingSalutation;
    }

    public void setBillingSalutation(String billingSalutation) {
        this.billingSalutation = billingSalutation;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CustomerDTO customerDTO = (CustomerDTO) o;
        if (customerDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), customerDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "CustomerDTO{" +
            "id=" + getId() +
            ", reference=" + getReference() +
            ", prefix='" + getPrefix() + "'" +
            ", name='" + getName() + "'" +
            ", contractualSalutation='" + getContractualSalutation() + "'" +
            ", contractualAddress='" + getContractualAddress() + "'" +
            ", billingSalutation='" + getBillingSalutation() + "'" +
            ", billingAddress='" + getBillingAddress() + "'" +
            ", remark='" + getRemark() + "'" +
            "}";
    }

    @JsonComponent
    public static class CustomerJsonSerializer extends JsonSerializer<CustomerDTO> {

        @Override
        public void serialize(CustomerDTO dto, JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {

            jsonGenerator.writeStartObject();
            for (Field prop : CustomerDTO.class.getDeclaredFields()) {
                toJSon(dto, jsonGenerator, prop);
            }

            jsonGenerator.writeEndObject();
        }

        private void toJSon(CustomerDTO dto, JsonGenerator jsonGenerator, Field prop) throws IOException {
            if (getLoginUserRole().isAllowedToRead(prop)) {
                final String fieldName = prop.getName();
                if (Integer.class.isAssignableFrom(prop.getType()) || int.class.isAssignableFrom(prop.getType())) {
                    jsonGenerator.writeNumberField(fieldName, (int) get(dto, prop));
                } else if (Long.class.isAssignableFrom(prop.getType()) || long.class.isAssignableFrom(prop.getType())) {
                    jsonGenerator.writeNumberField(fieldName, (long) get(dto, prop));
                } else if (String.class.isAssignableFrom(prop.getType())) {
                    jsonGenerator.writeStringField(fieldName, (String) get(dto, prop));
                } else {
                    throw new NotImplementedException("property type not yet implemented" + prop);
                }
            }
        }

        private Object get(CustomerDTO dto, Field field) {
            try {
                field.setAccessible(true);
                return field.get(dto);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private Role getLoginUserRole() {
            return SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);
        }

        private Object invoke(Object dto, Method method) {
            try {
                return method.invoke(dto);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @JsonComponent
    public static class UserJsonDeserializer extends JsonDeserializer<CustomerDTO> {

        @Override
        public CustomerDTO deserialize(JsonParser jsonParser,
                                       DeserializationContext deserializationContext) throws IOException,
            JsonProcessingException {

            TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);

            CustomerDTO dto = new CustomerDTO();
            dto.setId(((IntNode) treeNode.get("id")).asLong());
            dto.setNumber(((IntNode) treeNode.get("number")).asInt());
            dto.setPrefix(((TextNode) treeNode.get("prefix")).asText());
            dto.setName(((TextNode) treeNode.get("name")).asText());
            dto.setContractualAddress(((TextNode) treeNode.get("contractualAddress")).asText());
            dto.setContractualSalutation(((TextNode) treeNode.get("contractualSalutation")).asText());
            dto.setBillingAddress(((TextNode) treeNode.get("billingAddress")).asText());
            dto.setBillingSalutation(((TextNode) treeNode.get("billingSalutation")).asText());

            return dto;
        }
    }
}

enum Role {
    NOBODY(0), HOSTMASTER(1), ADMIN(2), SUPPORTER(3),
    ANY_CUSTOMER_CONTACT(20), CONTRACTUAL_CONTACT(21), FINANCIAL_CONTACT(22), TECHNICAL_CONTACT(22),
    ANY_CUSTOMER_USER(80),
    ANYBODY(99);

    private final int level;

    Role(final int level) {
        this.level = level;
    }

    boolean covers(final Role role) {
        return this == role || this.level < role.level;
    }

    public boolean isAllowedToInit(Field field) {

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.init());
    }

    public boolean isAllowedToUpdate(Field field) {

        final Role loginUserRole = SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.update());
    }

    public boolean isAllowedToRead(Field field) {

        final Role loginUserRole = SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.read());
    }

    private boolean isRoleCovered(Role[] requiredRoles) {
        for (Role accessAllowedForRole : requiredRoles) {
            if (this.covers(accessAllowedForRole)) {
                return true;
            }
        }
        return false;
    }

}

@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface AccessFor {
    Role[] init() default Role.NOBODY;

    Role[] update() default Role.NOBODY;

    Role[] read() default Role.NOBODY;
}

