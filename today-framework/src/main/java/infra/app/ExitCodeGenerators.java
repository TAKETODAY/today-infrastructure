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

package infra.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import infra.core.Ordered;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.annotation.Order;
import infra.lang.Assert;

/**
 * Maintains an ordered collection of {@link ExitCodeGenerator} instances and allows the
 * final exit code to be calculated. Generators are ordered by {@link Order @Order} and
 * {@link Ordered}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author GenKui Du
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getExitCode()
 * @see ExitCodeGenerator
 * @since 4.0 2022/1/16 20:36
 */
class ExitCodeGenerators implements Iterable<ExitCodeGenerator> {

  private final ArrayList<ExitCodeGenerator> generators = new ArrayList<>();

  void addAll(Throwable exception, ExitCodeExceptionMapper... mappers) {
    Assert.notNull(exception, "Exception is required");
    Assert.notNull(mappers, "Mappers is required");
    addAll(exception, Arrays.asList(mappers));
  }

  void addAll(Throwable exception, Iterable<? extends ExitCodeExceptionMapper> mappers) {
    Assert.notNull(exception, "Exception is required");
    Assert.notNull(mappers, "Mappers is required");
    for (ExitCodeExceptionMapper mapper : mappers) {
      add(exception, mapper);
    }
  }

  void add(Throwable exception, ExitCodeExceptionMapper mapper) {
    Assert.notNull(exception, "Exception is required");
    Assert.notNull(mapper, "Mapper is required");
    add(new MappedExitCodeGenerator(exception, mapper));
  }

  void addAll(ExitCodeGenerator... generators) {
    Assert.notNull(generators, "Generators is required");
    addAll(Arrays.asList(generators));
  }

  void addAll(Iterable<? extends ExitCodeGenerator> generators) {
    Assert.notNull(generators, "Generators is required");
    for (ExitCodeGenerator generator : generators) {
      add(generator);
    }
  }

  void add(ExitCodeGenerator generator) {
    Assert.notNull(generator, "Generator is required");
    this.generators.add(generator);
    AnnotationAwareOrderComparator.sort(this.generators);
  }

  @Override
  public Iterator<ExitCodeGenerator> iterator() {
    return this.generators.iterator();
  }

  /**
   * Get the final exit code that should be returned. The final exit code is the first
   * non-zero exit code that is {@link ExitCodeGenerator#getExitCode generated}.
   *
   * @return the final exit code.
   */
  int getExitCode() {
    int exitCode = 0;
    for (ExitCodeGenerator generator : this.generators) {
      try {
        int value = generator.getExitCode();
        if (value != 0) {
          exitCode = value;
          break;
        }
      }
      catch (Exception ex) {
        exitCode = 1;
        ex.printStackTrace();
      }
    }
    return exitCode;
  }

  /**
   * Adapts an {@link ExitCodeExceptionMapper} to an {@link ExitCodeGenerator}.
   */
  private record MappedExitCodeGenerator(
          Throwable exception, ExitCodeExceptionMapper mapper) implements ExitCodeGenerator {

    @Override
    public int getExitCode() {
      return this.mapper.getExitCode(this.exception);
    }

  }

}
