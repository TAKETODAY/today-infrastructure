/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.scripting.config;

import org.w3c.dom.Element;

import java.util.List;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionDefaults;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.xml.AbstractBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.BeanDefinitionParserDelegate;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.beans.factory.xml.XmlReaderContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scripting.support.ScriptFactoryPostProcessor;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.xml.DomUtils;

/**
 * BeanDefinitionParser implementation for the '{@code <lang:groovy/>}',
 * '{@code <lang:std/>}' and '{@code <lang:bsh/>}' tags.
 * Allows for objects written using dynamic languages to be easily exposed with
 * the {@link cn.taketoday.beans.factory.BeanFactory}.
 *
 * <p>The script for each object can be specified either as a reference to the
 * resource containing it (using the '{@code script-source}' attribute) or inline
 * in the XML configuration itself (using the '{@code inline-script}' attribute.
 *
 * <p>By default, dynamic objects created with these tags are <strong>not</strong>
 * refreshable. To enable refreshing, specify the refresh check delay for each
 * object (in milliseconds) using the '{@code refresh-check-delay}' attribute.
 *
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:49
 */
class ScriptBeanDefinitionParser extends AbstractBeanDefinitionParser {

  private static final String ENGINE_ATTRIBUTE = "engine";

  private static final String SCRIPT_SOURCE_ATTRIBUTE = "script-source";

  private static final String INLINE_SCRIPT_ELEMENT = "inline-script";

  private static final String SCOPE_ATTRIBUTE = "scope";

  private static final String AUTOWIRE_ATTRIBUTE = "autowire";

  private static final String DEPENDS_ON_ATTRIBUTE = "depends-on";

  private static final String INIT_METHOD_ATTRIBUTE = "init-method";

  private static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";

  private static final String SCRIPT_INTERFACES_ATTRIBUTE = "script-interfaces";

  private static final String REFRESH_CHECK_DELAY_ATTRIBUTE = "refresh-check-delay";

  private static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

  private static final String CUSTOMIZER_REF_ATTRIBUTE = "customizer-ref";

  /**
   * The {@link cn.taketoday.scripting.ScriptFactory} class that this
   * parser instance will create bean definitions for.
   */
  private final String scriptFactoryClassName;

  /**
   * Create a new instance of this parser, creating bean definitions for the
   * supplied {@link cn.taketoday.scripting.ScriptFactory} class.
   *
   * @param scriptFactoryClassName the ScriptFactory class to operate on
   */
  public ScriptBeanDefinitionParser(String scriptFactoryClassName) {
    this.scriptFactoryClassName = scriptFactoryClassName;
  }

  /**
   * Parses the dynamic object element and returns the resulting bean definition.
   * Registers a {@link ScriptFactoryPostProcessor} if needed.
   */
  @Override
  @SuppressWarnings("deprecation")
  @Nullable
  protected BeanDefinition parseInternal(Element element, ParserContext parserContext) {
    // Engine attribute only supported for <lang:std>
    String engine = element.getAttribute(ENGINE_ATTRIBUTE);

    // Resolve the script source.
    String value = resolveScriptSource(element, parserContext.getReaderContext());
    if (value == null) {
      return null;
    }

    // Set up infrastructure.
    LangNamespaceUtils.registerScriptFactoryPostProcessorIfNecessary(parserContext.getRegistry());

    // Create script factory bean definition.
    BeanDefinition bd = new BeanDefinition();
    bd.setBeanClassName(this.scriptFactoryClassName);
    bd.setSource(parserContext.extractSource(element));
    bd.setAttribute(ScriptFactoryPostProcessor.LANGUAGE_ATTRIBUTE, element.getLocalName());

    // Determine bean scope.
    String scope = element.getAttribute(SCOPE_ATTRIBUTE);
    if (StringUtils.isNotEmpty(scope)) {
      bd.setScope(scope);
    }

    // Determine autowire mode.
    String autowire = element.getAttribute(AUTOWIRE_ATTRIBUTE);
    int autowireMode = parserContext.getDelegate().getAutowireMode(autowire);
    // Only "byType" and "byName" supported, but maybe other default inherited...
    if (autowireMode == BeanDefinition.AUTOWIRE_AUTODETECT) {
      autowireMode = BeanDefinition.AUTOWIRE_BY_TYPE;
    }
    else if (autowireMode == BeanDefinition.AUTOWIRE_CONSTRUCTOR) {
      autowireMode = BeanDefinition.AUTOWIRE_NO;
    }
    bd.setAutowireMode(autowireMode);

    // Parse depends-on list of bean names.
    String dependsOn = element.getAttribute(DEPENDS_ON_ATTRIBUTE);
    if (StringUtils.isNotEmpty(dependsOn)) {
      bd.setDependsOn(StringUtils.tokenizeToStringArray(
              dependsOn, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS));
    }

    // Retrieve the defaults for bean definitions within this parser context
    BeanDefinitionDefaults beanDefinitionDefaults = parserContext.getDelegate().getBeanDefinitionDefaults();

    // Determine init method and destroy method.
    String initMethod = element.getAttribute(INIT_METHOD_ATTRIBUTE);
    if (StringUtils.isNotEmpty(initMethod)) {
      bd.setInitMethods(initMethod);
    }
    else if (beanDefinitionDefaults.getInitMethodName() != null) {
      bd.setInitMethods(beanDefinitionDefaults.getInitMethodName());
    }

    if (element.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
      String destroyMethod = element.getAttribute(DESTROY_METHOD_ATTRIBUTE);
      bd.setDestroyMethod(destroyMethod);
    }
    else if (beanDefinitionDefaults.getDestroyMethodName() != null) {
      bd.setDestroyMethod(beanDefinitionDefaults.getDestroyMethodName());
    }

    // Attach any refresh metadata.
    String refreshCheckDelay = element.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);
    if (StringUtils.hasText(refreshCheckDelay)) {
      bd.setAttribute(ScriptFactoryPostProcessor.REFRESH_CHECK_DELAY_ATTRIBUTE, Long.valueOf(refreshCheckDelay));
    }

