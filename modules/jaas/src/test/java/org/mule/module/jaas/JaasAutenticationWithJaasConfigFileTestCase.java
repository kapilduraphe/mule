/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jaas;

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.security.MuleCredentials;
import org.mule.tck.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

public class JaasAutenticationWithJaasConfigFileTestCase extends FunctionalTestCase
{

    public void testCaseGoodAuthentication() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Marie.Rizzo", "dragon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        MuleMessage m = client.send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertEquals("Test Received", m.getPayloadAsString());
    }

    public void testCaseDifferentGoodAuthentication() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        MuleMessage m = client.send("vm://test", "Test", props);

        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertEquals("Test Received", m.getPayloadAsString());
    }

    public void testCaseWrongCombinationOfCorrectUsernameAndPassword() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Marie.Rizzo", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        try
        {
            client.send("vm://test", "Test", props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testCaseBadUserName() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Evil", "dragon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        try
        {
            client.send("vm://test", "Test", props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testCaseBadPassword() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("Marie.Rizzo", "evil", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        try
        {
            client.send("vm://test", "Test", props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    protected String getConfigResources()
    {
        return "mule-conf-for-jaas-conf-file.xml";
    }
}
