User Interface
--------------

## Contents

<!-- toc -->

- [Introduction](#introduction)
  * [Traversal Scopes](#traversal-scopes)
  * [Request Statuses](#request-statuses)
  * [REST Resource & Actions](#rest-resource--actions)
  * [Traversal Request Format](#traversal-request-format)

<!-- tocstop -->

## Introduction

Work enters the system via a REST interface, which initializes a
RequestWorkspace (may be virtual depending on WorkDB implementation)
based on the user's request options (should be a REST POST). This
results in a RequestID (currently a String) which is passed back to the
user immediately, allowing them to check progress, grab logs, or
retrieve results (when available) for the request.

This REST interface will have an accompanying Javascript-driven UI and
Swagger documentation.

### Traversal Scopes

Any user request for traversal will have to include a scope to which the
traversal should be constrained. The following scopes are defined:

-   **RUNTIME:** This is anything required for the package to be
    executed (excluding things assumed to be present in the base
    environment before the graph nodes are added)
-   **BUILD_TIME:** This is anything beyond **RUNTIME** that the
    package required in order to build, including build tooling, test
    frameworks / fixtures, etc.
-   **EXTRA:** This is a set of relationships that are completely
    outside of the normal traversal bounds
    -   This will not be a scope that the user can select when
        requesting a traversal
    -   It will be used to capture metadata that we might use someday,
        and which will prevent us from having to re-parse in the event
        we find a use for it (*eg. Maven managed dependencies*)

### Request Statuses

Any traversal will progress through a variety of statuses and end in
either an error state or success. The following statuses are defined:

-   **TRAVERSING:** Traversal (including selection and resolution) is in
    progress
-   **FORMATTING:** Traversal is complete and the result is being
    formatted
-   **ERROR:** Something went wrong, and either traversal or formatting
    could not proceed
-   **SUCCESS:** Both traversal and formatting completed normally (but
    could still have node-level errors)

### REST Resource & Actions

The Traverse REST interface will consist of the following actions:

-   **POST:** Start a new request
-   **GET:** 400 with message saying this doesn't make sense without a
    requestId
-   **GET /{requestId}/status:** Retrieve the status of the request. One
    of: `{TRAVERSING, FORMATTING, ERROR, SUCCESS}`
-   **GET /{requestId}/logs:** Retrieve the log of the current
    traversal. \
    -   *NOTE:* Allow range specification in the HTTP header for this.
-   **GET /{requestId}:** Retrieve the result of the request. \
    -   *NOTE:* This will return 204 No Content with a HTTP header
        '`Traverse-State: {TRAVERSING|FORMATTING}`' if the request is
        not done.
-   **DELETE /{requestId}:** Delete the RequestWorkspace + WorkItems +
    logs + formatted results related to the given RequestID

 

**NOTE:** We will store request output (and intermediate state) until
deleted, up to a point. After that point (*TBD*) we should
garbage-collect the request.

### Traversal Request Format

User requests for graph traversal will be submitted via REST POST, using
a JSON that specifies options for the traversal. That format will look
like this:

```
{
    "workspaceId": "IPreferThisUniqueId",
    "roots": [
        "maven:org.commonjava.indy:indy-core:1.1.8",
        "maven:org.commonjava.cartographer:cartographer-core:2.0-pre1"
    ],
    "scope": "runtime",
    "depth": 3,
    "versions" : {
        "maven:org.commonjava.cdi.util:weft": "1.4.1",
        "maven:javax.servlet:javax.servlet-api":
        "maven:org.jboss.spec.javax.servlet:jboss-servlet-api_3.0_spec:1.0.1"
    },
    "exclusions": [
        "maven:commons-logging:commons-logging"
     ],
    "result": "<TODO-result-format-spec>"
} 
```

Some notes about the above:

-   workspaceId is optional, and will be generated / returned to the
    user
-   GAVs are prefixed with "maven", which gives us namespace support
    where we might be able to specify NPM or RPM coordinates alongside
    maven-specific coordinates (GAVs)
-   depth is optional, and will limit how far a traverse goes. This is
    useful for supporting UIs and things like that, where fully
    traversing the graph may overwhelm the user with irrelevant detail
-   versions is the mapping of user-provided versioning overrides
    -   it may be a mapping to a simple string, in which case the two
        should be joined together to provide the PkgVersion value in the
        RequestWorkspace selectedVersions map
    -   It may direct the traverse to use a completely different
        coordinate instead of the originating PkgId. This is like Maven
        POM relocations, or a way of saying, "if you find this, use this
        other thing instead"
-   exclusions will prevent SUB-GRAPHs from being included. The traverse
    will not proceed beyond these nodes when they're encountered
-   result formatting is a TODO. Options for managing result formats
    include:
    -   ourselves, in Cartographer
    -   via deployment configuration
    -   via separate REST interface (with more auth / admin controls)
