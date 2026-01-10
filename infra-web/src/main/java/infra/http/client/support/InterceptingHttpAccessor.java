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

package infra.http.client.support;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.InterceptingClientHttpRequestFactory;
import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.web.client.RestTemplate;

/**
 * Base class for {@link infra.web.client.RestTemplate}
 * and other HTTP accessing gateway helpers, adding interceptor-related
 * properties to {@link HttpAccessor}'s common properties.
 *
 * <p>Not intended to be used directly.
 * See {@link infra.web.client.RestTemplate} for an entry point.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClientHttpRequestInterceptor
 * @see InterceptingClientHttpRequestFactory
 * @see infra.web.client.RestTemplate
 * @since 4.0
 */
public abstract class InterceptingHttpAccessor extends HttpAccessor {

  private final ArrayList<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

  @Nullable
  private volatile ClientHttpRequestFactory interceptingRequestFactory;

  /**
   * Set the request interceptors that this accessor should use.
   * <p>The interceptors will get immediately sorted according to their
   * {@linkplain AnnotationAwareOrderComparator#sort(List) order}.
   *
   * @see #getRequestFactory()
   * @see AnnotationAwareOrderComparator
   */
  public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
    Assert.noNullElements(interceptors, "'interceptors' must not contain null elements");
    // Take getInterceptors() List as-is when passed in here
    if (this.interceptors != interceptors) {
      this.interceptors.clear();
      this.interceptors.addAll(interceptors);
      this.interceptors.trimToSize();
      AnnotationAwareOrderComparator.sort(this.interceptors);
    }
  }

  /**
   * Set the request interceptors that this accessor should use.
   * <p>The interceptors will get immediately sorted according to their
   * {@linkplain AnnotationAwareOrderComparator#sort(List) order}.
   * <p><strong>Note:</strong> This method does not support concurrent changes,
   * and in most cases should not be called after initialization on startup.
   * See also related note on {@link RestTemplate}
   * regarding concurrent configuration changes.
   *
   * @see #getRequestFactory()
   * @see AnnotationAwareOrderComparator
   */
  public void setInterceptors(ClientHttpRequestInterceptor... interceptors) {
    Assert.noNullElements(interceptors, "'interceptors' must not contain null elements");
    this.interceptors.clear();
    CollectionUtils.addAll(this.interceptors, interceptors);
    this.interceptors.trimToSize();
    AnnotationAwareOrderComparator.sort(this.interceptors);
  }

  /**
   * Get the request interceptors that this accessor uses.
   * <p>The returned {@link List} is active and may be modified. Note,
   * however, that the interceptors will not be resorted according to their
   * {@linkplain AnnotationAwareOrderComparator#sort(List) order} before the
   * {@link ClientHttpRequestFactory} is built.
   */
  public List<ClientHttpRequestInterceptor> getInterceptors() {
    return this.interceptors;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
    super.setRequestFactory(requestFactory);
    this.interceptingRequestFactory = null;
  }

  /**
   * Overridden to expose an {@link InterceptingClientHttpRequestFactory}
   * if necessary.
   *
   * @see #getInterceptors()
   */
  @Override
  public ClientHttpRequestFactory getRequestFactory() {
    if (!interceptors.isEmpty()) {
      ClientHttpRequestFactory factory = this.interceptingRequestFactory;
      if (factory == null) {
        synchronized(this) {
          factory = this.interceptingRequestFactory;
          if (factory == null) {
            factory = new InterceptingClientHttpRequestFactory(
                    super.getRequestFactory(), interceptors, getBufferingPredicate());
            this.interceptingRequestFactory = factory;
          }
        }
      }
      return factory;
    }
    else {
      return super.getRequestFactory();
    }
  }

}
