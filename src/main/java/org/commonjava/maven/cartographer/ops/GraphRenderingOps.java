/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.mutate.ManagedDependencyMutator;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.print.DependencyTreeRelationshipPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.ListPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.StructureRelationshipPrinter;
import org.commonjava.maven.atlas.graph.traverse.print.TreePrinter;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.commonjava.maven.atlas.ident.version.CompoundVersionSpec;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.dto.GraphCalculation;
import org.commonjava.maven.cartographer.dto.GraphComposition;

@ApplicationScoped
public class GraphRenderingOps
{

    @Inject
    private CartoDataManager data;

    @Inject
    private CalculationOps calcOps;

    protected GraphRenderingOps()
    {
    }

    public GraphRenderingOps( final CartoDataManager data )
    {
        this.data = data;
    }

    public void depTree( final ProjectVersionRef ref, final ProjectRelationshipFilter filter,
                         final boolean collapseTransitives, final Map<String, Set<ProjectVersionRef>> labels,
                         final PrintWriter writer )
        throws CartoDataException
    {
        depTree( ref, filter, new ManagedDependencyMutator(), collapseTransitives, labels, null, writer );
    }

    public void depTree( final ProjectVersionRef ref, final ProjectRelationshipFilter filter,
                         final GraphMutator mutator, final boolean collapseTransitives,
                         final Map<String, Set<ProjectVersionRef>> labels, final PrintWriter writer )
        throws CartoDataException
    {
        depTree( ref, filter, mutator, collapseTransitives, labels, null, writer );
    }

    public void depTree( final ProjectVersionRef ref, final ProjectRelationshipFilter filter,
                         final boolean collapseTransitives, final Map<String, Set<ProjectVersionRef>> labels,
                         final StructureRelationshipPrinter relPrinter, final PrintWriter writer )
        throws CartoDataException
    {
        depTree( ref, filter, new ManagedDependencyMutator(), collapseTransitives, labels, relPrinter, writer );
    }

