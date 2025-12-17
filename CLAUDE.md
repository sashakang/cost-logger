# Project Instructions

## Consensus Pipeline - Plan Analysis Prompts

When analyzing implementation plans before coding, use these prompts with sub-agents:

### Architecture Simplifier - Plan Analysis

```
Analyze this Android app implementation plan for architectural concerns:

## Plan to Review:
{PLAN_CONTENT}

## Evaluation Criteria:

1. **Over-Engineering Detection**
   - Are there unnecessary abstraction layers proposed?
   - Could any proposed interfaces be replaced with concrete implementations?
   - Are there planned classes that will only have one implementation?

2. **Pattern Appropriateness**
   - Is the proposed architecture (MVVM/MVI/MVP) appropriate for the feature scope?
   - Are UseCase classes justified, or could logic live in ViewModel?
   - Is the Repository pattern needed, or is direct data access simpler?

3. **Complexity vs Value**
   - Does each proposed layer add testability or maintainability value?
   - Could the same goal be achieved with fewer components?
   - Are there proposed abstractions solving hypothetical future problems?

4. **Android-Specific Considerations**
   - Are lifecycle-aware components used appropriately (not over-used)?
   - Is dependency injection setup proportional to app complexity?
   - Are Compose vs View decisions justified?

## Output Format:
- APPROVE: Plan is appropriately scoped
- SIMPLIFY: List specific reductions with rationale
- REJECT: Fundamental over-engineering concerns

Provide specific, actionable feedback.
```

### Android Kotlin Developer - Plan Analysis

```
Analyze this Android app implementation plan for technical feasibility and best practices:

## Plan to Review:
{PLAN_CONTENT}

## Evaluation Criteria:

1. **Technical Feasibility**
   - Are the proposed APIs/libraries compatible with target SDK versions?
   - Are there any deprecated approaches being planned?
   - Will the architecture work with Jetpack Compose / View system as specified?

2. **Kotlin Best Practices**
   - Can proposed classes leverage data classes, sealed classes, or value classes?
   - Are coroutines/Flow used appropriately for async operations?
   - Could extension functions simplify the proposed design?

3. **Android Platform Alignment**
   - Does the plan handle configuration changes correctly?
   - Is lifecycle management properly considered?
   - Are permissions, background work, and storage handled per current guidelines?

4. **Implementation Complexity**
   - Estimate LOC for each component
   - Identify potential integration challenges
   - Flag any areas requiring significant boilerplate

5. **Testing Strategy**
   - Is the proposed structure testable?
   - What test types are needed (unit, instrumented, UI)?
   - Are there any untestable patterns proposed?

## Output Format:
- APPROVE: Plan is technically sound
- REVISE: List specific technical concerns with alternatives
- REJECT: Critical technical issues that block implementation

Provide Kotlin/Android-specific recommendations.
```

### Plan Consensus Workflow

1. Draft implementation plan
2. Launch both agents **in parallel**:
   - `architecture-simplifier` with Architecture prompt
   - `android-kotlin-dev` with Technical prompt
3. **Consensus required**: Both must APPROVE or agree on specific revisions
4. **If NO consensus** → revise plan based on feedback → re-run agents
5. **Repeat until unanimous** agreement is reached
6. Present consensus-backed plan to user for approval
7. Proceed to implementation only after user approves
