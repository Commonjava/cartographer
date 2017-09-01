Workflow
--------

## Contents

<!-- toc -->
- [Introduction](#introduction)
- [Route Names & Workflow](#route-names--workflow)
  * [REGISTER](#register)
    + [Data Flow](#data-flow)
    + [Routing Notes](#routing-notes)
  * [END-DETECT](#end-detect)
    + [Logic](#logic)
    + [Data Flow](#data-flow-1)
  * [SELECT-SYNC (Package-Specific with Default)](#select-sync-package-specific-with-default)
    + [Logic](#logic-1)
    + [Package-Specific Variation](#package-specific-variation)
    + [WARNING: Potential Race Condition!](#warning-potential-race-condition)
    + [Data Flow](#data-flow-2)
  * [SELECT (Package-Specific)](#select-package-specific)
    + [Logic (Most Implementations)](#logic-most-implementations)
    + [Data Flow](#data-flow-3)
  * [RESOLVE-SYNC](#resolve-sync)
    + [WARNING: Synchronization Desired Here!](#warning-synchronization-desired-here)
    + [Logic](#logic-2)
    + [Data Flow](#data-flow-4)
  * [RESOLVE (Package-Specific)](#resolve-package-specific)
    + [Logic](#logic-3)
    + [Scoping Relationships](#scoping-relationships)
    + [Data Flow](#data-flow-5)
  * [TRAVERSE](#traverse)
    + [Logic](#logic-4)
    + [New WorkItem Creation](#new-workitem-creation)
    + [Scope Manipulations](#scope-manipulations)
    + [Data Flow](#data-flow-6)
  * [FORMAT](#format)
    + [Notes](#notes)

<!-- tocstop -->

## Introduction

Graph discovery and traversal in Cartographer 2.0 is modeled as a series
of processing queues, with each queue designed to be handled by multiple
processor nodes, referenced below as services. A service refers to a type of node, and there may be multiple nodes
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

Establish RequestWorkspace in WorkDB, including first WorkItems to traverse, with selected PkgVersions set from user request

**NOTE:** This does not happen as a result of a message sent through the MQ; it is directly tied to the service that handles REST requests.

#### Data Flow

* **Input:** UserRequest (from [User Interface](user-interface.md))
* **Output:** RequestWorkspace, with RequestId and initial `pending` WorkItems populated

#### Routing Notes

* After this step, we will respond to the user via the REST interface, with the RequestId and a series of HTTP Location-style headers / Rel links to progress, 	results, etc.
* This step will translate to the **END-DETECT** step next, which will see the initial work items in the `pending` field and start the traversal process.

### END-DETECT

If any of the criteria for stopping traversal are met, stop. Otherwise, if the RequestWorkspace contains WorkItems in the `pending` field, and those items DO NOT exceed a max-depth setting in the RequestWorkspace (if set), then continue the traversal. This workflow step is responsible for determining whether the traversal is done.

#### Logic

1. If any of the following are met, go to the **FORMAT** workflow step
    - If there are no next nodes awaiting processing
    - If maximum traversal depth is set on the user request, and
        all next nodes are beyond this maximum depth
2.  **SPLIT:** Otherwise, for each next node, proceed to the **SELECT-SYNC** workflow step

#### Data Flow

* **Input:** WorkId
* *Output:*
    1.  Direct return: List<WorkId>
    2.  @OutHeaders Map entries.
        1. When traverse is complete:
	        * `end-detect-result: DONE`
    	    * `end-detect-reason: {MAX_DEPTH_REACHED | TRAVERSE_COMPLETE`}
    	2.  When traverse is still in progress:
        	* `end-detect-result: IN_PROGRESS`
* *Workflow Effects:*
    1.  Return List<WorkId> message sent through built-in Camel route splitter()
    2.  No effect on input WorkItem

### SELECT-SYNC (Package-Specific with Default)

This step is responsible for determining whether a PkgId is already being selected for a given traversal (RequestWorkspace). Selection means taking a PkgId and all of the available version information (initial user value, selection history, and version advice from the relationship), and resolving that to a PkgVersion. 

**NOTE:** It's possible for the selection result to reference a PkgVersion that's related to a *completely different PkgId*. This is possible in systems like Maven, where a given POM file can specify a relocation to a new GroupID/ArtifactID/Version. When doing traversals, we have to honor relocations and aliases like this in order to preserve consistency with native packaging tools. 

#### Logic

There are three possible states for a given PkgId:

1. `UNKNOWN` - This PkgId hasn't been selected, and isn't in the process of being selected. In this case, selection should proceed with the given WorkItem.
2. `SELECTING` - Selection for this PkgId is in progress. Depending on the package-specific selector in use, we might want the current WorkItem to wait a little while and check back in to see if it's done.
3. `DONE` - Selection has already happened on this PkgId during this traversal. Depending on the package-specific selector in use, we might want to use that version instead of doing selectoin over again.

This step will check the map of selected PkgVersions, along with a synchronization data structure that holds the list of things currently being selected. Both of these will be keyed to the RequestWorkspace. If a PkgId is in the mapping of selected versions, the result should be `DONE`. If a PkgId is in the selecting synchronization set, the result should be `SELECTING`. Otherwise, this workflow step knows nothing about this PkgId, and will return `UNKNNOWN`.

If the result is `DONE` the WorkItem should have its `selected` PkgVersion field populated. An additional header will also be set to help with routing in the **SELECT** step. See below for more information.

#### Package-Specific Variation

In some packaging systems like NPM, any number of versions of a single PkgId can co-exist in the same traversal result. For systems like this, it makes sense to look for user overrides and then just accept whatever version was in the target version advice from the relationship. Also, in systems like this, there's no reason to try to synchronize selection of PkgId across the RequestWorkspace, since they're allowed to traverse freely without de-duplication.

#### WARNING: Potential Race Condition!

If we return `UNKNOWN` here, we have to assume the next step is selection. In order to avoid a race condition between **SELECT-SYNC** and **SELECT**, we need to synchronize this step on the PkgId somehow, and critical, *add the PkgId to the synchronization set here* in anticipation of selection (in the next step). Otherwise, we may cross this check with a state of `UNKNOWN` and proceed to the `SELECT` step with multiple WorkItems referencing the same PkgId, at the same time. If that happens, it's nondeterministic which selected PkgVersion will ultimately be stored in the version selection map, and we may end up with multiple versions of a PkgId present in the traverse result.

#### Data Flow

* **Input:** WorkId
* *Output:*
	1. Direct return: WorkId (unchanged)
	2. @OutHeaders Map entries:
		1. When `UNKNOWN`, add `select-status: UNKNOWN`
		2. When `SELECTING`, add `select-status: SELECTING`
		3. When `DONE`:
			* `select-status: DONE`
			* `selection-result: AVOIDED`
* *Effects:*
    1.  If `selected` PkgVersion field is set in WorkItem, skip selection. This happens when the traverse starts, since the user normally sets the roots for traversal concretely. It can also happen anytime another part of the current traverse makes a selection for this PkgId (depending on package-specific details).
    2.  Defer to package-specific SelectionSynchronizer implementation (see above for explanation) to determine whether to:
    	* Proceed with the PkgVersion selection process
    	* Pause the selection process for this WorkItem

### SELECT (Package-Specific)

Select a PkgVersion for the original target PkgId, some version advice from the inbound relationship, existing selection history, and user-requested overrides. This will involve using package-specific metadata to determine what version to select, especially when there is no selection history or user override, and the version advice is non-concrete (an expression of some sort).

#### Logic (Most Implementations)

1.  If version exists in selection history map (from user request or previous traversal in another path), use that PkgVersion
2.  Use GraphDB / metadata resolution to find best PkgVersion based on version advice from relationship

#### Data Flow

* **Inputs:** WorkId
* *Output:*
	1. Direct return: WorkId (unchanged)
	2. @OutHeaders Map entries:
		1. If selection succeeds: `selection-result: DONE`
		2. If selection failed: `selection-result: FAILED`
* *Effects:*
    *  Camel @OutHeaders used to route for success / error
    *  On error, set "error" field in WorkItem
    *  On success, set WorkItem `selected` field (PkgVersion)

### RESOLVE-SYNC

This step attempts to prevent duplication of effort when it comes to resolving the relationships declared by some PkgVersion's accompanying metadata. Because the process involves downloading and parsing metadata, it can be faily expensive to execute. Also, once downloaded, parsed, and stored, there should be no need to do this ever again.

**NOTE:** This step will synchronize activities across active traversals.

#### WARNING: Synchronization Desired Here!

If we return `UNRESOLVED` here, we have to assume the next step is to resolve the PkgVersion. In order to avoid a race condition between **RESOLVE-SYNC** and **RESOLVE**, we need to synchronize this step on the PkgVersion somehow, and critically, *add the PkgVersion to the synchronization set here* in anticipation of resolution (in the next step). Otherwise, we may cross this check with a state of `UNRESOLVED` and proceed to the `RESOLVE` step with multiple WorkItems referencing the same PkgVersion, possibly in different traversals, at the same time. If that happens, we risk wasting resources resolving the same relationship set more than once, potentially many times depending on load.

*Again, these resolution steps should be synchronized on PkgVersion across all traversals on the system.*

#### Logic

1.  If PkgVersion is already marked resolved in the GraphDB, skip resolution
2.  If PkgVersion resolution is in progress, put off this WorkItem until later to see if we can avoid attempting resolution
3.  Otherwise, proceed to resolution

#### Data Flow

* **Inputs:** WorkId
* *Output:*
	1. Direct return: WorkId (unchanged)
	2. @OutHeaders Map entries:
		* If the GraphDB already contains this PkgVersion in a `resolved` state:
			* `resolve-status: DONE`
			* `resolution-result: AVOIDED`
		* If the WorkDB contains an entry in its synchronization set for the current PkgVersion, `resolve-status: RESOLVING`
		* Otherwise, the PkgVersion isn't resolved. Set `resolve-status: UNRESOLVED`

### RESOLVE (Package-Specific)

Resolve the outbound relationships to other packages that are associated with a PkgVersion. This will involve downloading and parsing metadata related to the PkgVersion, and storing the resulting Relationship instances (along with annotations, potentially) in the GraphDB.

#### Logic

1.  Use a means of retrieving metadata from a remote source
2.  Parse the metadata into a series of Relationships
3.  Store the relationships in the GraphDB and mark the PkgVersion as resolved in that DB

#### Scoping Relationships

In package types where scope doesn't really exist as a concept, everything should be set to `RUNTIME`.

#### Data Flow

* **Inputs:** WorkId
* *Output:*
	1. Direct return: WorkId (unchanged)
	2. @OutHeaders Map entries:
		* If the PkgVersion metadata could not be found, `resolution-result: FAILED`
		* If there was an exception while processing the metadata,`resolution-result: ERROR`
		* If the PkgVersion metadata was parsed and stored in the GraphDB as a set of relatioships: `resolution-result: DONE`
* *Effects:*
    *  Camel @OutHeaders used to route for success / error
    *  On error, set "error" field in WorkItem

### TRAVERSE

Find the next nodes for traversal given a resolved PkgVersion. 

#### Logic 

This involves looking up outbound relationships in the GraphDB (relationships where the source is the PkgVersion given by the `selected` field in the current WorkItem), then filtering them based on scopes that match the one given in the WorkItem. Finally, after filtering the Relationship objects, the remaining instances should be used to create a new set of WorkItems that will be added to the `pending` set in the current RequestWorkspace.

Specifically:

1.  Retrieve the outbound relationships associated with a PkgVersion
2.  Filter out Relationships we don't want to traverse:
	* Using traversal scope. If the WorkItem has scope `BUILD_TIME`, then allow `BUILD_TIME` and `RUNTIME`. If the scope is `RUNTIME`, the only allow `RUNTIME` scoped relationships. **NOTE:** We currently only store `EXTRA` scoped relationships, and have no current plan to use them. We want to capture them to avoid having to reparse if we do decide to use them.
	* Using exclusions: If a relationship targets a PkgId that's in the current WorkItem's exclusions set, we don't want to traverse it. 
3.  Create a new WorkItem for each filtered relationship, using the initial state given below
4.  Add new WorkItems to the `pending` field in the current RequestWorkspace (via WorkDB methods)

#### New WorkItem Creation

WorkItems that represent the next nodes to traverse from the current one will be created with a mixture of information from the current WorkItem and the Relationship with which they are associated. The initial state will include:

* `parent` WorkId (this establishes a path through the graph to the current WorkItem)
* `ordinal` (the index into the order of relationships as parsed from the current PkgVersion's metadata during resolution)
* `depth` (incremented from current WorkItem's depth)
* `target` PkgId which is the target of the Relationship this WorkItem represents
* `targetVersionAdvice` String representing the version expression declared in the relationship, which will be used during PkgVersion selection later
* `scope` TraverseScope based on the scope of the parent. When parent scope is BUILD_TIME, this new scope will be RUNTIME. Otherwise, it remains unchanged from the parent.
* `exclusions` Accumulated set of PkgId's to exclude from traversal. This accumulates as we run each branch of the traversal, based on parent exclusions and exclusions listed in the current Relationship.

#### Scope Manipulations

It's possible that our use of TraversalScope mutation in this step isn't correct for certain package types. However, based on my current understanding of scopes (where scopes are even used), it's a pretty good set of assumptions. In package types where scope doesn't really exist as a concept, everything should be set to `RUNTIME`.

#### Data Flow

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

Pull all of the WorkItems related to a RequestId, and format them according to user request

#### Notes

1.  **TODO:** It's not clear how we'll specify an output format...potentially as a series of pre-configured formats, or inlined Groovy (or other spec format) in the user request
2.  Formatting should allow for path-oriented and node-oriented formats
