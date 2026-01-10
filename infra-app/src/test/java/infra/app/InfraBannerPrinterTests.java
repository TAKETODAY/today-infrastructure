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

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.context.support.GenericApplicationContext;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.logging.Logger;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
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

}