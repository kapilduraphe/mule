/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.temporary.MuleMessage;

import org.junit.Test;

public class JmsSubscriberTestCase extends AbstractJmsTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "jms-subscriber-test-case";
    }

    @Test
    public void subscribe() throws Exception
    {
        String textMessage = "my message";
        sendMessageToQueue("myQueue", textMessage);
        MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT);
        assertThat(message.getPayload(), is("my message"));
    }


}