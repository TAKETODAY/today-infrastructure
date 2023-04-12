/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.web;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.FileSystemResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.support.AbstractContextLoader;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.support.GenericWebApplicationContext;
import jakarta.servlet.ServletContext;

/**
 * Abstract, generic extension of {@link AbstractContextLoader} that loads a
 * {@link GenericWebApplicationContext}.
 *
 * <p>If instances of concrete subclasses are invoked via the
 * {@link SmartContextLoader SmartContextLoader}
 * SPI, the context will be loaded from the {@link MergedContextConfiguration}
 * provided to {@link #loadContext(MergedContextConfiguration)}. In such cases, a
 * {@code SmartContextLoader} will decide whether to load the context from
 * <em>locations</em> or <em>annotated classes</em>. Note that {@code
 * AbstractGenericWebContextLoader} does not support the {@code
 * loadContext(String... locations)} method from the legacy
 * {@link ContextLoader ContextLoader} SPI.
 *
 * <p>Concrete subclasses must provide an appropriate implementation of
 * {@link #loadBeanDefinitions}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see #loadContext(MergedContextConfiguration)
 * @see #loadContext(String...)
 * @since 4.0
 */
public abstract class AbstractGenericWebContextLoader extends AbstractContextLoader {

  protected static final Logger logger = LoggerFactory.getLogger(AbstractGenericWebContextLoader.class);

  // SmartContextLoader

  /**
   * Load a Infra {@link WebApplicationContext} from the supplied
   * {@link MergedContextConfiguration}.
   * <p>Implementation details:
   * <ul>
   * <li>Calls {@link #validateMergedContextConfiguration(WebMergedContextConfiguration)}
   * to allow subclasses to validate the supplied configuration before proceeding.</li>
   * <li>Creates a {@link GenericWebApplicationContext} instance.</li>
   * <li>If the supplied {@code MergedContextConfiguration} references a
   * {@linkplain MergedContextConfiguration#getParent() parent configuration},
   * the corresponding {@link MergedContextConfiguration#getParentApplicationContext()
   * ApplicationContext} will be retrieved and
   * {@linkplain GenericWebApplicationContext#setParent(ApplicationContext) set as the parent}
   * for the context created by this method.</li>
   * <li>Delegates to {@link #configureWebResources} to create the
   * {@link MockServletContext} and set it in the {@code WebServletApplicationContext}.</li>
   * <li>Calls {@link #prepareContext} to allow for customizing the context
   * before bean definitions are loaded.</li>
   * <li>Calls {@link #customizeBeanFactory} to allow for customizing the
   * context's {@code StandardBeanFactory}.</li>
   * <li>Delegates to {@link #loadBeanDefinitions} to populate the context
   * from the locations or classes in the supplied {@code MergedContextConfiguration}.</li>
   * <li>Delegates to {@link AnnotationConfigUtils} for
   * {@linkplain AnnotationConfigUtils#registerAnnotationConfigProcessors registering}
   * annotation configuration processors.</li>
   * <li>Calls {@link #customizeContext} to allow for customizing the context
   * before it is refreshed.</li>
   * <li>{@link ConfigurableApplicationContext#refresh Refreshes} the
   * context and registers a JVM shutdown hook for it.</li>
   * </ul>
   *
   * @return a new web application context
   * @see SmartContextLoader#loadContext(MergedContextConfiguration)
   * @see GenericWebApplicationContext
   */
  @Override
  public final ConfigurableApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
    Assert.isTrue(mergedConfig instanceof WebMergedContextConfiguration,
            () -> String.format("Cannot load WebServletApplicationContext from non-web merged context configuration %s. " +
                    "Consider annotating your test class with @WebAppConfiguration.", mergedConfig));

