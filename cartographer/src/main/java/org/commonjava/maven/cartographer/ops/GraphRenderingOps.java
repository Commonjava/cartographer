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
package org.commonjava.maven.cartographer.ops;

import static org.commonjava.maven.cartographer.agg.AggregationUtils.collectProjectReferences;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.print.DependencyTreeRelationshipPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.ListPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.StructureRelationshipPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.TreePrinter;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.commonjava.maven.atlas.ident.version.CompoundVersionSpec;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoGraphUtils;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.PomRecipe;
import org.commonjava.maven.cartographer.dto.resolve.DTOResolver;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationExpander;

@ApplicationScoped
public class GraphRenderingOps
{

    @Inject
    protected CalculationOps calcOps;

    @Inject
    protected ResolveOps resolveOps;

    @Inject
    protected LocationExpander locationExpander;

    @Inject
    protected RelationshipGraphFactory graphFactory;

    @Inject
    protected DTOResolver dtoResolver;

    protected GraphRenderingOps()
    {
    }

    public GraphRenderingOps( final CalculationOps calcOps, final ResolveOps resolveOps,
                              final RelationshipGraphFactory graphFactory, final DTOResolver dtoResolver )
    {
        this.calcOps = calcOps;
        this.resolveOps = resolveOps;
        this.graphFactory = graphFactory;
        this.dtoResolver = dtoResolver;
    }

    public void depTree( final RelationshipGraph graph, final boolean collapseTransitives,
                         final Map<String, Set<ProjectVersionRef>> labels, final PrintWriter writer )
    {
        depTree( graph, collapseTransitives, null, writer );
    }

    public void depTree( final RelationshipGraph graph, final boolean collapseTransitives,
                         Map<String, Set<ProjectVersionRef>> labels, StructureRelationshipPrinter relPrinter,
                         final PrintWriter writer )
    {
        if ( graph != null )
        {
            final Set<ProjectRelationship<?>> rels = graph.getAllRelationships();
            final Map<ProjectVersionRef, List<ProjectRelationship<?>>> byDeclaring =
                RelationshipUtils.mapByDeclaring( rels );

            if ( relPrinter == null )
            {
                relPrinter = new DependencyTreeRelationshipPrinter();
            }

            // TODO: Reinstate transitive collapse IF we can find a way to make output consistent.
            final TreePrinter printer = new TreePrinter( relPrinter/*, collapseTransitives*/);

            final Set<ProjectVersionRef> roots = graph.getParams()
                                                      .getRoots();
            labels = getLabels( graph, roots, labels );
            for ( final ProjectVersionRef root : roots )
            {
                printer.printStructure( root, byDeclaring, labels, writer );
            }

        }
    }

    public void depTree( final String workspaceId, final GraphComposition comp, final boolean collapseTransitives,
                         final PrintWriter writer )
        throws CartoDataException
    {
        depTree( workspaceId, comp, collapseTransitives, new DependencyTreeRelationshipPrinter(), writer );
    }

    public void depTree( final String workspaceId, final GraphComposition comp, final boolean collapseTransitives,
                         StructureRelationshipPrinter relPrinter, final PrintWriter writer )
        throws CartoDataException
    {
        final GraphCalculation calculated = calcOps.calculate( comp, workspaceId );
        if ( calculated != null )
        {
            final Set<ProjectRelationship<?>> rels = calculated.getResult();

            final Map<ProjectVersionRef, List<ProjectRelationship<?>>> byDeclaring =
                RelationshipUtils.mapByDeclaring( rels );

            if ( relPrinter == null )
            {
                relPrinter = new DependencyTreeRelationshipPrinter();
            }

            final Set<ProjectVersionRef> roots = calculated.getResultingRoots();

            final Map<String, Set<ProjectVersionRef>> labels = getLabels( workspaceId, roots );

            // TODO: Reinstate transitive collapse IF we can find a way to make output consistent.
            final TreePrinter printer = new TreePrinter( relPrinter );
            for ( final ProjectVersionRef root : calculated.getResultingRoots() )
            {
                writer.printf( "Dependency tree for '%s':\n" );
                printer.printStructure( root, byDeclaring, labels, writer );
                writer.println();
            }
        }
    }

