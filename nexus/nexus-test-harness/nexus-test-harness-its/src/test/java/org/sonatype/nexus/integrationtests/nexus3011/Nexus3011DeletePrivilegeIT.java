package org.sonatype.nexus.integrationtests.nexus3011;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsCollectionContaining.hasItems;
import static org.sonatype.nexus.integrationtests.ITGroups.SECURITY;

import org.hamcrest.MatcherAssert;
import org.restlet.data.MediaType;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.sonatype.security.rest.model.RoleResource;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Nexus3011DeletePrivilegeIT
    extends AbstractNexusIntegrationTest
{

    private static final String ROLE_ID = "nexus3011-role";

    private PrivilegesMessageUtil privUtil = new PrivilegesMessageUtil( this, XStreamFactory.getXmlXStream(),
                                                                        MediaType.APPLICATION_XML );

    private RoleMessageUtil roleUtil = new RoleMessageUtil( this, XStreamFactory.getXmlXStream(),
                                                            MediaType.APPLICATION_XML );

    private static final String READ_PRIV_ID = "999a27d0bf1";

    private static final String CREATE_PRIV_ID = "999a15cfb91";

    private static final String UPDATE_PRIV_ID = "999a2d10b38";

    private static final String DELETE_PRIV_ID = "999a35c0ab5";

    private static final String[] PRIVS = new String[] { READ_PRIV_ID, CREATE_PRIV_ID, UPDATE_PRIV_ID, DELETE_PRIV_ID };

    @Test(groups = SECURITY)
    public void deletePriv()
        throws Exception
    {
        RoleResource role = roleUtil.getRole( ROLE_ID );
        Assert.assertNotNull( role );
        MatcherAssert.assertThat( role.getPrivileges(), hasItems( PRIVS ) );
        privUtil.assertExists( PRIVS );

        // remove read
        Assert.assertTrue( privUtil.delete( READ_PRIV_ID ).getStatus().isSuccess() );
        role = roleUtil.getRole( ROLE_ID );
        MatcherAssert.assertThat( role.getPrivileges(), not( hasItems( READ_PRIV_ID ) ) );
        MatcherAssert.assertThat( role.getPrivileges(), hasItems( CREATE_PRIV_ID, UPDATE_PRIV_ID, DELETE_PRIV_ID ) );

        // remove create
        Assert.assertTrue( privUtil.delete( CREATE_PRIV_ID ).getStatus().isSuccess() );
        role = roleUtil.getRole( ROLE_ID );
        MatcherAssert.assertThat( role.getPrivileges(), not( hasItems( READ_PRIV_ID, CREATE_PRIV_ID ) ) );
        MatcherAssert.assertThat( role.getPrivileges(), hasItems( UPDATE_PRIV_ID, DELETE_PRIV_ID ) );

        // remove update
        Assert.assertTrue( privUtil.delete( UPDATE_PRIV_ID ).getStatus().isSuccess() );
        role = roleUtil.getRole( ROLE_ID );
        MatcherAssert.assertThat( role.getPrivileges(), not( hasItems( READ_PRIV_ID, CREATE_PRIV_ID, UPDATE_PRIV_ID ) ) );
        MatcherAssert.assertThat( role.getPrivileges(), hasItems( DELETE_PRIV_ID ) );

        // remove delete
        Assert.assertTrue( privUtil.delete( DELETE_PRIV_ID ).getStatus().isSuccess() );
        role = roleUtil.getRole( ROLE_ID );
        MatcherAssert.assertThat( role.getPrivileges(),
                           not( hasItems( READ_PRIV_ID, CREATE_PRIV_ID, UPDATE_PRIV_ID, DELETE_PRIV_ID ) ) );
        Assert.assertTrue( role.getPrivileges().isEmpty() );

        privUtil.assertNotExists( PRIVS );
    }
}
