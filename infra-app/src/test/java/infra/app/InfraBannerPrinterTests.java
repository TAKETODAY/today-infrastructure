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

package infra.app;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.context.support.GenericApplicationContext;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.logging.Logger;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 22:16
 */
class InfraBannerPrinterTests {

  @Test
  void shouldRegisterRuntimeHints() {
    RuntimeHints runtimeHints = new RuntimeHints();
    new InfraBannerPrinter.Hints().registerHints(runtimeHints,
            getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.resource().forResource("banner.txt")).accepts(runtimeHints);
  }

  @Test
  void shouldUseUtf8() {
    ResourceLoader resourceLoader = new GenericApplicationContext();
    Resource resource = resourceLoader.getResource("classpath:/banner-utf8.txt");
    InfraBannerPrinter printer = new InfraBannerPrinter(resourceLoader,
            new ResourceBanner(resource));
    Logger log = mock(Logger.class);
    printer.print(new MockEnvironment(), InfraBannerPrinterTests.class, log);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    then(log).should().info(captor.capture());
    assertThat(captor.getValue()).isEqualToIgnoringNewLines("\uD83D\uDE0D Infra App! \uD83D\uDE0D");
  }

  @Test
  void configuredResourceTakesPrecedenceOverFallback() {
    MockEnvironment environment = new MockEnvironment().withProperty(Banner.BANNER_LOCATION, "classpath:/banner-utf8.txt");
    InfraBannerPrinter printer = new InfraBannerPrinter(new GenericApplicationContext(),
            (env, source, out) -> out.print("fallback"));
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    printer.print(environment, null, new PrintStream(bytes, false, StandardCharsets.UTF_8));

    assertThat(bytes.toString(StandardCharsets.UTF_8)).isEqualToIgnoringNewLines("\uD83D\uDE0D Infra App! \uD83D\uDE0D");
  }

  @Test
  void ignoresLiquibaseBannerAndPrintsBuiltInBanner() throws IOException {
    ResourceLoader loader = mock(ResourceLoader.class);
    Resource resource = mock(Resource.class);
    given(loader.getResource(Banner.BANNER_LOCATION_TXT)).willReturn(resource);
    given(resource.exists()).willReturn(true);
    given(resource.getURL()).willReturn(URI.create("jar:file:/liquibase-core.jar!/banner.txt").toURL());
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    new InfraBannerPrinter(loader, null).print(new MockEnvironment(), null,
            new PrintStream(bytes, false, StandardCharsets.UTF_8));

    assertThat(bytes.toString(StandardCharsets.UTF_8)).contains("______", " (v");
  }

  @Test
  void unreadableResourceUrlFallsBackAndPrintedBannerRemembersSourceClass() throws IOException {
    ResourceLoader loader = mock(ResourceLoader.class);
    Resource resource = mock(Resource.class);
    given(loader.getResource(Banner.BANNER_LOCATION_TXT)).willReturn(resource);
    given(resource.exists()).willReturn(true);
    given(resource.getURL()).willThrow(new IOException("URL unavailable"));
    InfraBannerPrinter printer = new InfraBannerPrinter(loader,
            (env, source, out) -> out.println(source == null ? "unknown" : source.getSimpleName()));
    MockEnvironment environment = new MockEnvironment();
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bytes, false, StandardCharsets.UTF_8);

    Banner printed = printer.print(environment, getClass(), out);
    printed.printBanner(environment, null, out);
    printed.printBanner(environment, String.class, out);

    assertThat(bytes.toString(StandardCharsets.UTF_8).lines())
            .containsExactly("InfraBannerPrinterTests", "InfraBannerPrinterTests", "String");
  }

}
