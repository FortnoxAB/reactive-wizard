# Purpose of module
Providing the possibility to set up a database migrations using `Liquibase`

#How
Reacts to command line arguments (See `LiquibaseAutoBindModule` for a complete list)

Scans the classpath for files named `migrations.xml` and then uses `Liquibase` to run all migrations.xml files in an undefined order.
If you need order put your changesets in other files and just list in which order they should be run in one single migrations.xml
See Liquibase documentation for full reference.

# Key classes
`LiquibaseConfig` - the database configuration to use when making migrations. (Also includes good javadocs)
`TimoutLockService` - autoreleases liquibaselocks when unclean shutdowns otherwise could cause migrates to be locked waiting for a lock to be released.


# Extra info
If multiple modules have migrations.xml they will overwrite each other when put into a fatjar like the one suggested in the main README.
To have them merged you need to add a transformer to your maven-shade plugin like this

```
<transformer
implementation="org.apache.maven.plugins.shade.resource.XmlAppendingTransformer">
<resource>migrations.xml</resource>
</transformer>
```


