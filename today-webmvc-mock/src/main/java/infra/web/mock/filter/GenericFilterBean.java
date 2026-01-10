/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.mock.filter;

import org.jspecify.annotations.Nullable;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import infra.beans.BeanWrapper;
import infra.beans.BeansException;
import infra.beans.PropertyValue;
import infra.beans.PropertyValues;
import infra.beans.factory.BeanNameAware;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.context.EnvironmentAware;
import infra.core.env.Environment;
import infra.core.env.EnvironmentCapable;
import infra.core.io.Resource;
import infra.core.io.ResourceEditor;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.mock.api.Filter;
import infra.mock.api.FilterConfig;
import infra.mock.api.GenericMock;
import infra.mock.api.MockContext;
import infra.mock.api.MockException;
import infra.util.CollectionUtils;
import infra.util.StringUtils;
import infra.web.mock.MockContextAware;
import infra.web.mock.support.MockContextResourceLoader;
import infra.web.mock.support.StandardMockEnvironment;
import infra.web.mock.support.WebApplicationContextUtils;

/**
 * Simple base implementation of {@link Filter} which treats
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
 * implement the {@link Filter#doFilter} method.
 *
 * <p>This generic filter base class has no dependency on the Framework
 * {@link infra.context.ApplicationContext} concept.
 * Filters usually don't load their own context but rather access service
 * beans from the Framework root application context, accessible via the
 * filter's {@link #getMockContext() MockContext} (see
 * {@link WebApplicationContextUtils}).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initFilterBean
 * @see #doFilter
 * @since 4.0
 */
public abstract class GenericFilterBean implements Filter, BeanNameAware, EnvironmentAware,
        EnvironmentCapable, MockContextAware, InitializingBean, DisposableBean {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private String beanName;

  @Nullable
  private Environment environment;

  @Nullable
  private MockContext mockContext;

  @Nullable
  private FilterConfig filterConfig;

  private final Set<String> requiredProperties = new HashSet<>(4);

  /**
   * Stores the bean name as defined in the Framework bean factory.
   * <p>Only relevant in case of initialization as bean, to have a name as
   * fallback to the filter name usually provided by a FilterConfig instance.
   *
   * @see BeanNameAware
   * @see #getFilterName()
   */
  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  /**
   * Set the {@code Environment} that this filter runs in.
   * <p>Any environment set here overrides the {@link StandardMockEnvironment}
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
   * Create and return a new {@link StandardMockEnvironment}.
   * <p>Subclasses may override this in order to configure the environment or
   * specialize the environment type returned.
   */
  protected Environment createEnvironment() {
    return new StandardMockEnvironment();
  }

  /**
   * Stores the MockContext that the bean factory runs in.
   * <p>Only relevant in case of initialization as bean, to have a MockContext
   * as fallback to the context usually provided by a FilterConfig instance.
   *
   * @see MockContextAware
   * @see #getMockContext()
   */
  @Override
  public void setMockContext(MockContext mockContext) {
    this.mockContext = mockContext;
  }

  /**
   * Calls the {@code initFilterBean()} method that might
   * contain custom initialization of a subclass.
   * <p>Only relevant in case of initialization as bean, where the
   * standard {@code init(FilterConfig)} method won't be called.
   *
   * @see #initFilterBean()
   * @see #init(FilterConfig)
   */
  @Override
  public void afterPropertiesSet() throws MockException {
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
   * @throws MockException if bean properties are invalid (or required
   * properties are missing), or if subclass initialization fails.
   * @see #initFilterBean
   */
  @Override
  public final void init(FilterConfig filterConfig) throws MockException {
    Assert.notNull(filterConfig, "FilterConfig is required");
    this.filterConfig = filterConfig;

    // Set bean properties from init parameters.
    PropertyValues pvs = getFilterConfigPropertyValues(filterConfig, requiredProperties);
    if (!pvs.isEmpty()) {
      try {
        BeanWrapper bw = BeanWrapper.forBeanPropertyAccess(this);
        ResourceLoader resourceLoader = new MockContextResourceLoader(filterConfig.getMockContext());
        Environment env = this.environment;
        if (env == null) {
          env = new StandardMockEnvironment();
        }
        bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, env));
        initBeanWrapper(bw);
        bw.setPropertyValues(pvs, true);
      }
      catch (BeansException ex) {
        String msg = "Failed to set bean properties on filter '%s': %s".formatted(filterConfig.getFilterName(), ex.getMessage());
        logger.error(msg, ex);
        throw new MockException(msg, ex);
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
   * @see BeanWrapper#registerCustomEditor
   */
  protected void initBeanWrapper(BeanWrapper bw) throws BeansException { }

  /**
   * Subclasses may override this to perform custom initialization.
   * All bean properties of this filter will have been set before this
   * method is invoked.
   * <p>Note: This method will be called from standard filter initialization
   * as well as filter bean initialization in a Framework application context.
   * Filter name and MockContext will be available in both cases.
   * <p>This default implementation is empty.
   *
   * @throws MockException if subclass initialization fails
   * @see #getFilterName()
   * @see #getMockContext()
   */
  protected void initFilterBean() throws MockException { }

  /**
   * Make the FilterConfig of this filter available, if any.
   * Analogous to GenericServlet's {@code getServletConfig()}.
   * <p>Public to resemble the {@code getFilterConfig()} method
   * of the Servlet Filter version that shipped with WebLogic 6.1.
   *
   * @return the FilterConfig instance, or {@code null} if none available
   * @see GenericMock#getMockConfig()
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
   * @see GenericMock#getMockName()
   * @see FilterConfig#getFilterName()
   * @see #setBeanName
   */
  @Nullable
  protected String getFilterName() {
    return filterConfig != null ? filterConfig.getFilterName() : this.beanName;
  }

  /**
   * Make the MockContext of this filter available to subclasses.
   * Analogous to GenericServlet's {@code getMockContext()}.
   * <p>Takes the FilterConfig's MockContext by default.
   * If initialized as bean in a Framework application context,
   * it falls back to the MockContext that the bean factory runs in.
   *
   * @return the MockContext instance
   * @throws IllegalStateException if no MockContext is available
   * @see GenericMock#getMockContext()
   * @see FilterConfig#getMockContext()
   * @see #setMockContext
   */
  protected MockContext getMockContext() {
    if (this.filterConfig != null) {
      return this.filterConfig.getMockContext();
    }
    else if (this.mockContext != null) {
      return this.mockContext;
    }
    else {
      throw new IllegalStateException("No MockContext");
    }
  }

  /**
   * @param config the FilterConfig we'll use to take PropertyValues from
   * @param requiredProperties set of property names we need, where
   * we can't accept default values
   * @return PropertyValues
   * @throws MockException if any required properties are missing
   */
  private PropertyValues getFilterConfigPropertyValues(FilterConfig config, Set<String> requiredProperties) throws MockException {
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
      throw new MockException(
              "Initialization from FilterConfig for filter '%s' failed; the following required properties were missing: %s"
                      .formatted(config.getFilterName(), StringUtils.collectionToDelimitedString(missingProps, ", ")));
    }
    return propertyValues;
  }

}
