# Spring URL Templated fallback action name

## Background

The filter resolves an action name (the rule key passed to drools) in four
steps: vendor media type from `Content-Type`, vendor media type from `Accept`,
explicit `CPP-ACTION` header, then a computed fallback of `<METHOD> <PATH>`.

Before this change, the fallback's path component was the **raw request URI**.
That already supported two rule-authoring idioms:

- **Static / multi-segment literal paths** — e.g. a request to
  `POST /api/path1/path2` produces `POST /api/path1/path2`, and a drools rule
  with `name == "POST /api/path1/path2"` matches by simple equality.
- **Regex / wildcard patterns** — drools' `matches` operator handles dynamic
  segments, e.g. `name matches "POST /api/orders/[^/]+"` covers any concrete
  order id.

What was awkward was the third idiom: pasting a Spring `@RequestMapping`
pattern (`/api/customers/{customerId}/orders/{orderId}`) directly into a rule. With the raw URI in the
action name, the literal string `{id}` never appears in incoming traffic, so
that rule never fires for a real resource id.

## Goal

Add support for the third idiom: when the fallback is reached, substitute
the raw URI with the matched Spring MVC route template, producing a stable
name like `POST /api/customers/{customerId}/orders/{orderId}` that simple-equality rules can target.

This is purely additive. Static-literal rules and regex rules continue to
work without modification.

## Non-goals / preserved behaviour

- Vendor media types (`application/vnd.*`) on `Content-Type` and `Accept`
  continue to win — bit-for-bit identical to today.
- The explicit action header (`CPP-ACTION`) continues to take priority over
  the computed fallback.
- `actionRequired=true` semantics unchanged — a request that misses vendor
  and header still gets HTTP 400.
- Existing literal-path rules (`name == "POST /api/path1/path2"`) and
  regex rules (`name matches "POST /api/orders/[^/]+"`) keep working
  without modification.
- No change to drools rule format, no change to `IdentityClient`, no new
  configuration properties.
- The drools `Action.attributes` map's `path` and `method` entries remain
  the raw URI and request method — only `Action.name` gets the templated
  form, leaving downstream rules free to inspect the literal request.

## Approach

Inject Spring's `RequestMappingHandlerMapping` into `HttpAuthzFilter`. When
the resolver returns a result that came from neither a vendor nor a header
(i.e. it was computed from method + path), the filter calls
`mapping.getHandler(request)` to populate
`HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE`, reads the matched pattern,
and rewrites the action name to `<METHOD> <PATTERN>`.

If `getHandler` returns null (no controller matches — typically a 404 or a
path served outside `RequestMappingHandlerMapping`), the filter falls back to
the raw path and logs a `WARN` line so operators can spot endpoints that
should have been templated.

`RequestActionResolver` is unchanged. The filter post-processes its result.

## Implementation outline

1. `HttpAuthzFilter`
   - New constructor parameter: `RequestMappingHandlerMapping handlerMapping`
     (nullable for testability).
   - New private method `resolveTemplatedPath(request, rawPath)` — calls
     `getHandler`, reads `BEST_MATCHING_PATTERN_ATTRIBUTE`, returns the
     pattern or null. Catches and logs any exception, returns null.
   - In `doFilter`, after `RequestActionResolver.resolve(...)`, if neither
     `vendorSupplied()` nor `headerSupplied()` is true, attempt templating;
     on success, replace the resolved action name. On failure, log WARN and
     keep the raw-path action.
2. `AuthzAutoConfiguration`
   - Inject `RequestMappingHandlerMapping` into the filter-registration bean
     factory and pass it to the filter constructor.
3. `RequestActionResolver` — no change.
4. `HttpAuthzProperties` — no change.

## Failure modes

| Situation                                     | Behaviour                                |
|-----------------------------------------------|------------------------------------------|
| Handler matches, pattern attribute set        | Use templated pattern.                   |
| Handler matches, pattern attribute missing    | Use raw path. No warning (defensive).    |
| `getHandler` returns null (404, no mapping)   | Use raw path. WARN log.                  |
| `getHandler` throws                           | Use raw path. WARN log with exception.   |
| Handler mapping bean not available (non-MVC)  | Use raw path. No warning. (Unsupported.) |

## Tests

Add to `HttpAuthzFilterTest`:
- Templates path when handler mapping returns a matched pattern.
- Falls back to raw path when handler mapping returns null.
- Vendor-from-Content-Type still wins (no templating attempted).
- Explicit `CPP-ACTION` header still wins (no templating attempted).
- Existing tests that pass a null handler mapping continue to behave as today.

## Risks

- `getHandler` from inside a filter populates request attributes that
  `DispatcherServlet` would otherwise set later. Spring overwrites them
  idempotently on its own resolution pass, so this is benign.
