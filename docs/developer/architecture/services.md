Services
--------

## Contents

<!-- toc -->

- [SelectionSynchronizerRecipientList](#selectionsynchronizerrecipientlist)
- [SelectionSynchronizer (Package-Specific)](#selectionsynchronizer-package-specific)
  * [Default Implementation](#default-implementation)
  * [NPM Implementation (Potential)](#npm-implementation-potential)
- [NodeSelectorRecipientList](#nodeselectorrecipientlist)
- [NodeSelector (Package-Specific)](#nodeselector-package-specific)
  * [Maven Implementation](#maven-implementation)
  * [NPM Implementation](#npm-implementation)
- [ResolverSynchronizer](#resolversynchronizer)
- [NodeResolverRecipientList](#noderesolverrecipientlist)
- [NodeResolver (Package-Specific)](#noderesolver-package-specific)
  * [Maven Implementation](#maven-implementation-1)
  * [NPM Implementation](#npm-implementation-1)
- [NodeTraverser](#nodetraverser)
- [TraversalEndDetector](#traversalenddetector)
- [ResultFormatter](#resultformatter)

<!-- tocstop -->

## SelectionSynchronizerRecipientList

This service will listen on the **SELECT-SYNC** route, and is not
package-specific.

It will lookup the PackageInfo instance corresponding to the package
type in a given PkgId, and find the appropriate route for selection
synchronization. If no route is specified for a given package type, it
will route to **SELECT-SYNC-DEFAULT**.

## SelectionSynchronizer (Package-Specific)

### Default Implementation

This implementation will listen on the **SELECT-SYNC-DEFAULT** route,
which will be the default route for the
SelectionSynchronizerRecipientList if the matching PackageInfo doesn't
specify another route. SelectionSynchronizerRecipientList will be
responsible for routing sync requests from the **SELECT-SYNC** route.

The default route for the selection synchronizer will perform the
following logic:

1.  If PkgId is in the process of being selected, put off this WorkItem
    until later to continue selection (when selection history is
    established)
2.  Otherwise, proceed to **SELECT**

### NPM Implementation (Potential)

This implementation will listen on the **SELECT-SYNC-NPM** route, which
will be the value returned to the SelectionSynchronizerRecipientList
from the NPMPackageInfo implementation.
SelectionSynchronizerRecipientList will be responsible for routing sync
requests from the **SELECT-SYNC** route.

This implementation will NEVER avoid selection. Duplicate PkgIds are
allowed in the same traverse, resolved to different PkgVersions.

## NodeSelectorRecipientList

This service will listen on the **SELECT** route, and is not
package-specific.

It will lookup the PackageInfo instance corresponding to the package
type in a given PkgId, and find the appropriate route for selection. If
no corresponding PackageInfo is found, it will set an error on the
WorkItem and route to **ERROR**.

## NodeSelector (Package-Specific)

### Maven Implementation

The GAVSelector listens on the **SELECT-MAVEN** route, and is
package-specific. It relies on NodeSelectorRecipientList to route
selection requests from the **SELECT** route.

Map PkgId to Atlas ProjectRef. Retrieve list of available versions for
the ProjectRef by retrieving maven-metadata.xml file(s) and parsing.
Cache this to avoid hitting the upstream source too often.

### NPM Implementation

This implementation will listen on the **SELECT-NPM** route, and will be
package-specific. It will rely on NodeSelectorRecipientList to route
selection requests from the **SELECT** route.

Map PkgId to a package string, and retrieve / parse the package.json
associated with this package name. This will contain the list of
available versions. Cache this to avoid hitting the upstream source too
often.

## ResolverSynchronizer

The ResolverSynchronizer listens on the **RESOLVE-SYNC** route, and it
not package-specific.

This service will determine whether a given PkgVersion is already
resolved, or in the process of being resolved. If already resolved, it
will route to **TRAVERSE**. If resolution is in progress, it will put
off this WorkItem until later to re-attempt **RESOLVE-SYNC**, when
resolution should be complete and it can skip ahead to **TRAVERSE**.

If neither of the above conditions hold, then it forwards on to the
**RESOLVE** route.

## NodeResolverRecipientList

This service will listen on the **RESOLVE** route, and is not
package-specific.

It will lookup the PackageInfo instance corresponding to the package
type in a given PkgVersion, and find the appropriate route for resolving
it. If no corresponding PackageInfo is found, it will set an error on
the WorkItem and route to **ERROR**.

## NodeResolver (Package-Specific)

### Maven Implementation

The POMResolver listens on the **RESOLVE-MAVEN** route, and is
package-specific. It relies on NodeResolverRecipientList to route
resolution requests from the **RESOLVE** route.

Map PkgVersion to Atlas ProjectVersionRef, then lookup the corresponding
Maven POM file from the source(s) and parse into Atlas
ProjectRelationship instances. Map these to Relationship +
RelationshipMetadata, and store in the GraphDB. Map the Maven dependency
scope to runtime vs. build-time vs. extra (for special cases).

***RUNTIME***-scoped relationships:

-   Concrete (not managed) dependencies
-   Parent
-   BOM (required for parsing the pom.xml at all)

***BUILD_TIME***-scoped relationships:

-   Plugin declarations (not managed)
-   Plugin-level dependencies (not managed)
-   Extensions

***EXTRA***-scoped relationships:

-   Managed dependencies
-   Managed plugins

### NPM Implementation

This implementation will listen on the **RESOLVE-NPM** route, and will
be package-specific. It will rely on NodeResolverRecipientList to route
resolution requests from the **RESOLVE** route.

Map PkgId to a package string, and lookup the cached, parsed object
corresponding to the package.json (from the NodeSelector). Grab all
references to dependencies and turn these into Relationships, then store
them in the GraphDB. All will be scoped to runtime, since there is no
distinction between runtime and build-time in package.json.

## NodeTraverser

The NodeTraverser listens on the **TRAVERSE** route, and is not
package-specific.

This service will retrieve all Relationships whose source is the given
PkgVersion, using the GraphDB. Match the scope against the scope in the
WorkItem to filter for appropriate Relationship instances. For each,
create a new WorkItem, with the parent specified as the current WorkID,
and a TraversalScope that is potentially mutated. New WorkItems are
added to the pending set for the RequestWorkspace (via WorkDB), and the
current PkgVersion is marked as done in the workspace.

If the current scope if build-time, the scope on new WorkItems will be
runtime; if the current scope is runtime, the new scope will be
unchanged. The reason for this becomes apparent when you consider how
packages make use of other, related packages. If some related package B
is a build tool, testing framework, or something similar, then the
build-time environment for current package A only needs to include those
relationships of B that are necessary for it to function; the runtime
relationships. If A has a runtime relationship to B, then anything B
depends on for runtime support will also need to be available to A.

This is not a package-specific behavior; once relationships are classed
as either runtime or build-time, the above logic holds regardless of
package type.

## TraversalEndDetector

The TraversalEndDetector node listens on the **END-DETECT** route, and
it not package-specific.

Once traversal of a node (PkgVersion) is complete, it makes sense to see
whether there any new node traversals in progress or pending. If the
node we just traversed didn't result in any new nodes to traverse, and
no other PkgVersions are in the process of being traversed, then we're
done traversing the graph.

However, one other condition may apply that will halt graph traversal.
If the user specified a maximum traversal depth, and all of the nodes
pending traverse in the workspace have a depth larger than this maximum,
the traverse is done.

If neither of the above conditions applies, the next step is to take the
set of pending nodes and return them as a Splitter output. This means
they will be pushed into the **PRE-SELECT** route for processing and
traversal. If one of the end conditions above does apply, then the next
step is to send a message to the **FORMAT** route.

If the traversal continues, this will ensure the request status is
**TRAVERSING**.

Once the traversal is complete, it will change the request status to
**FORMATTING**.

## ResultFormatter

The ResultFormatter listens on the **FORMAT** route, and is not
package-specific.

This service will pull the WorkItems from the WorkDB that are associated
with a RequestID, along with the format details from the
RequestWorkspace (again, via WorkDB). It will then load and apply the
referenced formatter component to the WorkItems, resulting in a string
output that can be stored and returned to the user.

Once finished, this will change the request status to **SUCCESS**.
