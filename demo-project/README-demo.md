# Demo Service - Pulls in jarfile from azure repository

The goal of this demo module is to demonstrate a drools auth rules implementation
It uses a jarfile from azure artifactoru
See the dependency in build.gradle
i.e. implementation "uk.gov.hmcts.cp:cp-auth-rules-filter:1.0.0"

# Run the demo service

cd demo
gradle bootRun
curl -H "CJSCPPUID:b066839e-30bd-42d9-8101-38cf039d673f" http://localhost:8090/api/hello

# Debug the demo service

Run DemoApplication in idea in debug mode
( i.e. right click on the java and select Debug DemoApplication.main() )
Navigate to the jar file in "External Libraries"
i.e. Find uk.gov.hmcts.cp:cpauth-rules-filter.jar and add a debug point in HttpAuthFilter

curl -H "CJSCPPUID:b066839e-30bd-42d9-8101-38cf039d673f" http://localhost:8090/api/hello

# To do .... add test and run from github pipelines
