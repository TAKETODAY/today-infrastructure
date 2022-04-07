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

package cn.taketoday.framework.web.servlet.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.framework.web.context.ServerPortInfoApplicationContextInitializer;
import cn.taketoday.framework.web.servlet.DelegatingFilterProxyRegistrationBean;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.framework.web.servlet.ServletRegistrationBean;
import cn.taketoday.framework.web.servlet.server.MockServletWebServerFactory;
import cn.taketoday.mock.web.MockFilterChain;
import cn.taketoday.mock.web.MockFilterConfig;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.context.support.SessionScope;
import cn.taketoday.web.servlet.ServletContextAware;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import cn.taketoday.web.servlet.filter.GenericFilterBean;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link ServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@ExtendWith({ OutputCaptureExtension.class, MockitoExtension.class })
class ServletWebServerApplicationContextTests {

  private ServletWebServerApplicationContext context = new ServletWebServerApplicationContext();

  @Captor
  private ArgumentCaptor<Filter> filterCaptor;

  @AfterEach
  void cleanup() {
    this.context.close();
  }

  @Test
  void startRegistrations() {
    addWebServerFactoryBean();
    this.context.refresh();
    MockServletWebServerFactory factory = getWebServerFactory();
    // Ensure that the context has been setup
    assertThat(this.context.getServletContext()).isEqualTo(factory.getServletContext());
    then(factory.getServletContext()).should()
            .setAttribute(WebServletApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
    // Ensure WebApplicationContextUtils.registerWebApplicationScopes was called
    assertThat(this.context.getBeanFactory().getRegisteredScope(WebApplicationContext.SCOPE_SESSION))
            .isInstanceOf(SessionScope.class);
    // Ensure WebApplicationContextUtils.registerEnvironmentBeans was called
    assertThat(this.context.containsBean(WebServletApplicationContext.SERVLET_CONTEXT_BEAN_NAME)).isTrue();
  }

  @Test
  void doesNotRegistersShutdownHook() {
    // See gh-314 for background. We no longer register the shutdown hook
    // since it is really the callers responsibility. The shutdown hook could
    // also be problematic in a classic WAR deployment.
    addWebServerFactoryBean();
    this.context.refresh();
    assertThat(this.context).hasFieldOrPropertyWithValue("shutdownHook", null);
  }

  @Test
  void ServletWebServerInitializedEventPublished() {
    addWebServerFactoryBean();
    this.context.registerBeanDefinition("listener", new RootBeanDefinition(TestApplicationListener.class));
    this.context.refresh();
    List<ApplicationEvent> events = this.context.getBean(TestApplicationListener.class).receivedEvents();
    assertThat(events).hasSize(2).extracting("class").containsExactly(ServletWebServerInitializedEvent.class,
            ContextRefreshedEvent.class);
    ServletWebServerInitializedEvent initializedEvent = (ServletWebServerInitializedEvent) events.get(0);
    assertThat(initializedEvent.getSource().getPort() >= 0).isTrue();
    assertThat(initializedEvent.getApplicationContext()).isEqualTo(this.context);
  }

  @Test
  void localPortIsAvailable() {
    addWebServerFactoryBean();
    new ServerPortInfoApplicationContextInitializer().initialize(this.context);
    this.context.refresh();
    ConfigurableEnvironment environment = this.context.getEnvironment();
    assertThat(environment.containsProperty("local.server.port")).isTrue();
    assertThat(environment.getProperty("local.server.port")).isEqualTo("8080");
  }

  @Test
  void stopOnClose() {
    addWebServerFactoryBean();
    this.context.refresh();
    MockServletWebServerFactory factory = getWebServerFactory();
    this.context.close();
    then(factory.getWebServer()).should().stop();
  }

  @Test
  void applicationIsUnreadyDuringShutdown() {
    TestApplicationListener listener = new TestApplicationListener();
    addWebServerFactoryBean();
    this.context.refresh();
    this.context.addApplicationListener(listener);
    this.context.close();
    assertThat(listener.receivedEvents())
            .hasSize(2)
            .extracting("class")
            .contains(AvailabilityChangeEvent.class, ContextClosedEvent.class);
  }

  @Test
  void whenContextIsNotActiveThenCloseDoesNotChangeTheApplicationAvailability() {
    addWebServerFactoryBean();
    TestApplicationListener listener = new TestApplicationListener();
    this.context.addApplicationListener(listener);
    this.context.registerBeanDefinition("refreshFailure", new RootBeanDefinition(RefreshFailure.class));
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(this.context::refresh);
    this.context.close();
    assertThat(listener.receivedEvents()).isEmpty();
  }

  @Test
  void cannotSecondRefresh() {
    addWebServerFactoryBean();
    this.context.refresh();
    assertThatIllegalStateException().isThrownBy(() -> this.context.refresh());
  }

  @Test
  void servletContextAwareBeansAreInjected() {
    addWebServerFactoryBean();
    ServletContextAware bean = mock(ServletContextAware.class);
    this.context.registerBeanDefinition("bean", beanDefinition(bean));
    this.context.refresh();
    then(bean).should().setServletContext(getWebServerFactory().getServletContext());
  }

  @Test
  void missingServletWebServerFactory() {
    assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() -> this.context.refresh())
            .havingCause()
            .isInstanceOf(ApplicationContextException.class)
            .withMessageContaining("Unable to start ServletWebServerApplicationContext due to missing "
                    + "ServletWebServerFactory bean");
  }

