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

import javax.annotation.PostConstruct;
import javax.validation.constraints.*;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;


/**
 * A DTO for the Customer entity.
 */
@ReadableFor(Role.ANY_CUSTOMER_USER)
@WritableFor(Role.SUPPORTER)
public class CustomerDTO implements Serializable {

    @WritableFor(Role.NOBODY)
    private Long id;

    @NotNull
    @Min(value = 10000)
    @Max(value = 99999)
    private Integer number;

    @NotNull
    @Pattern(regexp = "[a-z][a-z0-9]+")
    private String prefix;

    @NotNull
    @Size(max = 80)
    private String name;

    @NotNull
    @Size(max = 400)
    @ReadableFor(Role.CONTRACTUAL_CONTACT)
    private String contractualAddress;

    @Size(max = 80)
    @ReadableFor(Role.CONTRACTUAL_CONTACT)
    @WritableFor(Role.CONTRACTUAL_CONTACT)
    private String contractualSalutation;

    @Size(max = 400)
    @ReadableFor(Role.CONTRACTUAL_CONTACT)
    private String billingAddress;

    @Size(max = 80)
    @ReadableFor(Role.CONTRACTUAL_CONTACT)
    @WritableFor(Role.CONTRACTUAL_CONTACT)
    private String billingSalutation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
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

    public String getContractualAddress() {
        return contractualAddress;
    }

    public void setContractualAddress(String contractualAddress) {
        this.contractualAddress = contractualAddress;
    }

    public String getContractualSalutation() {
        return contractualSalutation;
    }

    public void setContractualSalutation(String contractualSalutation) {
        this.contractualSalutation = contractualSalutation;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getBillingSalutation() {
        return billingSalutation;
    }

    public void setBillingSalutation(String billingSalutation) {
        this.billingSalutation = billingSalutation;
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
            ", number=" + getNumber() +
            ", prefix='" + getPrefix() + "'" +
            ", name='" + getName() + "'" +
            ", contractualAddress='" + getContractualAddress() + "'" +
            ", contractualSalutation='" + getContractualSalutation() + "'" +
            ", billingAddress='" + getBillingAddress() + "'" +
            ", billingSalutation='" + getBillingSalutation() + "'" +
            "}";
    }

    @JsonComponent
    public static class CustomerJsonSerializer extends JsonSerializer<CustomerDTO> {

        private Optional<String> login;

        @PostConstruct
        public void getLoginUser() {
            this.login = SecurityUtils.getCurrentUserLogin();
        }

        @Override
        public void serialize(CustomerDTO dto, JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {

            jsonGenerator.writeStartObject();
            try {
                for (PropertyDescriptor prop : Introspector.getBeanInfo(CustomerDTO.class).getPropertyDescriptors()) {
                    if (isRealProprety(prop)) {
                        toJSon(dto, jsonGenerator, prop);
                    }
                }
            } catch (IntrospectionException e) {
                throw new RuntimeException(e);
            }

//
//            jsonGenerator.writeNumberField("number", dto.getNumber());
//            jsonGenerator.writeStringField("prefix", dto.getPrefix());
//            jsonGenerator.writeStringField("name", dto.getName());
//            toJSonString(dto, jsonGenerator,"contractualAddress");
//            jsonGenerator.writeStringField("contractualSalutation", dto.getContractualSalutation());
//            jsonGenerator.writeStringField("billingAddress", dto.getBillingAddress());
//            jsonGenerator.writeStringField("billingSalutation", dto.getBillingSalutation());

            jsonGenerator.writeEndObject();
        }

        private boolean isRealProprety(PropertyDescriptor prop) {
            return prop.getWriteMethod() != null;
        }

        private void toJSonString(CustomerDTO user, JsonGenerator jsonGenerator, String fieldName) throws IOException {
            if (isReadAllowed(fieldName)) {
                jsonGenerator.writeStringField(fieldName, user.getContractualAddress());
            }
        }

        private void toJSon(CustomerDTO dto, JsonGenerator jsonGenerator, PropertyDescriptor prop) throws IOException {
            final String fieldName = prop.getName();
            if (isReadAllowed(fieldName)) {
                if (Integer.class.isAssignableFrom(prop.getPropertyType()) || int.class.isAssignableFrom(prop.getPropertyType())) {
                    jsonGenerator.writeNumberField(fieldName, (int) invoke(dto, prop.getReadMethod()));
                } else if (Long.class.isAssignableFrom(prop.getPropertyType()) || long.class.isAssignableFrom(prop.getPropertyType())) {
                    jsonGenerator.writeNumberField(fieldName, (long) invoke(dto, prop.getReadMethod()));
                } else if (String.class.isAssignableFrom(prop.getPropertyType())) {
                    jsonGenerator.writeStringField(fieldName, (String) invoke(dto, prop.getReadMethod()));
                } else {
                    throw new NotImplementedException("property type not yet implemented" + prop);
                }
            }
        }

        private Object invoke(Object dto, Method method) {
            try {
                return method.invoke(dto);
            } catch (IllegalAccessException|InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean isReadAllowed(String fieldName) {
            if ( fieldName.equals("contractualAddress") ) {
                return login.map(user -> user.equals("admin")).orElse(false);
            }
            return true;
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
    ANY_CUSTOMER_CONTACT(10), CONTRACTUAL_CONTACT(11),
    ANY_CUSTOMER_USER(30);

    private final int level;

    Role(final int level) {
        this.level = level;
    }
}

@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface ReadableFor {

}


@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface WritableFor {

}
