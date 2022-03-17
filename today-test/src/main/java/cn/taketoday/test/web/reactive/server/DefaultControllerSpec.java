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

package cn.taketoday.test.web.reactive.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.http.codec.ServerCodecConfigurer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.reactive.accept.RequestedContentTypeResolverBuilder;
import cn.taketoday.web.reactive.config.CorsRegistry;
import cn.taketoday.web.reactive.config.DelegatingWebFluxConfiguration;
import cn.taketoday.web.reactive.config.PathMatchConfigurer;
import cn.taketoday.web.reactive.config.ViewResolverRegistry;
import cn.taketoday.web.reactive.config.WebFluxConfigurer;
import cn.taketoday.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import cn.taketoday.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Default implementation of {@link WebTestClient.ControllerSpec}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultControllerSpec extends AbstractMockServerSpec<WebTestClient.ControllerSpec>
        implements WebTestClient.ControllerSpec {

  private final List<Object> controllers;

  private final List<Object> controllerAdvice = new ArrayList<>(8);

  private final TestWebFluxConfigurer configurer = new TestWebFluxConfigurer();

  DefaultControllerSpec(Object... controllers) {
    Assert.isTrue(!ObjectUtils.isEmpty(controllers), "At least one controller is required");
    this.controllers = instantiateIfNecessary(controllers);
  }

  private static List<Object> instantiateIfNecessary(Object[] specified) {
    List<Object> instances = new ArrayList<>(specified.length);
    for (Object obj : specified) {
      instances.add(obj instanceof Class ? BeanUtils.newInstance((Class<?>) obj) : obj);
    }
    return instances;
  }

  @Override
  public DefaultControllerSpec controllerAdvice(Object... controllerAdvices) {
    this.controllerAdvice.addAll(instantiateIfNecessary(controllerAdvices));
    return this;
  }

  @Override
  public DefaultControllerSpec contentTypeResolver(Consumer<RequestedContentTypeResolverBuilder> consumer) {
    this.configurer.contentTypeResolverConsumer = consumer;
    return this;
  }

  @Override
  public DefaultControllerSpec corsMappings(Consumer<CorsRegistry> consumer) {
    this.configurer.corsRegistryConsumer = consumer;
    return this;
  }

  @Override
  public DefaultControllerSpec argumentResolvers(Consumer<ArgumentResolverConfigurer> consumer) {
    this.configurer.argumentResolverConsumer = consumer;
    return this;
  }

  @Override
  public DefaultControllerSpec pathMatching(Consumer<PathMatchConfigurer> consumer) {
    this.configurer.pathMatchConsumer = consumer;
    return this;
  }

  @Override
  public DefaultControllerSpec httpMessageCodecs(Consumer<ServerCodecConfigurer> consumer) {
    this.configurer.messageCodecsConsumer = consumer;
    return this;
  }

  @Override
  public DefaultControllerSpec formatters(Consumer<FormatterRegistry> consumer) {
    this.configurer.formattersConsumer = consumer;
    return this;
  }

  @Override
  public DefaultControllerSpec validator(Validator validator) {
    this.configurer.validator = validator;
    return this;
  }

  @Override
  public DefaultControllerSpec viewResolvers(Consumer<ViewResolverRegistry> consumer) {
    this.configurer.viewResolversConsumer = consumer;
    return this;
  }

  @Override
  protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
    return WebHttpHandlerBuilder.applicationContext(initApplicationContext());
  }

  private ApplicationContext initApplicationContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    this.controllers.forEach(controller -> {
      String name = controller.getClass().getName();
      context.registerBean(name, Object.class, () -> controller);
    });
    this.controllerAdvice.forEach(advice -> {
      String name = advice.getClass().getName();
      context.registerBean(name, Object.class, () -> advice);
    });
    context.register(DelegatingWebFluxConfiguration.class);
    context.registerBean(WebFluxConfigurer.class, () -> this.configurer);
    context.refresh();
    return context;
  }

  private class TestWebFluxConfigurer implements WebFluxConfigurer {

    @Nullable
    private Consumer<RequestedContentTypeResolverBuilder> contentTypeResolverConsumer;

    @Nullable
    private Consumer<CorsRegistry> corsRegistryConsumer;

    @Nullable
    private Consumer<ArgumentResolverConfigurer> argumentResolverConsumer;

    @Nullable
    private Consumer<PathMatchConfigurer> pathMatchConsumer;

    @Nullable
    private Consumer<ServerCodecConfigurer> messageCodecsConsumer;

    @Nullable
    private Consumer<FormatterRegistry> formattersConsumer;

    @Nullable
    private Validator validator;

    @Nullable
    private Consumer<ViewResolverRegistry> viewResolversConsumer;

    @Override
    public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
      if (this.contentTypeResolverConsumer != null) {
        this.contentTypeResolverConsumer.accept(builder);
      }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
      if (this.corsRegistryConsumer != null) {
        this.corsRegistryConsumer.accept(registry);
      }
    }

    @Override
    public void configurePathMatching(PathMatchConfigurer configurer) {
      if (this.pathMatchConsumer != null) {
        this.pathMatchConsumer.accept(configurer);
      }
    }

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
      if (this.argumentResolverConsumer != null) {
        this.argumentResolverConsumer.accept(configurer);
      }
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
      if (this.messageCodecsConsumer != null) {
        this.messageCodecsConsumer.accept(configurer);
      }
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
      if (this.formattersConsumer != null) {
        this.formattersConsumer.accept(registry);
      }
    }

    @Override
    @Nullable
    public Validator getValidator() {
      return this.validator;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
      if (this.viewResolversConsumer != null) {
        this.viewResolversConsumer.accept(registry);
      }
    }
  }

}
