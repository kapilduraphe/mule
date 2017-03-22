/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.api;

// TODO(pablo.kraan): SPI - add javadocs
public class ExportedService {

  private final String serviceInterface;
  private final String serviceImplementation;

  public ExportedService(String serviceInterface, String serviceImplementation) {
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
