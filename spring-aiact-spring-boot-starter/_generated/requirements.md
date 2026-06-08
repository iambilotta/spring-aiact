# Requirements — spring-aiact-spring-boot-starter

Auto-generated from test sources by tracegate. Do NOT edit by hand: edit the test javadoc / docstring instead and rerun. Single source of truth is the test code.

**Convention**: category from the test name (`*Test`=FR, `*NfrTest`=NFR, `*InvariantTest`=INV, `*ContractTest`=CON; Python file markers `*invariant*`/`*nfr*`/`*contract*` map the same way; Playwright E2E tests join as **E2E**). Spec from doc-comment tags `@spec.given` / `@spec.when` / `@spec.then` (plus optional `@spec.adr` / `@spec.us`). Tests without a complete spec are listed with `(spec missing)` so they're visible and lintable.

## Coverage

- Total tests scanned: **10**
- With complete spec javadoc: **0** (0%)
- FR: 10

## Module `(root)`

### Functional Requirements

#### `FR-(root).HmacFailFast#failsToStartWhenSecretIsDefaultAndNoDevProfile`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/HmacFailFastTest.java`

#### `FR-(root).HmacFailFast#startsWhenDevProfileActive`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/HmacFailFastTest.java`

#### `FR-(root).HmacFailFast#startsWhenGuardExplicitlyDisabled`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/HmacFailFastTest.java`

#### `FR-(root).HmacFailFast#startsWhenSecretOverridden`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/HmacFailFastTest.java`

#### `FR-(root).ObjectMapperIsolation#aiActDoesNotPublishAnInternalObjectMapperBean`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/ObjectMapperIsolationTest.java`

#### `FR-(root).PayloadHasherDeterminism#hashesDifferOnDifferentPayloads`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/PayloadHasherDeterminismTest.java`

#### `FR-(root).PayloadHasherDeterminism#noneStrategyReturnsAStableMarker`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/PayloadHasherDeterminismTest.java`

#### `FR-(root).PayloadHasherDeterminism#sameJsonPayloadAlwaysHashesToTheSameValue`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/PayloadHasherDeterminismTest.java`


## Module `web`

### Functional Requirements

#### `FR-web.AiActTransparencyFilter#leavesNonAiSurfacesUntouched`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/web/AiActTransparencyFilterTest.java`

#### `FR-web.AiActTransparencyFilter#stampsTheDisclosureHeaderOnAResponseFromAnAiSurface`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-spring-boot-starter/src/test/java/com/iambilotta/spring/aiact/autoconfigure/web/AiActTransparencyFilterTest.java`
