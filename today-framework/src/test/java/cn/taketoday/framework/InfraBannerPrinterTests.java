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

package cn.taketoday.framework;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.logging.Logger;
import cn.taketoday.mock.env.MockEnvironment;

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