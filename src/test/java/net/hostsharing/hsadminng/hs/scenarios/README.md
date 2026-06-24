# UseCase-Tests

We define UseCase-tests as test for business-scenarios.
They test positive (successful) scenarios by using the REST-API.

Running these tests also creates test-reports which can be used as documentation about the necessary REST-calls for each scenario.

Clarification: Acceptance tests also test at the REST-API level but are more technical and also test negative (error-) scenarios.

## Generated Reports

Scenario reports render each HTTP request similar to a `curl` command line style, 
precisely as `HTTP METHOD '/path' \`, where `HTTP` could be an easy `curl`-wrapper.
This wrapper could, for example, add `--fail-with-body --show-error --no-progress-meter -X` to each call
as well as piping the output to `jq` to pretty-print the response body.

Headers are rendered in a `curl`-compatible style, one per line as `-H 'Name: value'`.
For readability, report headers are printed in a sensible, human-readable order.

Request bodies are rendered as pretty JSON in a `-d '...'` argument.

The real JWT bearer is not shown in reports, it could not be directly read by a human anyway.
Instead, the report uses a readable fake bearer notation with only the relevant JWT claims, for example:

```
-H 'Authorization: Bearer JWT {
    "comment": "some arbitrary user",
    "sub" : "uuid<some-user@example.org>",
    "groups" : [
      "/xyz-Service"
    ]
  }'
```

The `comment` just to descibe what kind of user a scenario requires;
it's usually not part of the real JWT.

The `sub` value is rendered as `uuid<subject-name>` to show that a real JWT contains a UUID,
while keeping the scenario report readable.
The `groups` claim is shown only if groups are present in the fake JWT.

## ... extends ScenarioTest

Each test-method in subclasses of ScenarioTest describes a business-scenario,
each using a main-use-case and given example data for the scenario. 
Each scenario test method should instantiate and execute exactly one top-level `UseCase` directly.
Only the given and expected parameters should vary between scenario test methods.

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

Scenario test methods declare cross-scenario dependencies with `@Produces` and `@Requires`.
If a scenario method is annotated with `@Produces`, call `.keep()` when the produced alias should refer
to the UUID from the main response `Location` header.


## ... extends UseCase

These classes consist of two parts:

Each `UseCase` should act as a single login subject/user.
If a workflow needs multiple acting users, split it into multiple use cases and scenario test methods
connected via `@Produces` and `@Requires`.
A `UseCase` may call other use cases internally when that is part of the workflow.

### Prerequisites of the Use-Case

The constructor may create prerequisites via `required(...)`.
These do not really belong to the use-case itself,
e.g. create business objects which, in the context of that use-case, would already exist.

This is similar to @Requires(...) just that no other test scenario produces this prerequisite.
Here, use-cases can be re-used, usually with different data.

### The Use-Case Itself

The use-case is implemented by the overridden `run(...)`-method which contains HTTP-calls
and returns the main `HttpResponse`.
Override `run(HttpStatus expectedStatus)` when the main request can expect a status other than `200 OK`;
plain `run()` is only suitable for legacy/simple `200 OK` use cases.

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

The verification-step is implemented by the overridden `verify(HttpResponse)`-method.

It may either assert the returned main response directly or perform additional HTTP requests needed
to verify the outcome.
