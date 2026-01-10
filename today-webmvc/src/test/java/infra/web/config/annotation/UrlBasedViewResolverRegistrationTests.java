/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.config.annotation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.test.util.ReflectionTestUtils;
import infra.web.view.UrlBasedViewResolver;
import infra.web.view.xslt.XsltView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 17:58
 */
class UrlBasedViewResolverRegistrationTests {

  @Test
  void constructorInitializesViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();

    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);

    assertThat(registration.getViewResolver()).isSameAs(viewResolver);
  }

  @Test
  void prefixSetsPrefixOnViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);
    String prefix = "/WEB-INF/views/";

    UrlBasedViewResolverRegistration result = registration.prefix(prefix);

    assertThat(result).isSameAs(registration);
    assertThat(viewResolver).extracting("prefix").isEqualTo(prefix);
  }

  @Test
  void suffixSetsSuffixOnViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);
    String suffix = ".jsp";

    UrlBasedViewResolverRegistration result = registration.suffix(suffix);

    assertThat(result).isSameAs(registration);
    assertThat(viewResolver).extracting("suffix").isEqualTo(suffix);
  }

  @Test
  void viewNamesSetsViewNamesOnViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);
    String[] viewNames = { "home", "about", "contact" };

    UrlBasedViewResolverRegistration result = registration.viewNames(viewNames);

    assertThat(result).isSameAs(registration);
    String[] viewNames1 = ReflectionTestUtils.getField(viewResolver, "viewNames");
    assertThat(viewNames1).containsExactly(viewNames);
  }

  @Test
  void attributesSetsAttributesMapOnViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);
    Map<String, Object> attributes = Map.of("attr1", "value1", "attr2", "value2");

    UrlBasedViewResolverRegistration result = registration.attributes(attributes);

    assertThat(result).isSameAs(registration);
    assertThat(viewResolver.getAttributesMap()).containsAllEntriesOf(attributes);
  }

  @Test
  void cacheLimitSetsCacheLimitOnViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);
    int cacheLimit = 512;

    UrlBasedViewResolverRegistration result = registration.cacheLimit(cacheLimit);

    assertThat(result).isSameAs(registration);
    assertThat(viewResolver.getCacheLimit()).isEqualTo(cacheLimit);
  }

  @Test
  void cacheEnablesCachingOnViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
    viewResolver.setCache(false); // Start with caching disabled
    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);

    UrlBasedViewResolverRegistration result = registration.cache(true);

    assertThat(result).isSameAs(registration);
    assertThat(viewResolver.isCache()).isTrue();
  }

  @Test
  void cacheDisablesCachingOnViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
    viewResolver.setCache(true); // Start with caching enabled
    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);

    UrlBasedViewResolverRegistration result = registration.cache(false);

    assertThat(result).isSameAs(registration);
    assertThat(viewResolver.isCache()).isFalse();
  }

  @Test
  void viewClassSetsViewClassOnViewResolver() {
    UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
    UrlBasedViewResolverRegistration registration = new UrlBasedViewResolverRegistration(viewResolver);
    Class<?> viewClass = XsltView.class;

    UrlBasedViewResolverRegistration result = registration.viewClass(viewClass);

    assertThat(result).isSameAs(registration);
    assertThat(viewResolver).extracting("viewClass").isEqualTo(viewClass);
  }

}