  @Test
  void tooManyWebServerFactories() {
    addWebServerFactoryBean();
    this.context.registerBeanDefinition("webServerFactory2",
            new RootBeanDefinition(MockServletWebServerFactory.class));
    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(() -> this.context.refresh())
            .havingCause()
            .isInstanceOf(ApplicationContextException.class)
            .withMessageContaining("Unable to start ServletWebServerApplicationContext due to "
                    + "multiple ServletWebServerFactory beans");

  }

  @Test
  void singleServletBean() {
    addWebServerFactoryBean();
    Servlet servlet = mock(Servlet.class);
    this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
    this.context.refresh();
    MockServletWebServerFactory factory = getWebServerFactory();
    then(factory.getServletContext()).should().addServlet("servletBean", servlet);
    then(factory.getRegisteredServlet(0).getRegistration()).should().addMapping("/");
  }

  @Test
  void orderedBeanInsertedCorrectly() {
    addWebServerFactoryBean();
    OrderedFilter filter = new OrderedFilter();
    this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
    registration.setFilter(mock(Filter.class));
    registration.setOrder(100);
    this.context.registerBeanDefinition("filterRegistrationBean", beanDefinition(registration));
    this.context.refresh();
    MockServletWebServerFactory factory = getWebServerFactory();
    then(factory.getServletContext()).should().addFilter("filterBean", filter);
    then(factory.getServletContext()).should().addFilter("object", registration.getFilter());
    assertThat(factory.getRegisteredFilter(0).getFilter()).isEqualTo(filter);
  }

