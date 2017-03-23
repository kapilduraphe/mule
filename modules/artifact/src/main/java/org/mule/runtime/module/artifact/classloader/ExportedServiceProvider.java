/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import java.net.URL;

// TODO(pablo.kraan): SPI - add javadoc
public class ExportedServiceProvider {


  private final String serviceInterface;
  private final URL resource;

  public ExportedServiceProvider(String serviceInterface, URL resource) {
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
