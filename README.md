# Common Platform (CP) Springboot Auth Rules

Springboot filter to apply auth rules on incoming web requests

## Contribute to This Repository

Contributions are welcome! Please see the [CONTRIBUTING.md](.github/CONTRIBUTING.md) file for
guidelines.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Generating jarfile with dependency files

Publishing to repository with "gradle publish" will generate .jar file + .module file + .pom file
The jar file contains the code / java classes
The module file contains the gradle dependencies
The pom file contains the maven dependencies ( same as the gradle module file )
But we dont have azure devops permissions to locally publish to azure
Our github repositories have an org level pat token with permissions to publish to azure repository
The azure artifacts can be browsed with public access here
https://dev.azure.com/hmcts/Artifacts/_artifacts/feed/hmcts-lib
Once a repository artifact has been used locally, it will be visible in our local .gradle repository
i.e.

```
cd $HOME/.gradle
find . -name cp-auth-rules-filter.* -ls
... 1884 21 Oct 11:11  ... cp-auth-rules-filter-1.0.1.pom
... 31053 21 Oct 11:11 ... cp-auth-rules-filter-1.0.1.jar
... 2992 21 Oct 11:11  ...  cp-auth-rules-filter-1.0.1.module
```

To test locally we can create a local jarfile with "gradle jar"
And reference this jarfile from our Spring Boot Application in build.gradle
i.e. implementation(files("../../cp-auth-rules-filter/build/libs/cp-auth-rules-filter-0.0.999.jar"))
But this will not load the required dependencies .. how to do this ??
We can inspect the dependencies by generating the maven pom and gradle module locally
And inspecting the files generated

```
gradle publishToMavenLocal
jar tvf ./build/libs/cp-auth-rules-filter-0.0.999.jar
cat build/publications/mavenJava/module.json
```

## Demo Project

To test the filter process using the published jarfile see child project "demo-project"

## Implementation notes

Some changes to previous implementation

1) The rules filter is on by default
   It can be turned off ( maybe for some integration tests ) by setting auth.rules.disabled=true
   It will log warnings for every url whilst it is disabled

2) It expects drools rules files with an extension of ".drl" to be under a resource folder of "
   drools" which are loaded on startup
   If no drools rules files are found, the application will fail to start
   See that rules are in sub folders named as per the package i.e. uk/gov to prevent drools warning
   such as

```
KieBuilderImpl    : File '/Projects/cp-auth-rules-filter/build/resources/test/drools/route-rules.drl' 
is in folder '/Users/colingreenwood/IdeaProjects/cp-auth-rules-filter/build/resources/test/drools' 
but declares package 'uk.gov.moj.cpp.authz.demo'. 
It is recommended to have a correspondance between package and folder names
```

## Drools Auth Filter - How does it work ?

Rules are set up in drools drl for every url
Urls can be excluded from filtering by adding to the excludePathPrefixes

An incoming request must have the rule filter header set with userId which must be uuid
i.e. CJSCPPUID=dfef853f-fd2e-488f-8b57-7bc115043cf3

The userid is used to construct a url that calls an identity provider to get logged in user
permissions
i.e. http://identity-server/usersgroups-query-api/query/api/rest/usersgroups/users/dfef853f-fd2e-488f-8b57-7bc115043cf3/permissions

This returns LoggedInUserPermissions json
i.e. See  [LoggedInUserPermissions.json](./src/test/resources/json/LoggedInUserPermissions.json)

The response is mapped to a IdentityResponse json
i.e. See  [IdentityResponse.json](./src/test/resources/json/IdentityResponse.json)

Then we construct a AuthPrincipal with userId and groups

We add AuthPrincipal classname onto the HttpRequest ( why ? )
i.e. httpRequest.setAttribute(AuthPrincipal.class.getName(), principal);

We create a new AuthAction with the url and attributes of the url rest method and path
i.e. See  [AuthAction.json](./src/test/resources/json/AuthAction.json)

UserAndGroupsProvider is an interface with a method isMemberOfAnyOfTheSuppliedGroups() that is used
within the drools rules
We create a new UserAndGroupsProvider from the user collected information

We pass the AuthAction and the UserAndGroupsProvider to drools evaluate.
The response from the drools evaluate is boolean indicating authorisation to perform the activity.

