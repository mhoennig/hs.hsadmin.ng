# UseCase-Tests

We define UseCase-tests as test for business-scenarios.
They test positive (successful) scenarios by using the REST-API.

Running these tests also creates test-reports which can be used as documentation about the necessary REST-calls for each scenario.

Clarification: Acceptance tests also test at the REST-API level but are more technical and also test negative (error-) scenarios.

## ... extends ScenarioTest

Each test-method in subclasses of ScenarioTest describes a business-scenario,
each utilizing a main-use-case and given example data for the scenario. 

To reduce the number of API-calls, intermediate results can be re-used.
This is controlled by two annotations:

### @Produces(....)

This annotation tells the test-runner that this scenario produces certain business object for re-use.
The UUID of the new business objects are stored in a key-value map using the provided keys.

There are two variants of this annotation:

#### A Single Business Object 
```
@Produces("key")
```

This variant is used when there is just a single business-object produced by the use-case.

#### Multiple Business Objects

```
@Produces(explicitly = "main-key", implicitly = {"other-key", ...}) 
```

This variant is used when multiple business-objects are produced by the use-case,
e.g. a Relation, a Person and a Contact.
The UUID of the business-object produced by the main-use-case gets stored as the key after "explicitly",
the others are listed after "implicitly"; 
if there is just one, leave out the surrounding braces.

### @Requires(...)

This annotation tells the test-runner that which business objects are required before this scenario can run.

Each subset must be produced by the same producer-method.


## ... extends UseCase

These classes consist of two parts:

### Prerequisites of the Use-Case

The constructor may create prerequisites via `required(...)`.
These do not really belong to the use-case itself,
e.g. create business objects which, in the context of that use-case, would already exist.

This is similar to @Requires(...) just that no other test scenario produces this prerequisite.
Here, use-cases can be re-used, usually with different data.

### The Use-Case Itself

The use-case is implemented by the `run()`-method which contains HTTP-calls.

Each HTTP-call is wrapped into either `obtain(...)` to keep the result in a placeholder variable,
the variable name is also used as a title.
Or it's wrapped into a `withTitle(...)` to assign a title.

The HTTP-call is followed by some assertions, e.g. the HTTP status and JSON-path-expression-matchers.

Use `${...}` for placeholders which need to be replaced with JSON quotes
(e.g. strings are quoted, numbers are not),
`%{...}` for placeholders which need to be rendered raw
and `&{...}` for placeholders which need to get URI-encoded.

If `???` is added before the closing brace, the property is optional.
This means, if it's not available in the properties, `null` is used.

Properties with null-values are removed from the JSON.
If you need to keep a null-value, e.g. to delete a property,
use `NULL` (all caps) in the template (not the variable value).

A special syntax is the infix `???`-operator like in: `${%{var1???}???%{var2???}%{var3???}}`.
In this case the first non-null value is used.



### The Use-Case Verification

The verification-step is implemented by the `verify()`-method which usually contains a HTTP-HTTP-call.

It can also contain a JSON-path verification to check if a certain value is in the result.

