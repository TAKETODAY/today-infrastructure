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

package infra.app.webmvc.test.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import infra.app.webmvc.test.config.InfraMockMvcBuilderCustomizer.DeferredLinesWriter;
import infra.app.webmvc.test.config.InfraMockMvcBuilderCustomizer.LinesWriter;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.mock.api.Filter;
import infra.mock.api.FilterChain;
import infra.mock.api.FilterConfig;
import infra.mock.api.MockContext;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.web.MockContextImpl;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraMockMvcBuilderCustomizer}.
 *
 * @author Madhura Bhave
 */
class InfraMockMvcBuilderCustomizerTests {

  @Test
  void whenCalledInParallelDeferredLinesWriterSeparatesOutputByThread() throws Exception {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    MockContext servletContext = new MockContextImpl();
    context.setMockContext(servletContext);
    context.register(FilterConfiguration.class);
    context.refresh();

    CapturingLinesWriter delegate = new CapturingLinesWriter();
    new DeferredLinesWriter(context, delegate);
    CountDownLatch latch = new CountDownLatch(10);
    for (int i = 0; i < 10; i++) {
      Thread thread = new Thread(() -> {
        for (int j = 0; j < 1000; j++) {
          DeferredLinesWriter writer = DeferredLinesWriter.get(context);
          assertThat(writer).isNotNull();
          writer.write(Arrays.asList("1", "2", "3", "4", "5"));
          writer.writeDeferredResult();
          writer.clear();
        }
        latch.countDown();
      });
      thread.start();
    }
    assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();

    assertThat(delegate.allWritten).hasSize(10000);
    assertThat(delegate.allWritten)
            .allSatisfy((written) -> assertThat(written).containsExactly("1", "2", "3", "4", "5"));
  }

  private static final class CapturingLinesWriter implements LinesWriter {

    List<List<String>> allWritten = new ArrayList<>();

    private final Object monitor = new Object();

    @Override
    public void write(List<String> lines) {
      List<String> written = new ArrayList<>(lines);
      synchronized(this.monitor) {
        this.allWritten.add(written);
      }
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class FilterConfiguration {

    @Bean
    TestFilter testFilter() {
      return new TestFilter();
    }

  }

  static class TestFilter implements Filter {

    @SuppressWarnings("NullAway.Init")
    private String filterName;

    private final Map<String, String> initParams = new HashMap<>();

    @Override
    public void init(FilterConfig filterConfig) {
      this.filterName = filterConfig.getFilterName();
      Collections.list(filterConfig.getInitParameterNames())
              .forEach((name) -> this.initParams.put(name, filterConfig.getInitParameter(name)));
    }

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) throws IOException, MockException {

    }

    @Override
    public void destroy() {

    }

  }

  static class OtherTestFilter implements Filter {

    @SuppressWarnings("NullAway.Init")
    private String filterName;

    private final Map<String, String> initParams = new HashMap<>();

    @Override
    public void init(FilterConfig filterConfig) {
      this.filterName = filterConfig.getFilterName();
      Collections.list(filterConfig.getInitParameterNames())
              .forEach((name) -> this.initParams.put(name, filterConfig.getInitParameter(name)));
    }

    @Override
    public void doFilter(MockRequest request, MockResponse response, FilterChain chain) throws IOException, MockException {

    }

    @Override
    public void destroy() {

    }

  }

}