## Action name resolution

The filter computes an "action name" for every authorised request and
passes it to drools as `Action.name`. Drools rules key on this string.

### Resolution priority

The filter walks four steps and stops at the first one that yields a name:

1. **Vendor media type from `Content-Type`** — e.g.
   `application/vnd.sjp.delete-financial-means+json` → `sjp.delete-financial-means`.
2. **Vendor media type from `Accept`** — first vendor match, left-to-right.
3. **Explicit action header** (default header name `CPP-ACTION`) — whatever
   string the client supplies, used verbatim. Recommended for plain
   `application/json` endpoints when you control the callers.
4. **Computed `<METHOD> <PATH>`** — used when none of the above apply.

`actionRequired=false` (the default) lets requests fall through to step 4.
Set `actionRequired=true` to reject any request that misses both a vendor
media type and the `CPP-ACTION` header with HTTP 400.

### Step 4: computed action names

The path component of step 4 is **the matched Spring MVC route template**
when one is available, otherwise the raw request URI.

| Incoming request                          | Resolved `Action.name`                  | Notes                                    |
|-------------------------------------------|-----------------------------------------|------------------------------------------|
| `GET /api/health`                         | `GET /api/health`                       | Literal route, no placeholders           |
| `POST /api/path1/path2`                   | `POST /api/path1/path2`                 | Multi-segment literal route              |
| `POST /api/orders/123`                    | `POST /api/orders/{id}`                 | Templated via `RequestMappingHandlerMapping` |
| `PUT /api/users/u1/orders/o9`             | `PUT /api/users/{userId}/orders/{orderId}` | Multiple placeholders                  |
| `POST /api/path/cat-7/item-42`            | `POST /api/path/{categoryId}/{itemId}`  | Adjacent placeholders                    |
| `GET /static/foo.css` (no controller)     | `GET /static/foo.css` + `WARN` log      | Falls back to raw URI                    |

### Authoring drools rules against step 4 names

Three idioms are available — pick whichever matches the route shape best:

```drl
// 1. Static / literal paths — always supported, simple equality.
$a: Action(name == "GET /api/health")

// 2. Regex matching against the raw URI — always supported, useful when
//    you do not want to depend on Spring's route templates (or for paths
//    served outside RequestMappingHandlerMapping).
$a: Action(name matches "POST /api/orders/[^/]+")

// 3. Spring-style placeholders — added by this change. Lets you copy the
//    @RequestMapping pattern straight into the rule and use plain equality.
$a: Action(name == "POST /api/orders/{id}")
```

Idioms 1 and 2 worked in earlier versions of this filter and continue to
work unchanged. Idiom 3 is what the templated step-4 fallback enables: a
request to `POST /api/orders/123` now produces the rule key
`POST /api/orders/{id}` (the matched route template), so a simple equality
check fires regardless of the concrete id.

### What this change did and did not change

- **Did not change:** vendor media types, `CPP-ACTION`, `actionRequired`
  semantics, the `path` and `method` attributes on the drools `Action`
  (still the raw URI and request method respectively), the rule file
  format, or any configuration property.
- **Did change:** when step 4 is reached, the path component is now the
  Spring-matched route template (`/api/orders/{id}`) instead of the raw
  URI (`/api/orders/123`). When no controller matches the request, the
  filter logs a `WARN` and falls back to the raw URI — preserving the
  pre-change shape for unmapped paths.

### Implementation note: prior art

The mechanism — having a servlet filter consult Spring MVC's
`RequestMappingHandlerMapping` to obtain the matched route template — is
the same technique Spring Security itself uses to support `{id}`-style
URL matchers (`MvcRequestMatcher`, since Spring Security 5.4 / Nov 2020;
`PathPatternRequestMatcher` in Spring Security 6). Our usage is strictly
narrower: we only *read* the matched pattern Spring sets on the request,
we don't *evaluate* a pattern against the request.

Full architectural justification, references to Spring documentation,
public-API surface, side-effect analysis, defensive failure modes, and
test coverage are documented in
[docs/specs/2026-04-25-templated-fallback-action-design.md](docs/specs/2026-04-25-templated-fallback-action-design.md).

