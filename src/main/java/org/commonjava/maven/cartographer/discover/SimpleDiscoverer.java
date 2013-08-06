package org.commonjava.maven.cartographer.discover;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.maven.cartographer.discover.DiscoveryUtils.selectSingle;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionUtils;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

public class SimpleDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = new Logger( getClass() );

    private final TransferManager transferManager;

    private final MavenModelProcessor modelProcessor;

    private final CartoDataManager dataManager;

    public SimpleDiscoverer( final CartoDataManager dataManager, final MavenModelProcessor modelProcessor,
                             final TransferManager transferManager )
    {
        this.dataManager = dataManager;
        this.modelProcessor = modelProcessor;
        this.transferManager = transferManager;
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        if ( ref.isSnapshot() )
        {
            return resolveSnapshot( ref, discoveryConfig );
        }
        else if ( !ref.getVersionSpec()
                      .isSingle() )
        {
            return resolveRange( ref, discoveryConfig );
        }
        else
        {
            return ref;
        }
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        ProjectVersionRef specific = ref;
        if ( !ref.isSpecificVersion() )
        {
            specific = resolveSpecificVersion( ref, discoveryConfig );
        }

        final Transfer transfer =
            retrieve( discoveryConfig, specific.getGroupId(), specific.getArtifactId(), specific.getVersionSpec()
                                                                                                .renderStandard(),
                      specific.getArtifactId() + "-" + specific.getVersionSpec()
                                                               .renderStandard() + ".pom" );
        Model model = null;
        if ( transfer != null && transfer.exists() )
        {
            final InputSource is = new InputSource();
            is.setLocation( transfer.getFullPath() );
            is.setModelId( specific.toString() );

            InputStream stream = null;
            try
            {
                stream = transfer.openInputStream();
                model = new MavenXpp3ReaderEx().read( stream, false, is );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to read POM for: '%s' from: '%s'. Reason: %s", e, specific, transfer,
                              e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to read POM for: '%s' from: '%s'. Reason: %s", e, specific, transfer,
                              e.getMessage() );
            }
            finally
            {
                closeQuietly( stream );
            }
        }

        if ( model != null )
        {
            return modelProcessor.storeModelRelationships( model, discoveryConfig.getDiscoverySource() );
        }

        return null;
    }

    private Transfer retrieve( final DiscoveryConfig discoveryConfig, final String groupId, final String... parts )
        throws CartoDataException
    {
        final String path = toGroupPath( groupId, parts );

        Transfer transfer;
        try
        {
            transfer = transferManager.retrieve( new SimpleLocation( discoveryConfig.getDiscoverySource()
                                                                                    .toString() ), path );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to retrieve: %s from: %s. Reason: %s", e, path,
                                           discoveryConfig.getDiscoverySource(), e.getMessage() );
        }

        return transfer;
    }

    private ProjectVersionRef resolveSnapshot( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final Transfer transfer =
            retrieve( discoveryConfig, ref.getGroupId(), ref.getArtifactId(), ref.getVersionSpec()
                                                                                 .renderStandard(),
                      "maven-metadata.xml" );
        if ( transfer != null && transfer.exists() )
        {
            String latest = null;
            InputStream stream = null;
            try
            {
                stream = transfer.openInputStream();
                final Metadata read = new MetadataXpp3Reader().read( stream );
                final Versioning versioning = read.getVersioning();

                latest = versioning.getLatest();
                if ( latest != null )
                {
                    selectSingle( VersionUtils.createSingleVersion( latest ), ref, dataManager );
                    return new ProjectVersionRef( ref, latest );
                }
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to parse snapshot metadata for: '%s'. Reason: %s", e, ref, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to parse snapshot metadata for: '%s'. Reason: %s", e, ref, e.getMessage() );
            }
            catch ( final InvalidVersionSpecificationException e )
            {
                logger.error( "Latest version in snapshot metadata '%s' is cannot be parsed. Reason: %s", e, latest,
                              e.getMessage() );
            }
            finally
            {
                closeQuietly( stream );
            }
        }

        return null;
    }

    private ProjectVersionRef resolveRange( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final Transfer transfer =
            retrieve( discoveryConfig, ref.getGroupId(), ref.getArtifactId(), ref.getVersionSpec()
                                                                                 .renderStandard(),
                      "maven-metadata.xml" );
        if ( transfer != null && transfer.exists() )
        {
            final List<String> allVersions = new ArrayList<String>();
            InputStream stream = null;
            try
            {
                stream = transfer.openInputStream();
                final Metadata read = new MetadataXpp3Reader().read( stream );
                final Versioning versioning = read.getVersioning();
                final List<String> versions = versioning.getVersions();

                if ( versions != null )
                {
                    for ( final String version : versions )
                    {
                        if ( !allVersions.contains( version ) )
                        {
                            allVersions.add( version );
                        }
                    }
                }
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to parse artifact-level metadata for: '%s'. Reason: %s", e, ref, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to parse artifact-level metadata for: '%s'. Reason: %s", e, ref, e.getMessage() );
            }
            finally
            {
                closeQuietly( stream );
            }

            final LinkedList<SingleVersion> specs = new LinkedList<SingleVersion>();
            if ( allVersions != null && !allVersions.isEmpty() )
            {
                for ( final String spec : allVersions )
                {
                    try
                    {
                        specs.add( VersionUtils.createSingleVersion( spec ) );
                    }
                    catch ( final InvalidVersionSpecificationException e )
                    {
                        logger.error( "Unparsable version spec found in metadata: '%s'. Reason: %s", e, spec,
                                      e.getMessage() );
                    }
                }
            }

            if ( !specs.isEmpty() )
            {
                Collections.sort( specs );
                SingleVersion ver = null;
                do
                {
                    ver = specs.removeLast();
                }
                while ( !ver.isConcrete() );

                if ( ver != null )
                {
                    selectSingle( ver, ref, dataManager );

                    return new ProjectVersionRef( ref, ver );
                }
            }
        }

        return null;
    }

    private String toGroupPath( final String groupId, final String... parts )
    {
        return toPath( groupId.replace( '.', '/' ), parts );
    }

    private String toPath( final String base, final String... parts )
    {
        final StringBuilder sb = new StringBuilder( base );
        for ( final String part : parts )
        {
            if ( sb.charAt( sb.length() - 1 ) != '/' && part.charAt( 0 ) != '/' )
            {
                sb.append( '/' );
            }

            sb.append( part );
        }

        return sb.toString();
    }

}
