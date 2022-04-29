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

package cn.taketoday.web.servlet.filter;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceEditor;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.context.support.ServletContextResourceLoader;
import cn.taketoday.web.context.support.StandardServletEnvironment;
import cn.taketoday.web.servlet.NestedServletException;
import cn.taketoday.web.servlet.ServletContextAware;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Simple base implementation of {@link jakarta.servlet.Filter} which treats
 * its config parameters ({@code init-param} entries within the
 * {@code filter} tag in {@code web.xml}) as bean properties.
 *
 * <p>A handy superclass for any type of filter. Type conversion of config
 * parameters is automatic, with the corresponding setter method getting
 * invoked with the converted value. It is also possible for subclasses to
 * specify required properties. Parameters without matching bean property
 * setter will simply be ignored.
 *
 * <p>This filter leaves actual filtering to subclasses, which have to
 * implement the {@link jakarta.servlet.Filter#doFilter} method.
 *
 * <p>This generic filter base class has no dependency on the Framework
 * {@link cn.taketoday.context.ApplicationContext} concept.
 * Filters usually don't load their own context but rather access service
 * beans from the Framework root application context, accessible via the
 * filter's {@link #getServletContext() ServletContext} (see
 * {@link cn.taketoday.web.context.support.WebApplicationContextUtils}).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initFilterBean
 * @see #doFilter
 * @since 4.0
 */
