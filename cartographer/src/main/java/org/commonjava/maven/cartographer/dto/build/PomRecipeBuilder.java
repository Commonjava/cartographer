package org.commonjava.maven.cartographer.dto.build;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.dto.PomRecipe;

public class PomRecipeBuilder<T extends PomRecipeBuilder<T, O, R>, O extends ResolverRecipeOwner<O, R>, R extends PomRecipe>
    extends RepositoryContentRecipeBuilder<T, O, R>
{

    public static final class StandalonePRB
        extends PomRecipeBuilder<StandalonePRB, StandaloneRecipeOwner<PomRecipe>, PomRecipe>
    {
        public StandalonePRB()
        {
            super( new StandaloneRecipeOwner<>() );
        }
    }

    public static StandalonePRB newPomRecipeBuilder()
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

    public PomRecipeBuilder( final O owner )
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
        final PomRecipe recipe = new PomRecipe();
        configure( recipe );
        configureMultiGraphs( recipe );
        configureRepoContent( recipe );
        configurePom( recipe );

        return (R) recipe;
    }

    protected void configurePom( final PomRecipe recipe )
    {
        recipe.setGenerateVersionRanges( generateVersionRanges );
        recipe.setGraphToManagedDeps( graphToManagedDeps );
        recipe.setOutput( output );
    }

}
