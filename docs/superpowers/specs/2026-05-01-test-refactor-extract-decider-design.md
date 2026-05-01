# Test refactor: extract `AuthzDecider` and `PathExclusionChecker`

Date: 2026-05-01
Author: Arcadius Ahouansou
Driver: review feedback from coling01 on PR #35

## Background

`HttpAuthzFilterTest.java` had grown to ~700 lines. Recent commits ("keep only
tests that cover my changes") brought it down to 358 lines, but the reviewer's
structural feedback still applies:

> we should reduce the number of tests by
> a) pulling methods out into other classes than we can throuroughly test in
>    that layer
> b) splitting groups of tests into separate test classes by function
>
> we should reduce the size by
> a) using annotated common mocks not keep repeating the same lines

The two issues are linked: a 700-line test file is a symptom of a
production class doing too much. `HttpAuthzFilter.doFilter` currently owns:
OPTIONS short-circuit, exclude-prefix loop, user-id check, action-required
check, templated-URL fallback wiring, identity fetch, group mapping,
principal building, attribute-map building, Drools evaluation, and the
401/400/403/200 decision. Two responsibilities (`RequestActionResolver`,
`SpringTemplatedUrlFallback`) are already extracted; the rest still cluster
in one place.

## Goal

Reduce the size and concentration of `HttpAuthzFilterTest` by:

1. Extracting two production seams that have clear independent responsibilities.
2. Splitting the existing tests across multiple top-level test files that mirror
   the new production structure.
3. Removing repetition in test setup (`ArgumentCaptor`, `MockHttpServletRequest`,
   `MockHttpServletResponse`).

## Non-goals

- No behaviour changes. The HTTP outcome for every existing input must stay
  identical.
- No changes to `RequestActionResolver`, `SpringTemplatedUrlFallback`,
  `DroolsAuthzEngine`, or `RequestUserAndGroupProvider` and their tests.
- No new test cases beyond what already exists in `HttpAuthzFilterTest`.
- No deprecation shims for `HttpAuthzFilter`'s constructor — the filter is
  wired by `AuthzAutoConfiguration`, not by direct construction in consumer
  code, so a constructor signature change is safe.

## Production changes

### `PathExclusionChecker` (new)

```java
public final class PathExclusionChecker {
    private final List<String> prefixes;

    public PathExclusionChecker(final List<String> prefixes) {
        this.prefixes = List.copyOf(prefixes);
    }

    public boolean isExcluded(final String pathWithinApplication) {
        for (final String prefix : prefixes) {
            if (pathWithinApplication.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
```

Wired by `AuthzAutoConfiguration` from `HttpAuthzProperties.getExcludePathPrefixes()`.

### `AuthzDecider` (new)

Owns the whole authorization pipeline that runs once we have a non-blank
user-id header and the path is not excluded.

```java
public final class AuthzDecider {

    public AuthzDecider(HttpAuthzProperties properties,
                        IdentityClient identityClient,
                        IdentityToGroupsMapper identityToGroupsMapper,
                        DroolsAuthzEngine droolsAuthzEngine,
                        SpringTemplatedUrlFallback springTemplatedUrlFallback);

    public Decision decide(HttpServletRequest request,
                           String userId,
                           String pathWithinApplication);

    public sealed interface Decision permits Allow, Deny {}
    public record Allow() implements Decision {}
    public record Deny(int status, String reason) implements Decision {}
}
```

Behaviour preserved verbatim from current `doFilter`:

1. Resolve action via `RequestActionResolver`.
2. If `properties.isActionRequired()` and neither vendor nor header supplied
   the action → `Deny(400, "Missing header: " + properties.getActionHeader())`.
3. Apply `springTemplatedUrlFallback` to compute the effective action.
4. `identityClient.fetchIdentity(userId)` → groups via
   `identityToGroupsMapper.toGroups(...)`.
5. Build `AuthzPrincipal` and **set it as a request attribute under
   `AuthzPrincipal.class.getName()`** before the engine call. This matches
   the current ordering: the attribute is set even on a subsequent deny.
6. Build the attribute map (`method`, `path`) and the `Action`.
7. Call `droolsAuthzEngine.evaluate(perRequestProvider, action)`.
8. Return `Allow` on true, `Deny(403, "Access denied")` on false.

### `HttpAuthzFilter` (slimmed)

```java
public final class HttpAuthzFilter implements Filter {

    private final HttpAuthzProperties properties;
    private final PathExclusionChecker exclusionChecker;
    private final AuthzDecider authzDecider;

    public HttpAuthzFilter(HttpAuthzProperties properties,
                           PathExclusionChecker exclusionChecker,
                           AuthzDecider authzDecider) { ... }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        final String path = new UrlPathHelper().getPathWithinApplication(httpRequest);

        if (OPTIONS.equalsIgnoreCase(httpRequest.getMethod())
                || exclusionChecker.isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }

        final String userId = httpRequest.getHeader(properties.getUserIdHeader());
        if (!StringUtils.hasText(userId)) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing header: " + properties.getUserIdHeader());
            return;
        }

        final Decision decision = authzDecider.decide(httpRequest, userId, path);
        if (decision instanceof Allow) {
            chain.doFilter(request, response);
        } else if (decision instanceof Deny deny) {
            httpResponse.sendError(deny.status(), deny.reason());
        }
    }
}
```

