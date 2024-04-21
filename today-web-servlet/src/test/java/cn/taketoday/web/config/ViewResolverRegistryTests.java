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

package cn.taketoday.web.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.core.Ordered;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.view.BeanNameViewResolver;
import cn.taketoday.web.view.ContentNegotiatingViewResolver;
import cn.taketoday.web.view.ViewResolver;
import cn.taketoday.web.view.freemarker.FreeMarkerConfigurer;
import cn.taketoday.web.view.freemarker.FreeMarkerViewResolver;
import cn.taketoday.web.view.groovy.GroovyMarkupConfigurer;
import cn.taketoday.web.view.groovy.GroovyMarkupViewResolver;
import cn.taketoday.web.view.json.MappingJackson2JsonView;
import cn.taketoday.web.view.script.ScriptTemplateConfigurer;
import cn.taketoday.web.view.script.ScriptTemplateViewResolver;
import cn.taketoday.web.view.xml.MarshallingView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 15:55
 */
class ViewResolverRegistryTests {

  private ViewResolverRegistry registry;

  @BeforeEach
  public void setup() {
    StaticWebApplicationContext context = new StaticWebApplicationContext();
    context.registerSingleton("freeMarkerConfigurer", FreeMarkerConfigurer.class);
    context.registerSingleton("groovyMarkupConfigurer", GroovyMarkupConfigurer.class);
    context.registerSingleton("scriptTemplateConfigurer", ScriptTemplateConfigurer.class);

    this.registry = new ViewResolverRegistry(new ContentNegotiationManager(), context);
  }

  @Test
  public void order() {
    assertThat(this.registry.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    this.registry.enableContentNegotiation();
    assertThat(this.registry.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }

  @Test
  public void hasRegistrations() {
    assertThat(this.registry.hasRegistrations()).isFalse();
    this.registry.freeMarker();
    assertThat(this.registry.hasRegistrations()).isTrue();
  }

  @Test
  public void hasRegistrationsWhenContentNegotiationEnabled() {
    assertThat(this.registry.hasRegistrations()).isFalse();
    this.registry.enableContentNegotiation();
    assertThat(this.registry.hasRegistrations()).isTrue();
  }

  @Test
  public void noResolvers() {
    assertThat(this.registry.getViewResolvers()).isNotNull();
    assertThat(this.registry.getViewResolvers()).isEmpty();
    assertThat(this.registry.hasRegistrations()).isFalse();
  }

  @Test
  public void customViewResolver() {
    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/", ".jsp");
    this.registry.viewResolver(viewResolver);
    assertThat(this.registry.getViewResolvers().get(0)).isSameAs(viewResolver);
  }

  @Test
  public void beanName() {
    this.registry.beanName();
    assertThat(this.registry.getViewResolvers()).hasSize(1);
    assertThat(registry.getViewResolvers().get(0).getClass()).isEqualTo(BeanNameViewResolver.class);
  }

  @Test
  public void freeMarker() {
    this.registry.freeMarker().prefix("/").suffix(".fmt").cache(false);
    FreeMarkerViewResolver resolver = checkAndGetResolver(FreeMarkerViewResolver.class);
    checkPropertyValues(resolver, "prefix", "/", "suffix", ".fmt", "cacheLimit", 0);
  }

  @Test
  public void freeMarkerDefaultValues() {
    this.registry.freeMarker();
    FreeMarkerViewResolver resolver = checkAndGetResolver(FreeMarkerViewResolver.class);
    checkPropertyValues(resolver, "prefix", "", "suffix", ".ftl");
  }

  @Test
  public void groovyMarkup() {
    this.registry.groovy().prefix("/").suffix(".groovy").cache(true);
    GroovyMarkupViewResolver resolver = checkAndGetResolver(GroovyMarkupViewResolver.class);
    checkPropertyValues(resolver, "prefix", "/", "suffix", ".groovy", "cacheLimit", 1024);
  }

  @Test
  public void groovyMarkupDefaultValues() {
    this.registry.groovy();
    GroovyMarkupViewResolver resolver = checkAndGetResolver(GroovyMarkupViewResolver.class);
    checkPropertyValues(resolver, "prefix", "", "suffix", ".tpl");
  }

  @Test
  public void scriptTemplate() {
    this.registry.scriptTemplate().prefix("/").suffix(".html").cache(true);
    ScriptTemplateViewResolver resolver = checkAndGetResolver(ScriptTemplateViewResolver.class);
    checkPropertyValues(resolver, "prefix", "/", "suffix", ".html", "cacheLimit", 1024);
  }

  @Test
  public void scriptTemplateDefaultValues() {
    this.registry.scriptTemplate();
    ScriptTemplateViewResolver resolver = checkAndGetResolver(ScriptTemplateViewResolver.class);
    checkPropertyValues(resolver, "prefix", "", "suffix", "");
  }

  @Test
  public void contentNegotiation() {
    MappingJackson2JsonView view = new MappingJackson2JsonView();
    this.registry.enableContentNegotiation(view);
    ContentNegotiatingViewResolver resolver = checkAndGetResolver(ContentNegotiatingViewResolver.class);
    assertThat(resolver.getDefaultViews()).isEqualTo(Arrays.asList(view));
    assertThat(this.registry.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }

  @Test
  public void contentNegotiationAddsDefaultViewRegistrations() {
    MappingJackson2JsonView view1 = new MappingJackson2JsonView();
    this.registry.enableContentNegotiation(view1);

    ContentNegotiatingViewResolver resolver1 = checkAndGetResolver(ContentNegotiatingViewResolver.class);
    assertThat(resolver1.getDefaultViews()).isEqualTo(Arrays.asList(view1));

    MarshallingView view2 = new MarshallingView();
    this.registry.enableContentNegotiation(view2);

    ContentNegotiatingViewResolver resolver2 = checkAndGetResolver(ContentNegotiatingViewResolver.class);
    assertThat(resolver2.getDefaultViews()).isEqualTo(Arrays.asList(view1, view2));
    assertThat(resolver2).isSameAs(resolver1);
  }

  @SuppressWarnings("unchecked")
  private <T extends ViewResolver> T checkAndGetResolver(Class<T> resolverType) {
    assertThat(this.registry.getViewResolvers()).isNotNull();
    assertThat(this.registry.getViewResolvers()).hasSize(1);
    assertThat(this.registry.getViewResolvers().get(0).getClass()).isEqualTo(resolverType);
    return (T) registry.getViewResolvers().get(0);
  }

  private void checkPropertyValues(ViewResolver resolver, Object... nameValuePairs) {
    DirectFieldAccessor accessor = new DirectFieldAccessor(resolver);
    for (int i = 0; i < nameValuePairs.length; i++, i++) {
      Object expected = nameValuePairs[i + 1];
      Object actual = accessor.getPropertyValue((String) nameValuePairs[i]);
      assertThat(actual).isEqualTo(expected);
    }
  }

}
