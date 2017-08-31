# Cartographer 2.0 Design

This series of documents describes the new architecture we'll be using in Cartographer 2.0. While there are still several questions to be answered, and known shortcomings to be handled in the design, these documents should provide a capable scaffold to build on.

## Contents

* [User Interface](user-interface.md) - Describes data formats, REST resources, and general user interactions with the system
* [Data Models](data-models.md) - Describes the data models used within the system for tracking graph structure and traversal progress
* [Data Storage](data-storage.md) - Describes how the data models are to be stored and accessed
* [Workflow](workflow.md) - Describes the steps that will be executed for each graph node during a traverse, along with some "bookend" type steps that happen at the beginning and end of the overall traversal process
* [Services](services.md) - Describes the services that execute the workflow steps, including information on how these might vary with package-specific details, and how we handle routing to package-specific implementations as we move through the steps of the workflow
* [Design TODO](design-todo.md) - Describes known issues that need to be incorporated into the architecture
* [Open Questions](open-questions.md) - List of known questions that need to be resolved into design TODO or answered in some other way, before the architecture can be considered complete



 
