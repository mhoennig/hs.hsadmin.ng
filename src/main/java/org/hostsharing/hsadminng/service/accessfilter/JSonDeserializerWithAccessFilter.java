package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;

import static org.hostsharing.hsadminng.service.util.ReflectionUtil.unchecked;

public class JSonDeserializerWithAccessFilter<T> {

    private final T dto;
    private final TreeNode treeNode;

    public JSonDeserializerWithAccessFilter(final JsonParser jsonParser, final DeserializationContext deserializationContext, Class<T> dtoClass) {
        this.treeNode = unchecked(() -> jsonParser.getCodec().readTree(jsonParser));
        this.dto = unchecked(() -> dtoClass.newInstance());
    }

    public T deserialize() {
//
//        CustomerDTO dto = new CustomerDTO();
//        dto.setId(((IntNode) treeNode.get("id")).asLong());
//        dto.setReference(((IntNode) treeNode.get("reference")).asInt());
//        dto.setPrefix(((TextNode) treeNode.get("prefix")).asText());
//        dto.setName(((TextNode) treeNode.get("name")).asText());
//        dto.setContractualAddress(((TextNode) treeNode.get("contractualAddress")).asText());
//        dto.setContractualSalutation(((TextNode) treeNode.get("contractualSalutation")).asText());
//        dto.setBillingAddress(((TextNode) treeNode.get("billingAddress")).asText());
//        dto.setBillingSalutation(((TextNode) treeNode.get("billingSalutation")).asText());
//        dto.setRemark(((TextNode) treeNode.get("remark")).asText());

        return dto;
    }
}
