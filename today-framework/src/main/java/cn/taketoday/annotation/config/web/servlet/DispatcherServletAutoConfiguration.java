/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.web.servlet;

import java.util.Set;

import cn.taketoday.annotation.config.web.WebMvcProperties;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionMessage;
import cn.taketoday.context.condition.ConditionMessage.Style;
import cn.taketoday.context.condition.ConditionOutcome;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.condition.InfraCondition;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.framework.web.servlet.MultipartConfigFactory;
import cn.taketoday.framework.web.servlet.ServletRegistrationBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.servlet.DispatcherServlet;
import jakarta.servlet.ServletRegistration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link DispatcherServlet}. Should work for a standalone application where an embedded
 * web server is already present and also for a deployable application
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(WebMvcProperties.class)
@ConditionalOnClass({ DispatcherServlet.class, ServletRegistration.class })
@DisableDIAutoConfiguration(after = ServletWebServerFactoryAutoConfiguration.class)
public class DispatcherServletAutoConfiguration {

  /**
   * The bean name for a DispatcherServlet that will be mapped to the root URL "/".
   */
  public static final String DEFAULT_DISPATCHER_SERVLET_BEAN_NAME = "dispatcherServlet";

  /**
   * The bean name for a ServletRegistrationBean for the DispatcherServlet "/".
   */
  public static final String DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME = "dispatcherServletRegistration";

  @Conditional(DefaultDispatcherServletCondition.class)
  @Component(name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
  static DispatcherServlet dispatcherServlet(WebMvcProperties webMvcProperties) {
    DispatcherServlet dispatcherServlet = new DispatcherServlet();
    dispatcherServlet.setThrowExceptionIfNoHandlerFound(webMvcProperties.throwExceptionIfNoHandlerFound);
    dispatcherServlet.setEnableLoggingRequestDetails(webMvcProperties.logRequestDetails);
    return dispatcherServlet;
  }

  @Component
  static CharacterEncodingServletInitializer characterEncodingInitializer(WebMvcProperties webMvcProperties) {
    var initializer = new CharacterEncodingServletInitializer();
    initializer.setRequestCharacterEncoding(webMvcProperties.servlet.requestEncoding);
    initializer.setResponseCharacterEncoding(webMvcProperties.servlet.responseEncoding);
    return initializer;
  }

  @Component
  @ConditionalOnProperty(prefix = "server.multipart", name = "enabled", matchIfMissing = true)
  @ConfigurationProperties(prefix = "server.multipart", ignoreUnknownFields = false)
  static MultipartConfigFactory multipartConfigFactory() {
    return new MultipartConfigFactory();
  }

  @Conditional(DispatcherServletRegistrationCondition.class)
  @Component(name = DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
  @ConditionalOnBean(value = DispatcherServlet.class, name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
  static DispatcherServletRegistrationBean dispatcherServletRegistration(
          @Nullable MultipartConfigFactory multipartConfigFactory,
          DispatcherServlet dispatcherServlet, WebMvcProperties webMvcProperties) {

    var registration = new DispatcherServletRegistrationBean(dispatcherServlet, webMvcProperties.servlet.path);
    registration.setName(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
    registration.setLoadOnStartup(webMvcProperties.servlet.loadOnStartup);

    if (multipartConfigFactory != null) {
      registration.setMultipartConfig(multipartConfigFactory.createMultipartConfig());
    }
    return registration;
  }

  @Order(Ordered.LOWEST_PRECEDENCE - 10)
  private static class DefaultDispatcherServletCondition extends InfraCondition {

    @Override
    public ConditionOutcome getMatchOutcome(
            ConditionContext context, AnnotatedTypeMetadata metadata) {
      var message = ConditionMessage.forCondition("Default DispatcherServlet");
      var beanFactory = context.getRequiredBeanFactory();
      var dispatchServletBeans = beanFactory.getBeanNamesForType(DispatcherServlet.class, false, false);
      if (dispatchServletBeans.contains(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
        return ConditionOutcome.noMatch(
                message.found("dispatcher servlet bean")
                        .items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        );
      }
      if (beanFactory.containsBean(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
        return ConditionOutcome.noMatch(
                message.found("non dispatcher servlet bean")
                        .items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        );
      }
      if (dispatchServletBeans.isEmpty()) {
        return ConditionOutcome.match(message.didNotFind("dispatcher servlet beans").atAll());
      }
      return ConditionOutcome.match(
              message.found("dispatcher servlet bean", "dispatcher servlet beans")
                      .items(Style.QUOTE, dispatchServletBeans)
                      .append("and none is named " + DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
      );
    }

  }

  @Order(Ordered.LOWEST_PRECEDENCE - 10)
  private static class DispatcherServletRegistrationCondition extends InfraCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      ConfigurableBeanFactory beanFactory = context.getRequiredBeanFactory();
      ConditionOutcome outcome = checkDefaultDispatcherName(beanFactory);
      if (!outcome.isMatch()) {
        return outcome;
      }
      return checkServletRegistration(beanFactory);
    }

    private ConditionOutcome checkDefaultDispatcherName(ConfigurableBeanFactory beanFactory) {
      boolean containsDispatcherBean = beanFactory.containsBean(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
      if (!containsDispatcherBean) {
        return ConditionOutcome.match();
      }
      Set<String> servlets = beanFactory.getBeanNamesForType(DispatcherServlet.class, false, false);
      if (!servlets.contains(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
        return ConditionOutcome.noMatch(
                startMessage()
                        .found("non dispatcher servlet")
                        .items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        );
      }
      return ConditionOutcome.match();
    }

    private ConditionOutcome checkServletRegistration(ConfigurableBeanFactory beanFactory) {
      var message = startMessage();
      var registrations = beanFactory.getBeanNamesForType(ServletRegistrationBean.class, false, false);
      boolean containsDispatcherRegistrationBean = beanFactory.containsBean(
              DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
      if (registrations.isEmpty()) {
        if (containsDispatcherRegistrationBean) {
          return ConditionOutcome.noMatch(
                  message.found("non servlet registration bean")
                          .items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
          );
        }
        return ConditionOutcome.match(message.didNotFind("servlet registration bean").atAll());
      }
      if (registrations.contains(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)) {
        return ConditionOutcome.noMatch(
                message.found("servlet registration bean")
                        .items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
        );
      }
      if (containsDispatcherRegistrationBean) {
        return ConditionOutcome.noMatch(
                message.found("non servlet registration bean")
                        .items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
        );
      }
      return ConditionOutcome.match(
              message.found("servlet registration beans")
                      .items(Style.QUOTE, registrations)
                      .append("and none is named " + DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
      );
    }

    private ConditionMessage.Builder startMessage() {
      return ConditionMessage.forCondition("DispatcherServlet Registration");
    }

  }

}
