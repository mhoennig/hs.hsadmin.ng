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

The use-case