  @Test
  void multipleServletBeans() {
    addWebServerFactoryBean();
    Servlet servlet1 = mock(Servlet.class, withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) servlet1).getOrder()).willReturn(1);
    Servlet servlet2 = mock(Servlet.class, withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) servlet2).getOrder()).willReturn(2);
    this.context.registerBeanDefinition("servletBean2", beanDefinition(servlet2));
    this.context.registerBeanDefinition("servletBean1", beanDefinition(servlet1));
    this.context.refresh();
    MockServletWebServerFactory factory = getWebServerFactory();
    ServletContext servletContext = factory.getServletContext();
    InOrder ordered = inOrder(servletContext);
    then(servletContext).should(ordered).addServlet("servletBean1", servlet1);
    then(servletContext).should(ordered).addServlet("servletBean2", servlet2);
    then(factory.getRegisteredServlet(0).getRegistration()).should().addMapping("/servletBean1/");
    then(factory.getRegisteredServlet(1).getRegistration()).should().addMapping("/servletBean2/");
  }

  @Test
  void multipleServletBeansWithMainDispatcher() {
    addWebServerFactoryBean();
    Servlet servlet1 = mock(Servlet.class, withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) servlet1).getOrder()).willReturn(1);
    Servlet servlet2 = mock(Servlet.class, withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) servlet2).getOrder()).willReturn(2);
    this.context.registerBeanDefinition("servletBean2", beanDefinition(servlet2));
    this.context.registerBeanDefinition("dispatcherServlet", beanDefinition(servlet1));
    this.context.refresh();
    MockServletWebServerFactory factory = getWebServerFactory();
    ServletContext servletContext = factory.getServletContext();
    InOrder ordered = inOrder(servletContext);
    then(servletContext).should(ordered).addServlet("dispatcherServlet", servlet1);
    then(servletContext).should(ordered).addServlet("servletBean2", servlet2);
    then(factory.getRegisteredServlet(0).getRegistration()).should().addMapping("/");
    then(factory.getRegisteredServlet(1).getRegistration()).should().addMapping("/servletBean2/");
  }

  @Test
  void servletAndFilterBeans() {
    addWebServerFactoryBean();
    Servlet servlet = mock(Servlet.class);
    Filter filter1 = mock(Filter.class, withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) filter1).getOrder()).willReturn(1);
    Filter filter2 = mock(Filter.class, withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) filter2).getOrder()).willReturn(2);
    this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
    this.context.registerBeanDefinition("filterBean2", beanDefinition(filter2));
    this.context.registerBeanDefinition("filterBean1", beanDefinition(filter1));
    this.context.refresh();
    MockServletWebServerFactory factory = getWebServerFactory();
    ServletContext servletContext = factory.getServletContext();
    InOrder ordered = inOrder(servletContext);
    then(factory.getServletContext()).should().addServlet("servletBean", servlet);
    then(factory.getRegisteredServlet(0).getRegistration()).should().addMapping("/");
    then(factory.getServletContext()).should(ordered).addFilter("filterBean1", filter1);
    then(factory.getServletContext()).should(ordered).addFilter("filterBean2", filter2);
    then(factory.getRegisteredFilter(0).getRegistration()).should()
            .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    then(factory.getRegisteredFilter(1).getRegistration()).should()
            .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
  }

  @Test
  void servletContextInitializerBeans() throws Exception {
    addWebServerFactoryBean();
    ServletContextInitializer initializer1 = mock(ServletContextInitializer.class,
            withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) initializer1).getOrder()).willReturn(1);
    ServletContextInitializer initializer2 = mock(ServletContextInitializer.class,
            withSettings().extraInterfaces(Ordered.class));
    given(((Ordered) initializer2).getOrder()).willReturn(2);
    this.context.registerBeanDefinition("initializerBean2", beanDefinition(initializer2));
    this.context.registerBeanDefinition("initializerBean1", beanDefinition(initializer1));
    this.context.refresh();
    ServletContext servletContext = getWebServerFactory().getServletContext();
    InOrder ordered = inOrder(initializer1, initializer2);
    then(initializer1).should(ordered).onStartup(servletContext);
    then(initializer2).should(ordered).onStartup(servletContext);
  }

  @Test
  void servletContextListenerBeans() {
    addWebServerFactoryBean();
    ServletContextListener initializer = mock(ServletContextListener.class);
    this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
    this.context.refresh();
    ServletContext servletContext = getWebServerFactory().getServletContext();
    then(servletContext).should().addListener(initializer);
  }

  @Test
  void unorderedServletContextInitializerBeans() throws Exception {
    addWebServerFactoryBean();
    ServletContextInitializer initializer1 = mock(ServletContextInitializer.class);
    ServletContextInitializer initializer2 = mock(ServletContextInitializer.class);
    this.context.registerBeanDefinition("initializerBean2", beanDefinition(initializer2));
    this.context.registerBeanDefinition("initializerBean1", beanDefinition(initializer1));
    this.context.refresh();
    ServletContext servletContext = getWebServerFactory().getServletContext();
    then(initializer1).should().onStartup(servletContext);
    then(initializer2).should().onStartup(servletContext);
  }

  @Test
  void servletContextInitializerBeansDoesNotSkipServletsAndFilters() throws Exception {
    addWebServerFactoryBean();
    ServletContextInitializer initializer = mock(ServletContextInitializer.class);
    Servlet servlet = mock(Servlet.class);
    Filter filter = mock(Filter.class);
    this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
    this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
    this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
    this.context.refresh();
    ServletContext servletContext = getWebServerFactory().getServletContext();
    then(initializer).should().onStartup(servletContext);
    then(servletContext).should().addServlet(anyString(), any(Servlet.class));
    then(servletContext).should().addFilter(anyString(), any(Filter.class));
  }

  @Test
  void servletContextInitializerBeansSkipsRegisteredServletsAndFilters() {
    addWebServerFactoryBean();
    Servlet servlet = mock(Servlet.class);
    Filter filter = mock(Filter.class);
    ServletRegistrationBean<Servlet> initializer = new ServletRegistrationBean<>(servlet, "/foo");
    this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
    this.context.registerBeanDefinition("servletBean", beanDefinition(servlet));
    this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
    this.context.refresh();
    ServletContext servletContext = getWebServerFactory().getServletContext();
    then(servletContext).should(atMost(1)).addServlet(anyString(), any(Servlet.class));
    then(servletContext).should(atMost(1)).addFilter(anyString(), any(Filter.class));
  }

  @Test
  void filterRegistrationBeansSkipsRegisteredFilters() {
    addWebServerFactoryBean();
    Filter filter = mock(Filter.class);
    FilterRegistrationBean<Filter> initializer = new FilterRegistrationBean<>(filter);
    this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
    this.context.registerBeanDefinition("filterBean", beanDefinition(filter));
    this.context.refresh();
    ServletContext servletContext = getWebServerFactory().getServletContext();
    then(servletContext).should(atMost(1)).addFilter(anyString(), any(Filter.class));
  }

  @Test
  void delegatingFilterProxyRegistrationBeansSkipsTargetBeanNames() {
    addWebServerFactoryBean();
    DelegatingFilterProxyRegistrationBean initializer = new DelegatingFilterProxyRegistrationBean("filterBean");
    this.context.registerBeanDefinition("initializerBean", beanDefinition(initializer));
    BeanDefinition filterBeanDefinition = beanDefinition(new IllegalStateException("Create FilterBean Failure"));
    filterBeanDefinition.setLazyInit(true);
    this.context.registerBeanDefinition("filterBean", filterBeanDefinition);
    this.context.refresh();
    ServletContext servletContext = getWebServerFactory().getServletContext();
    then(servletContext).should(atMost(1)).addFilter(anyString(), this.filterCaptor.capture());
    // Up to this point the filterBean should not have been created, calling
    // the delegate proxy will trigger creation and an exception
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> {
              this.filterCaptor.getValue().init(new MockFilterConfig());
              this.filterCaptor.getValue().doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                      new MockFilterChain());
            })
            .havingRootCause()
            .isInstanceOf(IllegalStateException.class)
            .withMessageContaining("Create FilterBean Failure");
  }

  @Test
  void postProcessWebServerFactory() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MockServletWebServerFactory.class);
    PropertyValues pv = new PropertyValues();
    pv.add("port", "${port}");
    beanDefinition.setPropertyValues(pv);
    this.context.registerBeanDefinition("webServerFactory", beanDefinition);
    PropertySourcesPlaceholderConfigurer propertySupport = new PropertySourcesPlaceholderConfigurer();
    Properties properties = new Properties();
    properties.put("port", 8080);
    propertySupport.setProperties(properties);
    this.context.registerBeanDefinition("propertySupport", beanDefinition(propertySupport));
    this.context.refresh();
    assertThat(getWebServerFactory().getWebServer().getPort()).isEqualTo(8080);
  }

  @Test
  void doesNotReplaceExistingScopes() {
    // gh-2082
    Scope scope = mock(Scope.class);
    ConfigurableBeanFactory factory = this.context.getBeanFactory();
    factory.registerScope(WebApplicationContext.SCOPE_REQUEST, scope);
    factory.registerScope(WebApplicationContext.SCOPE_SESSION, scope);
    addWebServerFactoryBean();
    this.context.refresh();
    assertThat(factory.getRegisteredScope(WebApplicationContext.SCOPE_REQUEST)).isSameAs(scope);
    assertThat(factory.getRegisteredScope(WebApplicationContext.SCOPE_SESSION)).isSameAs(scope);
  }

  @Test
  void servletRequestCanBeInjectedEarly(CapturedOutput output) {
    // gh-14990
    int initialOutputLength = output.length();
    addWebServerFactoryBean();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(WithAutowiredServletRequest.class);
    beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
    this.context.registerBeanDefinition("withAutowiredServletRequest", beanDefinition);
    this.context.addBeanFactoryPostProcessor((beanFactory) -> {
      WithAutowiredServletRequest bean = beanFactory.getBean(WithAutowiredServletRequest.class);
      assertThat(bean.getRequest()).isNotNull();
    });
    this.context.refresh();
    assertThat(output.toString().substring(initialOutputLength)).doesNotContain("Replacing scope");
  }

  @Test
  void webApplicationScopeIsRegistered() {
    addWebServerFactoryBean();
    this.context.refresh();
    assertThat(this.context.getBeanFactory().getRegisteredScope(WebApplicationContext.SCOPE_APPLICATION))
            .isNotNull();
  }

  private void addWebServerFactoryBean() {
    this.context.registerBeanDefinition("webServerFactory",
            new RootBeanDefinition(MockServletWebServerFactory.class));
  }

  MockServletWebServerFactory getWebServerFactory() {
    return this.context.getBean(MockServletWebServerFactory.class);
  }

  private BeanDefinition beanDefinition(Object bean) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setBeanClass(getClass());
    beanDefinition.setFactoryMethodName("getBean");
    ConstructorArgumentValues constructorArguments = new ConstructorArgumentValues();
    constructorArguments.addGenericArgumentValue(bean);
    beanDefinition.setConstructorArgumentValues(constructorArguments);
    return beanDefinition;
  }

  static <T> T getBean(T object) {
    if (object instanceof RuntimeException) {
      throw (RuntimeException) object;
    }
    return object;
  }

  static class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

    private Deque<ApplicationEvent> events = new ArrayDeque<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      this.events.add(event);
    }

    List<ApplicationEvent> receivedEvents() {
      List<ApplicationEvent> receivedEvents = new ArrayList<>();
      while (!this.events.isEmpty()) {
        receivedEvents.add(this.events.pollFirst());
      }
      return receivedEvents;
    }

  }

  @Order(10)
  static class OrderedFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    }

  }

  static class WithAutowiredServletRequest {

    private final ServletRequest request;

    WithAutowiredServletRequest(ServletRequest request) {
      this.request = request;
    }

    ServletRequest getRequest() {
      return this.request;
    }

  }

  static class RefreshFailure {

    RefreshFailure() {
      throw new RuntimeException("Fail refresh");
    }

  }

}
