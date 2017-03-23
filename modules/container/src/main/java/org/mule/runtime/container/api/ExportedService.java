/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.api;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

/**
 * Defines a service that will be exported by a module to other Mule artifacts via SPI.
 */
public class ExportedService {

  private final String serviceInterface;
  private final String serviceImplementation;

  /**
   * Create a new service
   *
   * @param serviceInterface fully qualified name of the interface that defines the service to be located using SPI. Non empty.
   * @param serviceImplementation fully qualified name of the class implementing the service. Non empty.
   */
  public ExportedService(String serviceInterface, String serviceImplementation) {
    checkArgument(!isEmpty(serviceInterface), "serviceInterface cannot be empty");
    checkArgument(!isEmpty(serviceImplementation), "serviceImplementation cannot be empty");

    this.serviceInterface = serviceInterface;
    this.serviceImplementation = serviceImplementation;
  }

  public String getServiceInterface() {
    return serviceInterface;
  }

  public String getServiceImplementation() {
    return serviceImplementation;
  }
}
