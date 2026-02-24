# Spring Boot 3.4.2 → 4.0.3 Upgrade — Research

**Researched:** 2026-02-24
**Domain:** Spring Boot 4.0 migration — Java / Maven / Spring ecosystem
**Confidence:** HIGH (official sources, migration guide, Spring blog posts, GitHub issues)

---

## Summary

Spring Boot 4.0.3 (released 2026-02-19) is a major version with breaking changes at every layer:
Jackson 3, renamed starters, restructured modules, and Spring Framework 7 / Jakarta EE 11 as
the foundation. The minimum Java requirement is **Java 17**; Java 25 is fully supported.

The two largest risks for this project are:

1. **Jackson 3** — group IDs, package names, and exception hierarchy all change. The project
   does not appear to use direct Jackson imports (no custom serializers found in the codebase),
   so the risk is low-medium in practice. Spring Boot 4 provides a transitional
   `spring.jackson.use-jackson2-defaults` property and a `spring-boot-jackson2` compatibility
   shim for teams that need a phased migration.

2. **Resilience4j** — `resilience4j-spring-boot4` is NOT yet on Maven Central. The PR is merged
   but only `2.3.1-SNAPSHOT` exists. The safe alternative is switching to the Spring Cloud
   Circuit Breaker abstraction (`spring-cloud-starter-circuitbreaker-resilience4j`), which is
   bundled inside Spring Cloud 2025.1.1 (Oakwood) and IS on Maven Central.

All other dependencies have GA-stable paths: Spring Cloud 2025.1.1, Spring AI 2.0.0-M2 (milestone,
no GA yet), maven-compiler-plugin 3.15.0.

**Primary recommendation:** Upgrade in this order — (1) pom.xml version bumps + renamed starters,
(2) Jackson 3 validation (likely zero code changes), (3) Resilience4j workaround, (4) Spring Cloud
Vault (artifact already correct, only BOM version changes).

---

## Answers to All Six Questions

### Q1 — Spring AI version compatible with Spring Boot 4.0.3

| Version | Status | Spring Boot Compatibility |
|---------|--------|--------------------------|
| 1.1.x (latest: 1.1.2) | GA / stable | Spring Boot 3.4.x / 3.5.x ONLY |
| 2.0.0-M1 | Milestone | Spring Boot 4.0 GA + Spring Framework 7.0 |
| 2.0.0-M2 | Milestone (latest) | Spring Boot 4.0 + Spring Framework 7.0 |

**Answer:** Use `spring-ai-bom` version **`2.0.0-M2`**. This is a milestone, not a GA release.
No Spring AI 2.x GA has been published as of 2026-02-24. The artifact group ID and artifact ID
are unchanged: `org.springframework.ai:spring-ai-bom`.

The milestone repository must remain in `pom.xml`:
```xml
<repository>
  <id>spring-milestones</id>
  <url>https://repo.spring.io/milestone</url>
</repository>
```

