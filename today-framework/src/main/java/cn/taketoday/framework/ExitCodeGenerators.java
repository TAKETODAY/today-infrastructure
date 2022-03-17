/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import cn.taketoday.lang.Assert;

/**
 * Maintains a collection of {@link ExitCodeGenerator} instances and allows the final exit
 * code to be calculated.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getExitCode()
 * @see ExitCodeGenerator
 * @since 4.0 2022/1/16 20:36
 */
class ExitCodeGenerators implements Iterable<ExitCodeGenerator> {

  private final ArrayList<ExitCodeGenerator> generators = new ArrayList<>();

  void addAll(Throwable exception, ExitCodeExceptionMapper... mappers) {
    Assert.notNull(exception, "Exception must not be null");
    Assert.notNull(mappers, "Mappers must not be null");
    addAll(exception, Arrays.asList(mappers));
  }

  void addAll(Throwable exception, Iterable<? extends ExitCodeExceptionMapper> mappers) {
    Assert.notNull(exception, "Exception must not be null");
    Assert.notNull(mappers, "Mappers must not be null");
    for (ExitCodeExceptionMapper mapper : mappers) {
      add(exception, mapper);
    }
  }

  void add(Throwable exception, ExitCodeExceptionMapper mapper) {
    Assert.notNull(exception, "Exception must not be null");
    Assert.notNull(mapper, "Mapper must not be null");
    add(new MappedExitCodeGenerator(exception, mapper));
  }

  void addAll(ExitCodeGenerator... generators) {
    Assert.notNull(generators, "Generators must not be null");
    addAll(Arrays.asList(generators));
  }

  void addAll(Iterable<? extends ExitCodeGenerator> generators) {
    Assert.notNull(generators, "Generators must not be null");
    for (ExitCodeGenerator generator : generators) {
      add(generator);
    }
  }

  void add(ExitCodeGenerator generator) {
    Assert.notNull(generator, "Generator must not be null");
    this.generators.add(generator);
  }

  @Override
  public Iterator<ExitCodeGenerator> iterator() {
    return this.generators.iterator();
  }

  /**
   * Get the final exit code that should be returned based on all contained generators.
   *
   * @return the final exit code.
   */
  int getExitCode() {
    int exitCode = 0;
    for (ExitCodeGenerator generator : this.generators) {
      try {
        int value = generator.getExitCode();
        if (value > 0 && value > exitCode || value < 0 && value < exitCode) {
          exitCode = value;
        }
      }
      catch (Exception ex) {
        exitCode = (exitCode != 0) ? exitCode : 1;
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