public abstract class GenericFilterBean implements Filter, BeanNameAware, EnvironmentAware,
        EnvironmentCapable, ServletContextAware, InitializingBean, DisposableBean {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private String beanName;

  @Nullable
  private Environment environment;

  @Nullable
  private ServletContext servletContext;

  @Nullable
  private FilterConfig filterConfig;

  private final Set<String> requiredProperties = new HashSet<>(4);

  /**
   * Stores the bean name as defined in the Framework bean factory.
   * <p>Only relevant in case of initialization as bean, to have a name as
   * fallback to the filter name usually provided by a FilterConfig instance.
   *
   * @see cn.taketoday.beans.factory.BeanNameAware
   * @see #getFilterName()
   */
  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  /**
   * Set the {@code Environment} that this filter runs in.
   * <p>Any environment set here overrides the {@link StandardServletEnvironment}
   * provided by default.
   * <p>This {@code Environment} object is used only for resolving placeholders in
   * resource paths passed into init-parameters for this filter. If no init-params are
   * used, this {@code Environment} can be essentially ignored.
   */
  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /**
   * Return the {@link Environment} associated with this filter.
   * <p>If none specified, a default environment will be initialized via
   * {@link #createEnvironment()}.
   */
  @Override
  public Environment getEnvironment() {
    if (this.environment == null) {
      this.environment = createEnvironment();
    }
    return this.environment;
  }

  /**
   * Create and return a new {@link StandardServletEnvironment}.
   * <p>Subclasses may override this in order to configure the environment or
   * specialize the environment type returned.
   */
  protected Environment createEnvironment() {
    return new StandardServletEnvironment();
  }

  /**
   * Stores the ServletContext that the bean factory runs in.
   * <p>Only relevant in case of initialization as bean, to have a ServletContext
   * as fallback to the context usually provided by a FilterConfig instance.
   *
   * @see cn.taketoday.web.servlet.ServletContextAware
   * @see #getServletContext()
   */
  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  /**
   * Calls the {@code initFilterBean()} method that might
   * contain custom initialization of a subclass.
   * <p>Only relevant in case of initialization as bean, where the
   * standard {@code init(FilterConfig)} method won't be called.
   *
   * @see #initFilterBean()
   * @see #init(jakarta.servlet.FilterConfig)
   */
  @Override
  public void afterPropertiesSet() throws ServletException {
    initFilterBean();
  }

  /**
   * Subclasses may override this to perform custom filter shutdown.
   * <p>Note: This method will be called from standard filter destruction
   * as well as filter bean destruction in a Framework application context.
   * <p>This default implementation is empty.
   */
  @Override
  public void destroy() { }

  /**
   * Subclasses can invoke this method to specify that this property
   * (which must match a JavaBean property they expose) is mandatory,
   * and must be supplied as a config parameter. This should be called
   * from the constructor of a subclass.
   * <p>This method is only relevant in case of traditional initialization
   * driven by a FilterConfig instance.
   *
   * @param property name of the required property
   */
  protected final void addRequiredProperty(String property) {
    this.requiredProperties.add(property);
  }

  /**
   * Standard way of initializing this filter.
   * Map config parameters onto bean properties of this filter, and
   * invoke subclass initialization.
   *
   * @param filterConfig the configuration for this filter
   * @throws ServletException if bean properties are invalid (or required
   * properties are missing), or if subclass initialization fails.
   * @see #initFilterBean
   */
  @Override
  public final void init(FilterConfig filterConfig) throws ServletException {
    Assert.notNull(filterConfig, "FilterConfig must not be null");
    this.filterConfig = filterConfig;

    // Set bean properties from init parameters.
    PropertyValues pvs = getFilterConfigPropertyValues(filterConfig, requiredProperties);
    if (!pvs.isEmpty()) {
      try {
        BeanWrapper bw = BeanWrapper.forBeanPropertyAccess(this);
        ResourceLoader resourceLoader = new ServletContextResourceLoader(filterConfig.getServletContext());
        Environment env = this.environment;
        if (env == null) {
          env = new StandardServletEnvironment();
        }
        bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, env));
        initBeanWrapper(bw);
        bw.setPropertyValues(pvs, true);
      }
      catch (BeansException ex) {
        String msg = "Failed to set bean properties on filter '" +
                filterConfig.getFilterName() + "': " + ex.getMessage();
        logger.error(msg, ex);
        throw new NestedServletException(msg, ex);
      }
    }

    // Let subclasses do whatever initialization they like.
    initFilterBean();

    if (logger.isDebugEnabled()) {
      logger.debug("Filter '{}' configured for use", filterConfig.getFilterName());
    }
  }

  /**
   * Initialize the BeanWrapper for this GenericFilterBean,
   * possibly with custom editors.
   * <p>This default implementation is empty.
   *
   * @param bw the BeanWrapper to initialize
   * @throws BeansException if thrown by BeanWrapper methods
   * @see cn.taketoday.beans.BeanWrapper#registerCustomEditor
   */
  protected void initBeanWrapper(BeanWrapper bw) throws BeansException { }

  /**
   * Subclasses may override this to perform custom initialization.
   * All bean properties of this filter will have been set before this
   * method is invoked.
   * <p>Note: This method will be called from standard filter initialization
   * as well as filter bean initialization in a Framework application context.
   * Filter name and ServletContext will be available in both cases.
   * <p>This default implementation is empty.
   *
   * @throws ServletException if subclass initialization fails
   * @see #getFilterName()
   * @see #getServletContext()
   */
  protected void initFilterBean() throws ServletException { }

  /**
   * Make the FilterConfig of this filter available, if any.
   * Analogous to GenericServlet's {@code getServletConfig()}.
   * <p>Public to resemble the {@code getFilterConfig()} method
   * of the Servlet Filter version that shipped with WebLogic 6.1.
   *
   * @return the FilterConfig instance, or {@code null} if none available
   * @see jakarta.servlet.GenericServlet#getServletConfig()
   */
  @Nullable
  public FilterConfig getFilterConfig() {
    return this.filterConfig;
  }

  /**
   * Make the name of this filter available to subclasses.
   * Analogous to GenericServlet's {@code getServletName()}.
   * <p>Takes the FilterConfig's filter name by default.
   * If initialized as bean in a Framework application context,
   * it falls back to the bean name as defined in the bean factory.
   *
   * @return the filter name, or {@code null} if none available
   * @see jakarta.servlet.GenericServlet#getServletName()
   * @see jakarta.servlet.FilterConfig#getFilterName()
   * @see #setBeanName
   */
  @Nullable
  protected String getFilterName() {
    return filterConfig != null ? filterConfig.getFilterName() : this.beanName;
  }

  /**
   * Make the ServletContext of this filter available to subclasses.
   * Analogous to GenericServlet's {@code getServletContext()}.
   * <p>Takes the FilterConfig's ServletContext by default.
   * If initialized as bean in a Framework application context,
   * it falls back to the ServletContext that the bean factory runs in.
   *
   * @return the ServletContext instance
   * @throws IllegalStateException if no ServletContext is available
   * @see jakarta.servlet.GenericServlet#getServletContext()
   * @see jakarta.servlet.FilterConfig#getServletContext()
   * @see #setServletContext
   */
  protected ServletContext getServletContext() {
    if (this.filterConfig != null) {
      return this.filterConfig.getServletContext();
    }
    else if (this.servletContext != null) {
      return this.servletContext;
    }
    else {
      throw new IllegalStateException("No ServletContext");
    }
  }

  /**
   * @param config the FilterConfig we'll use to take PropertyValues from
   * @param requiredProperties set of property names we need, where
   * we can't accept default values
   * @return PropertyValues
   * @throws ServletException if any required properties are missing
   */
  private PropertyValues getFilterConfigPropertyValues(
          FilterConfig config, Set<String> requiredProperties) throws ServletException {
    PropertyValues propertyValues = new PropertyValues();
    Set<String> missingProps = CollectionUtils.isNotEmpty(requiredProperties)
                               ? new HashSet<>(requiredProperties) : null;

    Enumeration<String> paramNames = config.getInitParameterNames();
    while (paramNames.hasMoreElements()) {
      String property = paramNames.nextElement();
      Object value = config.getInitParameter(property);
      propertyValues.add(new PropertyValue(property, value));
      if (missingProps != null) {
        missingProps.remove(property);
      }
    }

    // Fail if we are still missing properties.
    if (CollectionUtils.isNotEmpty(missingProps)) {
      throw new ServletException(
              "Initialization from FilterConfig for filter '" + config.getFilterName() +
                      "' failed; the following required properties were missing: " +
                      StringUtils.collectionToDelimitedString(missingProps, ", "));
    }
    return propertyValues;
  }

}