    public void depTree( final ProjectVersionRef ref, final ProjectRelationshipFilter filter,
                         final GraphMutator mutator, final boolean collapseTransitives,
                         final Map<String, Set<ProjectVersionRef>> labels, StructureRelationshipPrinter relPrinter,
                         final PrintWriter writer )
        throws CartoDataException
    {
        final EProjectGraph graph = data.getProjectGraph( filter, mutator, ref );
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
            new TreePrinter( relPrinter/*, collapseTransitives*/).printStructure( ref, byDeclaring, labels, writer );
        }
    }

    public void depTree( final GraphComposition comp, final boolean collapseTransitives, final PrintWriter writer )
        throws CartoDataException
    {
        depTree( comp, collapseTransitives, new DependencyTreeRelationshipPrinter(), writer );
    }

    public void depTree( final GraphComposition comp, final boolean collapseTransitives,
                         StructureRelationshipPrinter relPrinter, final PrintWriter writer )
        throws CartoDataException
    {
        final GraphCalculation calculated = calcOps.calculate( comp );
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

            final Map<String, Set<ProjectVersionRef>> labels = new HashMap<String, Set<ProjectVersionRef>>();
            labels.put( "ROOT", roots );

            labels.put( "NOT-RESOLVED", data.getAllIncompleteSubgraphs() );

            labels.put( "VARIABLE", data.getAllVariableSubgraphs() );

            // TODO: Reinstate transitive collapse IF we can find a way to make output consistent.
            final TreePrinter printer = new TreePrinter( relPrinter );
            for ( final ProjectVersionRef root : calculated.getResultingRoots() )
            {
                printer.printStructure( root, byDeclaring, labels, writer );
                writer.println();
            }
        }
    }

    public void depList( final ProjectVersionRef ref, final ProjectRelationshipFilter filter,
                         final Map<String, Set<ProjectVersionRef>> labels, final PrintWriter writer )
        throws CartoDataException
    {
        depList( ref, filter, new ManagedDependencyMutator(), labels, null, writer );
    }

    public void depList( final ProjectVersionRef ref, final ProjectRelationshipFilter filter,
                         final GraphMutator mutator, final Map<String, Set<ProjectVersionRef>> labels,
                         final PrintWriter writer )
        throws CartoDataException
    {
        depList( ref, filter, mutator, labels, null, writer );
    }

    public void depList( final ProjectVersionRef ref, final ProjectRelationshipFilter filter,
                         final Map<String, Set<ProjectVersionRef>> labels,
                         final StructureRelationshipPrinter relPrinter, final PrintWriter writer )
        throws CartoDataException
    {
        depList( ref, filter, new ManagedDependencyMutator(), labels, relPrinter, writer );
    }

    public void depList( final ProjectVersionRef ref, final ProjectRelationshipFilter filter,
                         final GraphMutator mutator, final Map<String, Set<ProjectVersionRef>> labels,
                         StructureRelationshipPrinter relPrinter, final PrintWriter writer )
        throws CartoDataException
    {
        final EProjectGraph graph = data.getProjectGraph( filter, mutator, ref );
        if ( graph != null )
        {
            final Set<ProjectRelationship<?>> rels = graph.getAllRelationships();
            final Map<ProjectVersionRef, List<ProjectRelationship<?>>> byDeclaring =
                RelationshipUtils.mapByDeclaring( rels );

            if ( relPrinter == null )
            {
                relPrinter = new DependencyTreeRelationshipPrinter();
            }

            new ListPrinter( relPrinter ).printStructure( ref, byDeclaring, labels, writer );
        }
    }

    public Model generateBOM( final ProjectVersionRef bomCoord, final ProjectRelationshipFilter filter,
                              final ProjectVersionRef... roots )
        throws CartoDataException
    {
        return generateBOM( bomCoord, filter, new ManagedDependencyMutator(), roots );
    }

    public Model generateBOM( final ProjectVersionRef bomCoord, final ProjectRelationshipFilter filter,
                              final GraphMutator mutator, final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final EProjectNet web = data.getProjectWeb( filter, mutator, roots );

        if ( web == null )
        {
            return null;
        }

        final Map<ProjectRef, ProjectRefCollection> projects = collectProjectReferences( web );

        final Model model = new Model();
        model.setGroupId( bomCoord.getGroupId() );
        model.setArtifactId( bomCoord.getArtifactId() );
        model.setVersion( ( (SingleVersion) bomCoord.getVersionSpec() ).renderStandard() );
        model.setPackaging( "pom" );
        model.setName( bomCoord.getArtifactId() + ":: Bill of Materials" );
        model.setDescription( "Generated by Cartographer at " + new Date() );

        final DependencyManagement dm = new DependencyManagement();
        model.setDependencyManagement( dm );

        for ( final Map.Entry<ProjectRef, ProjectRefCollection> entry : projects.entrySet() )
        {
            final ProjectRef r = entry.getKey();
            final ProjectRefCollection prc = entry.getValue();

            final VersionSpec spec = generateVersionSpec( prc.getVersionRefs() );
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

                dm.addDependency( d );
            }
        }

        return model;
    }

    private VersionSpec generateVersionSpec( final Set<ProjectVersionRef> refs )
    {
        final List<VersionSpec> versions = new ArrayList<VersionSpec>();
        for ( final ProjectVersionRef ref : refs )
        {
            final VersionSpec spec = ref.getVersionSpec();
            versions.add( spec );
        }

        Collections.sort( versions );

        if ( versions.size() == 1 )
        {
            return versions.get( 0 );
        }

        return new CompoundVersionSpec( null, versions );
    }

    public String dotfile( final ProjectVersionRef coord, final ProjectRelationshipFilter filter,
                           final ProjectVersionRef... roots )
        throws CartoDataException
    {
        return dotfile( coord, filter, new ManagedDependencyMutator(), roots );
    }

    public String dotfile( final ProjectVersionRef coord, final ProjectRelationshipFilter filter,
                           final GraphMutator mutator, final ProjectVersionRef... roots )
        throws CartoDataException
    {
        final EProjectNet web = data.getProjectWeb( filter, mutator, roots );

        if ( web != null )
        {
            final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>( web.getAllProjects() );
            final Collection<ProjectRelationship<?>> rels = web.getAllRelationships();

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