    // Attach any proxy target class metadata.
    String proxyTargetClass = element.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE);
    if (StringUtils.hasText(proxyTargetClass)) {
      bd.setAttribute(ScriptFactoryPostProcessor.PROXY_TARGET_CLASS_ATTRIBUTE, Boolean.valueOf(proxyTargetClass));
    }

    // Add constructor arguments.
    ConstructorArgumentValues cav = bd.getConstructorArgumentValues();
    int constructorArgNum = 0;
    if (StringUtils.isNotEmpty(engine)) {
      cav.addIndexedArgumentValue(constructorArgNum++, engine);
    }
    cav.addIndexedArgumentValue(constructorArgNum++, value);
    if (element.hasAttribute(SCRIPT_INTERFACES_ATTRIBUTE)) {
      cav.addIndexedArgumentValue(
              constructorArgNum++, element.getAttribute(SCRIPT_INTERFACES_ATTRIBUTE), "java.lang.Class[]");
    }

    // This is used for Groovy. It's a bean reference to a customizer bean.
    if (element.hasAttribute(CUSTOMIZER_REF_ATTRIBUTE)) {
      String customizerBeanName = element.getAttribute(CUSTOMIZER_REF_ATTRIBUTE);
      if (!StringUtils.hasText(customizerBeanName)) {
        parserContext.getReaderContext().error("Attribute 'customizer-ref' has empty value", element);
      }
      else {
        cav.addIndexedArgumentValue(constructorArgNum++, new RuntimeBeanReference(customizerBeanName));
      }
    }

    // Add any property definitions that need adding.
    parserContext.getDelegate().parsePropertyElements(element, bd);

    return bd;
  }

  /**
   * Resolves the script source from either the '{@code script-source}' attribute or
   * the '{@code inline-script}' element. Logs and {@link XmlReaderContext#error} and
   * returns {@code null} if neither or both of these values are specified.
   */
  @Nullable
  private String resolveScriptSource(Element element, XmlReaderContext readerContext) {
    boolean hasScriptSource = element.hasAttribute(SCRIPT_SOURCE_ATTRIBUTE);
    List<Element> elements = DomUtils.getChildElementsByTagName(element, INLINE_SCRIPT_ELEMENT);
    if (hasScriptSource && !elements.isEmpty()) {
      readerContext.error("Only one of 'script-source' and 'inline-script' should be specified.", element);
      return null;
    }
    else if (hasScriptSource) {
      return element.getAttribute(SCRIPT_SOURCE_ATTRIBUTE);
    }
    else if (!elements.isEmpty()) {
      Element inlineElement = elements.get(0);
      return "inline:" + DomUtils.getTextValue(inlineElement);
    }
    else {
      readerContext.error("Must specify either 'script-source' or 'inline-script'.", element);
      return null;
    }
  }

  /**
   * Scripted beans may be anonymous as well.
   */
  @Override
  protected boolean shouldGenerateIdAsFallback() {
    return true;
  }

}