Source: [Spring AI 2.0.0-M1 blog post](https://spring.io/blog/2025/12/11/spring-ai-2-0-0-M1-available-now/) |
[Spring AI releases](https://github.com/spring-projects/spring-ai/releases) |
[Spring AI Boot 4 compatibility epic](https://github.com/spring-projects/spring-ai/issues/3379)

**Confidence: HIGH** (official Spring blog + GitHub milestone)

---

### Q2 — Spring Cloud version compatible with Spring Boot 4.0.3

| Spring Cloud Train | Codename | Spring Boot Compat |
|-------------------|----------|--------------------|
| 2024.0.x | Leyton | Boot 3.3.x |
| 2025.0.0 | Northfields | Boot 3.5.x ONLY — incompatible with Boot 4.0.x |
| **2025.1.x** | **Oakwood** | **Boot 4.0.x** |

**Answer:** Use `spring-cloud-dependencies` version **`2025.1.1`**.
Spring Cloud 2025.1.0 was released 2025-11-25; 2025.1.1 followed on 2026-01-29.
The project currently uses `2024.0.0` (two trains behind).

Spring Cloud Vault is bundled as `spring-cloud-vault 5.0.1` in this train.
The starter artifact ID (`spring-cloud-starter-vault-config`) does not change.

Source: [Spring Cloud Supported Versions](https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions) |
[Spring Cloud 2025.1.1 blog](https://spring.io/blog/2026/01/29/spring-cloud-2025-1-1-aka-oakwood-has-been-released/)

**Confidence: HIGH**

---

### Q3 — Breaking changes affecting this project

#### 3a. Jackson 3 (HIGHEST IMPACT — must address)

Spring Boot 4 uses Jackson 3 by default.

| What changed | Old (Jackson 2) | New (Jackson 3) |
|---|---|---|
| Maven group ID | `com.fasterxml.jackson.core` | `tools.jackson.core` |
| Java package | `com.fasterxml.jackson.databind` | `tools.jackson.databind` |
| Exception hierarchy | `JsonProcessingException extends IOException` | `JacksonException extends RuntimeException` |
| Class name | `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer` |
| Class name | `JsonObjectSerializer` | `ObjectValueSerializer` |
| Annotation | `@JsonComponent` | `@JacksonComponent` |
| Spring property | `spring.jackson.read.*` | `spring.jackson.json.read.*` |
| Spring property | `spring.jackson.write.*` | `spring.jackson.json.write.*` |

**Migration path for this project:** Spring Boot 4 provides a compatibility shim.
If no custom serializers / `@JsonComponent` / catch-IOException-on-JSON-parse exist,
the property `spring.jackson.use-jackson2-defaults=true` in `application.yml` can
defer the full migration. Recommended: don't use the shim; audit code for direct Jackson
imports (there appear to be none in the service classes) and migrate cleanly.

If `catch (IOException e)` blocks are catching JSON parsing errors, those will silently
stop catching after the upgrade — they need to become `catch (JacksonException e)` or
`catch (RuntimeException e)`.

Source: [Spring introducing Jackson 3 blog](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring/) |
[Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

#### 3b. Renamed starters (REQUIRED — compilation fails without)

| Old artifact (Boot 3.x) | New artifact (Boot 4.x) | Used by this project |
|---|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` | YES |
| `spring-boot-starter-aop` | `spring-boot-starter-aspectj` | YES (for Resilience4j) |
| `spring-boot-starter-oauth2-client` | `spring-boot-starter-security-oauth2-client` | YES |
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` | NO |

**Note:** The old artifact names still exist in Boot 4.0 as deprecated aliases, so compilation
will not immediately fail — but they will be removed in a future release. Migrate now.

Source: [Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) |
[Modularizing Spring Boot blog](https://spring.io/blog/2025/10/28/modularizing-spring-boot/)

#### 3c. Spring Security / OAuth2

Spring Authorization Server moved into Spring Security 7.0. This project uses OAuth2 **client**
(Google login) not the authorization server, so the main impact is:
- Rename `spring-boot-starter-oauth2-client` → `spring-boot-starter-security-oauth2-client`
- No Spring Security API changes are known to affect standard `SecurityConfig` patterns using
  `HttpSecurity` and `oauth2Login()` — these remain stable in Spring Security 7

Source: [Spring Authorization Server moving to Spring Security 7](https://spring.io/blog/2025/09/11/spring-authorization-server-moving-to-spring-security-7-0/)

#### 3d. JPA / Hibernate

Hibernate 7.1 is included. Key change:
- `hibernate-jpamodelgen` annotation processor renamed to `hibernate-processor`
  (group: `org.hibernate.orm`, artifact: `hibernate-processor`)

**This project does NOT use the JPA static metamodel** (no `Entity_` generated classes visible
in the codebase), so this change has no practical impact. However, if MapStruct or any other
tool references `hibernate-jpamodelgen` transitively, the build may warn.

`@EntityScan` import path changed:
- Old: `org.springframework.boot.autoconfigure.domain.EntityScan`
- New: `org.springframework.boot.persistence.autoconfigure.EntityScan`

Check all `@EntityScan` usages in the project.

Source: [Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

#### 3e. Spring Data Redis / Spring Session

The project uses `spring-boot-starter-data-redis` for Valkey caching — this starter is
**not renamed**. However, if Spring Session with Redis is ever added:
- Properties formerly under `spring.session.redis.*` move to `spring.session.data.redis.*`

The project currently does not appear to use Spring Session, so this is low risk.

Redis auto-configuration also now auto-configures `MicrometerTracing` instead of
`MicrometerCommandLatencyRecorder` — relevant only if you customized the Micrometer Redis
integration. Standard Actuator/Prometheus setup is unaffected.

Source: [Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

#### 3f. Actuator

- Liveness and readiness probes are now **enabled by default** (previously disabled unless
  on Kubernetes). If the project relies on these probes NOT existing, add:
  `management.endpoint.health.probes.enabled=false`
- SSL info changes (not relevant to this project — no SSL cert management via Actuator)
- Actuator endpoint `@Nullable` annotation: must use `org.jspecify.annotations.Nullable`
  instead of `org.springframework.lang.Nullable` in custom endpoint parameters (not used here)

Source: [Spring Boot 4 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)

#### 3g. Virtual threads

The project already uses `spring.threads.virtual.enabled=true`. This property is unchanged
and continues to work in Boot 4. Additional auto-configured HTTP clients (JDK `HttpClient`)
now also use virtual threads when this property is true — no action needed.

**Confidence: HIGH** for all items in Q3 (official migration guide + release notes)

---

### Q4 — Resilience4j artifact for Spring Boot 4 (BLOCKING ISSUE)

**Current artifact:** `io.github.resilience4j:resilience4j-spring-boot3:2.2.0`

**Situation as of 2026-02-24:**
- `resilience4j-spring-boot4` artifact name is confirmed (PR #2384 merged)
- Only `2.3.1-SNAPSHOT` exists in Sonatype snapshots — **nothing on Maven Central**
- Resilience4j 2.3.0 was released 2026-01-03 but does NOT include the `spring-boot4` module

**Two migration options:**

**Option A (Recommended): Use Spring Cloud Circuit Breaker abstraction**
Switch to `org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j`.
This is part of Spring Cloud 2025.1.1 (Oakwood), already on Maven Central, and uses
Resilience4j internally. You get the same circuit breaker / retry behavior through the
Spring Cloud abstraction layer. This is the path of least resistance.

```xml
<!-- Remove this: -->
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
  <version>2.2.0</version>
</dependency>

<!-- Add this (version managed by Spring Cloud BOM): -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

**Option B (Not recommended for production): Snapshot**
Use `2.3.1-SNAPSHOT` from Sonatype. Snapshots are unstable and should not be used in
production. Wait for a GA release.

**Option C: Keep resilience4j-spring-boot3 temporarily**
The `spring-boot-starter-aop` → `spring-boot-starter-aspectj` rename may cause issues.
Spring Boot 4 still includes the old name as a deprecated alias, so `resilience4j-spring-boot3`
may continue to work with Boot 4.0 for now. This is fragile and should not be a permanent
solution.

**Action:** Monitor [Resilience4j issue #2371](https://github.com/resilience4j/resilience4j/issues/2371)
for the GA release of `resilience4j-spring-boot4`.

Source: [Resilience4j issue #2371](https://github.com/resilience4j/resilience4j/issues/2371) |
[PR #2384](https://github.com/resilience4j/resilience4j/pull/2384) |
[Maven Central search](https://central.sonatype.com/artifact/io.github.resilience4j/resilience4j-spring-boot3)

**Confidence: HIGH** (directly verified via GitHub issue and Maven Central search)

---

### Q5 — Java 25 support in Spring Boot 4.0.3

**Answer: YES — Java 25 is explicitly supported.**

From the official Spring Boot 4.0.3 system requirements page:
> "Spring Boot 4.0.3 requires Java 17 or later and is compatible up to and including Java 25."

The project uses Java 25 at runtime (via `<java.version>25</java.version>`) but currently
compiles with `<release>21</release>` in the Maven compiler plugin. This is a discrepancy that
should be resolved — change `<release>21</release>` to `<release>25</release>`.

**However:** There is a known issue with `maven-compiler-plugin 3.13.0` (current in the pom)
and Java 25 (see Q6 below). Use 3.15.0 which resolves all known Java 25 compatibility issues.

Source: [Spring Boot 4.0.3 System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)

**Confidence: HIGH**

---

### Q6 — Maven compiler plugin version for Java 25

| Version | Java 25 status | Notes |
|---------|---------------|-------|
| 3.13.0 (current) | PROBLEMATIC | Known ExceptionInInitializerError with Java 25 toolchains |
| 3.14.0 | Released 2025-02-21 | Known incompatibility issues with Java 25 (issue #986) |
| 3.14.1 | Released 2025-09-18 | Still has Java 25 issues per GitHub issue #986 |
| **3.15.0** | **RECOMMENDED** | Released 2026-01-27, resolves Java 25 issues |
| 4.0.0-beta-4 | Beta (Maven 4 only) | Requires Maven 4 — not recommended for production |

**Answer:** Use `maven-compiler-plugin` version **`3.15.0`**.

Also change `<release>21</release>` to `<release>25</release>` to match the declared
`<java.version>25</java.version>`.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.15.0</version>
  <configuration>
    <release>25</release>
    <annotationProcessorPaths>
      <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

Source: [Maven Compiler Plugin releases](https://github.com/apache/maven-compiler-plugin/releases) |
[Java 25 compatibility issue #986](https://github.com/apache/maven-compiler-plugin/issues/986)

**Confidence: MEDIUM-HIGH** (version numbers from search results cross-referenced with Apache Maven
releases page; 3.15.0 released 2026-01-27 is confirmed latest stable)

---

## Complete Upgrade Plan — Exact pom.xml Changes

### Step 1: Update parent POM

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.0.3</version>
  <relativePath/>
</parent>
```

### Step 2: Update BOM versions

```xml
<dependencyManagement>
  <dependencies>
    <!-- Spring AI BOM — milestone, requires milestone repo -->
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>2.0.0-M2</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <!-- Spring Cloud BOM — Boot 4.0 compatible train -->
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>2025.1.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### Step 3: Rename starters in `<dependencies>`

```xml
<!-- WAS: spring-boot-starter-web -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- WAS: spring-boot-starter-aop -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>

<!-- WAS: spring-boot-starter-oauth2-client -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security-oauth2-client</artifactId>
</dependency>
```

### Step 4: Resilience4j (replace with Spring Cloud Circuit Breaker)

```xml
<!-- REMOVE: -->
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
  <version>2.2.0</version>
</dependency>

<!-- ADD (version managed by Spring Cloud 2025.1.1 BOM): -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### Step 5: Maven compiler plugin

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.15.0</version>
  <configuration>
    <release>25</release>
    <annotationProcessorPaths>
      <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

### Step 6: application.yml changes

```yaml
# If using Spring Session with Redis (not currently in this project — future-proofing):
# spring.session.redis.* → spring.session.data.redis.*

# Optional — defer Jackson 3 migration while validating:
spring:
  jackson:
    use-jackson2-defaults: true   # Remove once Jackson 3 is validated
```

### Step 7: Java source code changes

Search for and fix (in priority order):

1. `@EntityScan` imports — change from:
   `org.springframework.boot.autoconfigure.domain.EntityScan`
   to: `org.springframework.boot.persistence.autoconfigure.EntityScan`

2. Jackson catch blocks — search for `catch (IOException` where the IOException could be
   a Jackson `JsonProcessingException`. Change to `catch (JacksonException`.

3. Any `@JsonComponent` → `@JacksonComponent` (package `tools.jackson.databind.annotation`)

4. Any `Jackson2ObjectMapperBuilderCustomizer` implementations →
   `JsonMapperBuilderCustomizer`

5. `org.springframework.lang.Nullable` in Actuator endpoint parameters →
   `org.jspecify.annotations.Nullable`

---

## Risk Assessment by Component

| Component | Risk | Reason |
|---|---|---|
| Spring Boot parent | LOW | Standard version bump |
| Spring Cloud / Vault | LOW | Direct BOM upgrade, API unchanged |
| Spring AI | MEDIUM | Still milestone, not GA |
| Spring Security OAuth2 | LOW | Starter rename only, API unchanged |
| Jackson 3 | MEDIUM | Package migration needed; shim available |
| Resilience4j | HIGH | No GA `spring-boot4` artifact; requires workaround |
| JPA / Hibernate | LOW | No static metamodel in use |
| Actuator / Micrometer | LOW | Probe defaults change; verify health endpoint |
| Virtual threads | LOW | Config property unchanged |
| Maven compiler | LOW | Plugin version bump + release=25 |

---

## What NOT to Do

- Do NOT use `spring-cloud-dependencies:2025.0.0` (Northfields) — it is explicitly
  incompatible with Spring Boot 4.0.1+ despite the similar year in the name
- Do NOT use `resilience4j-spring-boot3` as a long-term solution with Boot 4
- Do NOT use `maven-compiler-plugin` < 3.15.0 with Java 25
- Do NOT use Spring AI 1.x with Spring Boot 4.x (incompatible Spring Framework version)
- Do NOT set `<release>21</release>` when `<java.version>25</java.version>` — this produces
  Java 21 bytecode from a Java 25 JDK, disabling all Java 22-25 language features

---

## Open Questions / Blockers

1. **Resilience4j GA for Spring Boot 4**
   - What we know: PR merged, `2.3.1-SNAPSHOT` available
   - What's unclear: GA release date (maintainer was on medical leave as of Feb 2026)
   - Recommendation: Switch to Spring Cloud Circuit Breaker abstraction now, or wait and
     monitor [issue #2371](https://github.com/resilience4j/resilience4j/issues/2371)

2. **Spring AI 2.x GA**
   - What we know: `2.0.0-M2` is latest; no GA timeline announced
   - What's unclear: Whether M2 is stable enough for production use
   - Recommendation: Test with M2; if stability is required, stay on Spring Boot 3.5.x +
     Spring AI 1.1.2 until Spring AI 2.0 GA ships

3. **MapStruct compatibility with Java 25 / Spring Boot 4**
   - What we know: MapStruct 1.6.2 works with Spring Boot 3.x and Java 21
   - What's unclear: Whether 1.6.2 processes annotations correctly under Java 25 class format
   - Recommendation: Test `mvn compile` after pom changes; if MapStruct fails, check for
     a 1.7.x release or newer

---

## Sources

### Primary (HIGH confidence)
- [Spring Boot 4.0.3 System Requirements](https://docs.spring.io/spring-boot/system-requirements.html) — Java version support
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) — all breaking changes
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes) — feature overview
- [Spring Cloud Supported Versions](https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions) — compatibility matrix
- [Spring Cloud 2025.1.1 release](https://spring.io/blog/2026/01/29/spring-cloud-2025-1-1-aka-oakwood-has-been-released/) — Oakwood / Boot 4 pairing
- [Spring AI 2.0.0-M1 blog](https://spring.io/blog/2025/12/11/spring-ai-2-0-0-M1-available-now/) — Spring Boot 4 compatibility
- [Spring AI Boot 4 epic #3379](https://github.com/spring-projects/spring-ai/issues/3379) — implementation status

### Secondary (MEDIUM confidence)
- [Resilience4j issue #2371](https://github.com/resilience4j/resilience4j/issues/2371) — Spring Boot 4 support status (snapshot only)
- [Maven Compiler Plugin issue #986](https://github.com/apache/maven-compiler-plugin/issues/986) — Java 25 compatibility
- [Jackson 3 in Spring blog](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring/) — migration path
- [Spring AI releases page](https://github.com/spring-projects/spring-ai/releases) — version listing
- [Spring Boot 4.0.3 available now](https://spring.io/blog/2026/02/19/spring-boot-4-0-3-available-now/) — latest patch confirmation

### Tertiary (LOW confidence — cross-referenced above where used)
- Multiple community blog posts and Medium articles used for validation only

---

## Metadata

**Confidence breakdown:**
- Spring Boot 4.0.3 system requirements: HIGH — from official docs
- Spring Cloud 2025.1.1 for Boot 4: HIGH — from compatibility matrix
- Spring AI 2.0.0-M2 status: HIGH — from Spring blog (milestone caveat noted)
- Resilience4j blocker: HIGH — from GitHub issue direct inspection
- Maven compiler 3.15.0: MEDIUM-HIGH — from search results cross-referenced with Apache releases
- Jackson 3 migration: HIGH — from official Spring blog
- Starter renames: HIGH — from official migration guide

**Research date:** 2026-02-24
**Valid until:** 2026-04-01 (fast-moving ecosystem; recheck Resilience4j GA and Spring AI 2.0 GA)
