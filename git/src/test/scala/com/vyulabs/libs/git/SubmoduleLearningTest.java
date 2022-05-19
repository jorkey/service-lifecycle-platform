package com.vyulabs.libs.git;

/*******************************************************************************
 * Copyright (c) 2014 Rüdiger Herrmann
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Rüdiger Herrmann - initial API and implementation
 ******************************************************************************/
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_SUBMODULE_SECTION;
import static org.eclipse.jgit.lib.Constants.DOT_GIT_MODULES;
import static org.eclipse.jgit.submodule.SubmoduleStatusType.INITIALIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class SubmoduleLearningTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private Git library;
    private Git parent;

    @Test
    public void testAddSubmodule() throws Exception {
        String uri = library.getRepository().getDirectory().getCanonicalPath();
        SubmoduleAddCommand addCommand = parent.submoduleAdd();
        addCommand.setURI( uri );
        addCommand.setPath( "modules/library" );
        Repository repository = addCommand.call();
        repository.close();

        File parentWorkDir = parent.getRepository().getWorkTree();
        assertTrue( new File( parentWorkDir, "modules/library" ).isDirectory() );
        assertTrue( new File( parentWorkDir, "modules/library/readme.txt" ).isFile() );
        assertTrue( new File( parentWorkDir, ".gitmodules" ).isFile() );
    }

    @Test
    public void testListSubmodules() throws Exception {
        addLibrarySubmodule();

        Map<String, SubmoduleStatus> submodules = parent.submoduleStatus().call();

        assertEquals( 1, submodules.size() );
        SubmoduleStatus submoduleStatus = submodules.get( "modules/library" );
        assertEquals( INITIALIZED, submoduleStatus.getType() );
    }

    @Test
    public void testUpdateSubmodule() throws Exception {
        addLibrarySubmodule();
        ObjectId newHead = library.commit().setMessage( "Another change" ).call();

        File workDir = parent.getRepository().getWorkTree();
        Git librarySubmodule = Git.open( new File( workDir, "modules/library" ) );
        librarySubmodule.pull().call();
        librarySubmodule.close();
        parent.add().addFilepattern( "modules/library" ).call();
        parent.commit().setMessage( "Update submodule" ).call();

        assertEquals( newHead, getSubmoduleHead( "modules/library" ) );
    }

    @Test
    public void testRemoveSubmodule() throws Exception {
        String uri = library.getRepository().getDirectory().getCanonicalPath();
        Repository repository = parent.submoduleAdd().setURI( uri ).setPath( "modules/library" ).call();
        repository.close();

        removeSubmodule( parent.getRepository(), "modules/library" );

        Config gitSubmodulesConfig = getGitSubmodulesConfig( parent.getRepository() );
        StoredConfig repositoryConfig = parent.getRepository().getConfig();
        assertEquals( 0, gitSubmodulesConfig.getNames( CONFIG_SUBMODULE_SECTION, "modules/library" ).size() );
        assertEquals( 0, repositoryConfig.getNames( CONFIG_SUBMODULE_SECTION, "modules/library" ).size() );
        assertEquals( ObjectId.zeroId(), getSubmoduleHead( "modules/library" ) );
        assertFalse( new File( repository.getWorkTree(), "modules/library" ).exists() );
        assertTrue( parent.status().call().isClean() );
    }

    @Test
    public void testSubmoduleWalk() throws Exception {
        addLibrarySubmodule();

        int submoduleCount = 0;
        SubmoduleWalk walk = SubmoduleWalk.forIndex( parent.getRepository() );
        while( walk.next() ) {
            Repository submoduleRepository = walk.getRepository();
            Git.wrap( submoduleRepository ).fetch().call();
            submoduleRepository.close();
            submoduleCount++;
        }
        walk.close();

        assertEquals( 1, submoduleCount );
    }

    @Test
    public void testSubmoduleSync() throws Exception {
        addLibrarySubmodule();
        parent.getRepository().getConfig().unsetSection( "submodule", "modules/library" );

        parent.submoduleSync().call();

        SubmoduleWalk walk = SubmoduleWalk.forIndex( parent.getRepository() );
        walk.next();
        String configUrl = walk.getConfigUrl();
        walk.close();
        assertEquals( library.getRepository().getDirectory(), new File( configUrl ) );
    }

    @Before
    public void setUp() throws Exception {
        library = initRepository( "library" );
        populateLibraryRepository();
        parent = initRepository( "parent" );
    }

    @After
    public void tearDown() {
        library.close();
        parent.close();
    }

    private Git initRepository( String name ) throws Exception {
        return Git.init().setDirectory( tempFolder.newFolder( name ) ).call();
    }

    private void populateLibraryRepository() throws Exception {
        new File( library.getRepository().getWorkTree(), "readme.txt" ).createNewFile();
        library.add().addFilepattern( "." ).call();
        library.commit().setMessage( "Initial commit" ).call();
    }

    private ObjectId getSubmoduleHead( String path ) throws Exception {
        SubmoduleWalk walk = SubmoduleWalk.forIndex( parent.getRepository() );
        walk.setFilter( PathFilter.create( path ) );
        walk.next();
        ObjectId result = walk.getObjectId();
        walk.close();
        return result;
    }

    private Repository addLibrarySubmodule() throws IOException, GitAPIException {
        String uri = library.getRepository().getDirectory().getCanonicalPath();
        Repository repository = parent.submoduleAdd().setURI( uri ).setPath( "modules/library" ).call();
        repository.close();
        return repository;
    }

    private static void removeSubmodule( Repository repository, String submodulePath )
            throws Exception
    {
        StoredConfig gitSubmodulesConfig = getGitSubmodulesConfig( repository );
        gitSubmodulesConfig.unsetSection( CONFIG_SUBMODULE_SECTION, submodulePath );
        gitSubmodulesConfig.save();
        StoredConfig repositoryConfig = repository.getConfig();
        repositoryConfig.unsetSection( CONFIG_SUBMODULE_SECTION, submodulePath );
        repositoryConfig.save();
        Git git = Git.wrap( repository );
        git.add().addFilepattern( DOT_GIT_MODULES ).call();
        git.rm().setCached( true ).addFilepattern( submodulePath ).call();
        git.commit().setMessage( "Remove submodule" ).call();
        FileUtils.delete( new File( repository.getWorkTree(), submodulePath ), FileUtils.RECURSIVE );
    }

    private static StoredConfig getGitSubmodulesConfig( Repository repository ) {
        File gitSubmodulesFile = new File( repository.getWorkTree(), DOT_GIT_MODULES );
        return new FileBasedConfig( null, gitSubmodulesFile, FS.DETECTED );
    }

}