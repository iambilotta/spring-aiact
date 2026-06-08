# Requirements — spring-aiact-maven-plugin

Auto-generated from test sources by tracegate. Do NOT edit by hand: edit the test javadoc / docstring instead and rerun. Single source of truth is the test code.

**Convention**: category from the test name (`*Test`=FR, `*NfrTest`=NFR, `*InvariantTest`=INV, `*ContractTest`=CON; Python file markers `*invariant*`/`*nfr*`/`*contract*` map the same way; Playwright E2E tests join as **E2E**). Spec from doc-comment tags `@spec.given` / `@spec.when` / `@spec.then` (plus optional `@spec.adr` / `@spec.us`). Tests without a complete spec are listed with `(spec missing)` so they're visible and lintable.

## Coverage

- Total tests scanned: **7**
- With complete spec javadoc: **0** (0%)
- FR: 7

## Module `(root)`

### Functional Requirements

#### `FR-(root).HighRiskAnnotationValidator#compliantClassPassesValidation`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-maven-plugin/src/test/java/com/iambilotta/spring/aiact/mavenplugin/HighRiskAnnotationValidatorTest.java`

#### `FR-(root).HighRiskAnnotationValidator#datasetOptionalDisablesTheCheck`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-maven-plugin/src/test/java/com/iambilotta/spring/aiact/mavenplugin/HighRiskAnnotationValidatorTest.java`

#### `FR-(root).HighRiskAnnotationValidator#missingDatasetIsReportedEvenWhenAnotherSystemHasOneInADifferentPackage`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-maven-plugin/src/test/java/com/iambilotta/spring/aiact/mavenplugin/HighRiskAnnotationValidatorTest.java`

#### `FR-(root).HighRiskAnnotationValidator#missingPurposeIsReported`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-maven-plugin/src/test/java/com/iambilotta/spring/aiact/mavenplugin/HighRiskAnnotationValidatorTest.java`

#### `FR-(root).HighRiskAnnotationValidator#nonHighRiskClassesAreIgnored`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-maven-plugin/src/test/java/com/iambilotta/spring/aiact/mavenplugin/HighRiskAnnotationValidatorTest.java`

#### `FR-(root).RiskClassificationValidator#allowsNonProhibitedBands`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-maven-plugin/src/test/java/com/iambilotta/spring/aiact/mavenplugin/RiskClassificationValidatorTest.java`

#### `FR-(root).RiskClassificationValidator#refusesAProhibitedArticle5Practice`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-maven-plugin/src/test/java/com/iambilotta/spring/aiact/mavenplugin/RiskClassificationValidatorTest.java`
