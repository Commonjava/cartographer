Remaining Design Considerations
-------------------------------

-   Stubbing / infrastructure abstraction for localhost deployment
    -   Aim for ability to switch deployment types with change to
        configuration files (not class-level / dependency changes)
        -   80/20 rule will likely apply here
-   Packaging
    -   WildFly Swarm vs. Propulsor
        -   **Goal:** Provide a standard for service deployment that we
            can evangelize to Middleware / other NOS projects
    -   How to specify collection of Kubernetes pods?
        -   Can we templatize enough to make this useful in a generic
            sense?
            -   Use ConfigMaps to configure the main Consul deployment
                for example?
        -   Need something we can deploy for testing, then re-deploy
            **UNCHANGED** into staging / production
            -   Only changes should be configuration contents
-   Data Persistence: Hibernate OGM + Infinispan HotRod + Protobuf seems
    like a likely solution
    -   Needed for GraphDB / WorkDB data
    -   **Goal:** avoid single point of failure (database instance)
        without complex replication problems
    -   **Goal:** maintain referential integrity in each database / set
        of "tables"
    -   NOTE: Will have to use embedded ISPN on localhost deployment
        -   :question: Different persistence.xml? Maybe just use pre-loading
            substitution?
-   Service Discovery:
    -   Needed for discovery of:
        -   HotRod instances (or other remote data store)
        -   :question: Infinispan cache clustering?
        -   :question: MQ system?
    -   Possibly use Consul
        -   Adds a new dimension of complexity to server-side deployment
    -   Will have to be FACTORED OUT of the localhost deployment
        scenario
        -   Use abstraction to allow stubbing of "service discovery" on
            localhost deployment
        -   Avoid some (all?) issues when using embedded Infinispan
