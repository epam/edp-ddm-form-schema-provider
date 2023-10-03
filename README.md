# form-schema-provider

This service provides API for storing and retrieving form schemas using Redis storage/

### Local development:
###### Prerequisites:
* Redis storage is configured and running

###### Configuration:
Check `src/main/resources/application-local.yaml` and replace if needed:
  * spring.redis.* properties with your Redis storage values

###### Steps:
1. (Optional) Package application into jar file with `mvn clean package`
2. Add `--spring.profiles.active=local` to application run arguments
3. Run application with your favourite IDE or via `java -jar ...` with jar file, created above


### Test execution

* Tests could be run via maven command:
    * `mvn verify` OR using appropriate functions of your IDE. To avoid `The filename or extension is too long` error on Windows, please uncomment `<fork>false</fork>` in `spring-boot-maven-plugin` configuration.

### License

The form-schema-provider is Open Source software released under
the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).
