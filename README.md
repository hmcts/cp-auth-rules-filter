# Common Platform (CP) Springboot Auth Rules
Springboot filter to apply auth rules on incoming web requests

## Contribute to This Repository

Contributions are welcome! Please see the [CONTRIBUTING.md](.github/CONTRIBUTING.md) file for guidelines.

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
i.e.   implementation(files("../../cp-auth-rules-filter/build/libs/cp-auth-rules-filter-0.0.999.jar"))
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

2) It expects drools rules files with an extension of ".drl" to be under a resource folder of "drools" which are loaded on startup
   If no drools rules files are found, the application will fail to start
   See that rules are in sub folders named as per the package i.e. uk/gov to prevent drools warning such as
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

The userid is used to construct a url that calls an identity provider to get logged in user permissions
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

UserAndGroupsProvider is an interface with a method isMemberOfAnyOfTheSuppliedGroups() that is used within the drools rules
We create a new UserAndGroupsProvider from the user collected information

We pass the AuthAction and the  UserAndGroupsProvider to drools evaluate.
The response from the drools evaluate is boolean indicating authorisation to perform the activity.

