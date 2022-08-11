The symbolik self-link 'api-definition' is a workaround to align IntelliJ IDEAs relative link interpretation with the interpretation of OpenAPI-generator.
Where IDEA only uses the path of the file in which the `$ref` is used,
the OpenAPI-generator always uses the path of the root API definition to resolve relative links.

See also https://github.com/OpenAPITools/openapi-generator/issues/10320.
