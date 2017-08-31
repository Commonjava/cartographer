Data Storage
------------

There are two data sources in Cartographer - GraphDB and WorkDB.

-   **GraphDB** - This is the store for the structures being traversed;
    packages, their available versions, and the relationships between
    packages, along with resolution status for each.
-   **WorkDB** - This is the store for traversal state, including the
    initial conditions set by the user request and state related to the
    processing of each node in a traverse

Along with storing WorkItems,
the
WorkDB actually serves as the primary interface for interacting with
RequestWorkspaces. Services are expected to
**AVOID** working with RequestWorkspace instances directly. Instead,
they should store and retrieve information related to RequestWorkspace
state **VIA WorkDB**. This offers the opportunity for more efficient
storage and querying of the information contained in the
RequestWorkspace. It also means we can access information within the
workspace without incurring the costs of deserializing / construction
the whole workspace.

Current thinking is that we will use
Hibernate
OGM + Infinispan as the data store. This
offers a few advantages over traditional databases:

-   relatively simple clustering of data storage without complicated
    database replication configuration
-   potentially, co-location of data with services using it (depending
    on Infinispan configuration)
