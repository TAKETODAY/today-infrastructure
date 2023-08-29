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

package cn.taketoday.context.properties.processor.test;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * A testable {@link Processor}.
 *
 * @param <T> the type of element to help writing assertions
 * @author Stephane Nicoll
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TestableAnnotationProcessor<T> extends AbstractProcessor {

  private final BiConsumer<RoundEnvironmentTester, T> consumer;

  private final Function<ProcessingEnvironment, T> factory;

  private T target;

  public TestableAnnotationProcessor(BiConsumer<RoundEnvironmentTester, T> consumer,
          Function<ProcessingEnvironment, T> factory) {
    this.consumer = consumer;
    this.factory = factory;
  }

  @Override
  public synchronized void init(ProcessingEnvironment env) {
    this.target = this.factory.apply(env);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    RoundEnvironmentTester tester = new RoundEnvironmentTester(roundEnv);
    if (!roundEnv.getRootElements().isEmpty()) {
      this.consumer.accept(tester, this.target);
      return true;
    }
    return false;
  }

}
