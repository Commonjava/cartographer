package org.commonjava.maven.cartographer.request.build;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.request.PomRequest;

public class PomRequestBuilder<T extends PomRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends PomRequest>
    extends RepositoryContentRequestBuilder<T, O, R>
{

    public static final class StandalonePRB
        extends PomRequestBuilder<StandalonePRB, StandaloneRequestOwner<PomRequest>, PomRequest>
    {
        public StandalonePRB()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandalonePRB newPomRequestBuilder()
    {
        return new StandalonePRB();
    }

    private boolean generateVersionRanges;

    private ProjectVersionRef output;

    /**
     * Flag saying that all the deps from dependency graph should be placed in
     * the dependencyManagement section. If false standard dependencies section
     * will be used.
     */
    private boolean graphToManagedDeps;

    public PomRequestBuilder( final O owner )
    {
        super( owner );
    }

    public boolean isGenerateVersionRanges()
    {
        return generateVersionRanges;
    }

    public T withGenerateVersionRanges( final boolean generateVersionRanges )
    {
        this.generateVersionRanges = generateVersionRanges;
        return self;
    }

    public ProjectVersionRef getOutput()
    {
        return output;
    }

    public T withOutput( final ProjectVersionRef outputGav )
    {
        this.output = outputGav;
        return self;
    }

    /**
     * @return the flag saying that all the deps from dependency graph should be
     *         placed in the dependencyManagement section. If false standard
     *         dependencies section will be used.
     */
    public boolean isGraphToManagedDeps()
    {
        return graphToManagedDeps;
    }

    /**
     * @param graphToManagedDeps
     *            the flag saying that all the deps from dependency graph should
     *            be placed in the dependencyManagement section. If false
     *            standard dependencies section will be used
     */
    public T withGraphToManagedDeps( final boolean graphToManagedDeps )
    {
        this.graphToManagedDeps = graphToManagedDeps;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final PomRequest recipe = new PomRequest();
        configure( recipe );
        configureMultiGraphs( recipe );
        configureRepoContent( recipe );
        configurePom( recipe );

        return (R) recipe;
    }

    protected void configurePom( final PomRequest recipe )
    {
        recipe.setGenerateVersionRanges( generateVersionRanges );
        recipe.setGraphToManagedDeps( graphToManagedDeps );
        recipe.setOutput( output );
    }

}