    WebMergedContextConfiguration webMergedConfig = (WebMergedContextConfiguration) mergedConfig;

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Loading WebServletApplicationContext for merged context configuration %s.",
              webMergedConfig));
    }

    validateMergedContextConfiguration(webMergedConfig);

    GenericWebApplicationContext context = new GenericWebApplicationContext();

    ApplicationContext parent = mergedConfig.getParentApplicationContext();
    if (parent != null) {
      context.setParent(parent);
    }
    configureWebResources(context, webMergedConfig);
    prepareContext(context, webMergedConfig);
    customizeBeanFactory(context.getBeanFactory(), webMergedConfig);
    loadBeanDefinitions(context, webMergedConfig);
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    customizeContext(context, webMergedConfig);
    context.refresh();
    context.registerShutdownHook();
    return context;
  }

  /**
   * Validate the supplied {@link WebMergedContextConfiguration} with respect to
   * what this context loader supports.
   * <p>The default implementation is a <em>no-op</em> but can be overridden by
   * subclasses as appropriate.
   *
   * @param mergedConfig the merged configuration to validate
   * @throws IllegalStateException if the supplied configuration is not valid
   * for this context loader
   * @since 4.0
   */
  protected void validateMergedContextConfiguration(WebMergedContextConfiguration mergedConfig) {
    // no-op
  }

  /**
   * Configures web resources for the supplied web application context (WAC).
   * <h4>Implementation Details</h4>
   * <p>If the supplied WAC has no parent or its parent is not a WAC, the
   * supplied WAC will be configured as the Root WAC (see "<em>Root WAC
   * Configuration</em>" below).
   * <p>Otherwise the context hierarchy of the supplied WAC will be traversed
   * to find the top-most WAC (i.e., the root); and the {@link ServletContext}
   * of the Root WAC will be set as the {@code ServletContext} for the supplied
   * WAC.
   * <h4>Root WAC Configuration</h4>
   * <ul>
   * <li>The resource base path is retrieved from the supplied
   * {@code WebMergedContextConfiguration}.</li>
   * <li>A {@link ResourceLoader} is instantiated for the {@link MockServletContext}:
   * if the resource base path is prefixed with "{@code classpath:}", a
   * {@link DefaultResourceLoader} will be used; otherwise, a
   * {@link FileSystemResourceLoader} will be used.</li>
   * <li>A {@code MockServletContext} will be created using the resource base
   * path and resource loader.</li>
   * <li>The supplied {@link GenericWebApplicationContext} is then stored in
   * the {@code MockServletContext} under the
   * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} key.</li>
   * <li>Finally, the {@code MockServletContext} is set in the
   * {@code WebServletApplicationContext}.</li>
   * </ul>
   *
   * @param context the web application context for which to configure the web resources
   * @param webMergedConfig the merged context configuration to use to load the web application context
   */
  protected void configureWebResources(GenericWebApplicationContext context,
          WebMergedContextConfiguration webMergedConfig) {

    ApplicationContext parent = context.getParent();

    // If the WebServletApplicationContext has no parent or the parent is not a WebServletApplicationContext,
    // set the current context as the root WebServletApplicationContext:
    if (!(parent instanceof WebApplicationContext)) {
      String resourceBasePath = webMergedConfig.getResourceBasePath();
      ResourceLoader resourceLoader = (resourceBasePath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX) ?
                                       new DefaultResourceLoader() : new FileSystemResourceLoader());
      ServletContext servletContext = new MockServletContext(resourceBasePath, resourceLoader);
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
      context.setServletContext(servletContext);
    }
    else {
      ServletContext servletContext = null;
      // Find the root WebServletApplicationContext
      while (parent != null) {
        if (parent instanceof WebApplicationContext && !(parent.getParent() instanceof WebApplicationContext)) {
          servletContext = ((WebApplicationContext) parent).getServletContext();
          break;
        }
        parent = parent.getParent();
      }
      Assert.state(servletContext != null, "Failed to find root WebServletApplicationContext in the context hierarchy");
      context.setServletContext(servletContext);
    }
  }

  /**
   * Customize the internal bean factory of the {@code WebServletApplicationContext}
   * created by this context loader.
   * <p>The default implementation is empty but can be overridden in subclasses
   * to customize {@code StandardBeanFactory}'s standard settings.
   *
   * @param beanFactory the bean factory created by this context loader
   * @param webMergedConfig the merged context configuration to use to load the
   * web application context
   * @see #loadContext(MergedContextConfiguration)
   * @see StandardBeanFactory#setAllowBeanDefinitionOverriding
   * @see StandardBeanFactory#setAllowEagerClassLoading
   * @see StandardBeanFactory#setAllowCircularReferences
   * @see StandardBeanFactory#setAllowRawInjectionDespiteWrapping
   */
  protected void customizeBeanFactory(
          StandardBeanFactory beanFactory, WebMergedContextConfiguration webMergedConfig) {
  }

  /**
   * Load bean definitions into the supplied {@link GenericWebApplicationContext context}
   * from the locations or classes in the supplied {@code WebMergedContextConfiguration}.
   * <p>Concrete subclasses must provide an appropriate implementation.
   *
   * @param context the context into which the bean definitions should be loaded
   * @param webMergedConfig the merged context configuration to use to load the
   * web application context
   * @see #loadContext(MergedContextConfiguration)
   */
  protected abstract void loadBeanDefinitions(
          GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig);

  /**
   * Customize the {@link GenericWebApplicationContext} created by this context
   * loader <i>after</i> bean definitions have been loaded into the context but
   * <i>before</i> the context is refreshed.
   * <p>The default implementation simply delegates to
   * {@link AbstractContextLoader#customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)}.
   *
   * @param context the newly created web application context
   * @param webMergedConfig the merged context configuration to use to load the
   * web application context
   * @see #loadContext(MergedContextConfiguration)
   * @see #customizeContext(ConfigurableApplicationContext, MergedContextConfiguration)
   */
  protected void customizeContext(
          GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {

    super.customizeContext(context, webMergedConfig);
  }

  // ContextLoader

  /**
   * {@code AbstractGenericWebContextLoader} should be used as a
   * {@link SmartContextLoader SmartContextLoader},
   * not as a legacy {@link ContextLoader ContextLoader}.
   * Consequently, this method is not supported.
   *
   * @throws UnsupportedOperationException in this implementation
   * @see ContextLoader#loadContext(String[])
   */
  @Override
  public final ApplicationContext loadContext(String... locations) throws Exception {
    throw new UnsupportedOperationException(
            "AbstractGenericWebContextLoader does not support the loadContext(String... locations) method");
  }

}
