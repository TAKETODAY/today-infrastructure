/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jarmode.layertools;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.Assertions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import cn.taketoday.jarmode.layertools.TestPrintStream.PrintStreamAssert;
import cn.taketoday.util.FileCopyUtils;

/**
 * {@link PrintStream} that can be used for testing.
 *
 * @author Phillip Webb
 */
class TestPrintStream extends PrintStream implements AssertProvider<PrintStreamAssert> {

  private final Class<? extends Object> testClass;

  TestPrintStream(Object testInstance) {
    super(new ByteArrayOutputStream());
    this.testClass = testInstance.getClass();
  }

  @Override
  public PrintStreamAssert assertThat() {
    return new PrintStreamAssert(this);
  }

  @Override
  public String toString() {
    return this.out.toString();
  }

  static final class PrintStreamAssert extends AbstractAssert<PrintStreamAssert, TestPrintStream> {

    private PrintStreamAssert(TestPrintStream actual) {
      super(actual, PrintStreamAssert.class);
    }

    void hasSameContentAsResource(String resource) {
      try {
        InputStream stream = this.actual.testClass.getResourceAsStream(resource);
        String content = FileCopyUtils.copyToString(new InputStreamReader(stream, StandardCharsets.UTF_8));
        Assertions.assertThat(this.actual).hasToString(content);
      }
      catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

  }

}
