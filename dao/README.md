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

Use `DaoTransactions` any time that you need to run more than one CUD-operation, not only for transactional benefits but also for performance. Running multiple single queries (even selects) should be done using concatMap instead of flatMap, as the concurrency would otherwise saturate the connectionpool and block other requests from reaching the database. Running queries with concat will be very fast even if they are not concurrent, as one single connection will be reused and the associated caches in the database backend will often benefit from this.

Transactions in RW are special because they do not allow the developer to block the database-connection, they are "one-shot". Therefore, in order to design a parent-relation structure with foreign-keys, you need to either use UUIDs to generate all keys in code, allowing you to add all inserts to a single one-shot transaction, or if you need an ordered key, make a select to determine the next key and use the optimistic-locking pattern to let the database block concurrent inserts by failing on primary key conflict, which can be easily retried. 



