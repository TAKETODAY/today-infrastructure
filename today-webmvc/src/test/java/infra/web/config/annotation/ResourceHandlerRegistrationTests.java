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

import java.util.function.Function;

import infra.cache.Cache;
import infra.cache.concurrent.ConcurrentMapCache;
import infra.core.io.Resource;
import infra.http.CacheControl;
import infra.web.NotFoundHandler;
import infra.web.resource.ResourceHttpRequestHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 20:11
 */
class ResourceHandlerRegistrationTests {

  @Test
  void constructorWithValidPathPatterns() {
    String[] pathPatterns = { "/resources/**", "/static/**" };
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration(pathPatterns);

    assertThat(registration.getPathPatterns()).containsExactly(pathPatterns);
  }

  @Test
  void constructorWithEmptyPathPatternsThrowsException() {
    assertThatThrownBy(ResourceHandlerRegistration::new)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one path pattern is required for resource handling.");
  }

  @Test
  void constructorWithNullPathPatternsThrowsException() {
    assertThatThrownBy(() -> new ResourceHandlerRegistration((String[]) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("At least one path pattern is required for resource handling.");
  }

  @Test
  void addResourceLocationsWithStringLocations() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    String[] locations = { "classpath:/static/", "file:/var/www/" };

    ResourceHandlerRegistration result = registration.addResourceLocations(locations);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void addResourceLocationsWithNullStringLocations() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceHandlerRegistration result = registration.addResourceLocations((String[]) null);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void addResourceLocationsWithResourceLocations() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    Resource resource1 = mock(Resource.class);
    Resource resource2 = mock(Resource.class);
    Resource[] resources = { resource1, resource2 };

    ResourceHandlerRegistration result = registration.addResourceLocations(resources);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void addResourceLocationsWithNullResourceLocations() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceHandlerRegistration result = registration.addResourceLocations((Resource[]) null);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void setCachePeriod() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    Integer cachePeriod = 3600;

    ResourceHandlerRegistration result = registration.setCachePeriod(cachePeriod);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void setCachePeriodWithNullValue() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    ResourceHandlerRegistration result = registration.setCachePeriod(null);
    assertThat(result).isSameAs(registration);
  }

  @Test
  void setCacheControl() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    CacheControl cacheControl = CacheControl.noCache();

    ResourceHandlerRegistration result = registration.setCacheControl(cacheControl);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void setCacheControlWithNullValue() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceHandlerRegistration result = registration.setCacheControl(null);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void setUseLastModified() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceHandlerRegistration result = registration.setUseLastModified(false);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void setEtagGenerator() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    Function<Resource, String> etagGenerator = resource -> "etag";

    ResourceHandlerRegistration result = registration.setEtagGenerator(etagGenerator);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void setEtagGeneratorWithNullValue() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceHandlerRegistration result = registration.setEtagGenerator(null);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void setOptimizeLocations() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceHandlerRegistration result = registration.setOptimizeLocations(true);

    assertThat(result).isSameAs(registration);
  }

  @Test
  void resourceChainWithCacheResources() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceChainRegistration chainRegistration = registration.resourceChain(true);

