# Purpose of module
Provide a way to interact with the database.

#How
Interact with the db configured in the configuration file by creating java interfaces where the methods are annotated with either `@Query` or `@Update` annotation.
No implementation of the java interface is necessary and still the interfaces are possible to `@Inject` and used where needed.

There is also transactional support through the `DaoTransactions` interface.

# Key classes
`DaoClassScanner` - scans classpath for classes having methods annotated with `@Query` or `@Update`

`DatabaseConfig` - the configuration to be used when connecting to database.

`DbProxy` - the invocation handler to take care of the calls to the db interfaces.

`ParameterizedQuery` - a representation of the statement that will run against the db. Capable of creating PreparedStatement to be used in the jdbc connection.

`DaoTransactions` - a way to execute db-statements in an isolated transaction where _all_ statements must succeed to be committed

`@Query` - the annotation denoting a method should run a select statement.

`@Update` - the annotation denoting a method should run an insert/update/delete statement.

# Extra important info regarding this module
We don't support backpressure from the db. This is by design! 
Allowing backpressure and having a slow subscriber will empty the connection pool in no time.


