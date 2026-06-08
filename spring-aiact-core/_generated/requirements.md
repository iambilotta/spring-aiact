# Requirements — spring-aiact-core

Auto-generated from test sources by tracegate. Do NOT edit by hand: edit the test javadoc / docstring instead and rerun. Single source of truth is the test code.

**Convention**: category from the test name (`*Test`=FR, `*NfrTest`=NFR, `*InvariantTest`=INV, `*ContractTest`=CON; Python file markers `*invariant*`/`*nfr*`/`*contract*` map the same way; Playwright E2E tests join as **E2E**). Spec from doc-comment tags `@spec.given` / `@spec.when` / `@spec.then` (plus optional `@spec.adr` / `@spec.us`). Tests without a complete spec are listed with `(spec missing)` so they're visible and lintable.

## Coverage

- Total tests scanned: **53**
- With complete spec javadoc: **0** (0%)
- FR: 53

## Module `accuracy`

### Functional Requirements

#### `FR-accuracy.AccuracyEnforcer#enforceThrowsBelowThresholdSoABuildOrEvalHookCanGateOnIt`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/accuracy/AccuracyEnforcerTest.java`

#### `FR-accuracy.AccuracyEnforcer#failsAndSignalsWhenAGreaterEqualThresholdIsMissed`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/accuracy/AccuracyEnforcerTest.java`

#### `FR-accuracy.AccuracyEnforcer#honoursAnUpperBoundThresholdForErrorRates`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/accuracy/AccuracyEnforcerTest.java`

#### `FR-accuracy.AccuracyEnforcer#passesWhenAGreaterEqualThresholdIsMet`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/accuracy/AccuracyEnforcerTest.java`

#### `FR-accuracy.AccuracyEnforcer#readsTheThresholdStraightFromTheAnnotation`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/accuracy/AccuracyEnforcerTest.java`

#### `FR-accuracy.AccuracyEnforcer#rejectsAnUnparseableThreshold`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/accuracy/AccuracyEnforcerTest.java`

#### `FR-accuracy.AccuracyEnforcer#treatsABareNumberAsAGreaterEqualThreshold`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/accuracy/AccuracyEnforcerTest.java`


## Module `audit`

### Functional Requirements

#### `FR-audit.AiActLoggingAspect#disablingCaptureSkipsHashes`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/AiActLoggingAspectTest.java`

#### `FR-audit.AiActLoggingAspect#invocationProducesOneAuditRecord`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/AiActLoggingAspectTest.java`

#### `FR-audit.AiActLoggingAspect#thrownExceptionProducesAnomalyRecordWithSanitizedMetadata`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/AiActLoggingAspectTest.java`

#### `FR-audit.HmacChain#chainPropagatesPrevHmac`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/HmacChainTest.java`

#### `FR-audit.HmacChain#differentKeysProduceDifferentOutput`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/HmacChainTest.java`

#### `FR-audit.HmacChain#emptyKeyRejected`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/HmacChainTest.java`

#### `FR-audit.HmacChain#firstRecordSeedsFromAllZeroes`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/HmacChainTest.java`

#### `FR-audit.HmacChain#identicalInputsAndKeyProduceIdenticalOutput`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/HmacChainTest.java`

#### `FR-audit.HmacChain#verifyDetectsTampering`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/HmacChainTest.java`

#### `FR-audit.JdbcAuditLogService#appendsAndChainsThreeEventsAcrossADbRoundTrip`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/JdbcAuditLogServiceTest.java`

#### `FR-audit.JdbcAuditLogService#headReturnsTheLatestRecordHmac`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/JdbcAuditLogServiceTest.java`

#### `FR-audit.JdbcAuditLogService#recoversChainHeadAfterServiceRecreation`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/JdbcAuditLogServiceTest.java`

#### `FR-audit.JdbcAuditLogService#verifyDetectsTamperingInTheDatabase`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/JdbcAuditLogServiceTest.java`

#### `FR-audit.MetadataSanitizer#describeExceptionEmitsClassNameAndFingerprintNotMessage`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/MetadataSanitizerTest.java`

#### `FR-audit.MetadataSanitizer#describeExceptionWithoutMessageEmitsNoMessageMarker`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/MetadataSanitizerTest.java`

#### `FR-audit.MetadataSanitizer#dropsKeysOutsideTheWhitelist`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/MetadataSanitizerTest.java`

#### `FR-audit.MetadataSanitizer#emptyOrNullInputReturnsEmptyMap`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/MetadataSanitizerTest.java`

#### `FR-audit.MetadataSanitizer#truncatesValuesAtTheConfiguredMaxLength`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/MetadataSanitizerTest.java`

