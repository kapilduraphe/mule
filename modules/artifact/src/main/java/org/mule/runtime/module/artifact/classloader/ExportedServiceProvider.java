/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.net.URL;

/**
 * Defines a service provider to be exported via SPI
 */
public class ExportedServiceProvider {


  private final String serviceInterface;
  private final URL resource;

  /**
   * Creates a new service provider
   *
   * @param serviceInterface fully qualified name of the interface that defines the service to be located using SPI. Non empty.
   * @param resource resource to be returned when {code serviceInterface} is searched via SPI. Non null.
   */
  public ExportedServiceProvider(String serviceInterface, URL resource) {
    checkArgument(!isEmpty(serviceInterface), "serviceInterface cannot be empty");
    checkArgument(resource != null, "resource cannot be null");

    this.serviceInterface = serviceInterface;
    this.resource = resource;
  }

  public String getServiceInterface() {
    return serviceInterface;
  }

  public URL getResource() {
    return resource;
  }
}