### `AuthzAutoConfiguration` (updated)

Two new `@Bean` definitions:

- `PathExclusionChecker pathExclusionChecker(HttpAuthzProperties)` — built from
  `properties.getExcludePathPrefixes()`.
- `AuthzDecider authzDecider(...)` — constructor-injected with the existing
  beans.

`httpAuthzFilterRegistration` is updated to inject `properties`,
`pathExclusionChecker`, `authzDecider` only.

## Test changes

### Files (5 in total)

| File | Responsibility | Tests |
|---|---|---|
| `HttpAuthzFilterTest` | Filter orchestration only | OPTIONS forwards (verify chain invoked), excluded path forwards, missing user-id → 401, decider Allow → chain invoked, decider Deny → status written |
| `PathExclusionCheckerTest` | Prefix-match logic | matches single prefix, no match, multiple prefixes |
| `AuthzDeciderActionResolutionTest` | Action name source resolution | header action used, header method attribute, header path attribute, templated URL fallback (POST `/api/orders/123` → `POST /api/orders/{id}`), vendor Content-Type wins over header, vendor Accept used when no Content-Type |
| `AuthzDeciderOutcomeTest` | Allow / Deny outcomes | engine approves → Allow, engine rejects → Deny(403, "Access denied"), action-required + neither header nor vendor → Deny(400, "Missing header: ...") |
| `AuthzDeciderPrincipalTest` | Principal + attribute wiring | `AuthzPrincipal` request attribute set under `AuthzPrincipal.class.getName()`, `method` attribute, `path` attribute |

### Test hygiene applied across every new file

- `@Captor private ArgumentCaptor<Action> actionCaptor;` — Mockito initialises
  via `MockitoExtension`. Replaces every inline
  `final ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);`.
- `private MockHttpServletRequest req;` and
  `private MockHttpServletResponse res;` declared as fields and instantiated
  in `@BeforeEach`. **Not `@Mock`-annotated** — `MockHttpServletResponse` is a
  Spring fake (a real concrete class whose `getStatus()` returns the actual
  status set by `sendError`); annotating it `@Mock` would replace that
  behaviour with a Mockito stub. The reviewer's intent — "don't repeat
  `new MockHttpServletResponse()` everywhere" — is satisfied by the
  field + `@BeforeEach` pattern. A short comment in the file (one line)
  notes why these are not `@Mock`.
- Existing `@Mock` on `IdentityClient`, `IdentityToGroupsMapper`,
  `DroolsAuthzEngine`, `FilterChain` carries over unchanged.
- `mockIdentity(USER_123)` stays as a private static helper in each test
  class that needs it. It is two lines; sharing it across files via a
  testsupport class is not worth a new abstraction.

### Constants

`TestConstants.java` already exists in `src/test/java/.../testsupport/`. The
test files will lift the constants currently inlined in `HttpAuthzFilterTest`
(`USER_ID_HEADER`, `ACTION_HEADER`, `METHOD_GET`, `METHOD_POST`, `PATH_HELLO`,
etc.) into `TestConstants` to avoid five copies.

## Migration

Single PR. Order of work in the implementation plan:

1. Add `PathExclusionChecker` + `PathExclusionCheckerTest`.
2. Add `AuthzDecider` + the three decider test files (action-resolution,
   outcome, principal). Carry behaviour over verbatim.
3. Slim `HttpAuthzFilter`, rewrite `HttpAuthzFilterTest`.
4. Update `AuthzAutoConfiguration` (two new beans, updated filter
   registration).
5. Lift shared constants into `TestConstants`.
6. `./gradlew test` — every prior assertion must still pass against the
   moved test code.

## Risks

- **Behaviour drift on the request-attribute set point.** Currently the
  attribute is set unconditionally before Drools evaluation. The decider
  must do the same — set it before returning `Deny(403, ...)`. Covered by
  an explicit test in `AuthzDeciderPrincipalTest` ("attribute set even when
  engine rejects").
- **Constructor change to `HttpAuthzFilter`.** Mitigated: only consumer is
  `AuthzAutoConfiguration` in this repo. Library consumers wire via the
  auto-config, not by direct `new HttpAuthzFilter(...)`.
- **`Decision` sealed interface requires Java 17+.** Build toolchain is
  Java 21 (per `build.gradle`), so this is safe.
- **Mocking `final` `AuthzDecider` in `HttpAuthzFilterTest`.** Spring Boot
  4.0.3 brings Mockito 5.x, which uses the inline mock maker by default —
  final classes are mockable. No `MockitoSettings` opt-in needed.
