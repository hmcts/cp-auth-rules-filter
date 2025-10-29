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
find . -name cp-springboot-auth-rules.* -ls
... 1884 21 Oct 11:11  ... cp-springboot-auth-rules-filter-1.0.1.pom
... 31053 21 Oct 11:11 ... cp-springboot-auth-rules-filter-1.0.1.jar
... 2992 21 Oct 11:11  ...  cp-springboot-auth-rules-filter-1.0.1.module
```


To test locally we can create a local jarfile with "gradle jar"
And reference this jarfile from our Spring Boot Application in build.gradle
i.e.   implementation(files("../../cp-springboot-audit-filter/build/libs/cp-springboot-audit-filter-0.0.999.jar"))
But this will not load the required dependencies .. how to do this ??
We can inspect the dependencies by generating the maven pom and gradle module locally 
And inspecting the files generated
```
gradle publishToMavenLocal
jar tvf ./build/libs/cp-springboot-auth-rules-0.0.999.jar
cat build/publications/mavenJava/module.json
```

## Implementation notes
Some changes to previous implementation
1) It is on by default, if you dont need to use it then dont bring it in to your application
   It can be turned off ( maybe for some integration tests ) by setting
``` application.yml
auth:
    rules:
          disabled: true
```

2) It expects drools rules files with an extension of ".drl" to be under a resource folder of "drools" which are loaded on startup
   If no drools rules files are found, the application will fail to start
   To prevent drools warning such as


## Drools Auth Filter - How does it work ?