#### `FR-audit.NdjsonAuditLogService#appendsAndChainsThreeEvents`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/NdjsonAuditLogServiceTest.java`

#### `FR-audit.NdjsonAuditLogService#recoversChainHeadAfterServiceRecreation`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/NdjsonAuditLogServiceTest.java`

#### `FR-audit.NdjsonAuditLogService#verifyDetectsTamperingOnDisk`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/NdjsonAuditLogServiceTest.java`

#### `FR-audit.NdjsonAuditLogService#verifyReturnsCleanReportOnGoodLog`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/NdjsonAuditLogServiceTest.java`

#### `FR-audit.NdjsonMultiWriter#chainCorruptsWithoutFileLockAcrossTwoConcurrentWriters`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/NdjsonMultiWriterTest.java`

#### `FR-audit.NdjsonMultiWriter#chainStaysValidWithFileLockEnabledAcrossTwoConcurrentWriters`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/audit/NdjsonMultiWriterTest.java`


## Module `oversight`

### Functional Requirements

#### `FR-oversight.OversightService#recordsAcceptAsOverrideKind`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/oversight/OversightServiceTest.java`

#### `FR-oversight.OversightService#recordsFlagAnomalyAsAnomalyKind`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/oversight/OversightServiceTest.java`

#### `FR-oversight.OversightService#recordsStopAsStopKindWithoutLinkedEventId`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/oversight/OversightServiceTest.java`

#### `FR-oversight.OversightService#rejectsMissingActor`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/oversight/OversightServiceTest.java`

#### `FR-oversight.OversightService#rejectsMissingLinkedEventIdForNonStopDecision`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/oversight/OversightServiceTest.java`

#### `FR-oversight.OversightService#rejectsMissingSystemId`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/oversight/OversightServiceTest.java`

#### `FR-oversight.OversightService#rejectsUnknownDecision`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/oversight/OversightServiceTest.java`

#### `FR-oversight.OversightService#truncatesLongReasonAtSanitizerLimit`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/oversight/OversightServiceTest.java`


## Module `retention`

### Functional Requirements

#### `FR-retention.RetentionPolicyService#chainStaysVerifiableForKeptSliceAfterPrune`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/retention/RetentionPolicyServiceTest.java`

#### `FR-retention.RetentionPolicyService#emptyFileNoOps`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/retention/RetentionPolicyServiceTest.java`

#### `FR-retention.RetentionPolicyService#leavesFileIntactWhenNothingIsOld`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/retention/RetentionPolicyServiceTest.java`

#### `FR-retention.RetentionPolicyService#prunesRecordsOlderThanCutoff`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/retention/RetentionPolicyServiceTest.java`


## Module `risk`

### Functional Requirements

#### `FR-risk.RiskClassification#classifiesAnExplicitlyDeclaredBand`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/risk/RiskClassificationTest.java`

#### `FR-risk.RiskClassification#defaultsToMinimalWhenNothingIsDeclared`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/risk/RiskClassificationTest.java`

#### `FR-risk.RiskClassification#inferssHighRiskFromTheHighRiskSystemAnnotation`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/risk/RiskClassificationTest.java`

#### `FR-risk.RiskClassification#prohibitedIsTheOnlyBandRefusedByConstruction`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/risk/RiskClassificationTest.java`


## Module `security`

### Functional Requirements

#### `FR-security.AiActMockEndpointGuard#allowAllByDefaultLetsThroughUnknownSystems`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/security/AiActMockEndpointGuardTest.java`

#### `FR-security.AiActMockEndpointGuard#allowsExplicitlyConfiguredCombination`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/security/AiActMockEndpointGuardTest.java`

#### `FR-security.AiActMockEndpointGuard#denyTrumpsAllow`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/security/AiActMockEndpointGuardTest.java`


## Module `transparency`

### Functional Requirements

#### `FR-transparency.TransparencyDisclosure#resolvesAnInteractionDisclosureFromTheAnnotation`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/transparency/TransparencyDisclosureTest.java`

#### `FR-transparency.TransparencyDisclosure#returnsNullWhenTheTypeDoesNotDeclareTransparency`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/transparency/TransparencyDisclosureTest.java`

#### `FR-transparency.TransparencyDisclosure#usesTheDeclaredDisclosureMessageWhenProvided`

- _(spec missing — add `@spec.given` / `@spec.when` / `@spec.then` javadoc)_
- **File**: `spring-aiact-core/src/test/java/com/iambilotta/spring/aiact/transparency/TransparencyDisclosureTest.java`