    assertThat(chainRegistration).isNotNull();
    assertThat(registration).extracting("resourceChainRegistration").isSameAs(chainRegistration);
  }

  @Test
  void resourceChainWithCacheResourcesAndCache() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    Cache cache = new ConcurrentMapCache("testCache");

    ResourceChainRegistration chainRegistration = registration.resourceChain(true, cache);

    assertThat(chainRegistration).isNotNull();
    assertThat(registration).extracting("resourceChainRegistration").isSameAs(chainRegistration);
  }

  @Test
  void notFoundHandler() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    NotFoundHandler notFoundHandler = mock(NotFoundHandler.class);

    ResourceHandlerRegistration result = registration.notFoundHandler(notFoundHandler);

    assertThat(result).isSameAs(registration);
    assertThat(registration.notFoundHandler).isSameAs(notFoundHandler);
  }

  @Test
  void notFoundHandlerWithNullValue() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceHandlerRegistration result = registration.notFoundHandler(null);

    assertThat(result).isSameAs(registration);
    assertThat(registration.notFoundHandler).isNull();
  }

  @Test
  void getRequestHandlerReturnsHandlerWithDefaults() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");

    ResourceHttpRequestHandler handler = registration.getRequestHandler();

    assertThat(handler).isNotNull();
    assertThat(handler.getLocations()).isEmpty();
    assertThat(handler.getCacheControl()).isNull();
    assertThat(handler.getCacheSeconds()).isEqualTo(-1); // Default value
  }

  @Test
  void getRequestHandlerReturnsHandlerWithConfiguredValues() throws Exception {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    registration.addResourceLocations("classpath:/static/")
            .setCachePeriod(3600)
            .setUseLastModified(false)
            .setOptimizeLocations(true);

    ResourceHttpRequestHandler handler = registration.getRequestHandler();

    assertThat(handler).isNotNull();
    assertThat(handler.getLocations()).isEmpty();
    assertThat(handler.getCacheSeconds()).isEqualTo(3600);
    assertThat(handler.isUseLastModified()).isFalse();
    assertThat(handler.isOptimizeLocations()).isTrue();
  }

  @Test
  void getPathPatternsReturnsSameInstance() {
    String[] pathPatterns = { "/resources/**" };
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration(pathPatterns);

    String[] patterns1 = registration.getPathPatterns();
    String[] patterns2 = registration.getPathPatterns();

    assertThat(patterns1).isSameAs(patterns2);
  }

  @Test
  void chainMethodsReturnSameInstance() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    String[] locations = { "classpath:/static/" };
    Integer cachePeriod = 3600;
    CacheControl cacheControl = CacheControl.noCache();
    Function<Resource, String> etagGenerator = resource -> "etag";

    ResourceHandlerRegistration result1 = registration.addResourceLocations(locations);
    ResourceHandlerRegistration result2 = result1.setCachePeriod(cachePeriod);
    ResourceHandlerRegistration result3 = result2.setCacheControl(cacheControl);
    ResourceHandlerRegistration result4 = result3.setUseLastModified(false);
    ResourceHandlerRegistration result5 = result4.setEtagGenerator(etagGenerator);
    ResourceHandlerRegistration result6 = result5.setOptimizeLocations(true);

    assertThat(result1).isSameAs(registration);
    assertThat(result2).isSameAs(registration);
    assertThat(result3).isSameAs(registration);
    assertThat(result4).isSameAs(registration);
    assertThat(result5).isSameAs(registration);
    assertThat(result6).isSameAs(registration);
  }

  @Test
  void getRequestHandlerWithResourceChainRegistration() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    registration.resourceChain(true);

    ResourceHttpRequestHandler handler = registration.getRequestHandler();

    assertThat(handler).isNotNull();
    assertThat(handler.getResourceResolvers()).isNotNull();
    assertThat(handler.getResourceTransformers()).isNotNull();
  }

  @Test
  void getRequestHandlerWithCacheControlOverridesCachePeriod() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    registration.setCachePeriod(3600);
    registration.setCacheControl(CacheControl.noCache());

    ResourceHttpRequestHandler handler = registration.getRequestHandler();

    assertThat(handler.getCacheControl()).isNotNull();
    assertThat(handler.getCacheSeconds()).isEqualTo(-1); // CacheControl takes precedence
  }

  @Test
  void getRequestHandlerWithNotFoundHandler() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    NotFoundHandler notFoundHandler = mock(NotFoundHandler.class);
    registration.notFoundHandler(notFoundHandler);

    ResourceHttpRequestHandler handler = registration.getRequestHandler();
    assertThat(handler).extracting("notFoundHandler").isSameAs(notFoundHandler);
  }

  @Test
  void getRequestHandlerWithEtagGenerator() {
    ResourceHandlerRegistration registration = new ResourceHandlerRegistration("/resources/**");
    Function<Resource, String> etagGenerator = resource -> "etag";
    registration.setEtagGenerator(etagGenerator);

    ResourceHttpRequestHandler handler = registration.getRequestHandler();

    assertThat(handler.getEtagGenerator()).isSameAs(etagGenerator);
  }

}