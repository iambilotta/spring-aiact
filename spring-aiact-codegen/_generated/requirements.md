# Requirements — spring-aiact-codegen

Auto-generated from test sources by tracegate. Do NOT edit by hand: edit the test javadoc / docstring instead and rerun. Single source of truth is the test code.

**Convention**: category from the test name (`*Test`=FR, `*NfrTest`=NFR, `*InvariantTest`=INV, `*ContractTest`=CON; Python file markers `*invariant*`/`*nfr*`/`*contract*` map the same way; Playwright E2E tests join as **E2E**). Spec from doc-comment tags `@spec.given` / `@spec.when` / `@spec.then` (plus optional `@spec.adr` / `@spec.us`). Tests without a complete spec are listed with `(spec missing)` so they're visible and lintable.

## Coverage

- Total tests scanned: **8**
- With complete spec javadoc: **0** (0%)
- FR: 8

## Module `(root)`

### Functional Requirements

#### `FR-(root).AuditExportPackager#manifestHmacVerifiesAgainstTheManifestText`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-codegen/src/test/java/com/iambilotta/spring/aiact/codegen/AuditExportPackagerTest.java`

#### `FR-(root).AuditExportPackager#packageContainsAllEntriesPlusSignedManifest`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-codegen/src/test/java/com/iambilotta/spring/aiact/codegen/AuditExportPackagerTest.java`


## Module `datasheet`

### Functional Requirements

#### `FR-datasheet.DatasetDatasheetRenderer#rendersBiasesWhenDeclared`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-codegen/src/test/java/com/iambilotta/spring/aiact/codegen/datasheet/DatasetDatasheetRendererTest.java`

#### `FR-datasheet.DatasetDatasheetRenderer#rendersGapWhenBiasesAreEmpty`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-codegen/src/test/java/com/iambilotta/spring/aiact/codegen/datasheet/DatasetDatasheetRendererTest.java`


## Module `fria`

### Functional Requirements

#### `FR-fria.FriaScaffoldDesign#generatesAFriaScaffoldFromTheAnnotationModel`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-codegen/src/test/java/com/iambilotta/spring/aiact/codegen/fria/FriaScaffoldDesignTest.java`


## Module `markdown`

### Functional Requirements

#### `FR-markdown.TechnicalFileMarkdownRenderer#rendersAllNineSectionsWithGapPlaceholdersWhenEmpty`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-codegen/src/test/java/com/iambilotta/spring/aiact/codegen/markdown/TechnicalFileMarkdownRendererTest.java`

#### `FR-markdown.TechnicalFileMarkdownRenderer#rendersTablesWhenSectionsArePopulated`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-codegen/src/test/java/com/iambilotta/spring/aiact/codegen/markdown/TechnicalFileMarkdownRendererTest.java`


## Module `pdf`

### Functional Requirements

#### `FR-pdf.DeclarationOfConformityPdfGenerator#rendersValidPdfHeader`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-codegen/src/test/java/com/iambilotta/spring/aiact/codegen/pdf/DeclarationOfConformityPdfGeneratorTest.java`
