Maven Dependency Traversal with Exclusion
-----------------------------------------

This example will show how exclusions accumulate during the traversal, using a standard Maven dependency exclusion to illustrate the point.

## The POMs

In this example, we have two POMs:

* [jhttpc/pom.xml](jhttpc-pom.xml)
* [httpclient/pom.xml](httpclient-pom.xml)

It's worth noting that these are partial copies of the actual POMs under discussion. I've trimmed out most of the parts that don't affect this example.

As you look at these POMs, it's worth noticing:

* jhttpc declares a dependency on httpclient
* jhttpc's dependency on httpclient excludes commons-logging
	* jhttpc actually uses slf4j in reality, and the two logging frameworks conflict
* httpclient declares a compile-time dependency on commons-logging
	* compile-scoped dependencies are also included in the runtime dependency set for a project

## The Traversal Request

Imaging we submit the following traversal request to Cartographer 2.0:

```
{
	"roots": [
        "maven:org.commonjava.util:jhttpc:1.6"
    ],
    "scope": "runtime",
	"result": "some-format-name"
} 
```
This request directs Cartographer to start traversing with the jhttpc POM, resolve the runtime-scoped relationships, and run the results through some format called `some-format-name`.

Cartographer will execute the following steps:

1. Initialize a RequestWorkspace with a generated RequestId
   a. Create a new WorkItem (with generated WorkId) with selected PkgVersion: "maven:org.commonjava.util:jhttpc:1.6"
   b. WorkItem fields `target` and `targetVersionAdvice` can be filled from the supplied PkgVersion
   c. This WorkItem is a special case, since the user provided a concrete PkgVersion in the request.
   d. Add the new WorkItem to the WorkDB and its WorkId to the RequestWorkspace `pending` field, via WorkDB
2. Submit a new message with the RequestId as payload to the END-DETECT route in Cartographer's service bus
3. Return the requestId to the user, along with HTTP headers providing URLs to check progress, retrieve logs, and retrieve results
4. END-DETECT checks the RequestWorkspace and sees that the `pending` field contains WorkIds.
	a. It moves the `pending` WorkIds into the `current` set in the workspace.
	b. It returns the former contents of the pending Set{WorkId:<jhttpc>} to the END-DETECT route.
5. The END-DETECT route splits the set of WorkIds from the EndDetector and sends them to the SELECT-SYNC route. This results in a single WorkId, for jhttpc, being passed on.
6. The SelectionSynchronizerRecipientList looks at the jhttpc WorkItem, notices it's a `maven` package type (from the prefix of PkgId and PkgVersion string representations)
	a. It finds a PackageInfo keyed to `maven` in the PackageInfoRegistry, and finds that it returns null for getSelectionSynchronizerRoute().
	b. It returns the default selection synchronizer route DEFAULT-SELECTION-SYNC
7. The DefaultSelectionSynchronizer notices jhttpc has already been selected (from the request initialization), so it:
	a. Registers jhttpc 1.6 in the selectedVersions map in the RequestWorkspace
	b. Forwards the WorkId on to the RESOLVE-SYNC route.
8. The ResolveSynchronizer notices jhttpc isn't being resolved yet, and forwards the WorkId to RESOLVE.
9. The NodeResolverRecipientList looks at the jhttpc WorkItem, notices it's a `maven` package type (from the prefix of PkgId and PkgVersion string representations)
	a. It finds a PackageInfo keyed to `maven` in the PackageInfoRegistry, and returns the result of getResolverRoute(), which is `resolve-maven`.
10. POMResolver, listening on `resolve-maven`, retrieves org/commonjava/util/jhttpc/1.6/jhttpc-1.6.pom from a remote Maven repository that was configured in the Cartographer config files.
	a. It parses the POM and constructs Relationship instances with the following information from the POM:
	    * Targeting PkgId: "maven:org.commonjava:commonjava", targetVersionAdvice: "12"
	   		* Annotation: maven:reltype -> parent
	   		* Annotation: maven:artifactType -> pom
	   		* Annotation: maven:scope -> implied:runtime
	   		* Scope: runtime
	   	* Targeting PkgId: "maven:org.apache.httpcomponents:httpclient", targetVersionAdvice: "4.5.3"
	   		* Annotation: maven:reltype -> dependency
	   		* Annotation: maven:artifactType -> jar
	   		* Annotation: maven:scope -> native:compile
	   		* Exclusions: Set{PkgId:"maven:commons-logging:commons-logging"}
	   		* Scope: runtime
	b. It stores these relationships in the GraphDB
	c. It marks PkgVersion: "maven:org.commonjava.util:jhttpc:1.6" as resolved
	d. It passes the jhttpc WorkId on to the TRAVERSE route.
11. NodeTraverser, listening on TRAVERSE, pulls these newly created relationships from the GraphDB, then:
	a. Filters for scope: runtime
	b. Notices that all relationships are available in the runtime scope
	c. Creates WorkItems for each relationship, with the following info:
		* Generated WorkId for commonjava parent POM:
			* parent: jhttpc WorkId
			* ordinal: 0
			* depth: 1 (jhttpc depth was 0)
			* target PkgId: "maven:org.commonjava:commonjava"
			* targetVersionAdvice: "12"
			* scope: runtime
		* Generated WorkId for httpclient dependency jar:
			* parent: jhttpc WorkId
			* ordinal: 1
			* depth: 1 (jhttpc depth was 0)
			* target PkgId: "maven:org.apache.httpcomponents:httpclient"
			* targetVersionAdvice: "4.5.3"
			* scope: runtime
			* exclusions: Set{PkgId:"maven:commons-logging:commons-logging"}
	d. Adds the new WorkItems to the WorkDB, then adds the WorkIds to the RequestWorkspace's `pending` set, via WorkDB
	e. Moves jhttpc WorkId from the `current` set to the `done` set in RequestWorkspace 
	f. Passes the RequestId on to the END-DETECT route.
