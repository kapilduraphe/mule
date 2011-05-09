/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.SecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageDispatcher;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.endpoint.DynamicOutboundEndpoint;
import org.mule.tck.security.TestSecurityFilter;
import org.mule.tck.testmodels.mule.TestMessageDispatcher;
import org.mule.tck.testmodels.mule.TestMessageDispatcherFactory;
import org.mule.transformer.simple.OutboundAppendTransformer;
import org.mule.transformer.simple.ResponseAppendTransformer;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Tests flow of messages from {@link org.mule.endpoint.DynamicOutboundEndpoint#process(org.mule.api.MuleEvent)} down to
 * {@link org.mule.transport.AbstractMessageDispatcher} and the chain of MessageProcessor's that
 * implement the outbound endpoint processing.
 */
public class DynamicOutboundEndpointTestCase extends AbstractMessageProcessorTestCase
{
    protected FakeMessageDispatcher dispacher;
    protected MuleEvent testOutboundEvent;

    public void testDefaultFlowRequestResponse() throws Exception
    {
         OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null,
                null, MessageExchangePattern.REQUEST_RESPONSE, null);

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        testOutboundEvent = createTestOutboundEvent(endpoint);
        //First Request creates the real endpoint
        endpoint.process(testOutboundEvent);

        setUpFakeDispatcher(endpoint);
        MuleEvent result = endpoint.process(testOutboundEvent);

        assertMessageSentEqual(true);

        // Response message is not the same because we rewrite the response event and
        // this change the properties
        // (See: OutboundRewriteResponseEventMessageProcessor)
        assertNotSame(responseMessage, result.getMessage());

        // Everything else about the message apart from addition of encoding property
        // is the same though
        assertMessageEqualEncodingPropertyAdded(responseMessage, result.getMessage());

    }

    public void testDefaultFlowOneWay() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint( null, null, null,
            null, MessageExchangePattern.ONE_WAY, null);

        testOutboundEvent = createTestOutboundEvent(endpoint);
        //First Request creates the real endpoint
        endpoint.process(testOutboundEvent);
        Thread.sleep(1000);

        setUpFakeDispatcher(endpoint);

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        MuleEvent result = endpoint.process(testOutboundEvent);

        dispacher.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        assertMessageSentEqual(false);
        assertNull(result);
    }

    public void testSecurityFilterAccept() throws Exception
    {

        OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(true), null,
                null, MessageExchangePattern.REQUEST_RESPONSE, null);

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        testOutboundEvent = createTestOutboundEvent(endpoint);
        //First Request creates the real endpoint
        endpoint.process(testOutboundEvent);

        setUpFakeDispatcher(endpoint);
        MuleEvent result = endpoint.process(testOutboundEvent);

        assertMessageSentEqual(true);

        // Response message is not the same because we rewrite the response event and
        // this change the properties
        // (See: OutboundRewriteResponseEventMessageProcessor)
        assertNotSame(responseMessage, result.getMessage());

        // Everything else about the message apart from addition of encoding property
        // is the same though
        assertMessageEqualEncodingPropertyAdded(responseMessage, result.getMessage());
    }

    public void testSecurityFilterNotAccept() throws Exception
    {
        TestSecurityNotificationListener securityNotificationListener = new TestSecurityNotificationListener();
        muleContext.registerListener(securityNotificationListener);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, new TestSecurityFilter(false), null,
                null, MessageExchangePattern.REQUEST_RESPONSE, null);

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        testOutboundEvent = createTestOutboundEvent(endpoint);
        //First Request creates the real endpoint
        try
        {
            endpoint.process(testOutboundEvent);
            fail("Exception expected");
        }
        catch (TestSecurityFilter.StaticMessageUnauthorisedException e)
        {
            testOutboundEvent.getFlowConstruct().getExceptionListener().handleException(e, testOutboundEvent);
        }

        setUpFakeDispatcher(endpoint);
        try
        {
            endpoint.process(testOutboundEvent);
            fail("Exception expected");
        }
        catch (TestSecurityFilter.StaticMessageUnauthorisedException e)
        {
            testOutboundEvent.getFlowConstruct().getExceptionListener().handleException(e, testOutboundEvent);
        }

        assertMessageNotSent();

        assertTrue(securityNotificationListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(SecurityNotification.SECURITY_AUTHENTICATION_FAILED,
            securityNotificationListener.securityNotification.getAction());
        assertEquals(securityNotificationListener.securityNotification.getResourceIdentifier(),
            TestSecurityFilter.StaticMessageUnauthorisedException.class.getName());
    }

    public void testSendNotfication() throws Exception
    {
        TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        MuleEvent outboundEvent = createTestOutboundEvent(endpoint);
        endpoint.process(outboundEvent);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_SENT, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(outboundEvent.getMessage().getPayload(),
            ((MuleMessage) listener.messageNotification.getSource()).getPayload());
    }

    public void testDispatchNotfication() throws Exception
    {
        TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.ONE_WAY, null);
        MuleEvent outboundEvent = createTestOutboundEvent(endpoint);
        endpoint.process(outboundEvent);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_DISPATCHED, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(outboundEvent.getMessage().getPayload(),
            ((MuleMessage) listener.messageNotification.getSource()).getPayload());
    }

    public void testTransformers() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, new OutboundAppendTransformer(),
            new ResponseAppendTransformer(), MessageExchangePattern.REQUEST_RESPONSE, null);
        
         assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        testOutboundEvent = createTestOutboundEvent(endpoint);
        //First Request creates the real endpoint
        endpoint.process(testOutboundEvent);

        setUpFakeDispatcher(endpoint);
        MuleEvent result = endpoint.process(testOutboundEvent);

        assertMessageSent(true);

        assertEquals(TEST_MESSAGE + OutboundAppendTransformer.APPEND_STRING,
        dispacher.sensedSendEvent.getMessageAsString());

        assertNotNull(result);
        assertEquals(RESPONSE_MESSAGE + ResponseAppendTransformer.APPEND_STRING, result.getMessageAsString());
    }

    public void testConnectorNotStarted() throws Exception
    {
        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        testOutboundEvent = createTestOutboundEvent(endpoint);
        // Force the creation of the real endpoint
        endpoint.process(testOutboundEvent);
        endpoint.getConnector().stop();

        try
        {
            endpoint.process(testOutboundEvent);
            fail("exception expected");
        }
        catch (LifecycleException e)
        {
            // expected
        }
    }

    public void testTimeoutSetOnEvent() throws Exception
    {

        int testTimeout = 999;

        OutboundEndpoint endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);

        assertTrue(endpoint instanceof DynamicOutboundEndpoint);

        testOutboundEvent = createTestOutboundEvent(endpoint);
        testOutboundEvent.getMessage()
            .setOutboundProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, testTimeout);

        //First Request creates the real endpoint
        endpoint.process(testOutboundEvent);

        setUpFakeDispatcher(endpoint);
        endpoint.process(testOutboundEvent);

        assertEquals(testTimeout, dispacher.sensedSendEvent.getTimeout());
    }

    public void testTransaction()
    {
        // TODO
    }

    public void testExceptionHandling()
    {
        // TODO
    }

    protected MuleEvent assertMessageSent(boolean sync) throws MuleException
    {
        MuleEvent event;
        if (sync)
        {
            assertNull(dispacher.sensedDispatchEvent);
            assertNotNull(dispacher.sensedSendEvent);
            event = dispacher.sensedSendEvent;
        }
        else
        {
            assertNull(dispacher.sensedSendEvent);
            assertNotNull(dispacher.sensedDispatchEvent);
            event = dispacher.sensedDispatchEvent;
        }
        assertNotNull(event.getMessage());
        return event;
    }

    protected MuleEvent assertMessageSentEqual(boolean sync) throws MuleException
    {
        MuleEvent event = assertMessageSent(sync);
        assertEquals(TEST_MESSAGE, event.getMessageAsString());
        assertEquals("value1", event.getMessage().getOutboundProperty("prop1"));
        return event;
    }

    protected void assertMessageNotSent() throws MuleException
    {
        assertNull(dispacher.sensedSendEvent);
        assertNull(dispacher.sensedDispatchEvent);
    }

    protected void assertMessageEqualEncodingPropertyAdded(MuleMessage expect, MuleMessage actual)
    {
        assertEquals(expect.getPayload(), actual.getPayload());
        assertEquals(expect.getEncoding(), actual.getEncoding());
        assertEquals(expect.getUniqueId(), actual.getUniqueId());
        assertEquals(expect.getExceptionPayload(), actual.getExceptionPayload());

        // Outbound endcodin property is added
        assertEquals(muleContext.getConfiguration().getDefaultEncoding(),
                     actual.getOutboundProperty(MuleProperties.MULE_ENCODING_PROPERTY));

    }

    protected OutboundEndpoint createOutboundEndpoint(Filter filter,
                                                      SecurityFilter securityFilter,
                                                      Transformer in,
                                                      Transformer response,
                                                      MessageExchangePattern exchangePattern,
                                                      TransactionConfig txConfig) throws Exception
    {

        return createTestOutboundEndpoint("test://localhost:#[header:port]", filter, securityFilter, in, response,
            exchangePattern, txConfig);
    }

    static class FakeMessageDispatcher extends TestMessageDispatcher
    {
        Latch latch = new Latch();
        MuleEvent sensedSendEvent;
        MuleEvent sensedDispatchEvent;

        public FakeMessageDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected MuleMessage doSend(MuleEvent event) throws Exception
        {
            sensedSendEvent = event;
            latch.countDown();
            return responseMessage;
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            sensedDispatchEvent = event;
            latch.countDown();
        }
    }

    protected void setUpFakeDispatcher(OutboundEndpoint endpoint)
    {
        dispacher = new DynamicOutboundEndpointTestCase.FakeMessageDispatcher(endpoint);
        Connector connector = endpoint.getConnector();
        connector.setDispatcherFactory(new TestMessageDispatcherFactory()
        {
            @Override
            public MessageDispatcher create(OutboundEndpoint ep) throws MuleException
            {
                return dispacher;
            }

        });
    }

}
