/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.web;

import java.util.List;
import java.util.Map;

import infra.annotation.ConditionalOnWebApplication;
import infra.aop.framework.autoproxy.AutoProxyUtils;
import infra.beans.BeansException;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionMessage;
import infra.context.condition.ConditionOutcome;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.condition.InfraCondition;
import infra.context.condition.SearchStrategy;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.type.AnnotatedTypeMetadata;
import infra.http.MediaType;
import infra.lang.Nullable;
import infra.logging.LoggerFactory;
import infra.stereotype.Component;
import infra.ui.template.TemplateAvailabilityProviders;
import infra.web.DispatcherHandler;
import infra.web.RequestContext;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.server.ServerProperties;
import infra.web.server.error.BasicErrorController;
import infra.web.server.error.DefaultErrorAttributes;
import infra.web.server.error.DefaultErrorViewResolver;
import infra.web.server.error.ErrorAttributes;
import infra.web.server.error.ErrorController;
import infra.web.server.error.ErrorViewResolver;
import infra.web.util.HtmlUtils;
import infra.web.view.BeanNameViewResolver;
import infra.web.view.View;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to render errors via an MVC error
 * controller.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
// Load before the main WebMvcAutoConfiguration so that the error View is available
@ConditionalOnWebApplication
@ConditionalOnClass({ DispatcherHandler.class })
@DisableDIAutoConfiguration(before = WebMvcAutoConfiguration.class)
@EnableConfigurationProperties({ ServerProperties.class, WebMvcProperties.class })
public class ErrorMvcAutoConfiguration {

  @Component
  @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
  public static DefaultErrorAttributes errorAttributes() {
    return new DefaultErrorAttributes();
  }

  @Component
  @ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
  static BasicErrorController basicErrorController(ErrorAttributes errorAttributes, ServerProperties serverProperties,
          List<ErrorViewResolver> errorViewResolvers, ReturnValueHandlerManager returnValueHandler) {
    return new BasicErrorController(errorAttributes,
            serverProperties.error, errorViewResolvers, returnValueHandler);
  }

  @Component
  static PreserveErrorControllerTargetClassPostProcessor preserveErrorControllerTargetClassPostProcessor() {
    return new PreserveErrorControllerTargetClassPostProcessor();
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({ WebProperties.class, WebMvcProperties.class })
  static class DefaultErrorViewResolverConfiguration {

    @Component
    @ConditionalOnMissingBean(ErrorViewResolver.class)
    DefaultErrorViewResolver conventionErrorViewResolver(
            ApplicationContext applicationContext, WebProperties webProperties) {
      return new DefaultErrorViewResolver(applicationContext, webProperties.resources.staticLocations);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "server.error.whitelabel", name = "enabled", matchIfMissing = true)
  @Conditional(ErrorTemplateMissingCondition.class)
  protected static class WhitelabelErrorViewConfiguration {

    @Component(name = "error")
    @ConditionalOnMissingBean(name = "error")
    static View defaultErrorView() {
      return new StaticView();
    }

    // If the user adds @EnableWebMvc then the bean name view resolver from
    // WebMvcAutoConfiguration disappears, so add it back in to avoid disappointment.
    @Component
    @ConditionalOnMissingBean
    static BeanNameViewResolver beanNameViewResolver() {
      return WebMvcAutoConfiguration.beanNameViewResolver();
    }

  }

  /**
   * {@link InfraCondition} that matches when no error template view is detected.
   */
  private static class ErrorTemplateMissingCondition extends InfraCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      ConditionMessage.Builder message = ConditionMessage.forCondition("ErrorTemplate Missing");
      var providers = new TemplateAvailabilityProviders(context.getClassLoader());
      var provider = providers.getProvider("error", context.getEnvironment(),
              context.getClassLoader(), context.getResourceLoader());
      if (provider != null) {
        return ConditionOutcome.noMatch(message.foundExactly("template from " + provider));
      }
      return ConditionOutcome.match(message.didNotFind("error template view").atAll());
    }

  }

  /**
   * Simple {@link View} implementation that writes a default HTML error page.
   */
  private static class StaticView implements View {

    @Override
    public void render(Map<String, ?> model, RequestContext request) throws Exception {
      if (request.isCommitted()) {
        String message = getMessage(model);
        LoggerFactory.getLogger(StaticView.class).error(message);
        return;
      }
      request.setContentType(MediaType.TEXT_HTML);
      StringBuilder builder = new StringBuilder();
      Object timestamp = model.get("timestamp");
      Object message = model.get("message");
      Object trace = model.get("trace");

      if (request.getContentType() == null) {
        request.setContentType(getContentType());
      }

      builder.append("<html><body><h1>Whitelabel Error Page</h1>")
              .append("<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>")
              .append("<div id='created'>").append(timestamp).append("</div>")
              .append("<div>There was an unexpected error (type=").append(htmlEscape(model.get("error")))
              .append(", status=").append(htmlEscape(model.get("status"))).append(").</div>");
      if (message != null) {
        builder.append("<div>").append(htmlEscape(message)).append("</div>");
      }
      if (trace != null) {
        builder.append("<div style='white-space:pre-wrap;'>").append(htmlEscape(trace)).append("</div>");
      }
      builder.append("</body></html>");
      request.getWriter().append(builder.toString());
    }

    @Nullable
    private String htmlEscape(@Nullable Object input) {
      return input != null ? HtmlUtils.htmlEscape(input.toString()) : null;
    }

    private String getMessage(Map<String, ?> model) {
      Object path = model.get("path");
      String message = "Cannot render error page for request [%s]".formatted(path);
      if (model.get("message") != null) {
        message += " and exception [%s]".formatted(model.get("message"));
      }
      message += " as the response has already been committed.";
      message += " As a result, the response may have the wrong status code.";
      return message;
    }

    @Override
    public String getContentType() {
      return "text/html";
    }

  }

  /**
   * {@link BeanFactoryPostProcessor} to ensure that the target class of ErrorController
   * MVC beans are preserved when using AOP.
   */
  static class PreserveErrorControllerTargetClassPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
      for (String errorControllerBean : beanFactory.getBeanNamesForType(
              ErrorController.class, false, false)) {
        try {
          beanFactory.getBeanDefinition(errorControllerBean)
                  .setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
        }
        catch (Throwable ex) {
          // Ignore
        }
      }
    }

  }

}
