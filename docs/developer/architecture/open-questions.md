Open Questions
--------------

-   We need a decent way to deal with Maven profiles
    -   Cartographer 1.0 required a list of active profiles in the user
        request (defaulted to all, I think)
    -   Traversal included profiles in the filtering logic
    -   Doing it this way would make the user request contain
        package-specific elements
    -   Doing it this way would make traversal a package-specific
        activity (possibly like SelectionSynchronizer, with a default
        implementation)
-   We need to figure out what's involved with using Hibernate OGM +
    ISPN. \
    -   Hotrod deployment, probably?
    -   Protobuf message formats, probably?
    -   Can Hotrod cluster easily?
-   If we do have to use Hotrod ISPN deployments, can we use another set
    of ISPN caches as L2 cache co-located with service nodes?
    -   Does that make sense to avoid serializing over the wire?
    -   Will that have a significant benefit over protobuf usage?
-   User requests may contain not only concrete PkgId -\> PkgVersion
    overrides
    -   They might contain PkgId -\> version range / version expression
        overrides
    -   We need to handle this in the RequestWorkspace and in the
        NodeSelector.
-   Logging: We need to have a **LOGGING** route in the system that will
    allow us to report things against either a RequestID or a WorkID
    -   Log messages like this should be available in a log for the
        user, even before the traversal / formatting is complete.
-   Log Ranging: We need to allow the user to retrieve a range on the
    log. What will that range refer to...maybe lines?
-   Request Garbage Collection
    -   We will keep state / logs / output related to a RequestID around
        for some time, then we need to garbage collect it.
    -   This will require some form of automatic expiration, probably
        with a reset for user access...so it expires some time after
        last access.