    private Map<String, Set<ProjectVersionRef>> getLabels( final String workspaceId, final Set<ProjectVersionRef> roots )
        throws CartoDataException
    {
        RelationshipGraph allWs = null;
        try
        {
            allWs = graphFactory.open( new ViewParams( workspaceId ), false );

            return getLabels( allWs, roots, null );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new CartoDataException(
                                          "Cannot open root-less graph in workspace: '{}' for tree labeling. Reason: {}",
                                          e, workspaceId, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( allWs );
        }
    }

    private Map<String, Set<ProjectVersionRef>> getLabels( final RelationshipGraph allWs,
                                                           final Set<ProjectVersionRef> roots,
                                                           Map<String, Set<ProjectVersionRef>> labels )
    {
        if ( labels == null )
        {
            labels = new HashMap<String, Set<ProjectVersionRef>>();
        }

        labels.put( "ROOT", roots );

        labels.put( "NOT-RESOLVED", allWs.getIncompleteSubgraphs() );

        labels.put( "VARIABLE", allWs.getVariableSubgraphs() );

        return labels;
    }

    public void depList( final RelationshipGraph graph, final Map<String, Set<ProjectVersionRef>> labels,
                         final PrintWriter writer )
        throws CartoDataException
    {
        depList( graph, labels, null, writer );
    }

    public void depList( final RelationshipGraph graph, Map<String, Set<ProjectVersionRef>> labels,
                         StructureRelationshipPrinter relPrinter, final PrintWriter writer )
        throws CartoDataException
    {
        final Set<ProjectVersionRef> roots = graph.getParams()
                                                  .getRoots();
        if ( roots == null || roots.isEmpty() )
        {
            throw new CartoDataException( "Cannot list dependencies of graph with no roots! (graph: {})", graph );
        }

        labels = getLabels( graph, roots, labels );

        if ( graph != null )
        {
            final Set<ProjectRelationship<?>> rels = graph.getAllRelationships();
            final Map<ProjectVersionRef, List<ProjectRelationship<?>>> byDeclaring =
                RelationshipUtils.mapByDeclaring( rels );

            if ( relPrinter == null )
            {
                relPrinter = new DependencyTreeRelationshipPrinter();
            }

            final ListPrinter listPrinter = new ListPrinter( relPrinter );
            for ( final ProjectVersionRef root : roots )
            {
                writer.printf( "Dependency list for '%s':\n" );
                listPrinter.printStructure( root, byDeclaring, labels, writer );
            }
        }
    }

    @SuppressWarnings( "null" )
    public Model generatePOM( final PomRecipe dto )
        throws CartoDataException
    {
        dtoResolver.resolve( dto );

        if ( dto == null )
        {
            return null;
        }

        final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolved =
            resolveOps.resolveRepositoryContents( dto );

        final Map<ProjectRef, ProjectRefCollection> projects = collectProjectReferences( resolved );

        final ProjectVersionRef pomCoord = dto.getOutput();

        final Model model = new Model();
        model.setModelVersion( "4.0.0" );
        model.setGroupId( pomCoord.getGroupId() );
        model.setArtifactId( pomCoord.getArtifactId() );
        model.setVersion( ( (SingleVersion) pomCoord.getVersionSpec() ).renderStandard() );
        model.setPackaging( "pom" );
        model.setDescription( "Generated by Cartographer at " + new Date() );

        final DependencyManagement dm;
        if ( dto.isGraphToManagedDeps() )
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
            final VersionSpec spec = generateVersionSpec( prc.getVersionRefs(), dto.isGenerateVersionRanges() );
            final Set<VersionlessArtifactRef> arts = prc.getVersionlessArtifactRefs();
            if ( arts == null )
            {
                continue;
            }

            for ( final VersionlessArtifactRef artifact : arts )
            {
                final Dependency d = new Dependency();

                d.setGroupId( r.getGroupId() );
                d.setArtifactId( r.getArtifactId() );
                d.setVersion( spec.renderStandard() );
                if ( !"jar".equals( artifact.getType() ) )
                {
                    d.setType( artifact.getType() );
                }

                if ( artifact.getClassifier() != null )
                {
                    d.setClassifier( artifact.getClassifier() );
                }

                if ( artifact.isOptional() )
                {
                    d.setOptional( true );
                }

                if ( dto.isGraphToManagedDeps() )
                {
                    dm.addDependency( d );
                }
                else
                {
                    model.addDependency( d );
                }
            }
        }

        List<Location> expLocations;
        final Location srcLocation = dto.getSourceLocation();
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
            repository.setId( expLocation.getName()
                                         .replaceAll( ".*:", "" ) );
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

    private VersionSpec generateVersionSpec( final Set<ProjectVersionRef> refs, final boolean generateVersionRanges )
    {
        final List<VersionSpec> versions = new ArrayList<VersionSpec>();
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

    public String dotfile( final ProjectVersionRef coord, final RelationshipGraph graph )
        throws CartoDataException
    {
        if ( graph != null )
        {
            final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>( graph.getAllProjects() );
            final Collection<ProjectRelationship<?>> rels = graph.getAllRelationships();

            final Map<ProjectVersionRef, String> aliases = new HashMap<ProjectVersionRef, String>();

            final StringBuilder sb = new StringBuilder();
            sb.append( "digraph " )
              .append( cleanDotName( coord.getGroupId() ) )
              .append( '_' )
              .append( cleanDotName( coord.getArtifactId() ) )
              .append( '_' )
              .append( cleanDotName( ( (SingleVersion) coord.getVersionSpec() ).renderStandard() ) )
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

                sb.append( "\n" )
                  .append( alias )
                  .append( " [label=\"" )
                  .append( r )
                  .append( "\"];" );
            }

            sb.append( "\n" );

            for ( final ProjectRelationship<?> rel : rels )
            {
                final String da = aliases.get( rel.getDeclaring() );
                final String ta = aliases.get( rel.getTarget()
                                                  .asProjectVersionRef() );

                sb.append( "\n" )
                  .append( da )
                  .append( " -> " )
                  .append( ta );

                appendRelationshipInfo( rel, sb );
                sb.append( ";" );
            }

            sb.append( "\n\n}\n" );
            return sb.toString();
        }

        return null;
    }

    private String cleanDotName( final String src )
    {
        return src.replace( ':', '_' )
                  .replace( '.', '_' )
                  .replace( '-', '_' );
    }

    @SuppressWarnings( "incomplete-switch" )
    private void appendRelationshipInfo( final ProjectRelationship<?> rel, final StringBuilder sb )
    {
        sb.append( " [type=\"" )
          .append( rel.getType()
                      .name() )
          .append( "\"" );
        switch ( rel.getType() )
        {
            case DEPENDENCY:
            {
                sb.append( " managed=\"" )
                  .append( ( (DependencyRelationship) rel ).isManaged() )
                  .append( "\"" );
                sb.append( " scope=\"" )
                  .append( ( (DependencyRelationship) rel ).getScope()
                                                           .realName() )
                  .append( "\"" );
                break;
            }
            case PLUGIN:
            {
                sb.append( " managed=\"" )
                  .append( ( (DependencyRelationship) rel ).isManaged() )
                  .append( "\"" );
                break;
            }
            case PLUGIN_DEP:
            {
                sb.append( " managed=\"" )
                  .append( ( (DependencyRelationship) rel ).isManaged() )
                  .append( "\"" );
                break;
            }
        }
        sb.append( "]" );
    }
}
