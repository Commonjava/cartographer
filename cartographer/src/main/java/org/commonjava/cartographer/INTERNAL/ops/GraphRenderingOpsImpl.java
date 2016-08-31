/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.cartographer.INTERNAL.ops;

import org.apache.maven.model.*;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.graph.GraphResolver;
import org.commonjava.cartographer.graph.RecipeResolver;
import org.commonjava.cartographer.graph.agg.ProjectRefCollection;
import org.commonjava.cartographer.graph.fn.MultiGraphAllInput;
import org.commonjava.cartographer.graph.fn.MultiGraphAllInputSelector;
import org.commonjava.cartographer.graph.fn.MultiGraphFunction;
import org.commonjava.cartographer.ops.GraphRenderingOps;
import org.commonjava.cartographer.ops.ResolveOps;
import org.commonjava.cartographer.request.GraphDescription;
import org.commonjava.cartographer.request.MultiRenderRequest;
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.print.DependencyTreeRelationshipPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.ListPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.StructureRelationshipPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.TreePrinter;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.*;
import org.commonjava.maven.atlas.ident.version.CompoundVersionSpec;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.PrintWriter;
import java.util.*;

import static org.commonjava.cartographer.INTERNAL.graph.agg.AggregationUtils.collectProjectReferences;

@ApplicationScoped
public class GraphRenderingOpsImpl
                implements GraphRenderingOps
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResolveOps resolveOps;

    @Inject
    private GraphResolver resolver;

    @Inject
    private LocationExpander locationExpander;

    @Inject
    private RecipeResolver recipeResolver;

    protected GraphRenderingOpsImpl()
    {
    }

    public GraphRenderingOpsImpl( final ResolveOps resolveOps, final GraphResolver resolver,
                                  final LocationExpander locationExpander, final RecipeResolver dtoResolver )
    {
        this.resolveOps = resolveOps;
        this.resolver = resolver;
        this.locationExpander = locationExpander;
        this.recipeResolver = dtoResolver;
    }

    @Override
    public void depTree( final RepositoryContentRequest recipe, final boolean collapseTransitives, final PrintWriter writer )
                    throws CartoDataException, CartoRequestException
    {
        depTree( recipe, collapseTransitives, new DependencyTreeRelationshipPrinter(), writer );
    }

    @Override
    public void depTree( final RepositoryContentRequest recipe, final boolean collapseTransitives,
                         final StructureRelationshipPrinter relPrinter, final PrintWriter writer )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        if ( recipe == null )
        {
            return;
        }

        final MultiGraphFunction<MultiGraphAllInput> extractor = ( input, graphMap ) -> {
            final Map<ProjectVersionRef, List<ProjectRelationship<?, ?>>> byDeclaring =
                            RelationshipUtils.mapByDeclaring( input.getAllRelationships() );

            final Map<String, Set<ProjectVersionRef>> labels = new HashMap<>();
            for ( final GraphDescription desc : graphMap.keySet() )
            {
                final RelationshipGraph graph = graphMap.get( desc );
                final Map<String, Set<ProjectVersionRef>> graphLabels =
                                getLabels( graph, input.getRoots(), new HashMap<>() );
                for ( final String label : graphLabels.keySet() )
                {
                    Set<ProjectVersionRef> allProjects = labels.get( label );
                    if ( allProjects == null )
                    {
                        allProjects = new HashSet<>();
                        labels.put( label, allProjects );
                    }

                    allProjects.addAll( graphLabels.get( label ) );
                }
            }

            // TODO: Reinstate transitive collapse IF we can find a way to make output consistent.
            final TreePrinter printer = new TreePrinter( relPrinter );
            for ( final ProjectVersionRef root : input.getRoots() )
            {
                writer.printf( "Dependency tree for '%s':\n", root );
                printer.printStructure( root, byDeclaring, labels, writer );
                writer.println();
            }
        };

        resolver.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, recipe, new MultiGraphAllInputSelector(), extractor );
    }

    private Map<String, Set<ProjectVersionRef>> getLabels( final RelationshipGraph allWs,
                                                           final Set<ProjectVersionRef> roots,
                                                           Map<String, Set<ProjectVersionRef>> labels )
    {
        if ( labels == null )
        {
            labels = new HashMap<>();
        }

        labels.put( "ROOT", roots );

        labels.put( "NOT-RESOLVED", allWs.getIncompleteSubgraphs() );

        labels.put( "VARIABLE", allWs.getVariableSubgraphs() );

        return labels;
    }

    @Override
    public void depList( final RepositoryContentRequest recipe, final PrintWriter writer )
                    throws CartoDataException, CartoRequestException
    {
        depList( recipe, new DependencyTreeRelationshipPrinter(), writer );
    }

    @Override
    public void depList( final RepositoryContentRequest recipe, final StructureRelationshipPrinter relPrinter,
                         final PrintWriter writer )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        if ( recipe == null )
        {
            return;
        }

        final MultiGraphFunction<MultiGraphAllInput> extractor = ( input, graphMap ) -> {
            final Map<ProjectVersionRef, List<ProjectRelationship<?, ?>>> byDeclaring =
                            RelationshipUtils.mapByDeclaring( input.getAllRelationships() );

            final Map<String, Set<ProjectVersionRef>> labels = new HashMap<>();
            for ( final GraphDescription desc : graphMap.keySet() )
            {
                final RelationshipGraph graph = graphMap.get( desc );
                final Map<String, Set<ProjectVersionRef>> graphLabels =
                                getLabels( graph, input.getRoots(), new HashMap<>() );
                for ( final String label : graphLabels.keySet() )
                {
                    Set<ProjectVersionRef> allProjects = labels.get( label );
                    if ( allProjects == null )
                    {
                        allProjects = new HashSet<>();
                        labels.put( label, allProjects );
                    }

                    allProjects.addAll( graphLabels.get( label ) );
                }
            }

            final ListPrinter listPrinter = new ListPrinter( relPrinter );
            for ( final ProjectVersionRef root : input.getRoots() )
            {
                writer.printf( "Dependency list for '%s':\n", root );
                listPrinter.printStructure( root, byDeclaring, labels, writer );
            }
        };

        resolver.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, recipe, new MultiGraphAllInputSelector(), extractor );
    }

    @Override
    public Model generatePOM( final PomRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        recipeResolver.resolve( recipe );

        if ( recipe == null )
        {
            return null;
        }

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolved =
                        resolveOps.resolveRepositoryContents( recipe );

        final Map<ProjectRef, ProjectRefCollection> projects = collectProjectReferences( resolved );

        final ProjectVersionRef pomCoord = recipe.getOutput();

        final Model model = new Model();
        model.setModelVersion( "4.0.0" );
        model.setGroupId( pomCoord.getGroupId() );
        model.setArtifactId( pomCoord.getArtifactId() );
        model.setVersion( pomCoord.getVersionSpec().renderStandard() );
        model.setPackaging( "pom" );
        model.setDescription( "Generated by Cartographer at " + new Date() );

        final DependencyManagement dm;
        if ( recipe.isGraphToManagedDeps() )
        {
            model.setName( pomCoord.getArtifactId() + ":: Bill of Materials" );
            dm = new DependencyManagement();
            model.setDependencyManagement( dm );
        }
        else
        {
            model.setName( pomCoord.getArtifactId() );
            dm = null;
        }

        for ( final Map.Entry<ProjectRef, ProjectRefCollection> entry : projects.entrySet() )
        {
            final ProjectRef r = entry.getKey();
            final ProjectRefCollection prc = entry.getValue();

            // TODO: This will reset the version for ALL referenced artifacts, regardless of actual references. This is not how Maven works...
            final VersionSpec spec = generateVersionSpec( prc.getVersionRefs(), recipe.isGenerateVersionRanges() );
            final Set<VersionlessArtifactRef> arts = prc.getVersionlessArtifactRefs();
            if ( arts == null )
            {
                continue;
            }

            VersionlessArtifactRef pomArtifact = null;
            boolean nonPomSeen = false;
            for ( final VersionlessArtifactRef artifact : arts )
            {
                if ( "pom".equals( artifact.getType() ) && artifact.getClassifier() == null )
                {
                    logger.debug( "Saving POM artifact for possible later inclusion (if no other artifact is found for this project): {}",
                                  artifact );
                    pomArtifact = artifact;
                }
                else
                {
                    logger.debug( "Including non-POM artifact: {}", artifact );
                    nonPomSeen = true;
                    logger.debug( "Adding dependency: {}", artifact );
                    addDependencyTo( model, artifact, spec, r, dm, recipe );
                }
            }

            if ( !nonPomSeen )
            {
                if ( pomArtifact == null )
                {
                    pomArtifact = new SimpleVersionlessArtifactRef( r, new SimpleTypeAndClassifier( "pom" ) );
                    logger.debug( "No artifacts found for: {}; created POM artifact for inclusion: {}", r,
                                  pomArtifact );
                }

                logger.debug( "Adding POM artifact to output, since non-POM was NOT encountered: {}", pomArtifact );
                addDependencyTo( model, pomArtifact, spec, r, dm, recipe );
            }
        }

        List<Location> expLocations;
        final Location srcLocation = recipe.getSourceLocation();
        try
        {
            expLocations = locationExpander.expand( srcLocation );
        }
        catch ( final TransferException ex )
        {
            throw new CartoDataException( "Failed to expand locations from " + srcLocation, ex );
        }

        for ( final Location expLocation : expLocations )
        {
            final Repository repository = new Repository();
            repository.setId( expLocation.getName().replaceAll( ".*:", "" ) );

            repository.setUrl( expLocation.getUri() );

            final RepositoryPolicy releasesPolicy = new RepositoryPolicy();
            releasesPolicy.setEnabled( expLocation.allowsReleases() );
            repository.setReleases( releasesPolicy );

            final RepositoryPolicy snapshotsPolicy = new RepositoryPolicy();
            snapshotsPolicy.setEnabled( expLocation.allowsSnapshots() );
            repository.setSnapshots( snapshotsPolicy );

            model.addRepository( repository );
        }

        return model;
    }

    private void addDependencyTo( final Model model, final VersionlessArtifactRef artifact, final VersionSpec spec,
                                  final ProjectRef ga, final DependencyManagement depMgmt, final PomRequest dto )
    {
        final Dependency d = new Dependency();

        d.setGroupId( ga.getGroupId() );
        d.setArtifactId( ga.getArtifactId() );
        d.setVersion( spec.renderStandard() );
        if ( !"jar".equals( artifact.getType() ) )
        {
            d.setType( artifact.getType() );
        }

        if ( artifact.getClassifier() != null )
        {
            d.setClassifier( artifact.getClassifier() );
        }

        if ( dto.isGraphToManagedDeps() )
        {
            depMgmt.addDependency( d );
        }
        else
        {
            model.addDependency( d );
        }
    }

    private VersionSpec generateVersionSpec( final Set<ProjectVersionRef> refs, final boolean generateVersionRanges )
    {
        final List<VersionSpec> versions = new ArrayList<>();
        for ( final ProjectVersionRef ref : refs )
        {
            final VersionSpec spec = ref.getVersionSpec();
            versions.add( spec );
        }

        Collections.sort( versions );

        if ( !generateVersionRanges || versions.size() == 1 )
        {
            return versions.get( 0 );
        }

        return new CompoundVersionSpec( null, versions );
    }

    @Override
    public String dotfile( final MultiRenderRequest recipe )
                    throws CartoDataException, CartoRequestException
    {
        final StringBuilder sb = new StringBuilder();
        final MultiGraphFunction<MultiGraphAllInput> extractor = ( input, graphMap ) -> {
            final Set<ProjectVersionRef> refs = new HashSet<>( input.getAllProjects() );
            final Collection<ProjectRelationship<?, ?>> rels = input.getAllRelationships();

            final Map<ProjectVersionRef, String> aliases = new HashMap<>();

            sb.append( "digraph " )
              .append( cleanDotName( recipe.getRenderParamWithPrecedingDefault( "Unknown Graph", "name", "coord" ) ) )
              .append( " {" );

            sb.append( "\nsize=\"300,20\"; resolution=72;\n" );

            for ( final ProjectVersionRef r : refs )
            {
                final String aliasBase = cleanDotName( r.toString() );

                String alias = aliasBase;
                int idx = 2;
                while ( aliases.containsValue( alias ) )
                {
                    alias = aliasBase + idx++;
                }

                aliases.put( r, alias );

                sb.append( "\n" ).append( alias ).append( " [label=\"" ).append( r ).append( "\"];" );
            }

            sb.append( "\n" );

            for ( final ProjectRelationship<?, ?> rel : rels )
            {
                final String da = aliases.get( rel.getDeclaring() );
                final String ta = aliases.get( rel.getTarget().asProjectVersionRef() );

                sb.append( "\n" ).append( da ).append( " -> " ).append( ta );

                appendRelationshipInfo( rel, sb );
                sb.append( ";" );
            }

            sb.append( "\n\n}\n" );
        };

        resolver.resolveAndExtractMultiGraph( AnyFilter.INSTANCE, recipe, new MultiGraphAllInputSelector(), extractor );
        return sb.toString();
    }

    private String cleanDotName( final String src )
    {
        return src.replace( ':', '_' ).replace( '.', '_' ).replace( '-', '_' );
    }

    @SuppressWarnings( "incomplete-switch" )
    private void appendRelationshipInfo( final ProjectRelationship<?, ?> rel, final StringBuilder sb )
    {
        sb.append( " [type=\"" ).append( rel.getType().name() ).append( "\"" );
        switch ( rel.getType() )
        {
            case DEPENDENCY:
            {
                sb.append( " managed=\"" ).append( rel.isManaged() ).append( "\"" );
                sb.append( " scope=\"" )
                  .append( ( (DependencyRelationship) rel ).getScope().realName() )
                  .append( "\"" );
                break;
            }
            case PLUGIN:
            {
                sb.append( " managed=\"" ).append( rel.isManaged() ).append( "\"" );
                break;
            }
            case PLUGIN_DEP:
            {
                sb.append( " managed=\"" ).append( rel.isManaged() ).append( "\"" );
                break;
            }
        }
        sb.append( "]" );
    }
}
