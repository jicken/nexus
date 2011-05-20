/*
 * Nexus Plugin for Maven
 * Copyright (C) 2009 Sonatype, Inc.                                                                                                                          
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.IOUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Ignore;
import org.junit.Test;
import org.sonatype.nexus.plugin.discovery.fixture.DefaultDiscoveryFixture;
import org.sonatype.nexus.restlight.m2settings.M2SettingsClient;
import org.sonatype.nexus.restlight.testharness.GETFixture;
import org.sonatype.nexus.restlight.testharness.RESTTestFixture;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ibm.icu.text.SimpleDateFormat;

public class DownloadSettingsTemplateMojoTest
    extends AbstractNexusMojoTest
{

    private DownloadSettingsTemplateMojo newMojo()
    {
        DownloadSettingsTemplateMojo mojo = new DownloadSettingsTemplateMojo();

        mojo.setPrompter( prompter );
        mojo.setDiscoverer( new DefaultDiscoveryFixture( secDispatcher, prompter, logger ) );
        mojo.setDispatcher( secDispatcher );

        return mojo;
    }

    @Test
    public void backupExistingSettingsInUserConfigDir()
        throws JDOMException, IOException, MojoExecutionException
    {
        printTestName();
        String token = "testToken";

        DownloadSettingsTemplateMojo mojo = newMojo();

        mojo.setUrl( getTemplateURL( token ) );
        mojo.setUsername( getExpectedUser() );
        mojo.setPassword( getExpectedPassword() );

        mojo.setDestination( SettingsDestination.user.toString() );

        mojo.setBackupFormat( "yyyyMMdd" );
        mojo.setDoBackup( true );

        Settings settings = new Settings();
        mojo.setSettings( settings );

        File confDir = File.createTempFile( "download-settings-template.maven-conf", ".test.dir" );
        confDir.delete();
        confDir.mkdirs();

        toDelete.add( confDir );

        mojo.setMavenUserConf( confDir );

        File settingsFile = new File( confDir, "settings.xml" );

        Document emptySettings = new Document().setRootElement( new Element( "settings" ) );

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( settingsFile );
            new XMLOutputter( Format.getPrettyFormat() ).output( emptySettings, writer );
        }
        finally
        {
            IOUtil.close( writer );
        }

        runMojoTest( mojo, token, settingsFile );

        String suffix = new SimpleDateFormat( mojo.getBackupFormat() ).format( new Date() );
        File backupSettingsFile = new File( confDir, "settings.xml." + suffix );

        assertTrue( "Backup file: " + backupSettingsFile + " should have been created.", backupSettingsFile.exists() );

        XMLOutputter outputter = new XMLOutputter( Format.getCompactFormat() );
        assertEquals( outputter.outputString( emptySettings ),
                      outputter.outputString( readTestDocumentFile( backupSettingsFile ) ) );
    }

    @Test
    @Ignore
    // Disabled, since the main Nexus URL is discovered unless -Durl= is specified. Default templateId
    // is used in this case, which is 'default'.
    public void getSettingsTemplatePromptForMissingURL()
        throws JDOMException, IOException, MojoExecutionException
    {
        printTestName();
        String token = "testToken";

        DownloadSettingsTemplateMojo mojo = newMojo();

        prompter.addExpectation( "URL", getTemplateURL( token ) );

        mojo.setUsername( getExpectedUser() );
        mojo.setPassword( getExpectedPassword() );

        Settings settings = new Settings();
        mojo.setSettings( settings );

        File target = File.createTempFile( "download-settings-template.", ".test.xml" );
        mojo.setTarget( target );

        toDelete.add( target );

        runMojoTest( mojo, token, target );

        prompter.verifyPromptsUsed();
    }

    @Test
    public void getSettingsTemplatePromptForMissingPassword()
        throws JDOMException, IOException, MojoExecutionException
    {
        printTestName();
        String token = "testToken";

        DownloadSettingsTemplateMojo mojo = newMojo();

        prompter.addExpectation( "Are you sure you want to use the Nexus URL", "y" );
        prompter.addExpectation( "Enter Username [" + getExpectedUser() + "]", getExpectedUser() );
        prompter.addExpectation( "Enter Password", getExpectedPassword() );

        mojo.setUrl( getTemplateURL( token ) );
        mojo.setUsername( getExpectedUser() );

        Settings settings = new Settings();
        mojo.setSettings( settings );

        File target = File.createTempFile( "download-settings-template.", ".test.xml" );
        mojo.setTarget( target );

        toDelete.add( target );

        runMojoTest( mojo, token, target );

        prompter.verifyPromptsUsed();
    }

    @Test
    public void getSettingsTemplateUsingServerFromExistingSettings()
        throws JDOMException, IOException, MojoExecutionException
    {
        printTestName();
        String token = "testToken";

        DownloadSettingsTemplateMojo mojo = newMojo();

        mojo.setUrl( getTemplateURL( token ) );
        
        String serverId = "server";
        mojo.setServerAuthId( serverId );

        Settings settings = new Settings();
        
        Server server = new Server();
        
        server.setId( serverId );
        server.setUsername( getExpectedUser() );
        server.setPassword( getExpectedPassword() );
        
        settings.addServer( server );
        
        mojo.setSettings( settings );

        File target = File.createTempFile( "download-settings-template.", ".test.xml" );
        mojo.setTarget( target );

        toDelete.add( target );

        runMojoTest( mojo, token, target );
    }

    @Test
    public void getSettingsTemplateToCustomTarget()
        throws JDOMException, IOException, MojoExecutionException
    {
        printTestName();
        String token = "testToken";

        DownloadSettingsTemplateMojo mojo = newMojo();

        mojo.setUrl( getTemplateURL( token ) );
        mojo.setUsername( getExpectedUser() );
        mojo.setPassword( getExpectedPassword() );

        Settings settings = new Settings();
        mojo.setSettings( settings );

        File target = File.createTempFile( "download-settings-template.", ".test.xml" );
        mojo.setTarget( target );

        toDelete.add( target );

        runMojoTest( mojo, token, target );
    }

    @Test
    public void getSettingsTemplateToGlobalConfigDirWithGlobalDestination()
        throws JDOMException, IOException, MojoExecutionException
    {
        printTestName();
        String token = "testToken";

        DownloadSettingsTemplateMojo mojo = newMojo();

        mojo.setUrl( getTemplateURL( token ) );
        mojo.setUsername( getExpectedUser() );
        mojo.setPassword( getExpectedPassword() );

        mojo.setDestination( SettingsDestination.global.toString() );

        Settings settings = new Settings();
        mojo.setSettings( settings );

        File confDir = File.createTempFile( "download-settings-template.maven-conf", ".test.dir" );
        confDir.delete();
        confDir.mkdirs();

        toDelete.add( confDir );

        mojo.setMavenHomeConf( confDir );

        File settingsFile = new File( confDir, "settings.xml" );

        runMojoTest( mojo, token, settingsFile );
    }

    @Test
    public void getSettingsTemplateToUserConfigDirWithUserDestination()
        throws JDOMException, IOException, MojoExecutionException
    {
        printTestName();
        String token = "testToken";

        DownloadSettingsTemplateMojo mojo = newMojo();

        mojo.setUrl( getTemplateURL( token ) );
        mojo.setUsername( getExpectedUser() );
        mojo.setPassword( getExpectedPassword() );

        mojo.setDestination( SettingsDestination.user.toString() );

        Settings settings = new Settings();
        mojo.setSettings( settings );

        File confDir = File.createTempFile( "download-settings-template.maven-conf", ".test.dir" );
        confDir.delete();
        confDir.mkdirs();

        toDelete.add( confDir );

        mojo.setMavenUserConf( confDir );

        File settingsFile = new File( confDir, "settings.xml" );

        runMojoTest( mojo, token, settingsFile );
    }

    @Test
    public void getSettingsTemplateToUserConfigDirWithUnspecifiedDestination()
        throws JDOMException, IOException, MojoExecutionException
    {
        printTestName();
        String token = "testToken";

        DownloadSettingsTemplateMojo mojo = newMojo();

        mojo.setUrl( getTemplateURL( token ) );
        mojo.setUsername( getExpectedUser() );
        mojo.setPassword( getExpectedPassword() );

        Settings settings = new Settings();
        mojo.setSettings( settings );

        File confDir = File.createTempFile( "download-settings-template.maven-conf", ".test.dir" );
        confDir.delete();
        confDir.mkdirs();

        toDelete.add( confDir );

        mojo.setMavenUserConf( confDir );

        File settingsFile = new File( confDir, "settings.xml" );

        runMojoTest( mojo, token, settingsFile );
    }

    private void runMojoTest( final DownloadSettingsTemplateMojo mojo, final String token, final File checkFile )
        throws JDOMException, IOException, MojoExecutionException
    {
        mojo.setLog( log );

        List<RESTTestFixture> conversation = new ArrayList<RESTTestFixture>();

        conversation.add( getVersionCheckFixture() );

        GETFixture settingsGet = new GETFixture( getExpectedUser(), getExpectedPassword() );
        settingsGet.setExactURI( M2SettingsClient.SETTINGS_TEMPLATE_BASE + token + M2SettingsClient.GET_CONTENT_ACTION );

        Document testDoc = readTestDocumentResource( "settings/settings-template-" + token + ".xml" );

        settingsGet.setResponseDocument( testDoc );

        conversation.add( settingsGet );

        fixture.setConversation( conversation );

        mojo.execute();

        Document doc = readTestDocumentFile( checkFile );

        XMLOutputter outputter = new XMLOutputter( Format.getCompactFormat() );
        assertEquals( outputter.outputString( testDoc ), outputter.outputString( doc ) );
    }

    protected String getTemplateURL( final String token )
    {
        return getBaseUrl() + M2SettingsClient.SETTINGS_TEMPLATE_BASE + token + M2SettingsClient.GET_CONTENT_ACTION;
    }

}