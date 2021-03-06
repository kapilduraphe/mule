/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.app.declaration.ConfigurationElementDeclaration;
import org.mule.runtime.api.app.declaration.ElementDeclaration;
import org.mule.runtime.api.app.declaration.OperationElementDeclaration;
import org.mule.runtime.api.app.declaration.SourceElementDeclaration;
import org.mule.runtime.api.app.declaration.TopLevelParameterDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ElementDeclarer;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeclarationElementModelFactoryTestCase {

  private static final String NAMESPACE = "mockns";
  private static final String NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/mockns";
  private static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/mockns/current/mule-mockns.xsd";
  private static final String CONTENT_NAME = "myCamelCaseName";
  private static final String BEHAVIOUR_NAME = "otherName";
  private static final String EXTENSION_NAME = "extension";
  private static final String OPERATION_NAME = "mockOperation";
  private static final String SOURCE_NAME = "source";
  private static final String CONFIGURATION_NAME = "configuration";
  private static final String CONNECTION_PROVIDER_NAME = "connection";
  private static final BaseTypeBuilder TYPE_BUILDER = BaseTypeBuilder.create(JAVA);

  @Mock
  private ExtensionModel extension;

  @Mock
  private ConfigurationModel configuration;

  @Mock
  private OperationModel operation;

  @Mock
  private ConnectionProviderModel connectionProvider;

  @Mock
  private ParameterModel contentParameter;

  @Mock
  private ParameterModel behaviourParameter;

  @Mock
  private ParameterGroupModel parameterGroupModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceModel source;

  @Mock
  private DslResolvingContext dslContext;

  @Mock
  private TypeCatalog typeCatalog;

  private ObjectType complexType;

  private ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Before
  public void before() {
    initMocks(this);

    initializeExtensionMock(extension);

    when(configuration.getName()).thenReturn(CONFIGURATION_NAME);
    when(configuration.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(configuration.getOperationModels()).thenReturn(asList(operation));
    when(configuration.getSourceModels()).thenReturn(asList(source));
    when(configuration.getConnectionProviders()).thenReturn(asList(connectionProvider));

    when(behaviourParameter.getName()).thenReturn(BEHAVIOUR_NAME);
    when(behaviourParameter.getExpressionSupport()).thenReturn(ExpressionSupport.NOT_SUPPORTED);
    when(behaviourParameter.getModelProperty(any())).thenReturn(empty());
    when(behaviourParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(behaviourParameter.getLayoutModel()).thenReturn(empty());
    when(behaviourParameter.getRole()).thenReturn(BEHAVIOUR);
    when(behaviourParameter.getType()).thenReturn(TYPE_LOADER.load(String.class));

    when(contentParameter.getName()).thenReturn(CONTENT_NAME);
    when(contentParameter.getExpressionSupport()).thenReturn(ExpressionSupport.SUPPORTED);
    when(contentParameter.getModelProperty(any())).thenReturn(empty());
    when(contentParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(contentParameter.getLayoutModel()).thenReturn(empty());
    when(contentParameter.getRole()).thenReturn(CONTENT);

    ObjectTypeBuilder type = TYPE_BUILDER.objectType();
    type.addField().key("field").value(TYPE_LOADER.load(String.class)).build();
    when(contentParameter.getType()).thenReturn(type.build());

    when(parameterGroupModel.getName()).thenReturn("GENERAL");
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    when(parameterGroupModel.getParameterModels()).thenReturn(asList(contentParameter));

    when(source.getName()).thenReturn(SOURCE_NAME);
    when(source.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(source.getSuccessCallback()).thenReturn(empty());
    when(source.getErrorCallback()).thenReturn(empty());
    when(operation.getName()).thenReturn(OPERATION_NAME);
    when(operation.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    visitableMock(operation, source);

    when(connectionProvider.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProvider.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));

    when(typeCatalog.getSubTypes(any())).thenReturn(emptySet());
    when(typeCatalog.getSuperTypes(any())).thenReturn(emptySet());
    when(typeCatalog.getAllBaseTypes()).thenReturn(emptySet());
    when(typeCatalog.getAllSubTypes()).thenReturn(emptySet());
    when(typeCatalog.getTypes()).thenReturn(emptySet());

    complexType = (ObjectType) TYPE_LOADER.load(ComplexTypePojo.class);
    when(typeCatalog.getType(any())).thenReturn(Optional.of(complexType));
    when(typeCatalog.containsBaseType(any())).thenReturn(false);

    when(dslContext.getExtension(any())).thenReturn(Optional.of(extension));
    when(dslContext.getExtensions()).thenReturn(singleton(extension));
    when(dslContext.getTypeCatalog()).thenReturn(typeCatalog);

    Stream.of(configuration, operation, connectionProvider, source)
        .forEach(model -> when(model.getAllParameterModels())
            .thenReturn(asList(contentParameter, behaviourParameter)));
  }

  private void initializeExtensionMock(ExtensionModel extension) {
    when(extension.getName()).thenReturn(EXTENSION_NAME);
    when(extension.getXmlDslModel()).thenReturn(XmlDslModel.builder()
        .setXsdFileName("mule-mockns.xsd")
        .setPrefix(NAMESPACE)
        .setNamespace(NAMESPACE_URI)
        .setSchemaLocation(SCHEMA_LOCATION)
        .setSchemaVersion("4.0")
        .build());
    when(extension.getSubTypes()).thenReturn(emptySet());
    when(extension.getImportedTypes()).thenReturn(emptySet());
    when(extension.getXmlDslModel()).thenReturn(XmlDslModel.builder()
        .setXsdFileName(EMPTY)
        .setPrefix(NAMESPACE)
        .setNamespace(NAMESPACE_URI)
        .setSchemaLocation(SCHEMA_LOCATION)
        .setSchemaVersion(EMPTY)
        .build());

    when(extension.getConfigurationModels()).thenReturn(asList(configuration));
    when(extension.getOperationModels()).thenReturn(asList(operation));
    when(extension.getSourceModels()).thenReturn(asList(source));
    when(extension.getConnectionProviders()).thenReturn(asList(connectionProvider));
  }

  protected <T> DslElementModel<T> create(ElementDeclaration declaration) {
    Optional<DslElementModel<T>> elementModel = DslElementModelFactory.getDefault(dslContext).create(declaration);
    if (!elementModel.isPresent()) {
      fail("Could not create element model for declared element: " + declaration.getName());
    }
    return elementModel.get();
  }


  @Test
  public void testConfigDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    ConfigurationElementDeclaration declaration = ext.newConfiguration(CONFIGURATION_NAME)
        .withRefName("sample")
        .withConnection(ext.newConnection(CONNECTION_PROVIDER_NAME)
            .withParameter(CONTENT_NAME, "#[{field: value}]")
            .getDeclaration())
        .withParameter(BEHAVIOUR_NAME, "additional")
        .getDeclaration();

    DslElementModel<ConfigurationModel> element = create(declaration);
    assertThat(element.getModel(), is(configuration));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONNECTION_PROVIDER_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONTENT_NAME).get().getConfiguration().get().getValue().get(), is("#[{field: value}]"));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is("additional"));

  }

  @Test
  public void testOperationDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    OperationElementDeclaration declaration = ext.newOperation(OPERATION_NAME)
        .withConfig(CONFIGURATION_NAME)
        .withParameter(BEHAVIOUR_NAME, "additional")
        .withParameter(CONTENT_NAME, "#[{field: value}]")
        .getDeclaration();

    DslElementModel<OperationModel> element = create(declaration);
    assertThat(element.getModel(), is(operation));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONTENT_NAME).get().getConfiguration().get().getValue().get(), is("#[{field: value}]"));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is("additional"));
  }

  @Test
  public void testSourceDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    SourceElementDeclaration declaration = ext.newSource(SOURCE_NAME)
        .withConfig(CONFIGURATION_NAME)
        .withParameter(BEHAVIOUR_NAME, "additional")
        .withParameter(CONTENT_NAME, "#[{field: value}]")
        .getDeclaration();

    DslElementModel<SourceModel> element = create(declaration);
    assertThat(element.getModel(), is(source));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONTENT_NAME).get().getConfiguration().get().getValue().get(), is("#[{field: value}]"));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is("additional"));
  }

  @Test
  public void testGlobalParameterDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    TopLevelParameterDeclaration declaration = ext.newGlobalParameter(SOURCE_NAME)
        .withRefName("globalParameter")
        .withValue(newObjectValue()
            .ofType(getId(complexType))
            .withParameter(BEHAVIOUR_NAME, "additional")
            .withParameter(CONTENT_NAME, "#[{field: value}]")
            .build())
        .getDeclaration();

    DslElementModel<SourceModel> element = create(declaration);
    assertThat(element.getModel(), is(complexType));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement(builder()
        .withName("my-camel-case-name")
        .withNamespace("mockns")
        .build()).get().getConfiguration().get()
        .getValue().get(), is("#[{field: value}]"));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is("additional"));
  }

  @Test
  public void testConfigNoConnectionNoParams() {

    ConfigurationModel emptyConfig = mock(ConfigurationModel.class);
    when(emptyConfig.getName()).thenReturn(CONFIGURATION_NAME);
    when(emptyConfig.getParameterGroupModels()).thenReturn(emptyList());
    when(emptyConfig.getOperationModels()).thenReturn(emptyList());
    when(emptyConfig.getSourceModels()).thenReturn(emptyList());
    when(emptyConfig.getConnectionProviders()).thenReturn(emptyList());

    ExtensionModel extensionModel = mock(ExtensionModel.class);
    initializeExtensionMock(extensionModel);
    when(extensionModel.getConfigurationModels()).thenReturn(asList(emptyConfig));

    ConfigurationElementDeclaration declaration =
        ElementDeclarer.forExtension(EXTENSION_NAME).newConfiguration(CONFIGURATION_NAME)
            .withRefName("sample")
            .getDeclaration();

    DslElementModel<ConfigurationModel> element = create(declaration);
    assertThat(element.getModel(), is(configuration));
    assertThat(element.getContainedElements().isEmpty(), is(true));
  }

  @Test
  public void testConfigNoParams() {

    ConfigurationElementDeclaration declaration = ElementDeclarer.forExtension(EXTENSION_NAME)
        .newConfiguration(CONFIGURATION_NAME)
        .withRefName("sample")
        .getDeclaration();

    DslElementModel<ConfigurationModel> element = create(declaration);
    assertThat(element.getModel(), is(configuration));
    assertThat(element.getContainedElements().isEmpty(), is(true));
  }


  @XmlHints(allowTopLevelDefinition = true)
  public static class ComplexTypePojo {

    @Parameter
    private String otherName;

    @Parameter
    @Content
    private String myCamelCaseName;

    public String getOtherName() {
      return otherName;
    }

    public void setOtherName(String otherName) {
      this.otherName = otherName;
    }

    public String getMyCamelCaseName() {
      return myCamelCaseName;
    }

    public void setMyCamelCaseName(String myCamelCaseName) {
      this.myCamelCaseName = myCamelCaseName;
    }
  }
}
