# Requirements — spring-aiact-core, grouped by User Story

Auto-generated companion to `requirements.md`. Tests link to a User Story via the javadoc tag `@spec.us=US-NNN-slug` (the slug points to a User Story defined in `PRODUCT.md`). Implementation-detail tests with no `@spec.us` are collected at the bottom; declared User Stories in PRODUCT.md with zero linked tests are listed as **not implemented yet**.

## Coverage

- Total tests scanned: **57**
- Tests linked to a User Story: **0**
- Tests without `@spec.us` (implementation detail): **57**
- User Stories declared in PRODUCT.md: **0**
- User Stories with at least one linked test: **0**
- User Stories declared but **not yet implemented**: **0**

## Implementation detail (no `@spec.us` link)

These tests are valid requirements but exist below the user-story horizon (unit-level mechanism, internal invariant, white-box assertion). Add `@spec.us` if a user story should claim them.

### Module `accuracy`

- `FR-accuracy.AccuracyEnforcer#enforceThrowsBelowThresholdSoABuildOrEvalHookCanGateOnIt`
- `FR-accuracy.AccuracyEnforcer#failsAndSignalsWhenAGreaterEqualThresholdIsMissed`
- `FR-accuracy.AccuracyEnforcer#honoursAnUpperBoundThresholdForErrorRates`
- `FR-accuracy.AccuracyEnforcer#passesWhenAGreaterEqualThresholdIsMet`
- `FR-accuracy.AccuracyEnforcer#readsTheThresholdStraightFromTheAnnotation`
- `FR-accuracy.AccuracyEnforcer#rejectsAnUnparseableThreshold`
- `FR-accuracy.AccuracyEnforcer#treatsABareNumberAsAGreaterEqualThreshold`
- `FR-accuracy.AiActAccuracyExtension#passesWhenEveryDeclaredMetricMeetsItsThreshold`
- `FR-accuracy.AiActAccuracyExtension#refusesToSilentlyPassWhenADeclaredMetricHasNoMeasuredValue`
- `FR-accuracy.AiActAccuracyExtension#rejectsASubjectThatDeclaresNoAccuracyMetric`
- `FR-accuracy.AiActAccuracyExtension#throwsNamingTheMetricWhenADeclaredMetricIsBelowThreshold`

### Module `audit`

- `FR-audit.AiActLoggingAspect#disablingCaptureSkipsHashes`
- `FR-audit.AiActLoggingAspect#invocationProducesOneAuditRecord`
- `FR-audit.AiActLoggingAspect#thrownExceptionProducesAnomalyRecordWithSanitizedMetadata`
- `FR-audit.HmacChain#chainPropagatesPrevHmac`
- `FR-audit.HmacChain#differentKeysProduceDifferentOutput`
- `FR-audit.HmacChain#emptyKeyRejected`
- `FR-audit.HmacChain#firstRecordSeedsFromAllZeroes`
- `FR-audit.HmacChain#identicalInputsAndKeyProduceIdenticalOutput`
- `FR-audit.HmacChain#verifyDetectsTampering`
- `FR-audit.JdbcAuditLogService#appendsAndChainsThreeEventsAcrossADbRoundTrip`
- `FR-audit.JdbcAuditLogService#headReturnsTheLatestRecordHmac`
- `FR-audit.JdbcAuditLogService#recoversChainHeadAfterServiceRecreation`
- `FR-audit.JdbcAuditLogService#verifyDetectsTamperingInTheDatabase`
- `FR-audit.MetadataSanitizer#describeExceptionEmitsClassNameAndFingerprintNotMessage`
- `FR-audit.MetadataSanitizer#describeExceptionWithoutMessageEmitsNoMessageMarker`
- `FR-audit.MetadataSanitizer#dropsKeysOutsideTheWhitelist`
- `FR-audit.MetadataSanitizer#emptyOrNullInputReturnsEmptyMap`
- `FR-audit.MetadataSanitizer#truncatesValuesAtTheConfiguredMaxLength`
- `FR-audit.NdjsonAuditLogService#appendsAndChainsThreeEvents`
- `FR-audit.NdjsonAuditLogService#recoversChainHeadAfterServiceRecreation`
- `FR-audit.NdjsonAuditLogService#verifyDetectsTamperingOnDisk`
- `FR-audit.NdjsonAuditLogService#verifyReturnsCleanReportOnGoodLog`
- `FR-audit.NdjsonMultiWriter#chainCorruptsWithoutFileLockAcrossTwoConcurrentWriters`
- `FR-audit.NdjsonMultiWriter#chainStaysValidWithFileLockEnabledAcrossTwoConcurrentWriters`

### Module `oversight`

- `FR-oversight.OversightService#recordsAcceptAsOverrideKind`
- `FR-oversight.OversightService#recordsFlagAnomalyAsAnomalyKind`
- `FR-oversight.OversightService#recordsStopAsStopKindWithoutLinkedEventId`
- `FR-oversight.OversightService#rejectsMissingActor`
- `FR-oversight.OversightService#rejectsMissingLinkedEventIdForNonStopDecision`
- `FR-oversight.OversightService#rejectsMissingSystemId`
- `FR-oversight.OversightService#rejectsUnknownDecision`
- `FR-oversight.OversightService#truncatesLongReasonAtSanitizerLimit`

### Module `retention`

- `FR-retention.RetentionPolicyService#chainStaysVerifiableForKeptSliceAfterPrune`
- `FR-retention.RetentionPolicyService#emptyFileNoOps`
- `FR-retention.RetentionPolicyService#leavesFileIntactWhenNothingIsOld`
- `FR-retention.RetentionPolicyService#prunesRecordsOlderThanCutoff`

### Module `risk`

- `FR-risk.RiskClassification#classifiesAnExplicitlyDeclaredBand`
- `FR-risk.RiskClassification#defaultsToMinimalWhenNothingIsDeclared`
- `FR-risk.RiskClassification#inferssHighRiskFromTheHighRiskSystemAnnotation`
- `FR-risk.RiskClassification#prohibitedIsTheOnlyBandRefusedByConstruction`

### Module `security`

- `FR-security.AiActMockEndpointGuard#allowAllByDefaultLetsThroughUnknownSystems`
- `FR-security.AiActMockEndpointGuard#allowsExplicitlyConfiguredCombination`
- `FR-security.AiActMockEndpointGuard#denyTrumpsAllow`

### Module `transparency`

- `FR-transparency.TransparencyDisclosure#resolvesAnInteractionDisclosureFromTheAnnotation`
- `FR-transparency.TransparencyDisclosure#returnsNullWhenTheTypeDoesNotDeclareTransparency`
- `FR-transparency.TransparencyDisclosure#usesTheDeclaredDisclosureMessageWhenProvided`
