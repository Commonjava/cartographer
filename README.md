# Dependency Graph Discovery for Maven-ish Artifacts

Cartographer builds on the [Atlas](https://github.com/Commonjava/atlas) artifact identity and graph database APIs, adding facilities for graph discovery and querying / traversal.

## Developer Info

### How to Write a New Functional Test

Functional tests in Cartographer are defined as part of its TCK (in the `tck` module). This is because Cartographer exposes several interfaces that form a sort of SPI which could be used as integration points into a larger application. A perfect example of this is the Depgrapher add-on for [AProx](http://commonjava.github.io/aprox/), which provides AProx-specific implementations for interfaces that resolve graph source URIs and download content for graph discovery and traversal. The TCK itself defines an interface called `CartoTCKDriver`, which is intended to provide the context-specific mechanisms for some of the setup tasks required to run the TCK tests.

The `ftests` module provides a standalone implementation of `CartoTCKDriver`, so the tests can be run without embedding Cartographer in another application.

#### Anatomy

Each test in the TCK consists of a filesytem directory structure under `src/main/resources` and a JUnit test (in `src/main/java`). The directory structure looks like this:

		graphs/
		└── simple-dep
		    ├── dto
		    │   └── pom.json
		    ├── output
		    │   └── deps.txt
		    └── repo
		        └── org
		            ├── bar
		            │   └── dep
		            │       └── 1.1
		            │           ├── dep-1.1.jar
		            │           └── dep-1.1.pom
		            └── foo
		                └── consumer
		                    └── 1
		                        ├── consumer-1.jar
		                        └── consumer-1.pom

The corresponding JUnit test looks like this:

		package org.commonjava.maven.cartographer.ftest;

		import org.apache.maven.model.Model;
		import org.commonjava.maven.cartographer.dto.PomRecipe;
		import org.junit.Test;

		public class SimpleProjectWithOneDepDownloadTest
		    extends AbstractCartographerTCK
		{

		    private static final String PROJECT = "simple-dep";

		    @Test
		    public void run()
		        throws Exception
		    {
		        final String dto = "pom.json";
		        final String depsTxt = "deps.txt";
		        final String repoResource = "/repo/org/foo/consumer/1/consumer-1.pom";
		        final int repoResourceTrim = 5;
		        final String alias = "test";

		        aliasRepo( alias, repoResource, repoResourceTrim );

		        final PomRecipe recipe = readRecipe( dto, PomRecipe.class );

		        final Model pom = carto.getRenderer()
		                               .generatePOM( recipe );

		        assertPomDeps( pom, false, depsTxt );
		    }

		    @Override
		    protected String getTestDir()
		    {
		        return PROJECT;
		    }

		}


**NOTE:** Why `src/main/...`? Remember, this module provides the tests without a driver. To run, the module will have to be listed as a dependency in another project/module that provides the driver implementation. This other project/module will also have to provide Surefire configuration to scan the TCK module for tests to run.

Some more information about the key parts of each TCK test:

* **The JUnit test class.** These are defined with one test method per class, to allow for the best possible parallelization in Surefire when the Maven build runs. Because of this, the test class name becomes a sort of descriptor for the test itself, which means the method name itself can be something simple like `run()`. This approach also ensures the output files in `target/surefire-reports` are associated 1:1 with each test.
* **The DTO JSON File.** This is a JSON representation of a recipe which would be passed into a given Cartographer ops class (eg. `ResolveOps.resolve(..)`). I have been using the key `test` for the source location, then aliasing this key to the actual repository location (based on the classpath). Eventually, we may need to introduce further aliases for excluded source locations or tests that use multiple locations.
* **The Repository.** Graph discovery depends on resolving POMs from a repository. However, we don't have to resolve from a HTTP repository; Galley (the tranport API) provides transports that work with file, jar, and zip locations (that is, paths within a given zip/jar file). Using this, we can setup an alias for eg. the `test` repository to the classpath location of the relevant `repo/` resource pathParts.
* **The Expected Output.** In the above example, we're providing a simple listing of `<groupId>:<artifactId>:<version>:<type>[:<classifier>]` that should be found in the generated POM. We will use helper methods to read these files and apply the appropriate assertions to the output.

The JUnit class can't be parameterized easily (except possibly as categories of operations) because we need to provide logic as to which operations should be tested and how to assert the results.