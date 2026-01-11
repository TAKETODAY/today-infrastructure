/*
 * Copyright 2012-present the original author or authors.
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

package infra.webmvc.config;

import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
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
public final class ErrorMvcAutoConfiguration {

  @Component
  @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
  public static DefaultErrorAttributes errorAttributes() {
    return new DefaultErrorAttributes();
  }

  @Component
  @ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
  public static BasicErrorController basicErrorController(ErrorAttributes errorAttributes, ServerProperties serverProperties,
          List<ErrorViewResolver> errorViewResolvers, ReturnValueHandlerManager returnValueHandler) {
    return new BasicErrorController(errorAttributes,
            serverProperties.error, errorViewResolvers, returnValueHandler);
  }

  @Component
  public static PreserveErrorControllerTargetClassPostProcessor preserveErrorControllerTargetClassPostProcessor() {
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
  private static final class ErrorTemplateMissingCondition extends InfraCondition {

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
  private static final class StaticView implements View {

    @Override
    public void render(Map<String, ?> model, RequestContext request) throws Exception {
      if (request.isCommitted()) {
        String message = getMessage(model);
        LoggerFactory.getLogger(StaticView.class).error(message);
        return;
      }

      PrintWriter writer = request.getWriter();
      Object timestamp = model.get("timestamp");
      Object message = model.get("message");
      Object trace = model.get("trace");

      if (request.getResponseContentType() == null) {
        request.setContentType(getContentType());
      }

      writer.append("<html><body><h1>Whitelabel Error Page</h1>")
              .append("<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>")
              .append("<div id='created'>").append(String.valueOf(timestamp)).append("</div>")
              .append("<div>There was an unexpected error (type=").append(htmlEscape(model.get("error")))
              .append(", status=").append(htmlEscape(model.get("status"))).append(").</div>");
      if (message != null) {
        writer.append("<div>").append(htmlEscape(message)).append("</div>");
      }
      if (trace != null) {
        writer.append("<div style='white-space:pre-wrap;'>").append(htmlEscape(trace)).append("</div>");
      }
      writer.append("</body></html>");

      writer.flush();
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