12. END-DETECT checks the RequestWorkspace and sees that the `pending` field contains WorkIds.
	a. It moves the `pending` WorkIds into the `current` set in the workspace.
	b. It returns the former contents of the pending set to the END-DETECT route.
13. The END-DETECT route splits the set of WorkIds from the EndDetector and sends them to the SELECT-SYNC route. This results in a two WorkIds being passed on. *For clarity, we'll ignore the parent pom and watch httpclient.*
14. The SelectionSynchronizerRecipientList looks at the httpclient WorkItem, notices it's a `maven` package type (from the prefix of PkgId and PkgVersion string representations)
	a. It finds a PackageInfo keyed to `maven` in the PackageInfoRegistry, and finds that it returns null for getSelectionSynchronizerRoute().
	b. It returns the default selection synchronizer route DEFAULT-SELECTION-SYNC
15. The DefaultSelectionSynchronizer notices httpclient has NOT been selected yet, so it forwards the WorkId to the SELECT route.
16. The NodeSelectorRecipientList looks at the httpclient WorkItem, notices it's a `maven` package type (from the prefix of PkgId and PkgVersion string representations)
	a. It finds a PackageInfo keyed to `maven` in the PackageInfoRegistry, and returns the result of getSelectorRoute(), which is `select-maven`.
17. GAVSelector, listening on `select-maven`, does the following:
	a. Looks at the RequestWorkspace and notices it has no entry for httpclient yet
	b. Selects the version given by the relationship
	c. Sets the PkgVersion: "maven:org.apache.httpcomponents:httpclient:4.5.3" in the `selected` field of the WorkItem
	d. Forwards the httpclient WorkId on to the RESOLVE-SYNC route.
18. The ResolveSynchronizer notices httpclient isn't being resolved yet, and forwards the WorkId to RESOLVE.
19. The NodeResolverRecipientList looks at the httpclient WorkItem, notices it's a `maven` package type (from the prefix of PkgId and PkgVersion string representations)
	a. It finds a PackageInfo keyed to `maven` in the PackageInfoRegistry, and returns the result of getResolverRoute(), which is `resolve-maven`.
20. POMResolver, listening on `resolve-maven`, retrieves `org/apache/httpcomponents/httpclient/4.5.3/httpclient-4.5.3.pom` from a remote Maven repository that was configured in the Cartographer config files.
	a. It parses the POM and constructs Relationship instances with the following information from the POM:
	    * Targeting PkgId: "maven:org.apache.httpcomponents:httpcomponents-client", targetVersionAdvice: "4.5.3"
	   		* Annotation: maven:reltype -> parent
	   		* Annotation: maven:artifactType -> pom
	   		* Annotation: maven:scope -> implied:runtime
	   		* Scope: runtime
	   	* Targeting PkgId: "maven:commons-logging:commons-logging", targetVersionAdvice: "1.2"
	   		* Annotation: maven:reltype -> dependency
	   		* Annotation: maven:artifactType -> jar
	   		* Annotation: maven:scope -> native:compile
	   		* Scope: runtime
	   	* *NOTE:* We're ignoring other relationships for simplicity
	b. It stores these relationships in the GraphDB
	c. It marks PkgVersion: "maven:org.apache.httpcomponents:httpclient:4.5.3" as resolved
	d. It passes the httpclient WorkId on to the TRAVERSE route.
21. NodeTraverser, listening on TRAVERSE, pulls these newly created relationships from the GraphDB, then:
	a. Filters for scope: runtime
	b. Notices that all relationships are available in the runtime scope
	c. Notices that one relationship is *excluded* by the httpclient WorkItem
	d. Creates WorkItem for the parent relationship, with the following info:
		* Generated WorkId for commonjava parent POM:
			* parent: httpclient WorkId
			* ordinal: 0
			* depth: 2 (httpclient depth was 1)
			* target PkgId: "maven:org.apache.httpcomponents:httpcomponents-client"
			* targetVersionAdvice: "4.5.3"
			* scope: runtime
	e. Adds the new WorkItems to the WorkDB, then adds the WorkIds to the RequestWorkspace's `pending` set, via WorkDB
	f. Moves httpclient WorkId from the `current` set to the `done` set in RequestWorkspace 
	g. Passes the RequestId on to the END-DETECT route.
22. END-DETECT checks the RequestWorkspace and sees that the `pending` field contains WorkIds.
	a. It moves the `pending` WorkIds into the `current` set in the workspace.
	b. It returns the former contents of the pending set to the END-DETECT route.
23. *...*

We won't follow the process beyond this, since the main point is to see how the exclusion resolved in *step 10.a* gets folded into the WorkItem in *step 11.c*, which then changes how httpclient relationships get traversed in *step 21.c*.
