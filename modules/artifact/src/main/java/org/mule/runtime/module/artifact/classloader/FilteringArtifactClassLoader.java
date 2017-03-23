/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static java.lang.Boolean.valueOf;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.identityHashCode;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import org.mule.runtime.module.artifact.classloader.exception.NotExportedClassException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a {@link ClassLoader} that filter which classes and resources can be resolved based on a {@link ClassLoaderFilter}
 */
public class FilteringArtifactClassLoader extends ClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  protected static final Logger logger = LoggerFactory.getLogger(FilteringArtifactClassLoader.class);

  private final ArtifactClassLoader artifactClassLoader;
  private final ClassLoaderFilter filter;
  private final List<ExportedServiceProvider> exportedServiceProviders;

  /**
   * Creates a new filtering classLoader
   * 
   * @param artifactClassLoader artifact classLoader to filter. Non null
   * @param filter filters access to classes and resources from the artifact classLoader. Non null
   * @param exportedServiceProviders
   */
  public FilteringArtifactClassLoader(ArtifactClassLoader artifactClassLoader, ClassLoaderFilter filter,
                                      List<ExportedServiceProvider> exportedServiceProviders) {
    checkArgument(artifactClassLoader != null, "ArtifactClassLoader cannot be null");
    checkArgument(filter != null, "Filter cannot be null");
    // TODO(pablo.kraan): SPI - fix javadoc
    // TODO(pablo.kraan): SPI - check arguments
    this.artifactClassLoader = artifactClassLoader;
    this.filter = filter;
    this.exportedServiceProviders = exportedServiceProviders;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (filter.exportsClass(name)) {
      return artifactClassLoader.getClassLoader().loadClass(name);
    } else {
      throw new NotExportedClassException(name, getArtifactId(), filter);
    }
  }

  @Override
  public URL getResource(String name) {

    String servicePath = "META-INF/services/";

    if (name.contains(servicePath)) {
      String serviceInterface = name.substring(name.indexOf(servicePath) + servicePath.length());

      //System.out.println("ZARAZA getResource for service: " + serviceInterface);
      Optional<ExportedServiceProvider> serviceProvider =
          exportedServiceProviders.stream().filter(s -> serviceInterface.equals(s.getServiceInterface())).findFirst();

      if (serviceProvider.isPresent()) {
        System.out
            .println("Artifact: " + this.getArtifactId() + "EXPORTED SERVICE FOUND AT: " + serviceProvider.get().getResource());
        return serviceProvider.get().getResource();
      } else {
        System.out
            .println("Artifact: " + this.getArtifactId() + "EXPORTED SERVICE NOT FOUND RETRYING WITH STANDARD RESOURCE LOOKUP");
      }
    }

    URL result = null;
    if (filter.exportsResource(name)) {
      URL resourceFromDelegate = getResourceFromDelegate(artifactClassLoader, name);
      System.out.println("Artifact: " + this.getArtifactId() + " true exports: " + name + " : " + resourceFromDelegate);
      result = resourceFromDelegate;
    } else {
      System.out.println("Artifact: " + this.getArtifactId() + " false exports: " + name);
      logClassloadingTrace(format("Resource '%s' not found in classloader for '%s'.", name, getArtifactId()));
      logClassloadingTrace(format("Filter applied for resource '%s': %s", name, getArtifactId()));
    }

    return result;
  }

  protected URL getResourceFromDelegate(ArtifactClassLoader artifactClassLoader, String name) {
    return artifactClassLoader.findResource(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    String servicePath = "META-INF/services/";

    if (name.contains(servicePath)) {
      String serviceInterface = name.substring(name.indexOf(servicePath) + servicePath.length());

      //System.out.println("ZARAZA getResource for service: " + serviceInterface);

      List<URL> exportedServiceProviders =
          this.exportedServiceProviders.stream().filter(s -> serviceInterface.equals(s.getServiceInterface()))
              .map(s -> s.getResource())
              .collect(Collectors.toList());

      if (!exportedServiceProviders.isEmpty()) {
        System.out.println("Artifact: " + this.getArtifactId() + "EXPORTED SERVICES FOUND AT: " + exportedServiceProviders);
        return new EnumerationAdapter<>(exportedServiceProviders);
      } else {
        System.out
            .println("Artifact: " + this.getArtifactId() + "EXPORTED SERVICES NOT FOUND RETRYING WITH STANDARD RESOURCE LOOKUP");
      }
    }

    if (filter.exportsResource(name)) {
      Enumeration<URL> resourcesFromDelegate = getResourcesFromDelegate(artifactClassLoader, name);
      List<URL> list = Collections.list(resourcesFromDelegate);
      System.out.println("Artifact: " + this.getArtifactId() + " true exports: " + name + " : " + list);

      return new EnumerationAdapter<>(list);
    } else {
      System.out.println("Artifact: " + this.getArtifactId() + " false exports: " + name);
      logClassloadingTrace(format("Resources '%s' not found in classloader for '%s'.", name, getArtifactId()));
      logClassloadingTrace(format("Filter applied for resources '%s': %s", name, getArtifactId()));
      return new EnumerationAdapter<>(emptyList());
    }
  }

  private void logClassloadingTrace(String message) {
    if (isVerboseClassLoading()) {
      logger.info(message);
    } else if (logger.isTraceEnabled()) {
      logger.trace(message);
    }
  }

  private Boolean isVerboseClassLoading() {
    return valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

  protected Enumeration<URL> getResourcesFromDelegate(ArtifactClassLoader artifactClassLoader, String name) throws IOException {
    return artifactClassLoader.findResources(name);
  }

  @Override
  public URL findResource(String name) {
    return artifactClassLoader.findResource(name);
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return artifactClassLoader.findResources(name);
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    return artifactClassLoader.findLocalClass(name);
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), artifactClassLoader.getArtifactId(), toHexString(identityHashCode(this)));
  }

  @Override
  public String getArtifactId() {
    return artifactClassLoader.getArtifactId();
  }

  @Override
  public <T extends ArtifactDescriptor> T getArtifactDescriptor() {
    return artifactClassLoader.getArtifactDescriptor();
  }

  @Override
  public ClassLoader getClassLoader() {
    return this;
  }

  @Override
  public void addShutdownListener(ShutdownListener listener) {
    artifactClassLoader.addShutdownListener(listener);
  }

  @Override
  public ClassLoaderLookupPolicy getClassLoaderLookupPolicy() {
    return artifactClassLoader.getClassLoaderLookupPolicy();
  }

  @Override
  public void dispose() {
    // Nothing to do here as this is just wrapper for another classLoader
  }

  @Override
  public URL findLocalResource(String resourceName) {
    URL localResource = artifactClassLoader.findLocalResource(resourceName);
    System.out.println("Artifact: " + this.getArtifactId() + " findLocalResource: " + resourceName + " : " + localResource);
    return localResource;
  }
}