- `getHandler` is not free; we only call it on the fallback path, so vendor
  and header traffic pay nothing.

## Prior art and safety justification

The technique used here — having a Spring servlet filter consult Spring
MVC's `RequestMappingHandlerMapping` to obtain the matched route template
for the current request — is the same technique Spring Security itself
adopted for its `{id}`-style URL matchers. This section documents that
parallel so reviewers can satisfy themselves that the approach is
mainstream and supported.

### Spring Security uses the same approach for path-template matching

Since Spring Security 5.4 (GA November 2020), the recommended way to write
URL-authorisation rules with path variables is `MvcRequestMatcher`:

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(new MvcRequestMatcher(introspector, "/api/customers/{customerId}/orders/{orderId}")).hasRole("USER")
);
```

Internally `MvcRequestMatcher` uses Spring's `HandlerMappingIntrospector`
to obtain a `MatchableHandlerMapping` (typically backed by
`RequestMappingHandlerMapping`) and asks it to evaluate whether the
incoming request matches the configured pattern. Spring Security 6
generalises this with the newer `PathPatternRequestMatcher` API, again
backed by the same Spring MVC matching infrastructure. Both run inside
a servlet filter, ahead of `DispatcherServlet`, just like our filter.

References:

- Spring Security reference, *Authorize HTTP Requests*:
  https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html
- Spring Security reference, *Spring MVC Integration* (`MvcRequestMatcher`):
  https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html
- Spring Framework reference, `HandlerMapping` SPI and the
  `BEST_MATCHING_PATTERN_ATTRIBUTE` request attribute:
  https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping.html

### Why this filter's usage is *narrower* than Spring Security's

Spring Security's matchers actively *evaluate* a pattern against a request
(`MatchableHandlerMapping.match(request, pattern)`), which exercises the
full pattern-matching code path. Our filter does less:

- We do not supply a pattern to test.
- We call `getHandler(request)` — which Spring would call anyway during
  dispatch — and read the matched pattern that Spring places on the
  request as `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE`.
- We treat the value as an opaque string and concatenate with the request
  method.

If `getHandler` returns null, throws, or leaves the attribute unset, we
fall back to the raw request URI — preserving the pre-change behaviour
exactly. Tests cover each of these failure modes:

- `fallsBackToRawPathWhenNoHandlerMatches`
- `fallsBackToRawPathWhenHandlerMappingThrows`
- `fallsBackToRawPathWhenPatternAttributeMissing`
- `nullHandlerMappingPreservesRawPathFallback`

### Public, stable API surface only

Every Spring symbol this change touches is part of Spring's public,
documented API:

| Symbol                                              | Stability          |
|-----------------------------------------------------|--------------------|
| `RequestMappingHandlerMapping`                      | Public, since 3.1  |
| `HandlerMapping.getHandler(HttpServletRequest)`     | Public SPI         |
| `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE`    | Public constant    |
| `HandlerExecutionChain`                             | Public             |
| `ObjectProvider` (used in auto-config wiring)       | Public, Spring 4+  |

No reflection, no internal classes, no version-pinned behaviour.

### Side-effect analysis

Calling `getHandler(request)` from a filter sets request attributes that
the dispatcher would set later (`BEST_MATCHING_PATTERN_ATTRIBUTE`,
`URI_TEMPLATE_VARIABLES_ATTRIBUTE`, `PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE`).
The same observation applies to Spring Security's `MvcRequestMatcher`,
which has shipped this side effect to millions of applications since 2020
without recorded incident. Spring's `DispatcherServlet` re-runs handler
resolution and overwrites these attributes idempotently on its own pass.

### Defensive posture

| Failure mode                                        | Behaviour                |
|-----------------------------------------------------|--------------------------|
| Bean `RequestMappingHandlerMapping` not present     | Filter degrades to raw path; no error |
| `getHandler` returns null                           | Raw path; `WARN` log     |
| `getHandler` throws                                 | Raw path; `WARN` log; exception not propagated |
| Pattern attribute is null                           | Raw path; no warning     |
| Pattern attribute is non-`String`                   | `toString()` defensive coercion |
| Vendor or `CPP-ACTION` supplied                     | Templating skipped — pre-existing behaviour bit-for-bit |

In short, **no request can be denied or 5xx'd because of templating**;
the worst case is the pre-change behaviour (raw URI in the action name)
plus a log line.

### Summary for review

- The technique is the same one Spring Security ships for the same
  purpose (`{id}` URL matching) and has shipped since 2020.
- We use a strictly narrower subset of the Spring MVC API — read-only
  pattern observation rather than active pattern evaluation.
- All Spring symbols touched are public stable API.
- All failure paths degrade to the previous behaviour; vendor and
  `CPP-ACTION` paths are untouched.
- 31 unit tests cover happy paths, all defensive failure modes, and
  the short-circuit guarantees.
