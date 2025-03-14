# SearchFilter4SpringBoot
This is a simple project filtering duplicated headers
from defined endpoints. 
This is necessary because the middleware used by Traefik 
does not allow duplicate headers and the SpringBoot proxy 
in conjunction with elasticsearch generates two headers 
with 'transfer-encoding' by default.

## Requirements
- Java 21 or higher

## Building
To build the project, you can use the following command:
```shell
./gradlew clean jar
```
The resulting jar-file can be found in the build/libs folder.

## Configuration
Configuration can be done via application.properties. 
Two settings are available for configuration:
- repo.search.endpointPattern
- repo.search.dedupHeaders

repo.search.endpointPattern
: This is a regular expression pattern that will be used to match the endpoints that will be filtered.
Default value is `(/[^/]+)?/api/v\\d+(/[^/]+)?/_?search$`
(e.g.: /context/api/v1/search, /context/api/v1/index/_search)

repo.search.dedupHeaders
: This is a comma separated list of headers that will be deduplicated.
Default value is `Transfer-encoding`

## Installation
For standard search endpoints it's sufficient to
add the following dependency to your project:
This can be easily done by copying the jar file 
in the lib folder and adding this to the load path.
```shell
java -cp .:PROJECT_JAR_FILE -Dloader.path="file://INSTALLATION_DIR/PROJECT_JAR_FILE,.
/lib/,." -jar PROJECT_JAR_FILE
```
