# Purpose of module
Enable configuration support and the possibility to `@Inject` configuration classes where needed.

# How
Configuration provided in a yml file is read, and the properties are mapped to configuration classes annotated with the `@Config`
annotation. Configuration file is pointed out by the last argument passed when starting the `Main.class`
There is also support for putting environment variables into the configs by setting the config value to an environment variable name 
embraced with double curly braces.
It will fetch the value from the environment by using the name you provide in the configuration file. 
Useful when running the application in a container environment

```
export ENVIRONMENT_VARIABLE_NAME=http://host:port/path
java Main.class configfile.yml


httpClient: {{ENVIRONMENT_VARIABLE_NAME}}
--> will be translated to 
httpClient: http://host:port/path
 
```

# Key classes
`Config` - annotation to put on configuration classes.
`ConfigFactory` - factory that creates javaobjects from the values in the configuration file.
`ConfigReader` - the class reading the configuration file and replacing any double curly values with values from the environment. 
`ConfigAutoBindModule` - making the configuration classes injectable.
 

