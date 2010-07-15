/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl.routers;

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.outbound.AbstractOutboundRouter;

/**
 * TODO
 */
public class ContentBasedRouter extends AbstractOutboundRouter
{
    @Override
    public MuleEvent route(MuleEvent theEvent) throws MessagingException
    {
        MuleMessage message = theEvent.getMessage();

        for (MessageProcessor target : targets)
        {
            if (isMatch(message))
            {
                try
                {
                    MuleEvent event = RequestContext.cloneAndUpdateEventEndpoint(theEvent, target);
                        return target.process(event);
                }
                catch (MuleException e)
                {
                    throw new MessagingException(e.getI18nMessage(), message, e);
                }
            }
        }
        //TODO
        throw new RuntimeException("Event not processed");
    }

    public boolean isMatch(MuleMessage message) throws MessagingException
    {
        for (MessageProcessor target : targets)
        {
            if (target instanceof ImmutableEndpoint)
            {
                ImmutableEndpoint endpoint = (ImmutableEndpoint)target;
                if (endpoint.getFilter() == null || endpoint.getFilter().accept(message))
                {
                    return true;
                }
            }
            else
            {
                return true;
            }
        }
        return false;
    }
}
