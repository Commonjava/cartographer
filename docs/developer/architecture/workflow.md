Workflow
--------

Graph discovery and traversal in Cartographer 2.0 is modeled as a series
of processing queues, with each queue designed to be handled by multiple
processor nodes, referenced below as services. A service refers to
a
type of node, and there may be multiple nodes
of any given service type in a Cartographer deployment. Tuning the
specific number of nodes for each service type should allow us to scale
the application.

Fortunately, Apache Camel makes this architecture very easy to achieve
with a message queue, such as ActiveMQ. Each node deploys separately and
listens on a given MQ Topic (allowing pub/sub handling and multiple
nodes), and publishes the result of its processing to another Topic.

Routing information (for determining the publish Topic after processing)
is handled by message headers (@OutHeaders map, in Camel). Messages
themselves consist of WorkId's (also, potentially RequestId, depending
on the route's needs), in order to keep message sizes (and serialization
overhead) to a minimum. Nodes are expected to inject (CDI @Inject)
GraphDB / WorkDB as necessary to perform tasks.

Some routes will be package-specific, allowing different package types
to implement components that know how to handle that package (or work
correctly with the assumptions built into that packaging system). In
these cases, special routes will be used that contain a RecipientList
component. Each of these RecipientList implementations depend on a
PackageInfoRegistry component, which maintains access to a database
(cache / mapping) of PackageInfo implementations, one per package type.
The RecipientList will lookup the package-specific route for the given
PkgId / PkgVersion it's working with, and forward the request to the
appropriate route for that pacakge type. In some cases, there may be
default implementations in case no package-specific route is available
(eg. SelectionSyncronizer, below), but in most cases unknown
package-specific routes will simply get forwarded to an error handler
that will stop the traverse and return the appropriate error message to
the user.

## Route Names & Workflow

The general workflow for a traverse is as follows:

1. Register
2. End-Detect
3. Select-Sync
4. Select
5. Resolve-Sync
6. Resolve
7. Traverse
8. Format

Understanding, of course, that End-Detect occurs at the very beginning of the traversal, then again after each node's Traverse step.

Let's look at these in some greater detail:

### REGISTER

Establish RequestWorkspace in WorkDB, including first
    WorkItems to traverse, with selected PkgVersions set from user
    request
    1.  *NOTE:* This does not happen as a result of a message sent
        through the MQ; it is directly tied to the service that handles
        REST requests.

### END-DETECT

If any of the criteria for stopping traversal are
    met, stop
    1.  If
        any of the following are met, go to
        **8
        / FORMAT**
        :
        1.  If there are no next nodes awaiting processing
        2.  If maximum traversal depth is set on the user request, and
            all next nodes are beyond this maximum depth

    2.  *SPLIT:* Otherwise, for each next node, proceed to **3 /
        SELECT-SYNC**
    3.  Input: WorkId
    4.  Output:
        1.  Direct return: List<WorkId>
        2.  When traverse is complete: \
            1.  `end-detect-result: DONE`
            2.  `end-detect-reason: {MAX_DEPTH_REACHED | TRAVERSE_COMPLETE`}

        3.  When traverse is still in progress:
            1.  `end-detect-result: IN_PROGRESS`

    5.  Effects:
        1.  Return List<WorkId> message sent through built-in Camel
            splitter()
        2.  No effect on input WorkItem

### SELECT-SYNC (also looped in from traversal, below)

    1.  If PkgVersion selected in WorkItem, skip selection
    2.  Defer to SelectionSynchronizer to determine whether to pause the
        selection process

### SELECT

Select PkgVersion given target PkgId, version advice
    from inbound relationship, traversal selection history, and user
    request overrides
    1.  If version exists in selection history map (from user request or
        previous traversal in another path), use that PkgVersion
    2.  Use GraphDB / metadata resolution to find best PkgVersion based
        on version advice from relationship
    3.  Effects on WorkItems:
        1.  Camel @OutHeaders used to route for success / error
        2.  On error, set "error" field in input WorkItem
        3.  Input WorkItem has "selected" field (PkgVersion) set
        4.  Returns WorkId (or possibly null). Allows input WorkId to be
            pushed to next step.

### RESOLVE-SYNC

    1.  If PkgVersion is already marked resolved in the GraphDB, skip
        resolution
    2.  If PkgVersion resolution is in progress, put off this WorkItem
        until later to see if we can avoid attempting resolution
    3.  Otherwise, proceed to resolution

### RESOLVE

Resolve the outbound relationships to other packages
    that are associated with a PkgVersion
    1.  Use a means of retrieving metadata from a remote source
    2.  Parse the metadata into a series of Relationships
    3.  Store the relationships in the GraphDB and mark the PkgVersion
        as resolved
    4.  Effects on WorkItems:
        1.  Camel @OutHeaders used to route for success / error
        2.  On error, set "error" field in input WorkItem
        3.  No other effect on input WorkItem
        4.  Returns WorkId (or possibly null). Allows input WorkId to be
            pushed to next step.

### TRAVERSE

Find the next nodes for traversal given a resolved
    PkgVersion
    1.  Retrieve the outbound relationships associated with a PkgVersion
    2.  Filter them appropriately for the request's traversal scope
        (potentially mutated by traversal history)
    3.  Add next nodes for processing (Next nodes are WorkItems with a
        target PkgId and version advice set from the Relationship) and
        **GOTO 2**
    4.  Effects on WorkItems:
        1.  New WorkItems created for each filtered Relationship, with
            the following initial state:
            1.  new WorkId (input WorkId's RequestID field + generated
                String identifier)
            2.  parent WorkId (this is the input WorkId)
            3.  ordinal (counter of accepted Relationships)
            4.  depth in traverse (parent WorkItem depth +1)
            5.  target for processing (PkgId)
            6.  target version advice from Relationship (String)
            7.  traverse scope (TraverseScope), which is calculated
                based on scope of input WorkItem
            8.  exclusions (Set<PkgId>) which is accumulated from
                parent WorkItem + exclusions set on the Relationship
                from which this new WorkItem was created

        2.  New WorkItems added to RequestWorkspace in pending field
        3.  Camel @OutHeaders used to route for success / error

### FORMAT

Pull all of the WorkItems related to a RequestId, and
    format them according to user request
    1.  *TODO:* It's not clear how we'll specify an output
        format...potentially as a series of pre-configured formats, or
        inlined Groovy (or other spec format) in the user request
    2.  Formatting should allow for path-oriented and node-oriented
        formats
