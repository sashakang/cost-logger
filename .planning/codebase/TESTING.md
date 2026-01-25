# Testing Patterns

**Analysis Date:** 2026-01-25

## Test Framework

**Runner:**
- JUnit 4 (local JVM unit tests) via `testImplementation("junit:junit:4.13.2")` in `app/build.gradle.kts`.
- AndroidX Test Runner for instrumented tests via `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` in `app/build.gradle.kts`.

**Assertion Library:**
- JUnit 4 assertions (dependency declared in `app/build.gradle.kts`).

**Run Commands:**
```bash
./gradlew testDebugUnitTest           # Run all JVM unit tests (documented in `AGENTS.md`)
./gradlew connectedDebugAndroidTest   # Run instrumented tests on device (documented in `AGENTS.md`)
./gradlew testDebugUnitTest --tests "com.notificationlogger.MyTest"  # Single JVM test class (documented in `AGENTS.md`)
```

## Test File Organization

**Location:**
- No test files detected under `app/src/test/` or `app/src/androidTest/`.

**Naming:**
- Not detected; no `*Test.kt` files present in `app/src/test/` or `app/src/androidTest/`.

**Structure:**
```
Not detected (no test directories populated under `app/src/test/` or `app/src/androidTest/`).
```

## Test Structure

**Suite Organization:**
```kotlin
Not detected (no test suites in `app/src/test/` or `app/src/androidTest/`).
```

**Patterns:**
- Setup/teardown patterns not detected; no tests found in `app/src/test/` or `app/src/androidTest/`.

## Mocking

**Framework:**
- Not detected; no mocking libraries declared in `app/build.gradle.kts`.

**Patterns:**
```kotlin
Not detected (no mock usage found under `app/src/test/` or `app/src/androidTest/`).
```

**What to Mock:**
- Not detected; no test guidance in code (checked `AGENTS.md`).

**What NOT to Mock:**
- Not detected; no test guidance in code (checked `AGENTS.md`).

## Fixtures and Factories

**Test Data:**
```kotlin
Not detected (no fixtures under `app/src/test/` or `app/src/androidTest/`).
```

**Location:**
- Not detected; no fixtures directories present in `app/src/test/` or `app/src/androidTest/`.

## Coverage

**Requirements:**
- None enforced; no coverage configuration found in `app/build.gradle.kts`.

**View Coverage:**
```bash
Not configured (no coverage tasks defined in `app/build.gradle.kts`).
```

## Test Types

**Unit Tests:**
- Not detected; no test classes under `app/src/test/`.

**Integration Tests:**
- Not detected; no integration test setup under `app/src/test/` or `app/src/androidTest/`.

**E2E Tests:**
- Not used; no E2E framework configured in `app/build.gradle.kts`.

## Common Patterns

**Async Testing:**
```kotlin
Not detected (no test code under `app/src/test/` or `app/src/androidTest/`).
```

**Error Testing:**
```kotlin
Not detected (no test code under `app/src/test/` or `app/src/androidTest/`).
```

---

*Testing analysis: 2026-01-25*
