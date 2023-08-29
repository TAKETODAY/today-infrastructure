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

package cn.taketoday.context.properties.processor.fieldvalues;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import cn.taketoday.context.properties.sample.fieldvalues.FieldValues;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for {@link FieldValuesParser} tests.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public abstract class AbstractFieldValuesProcessorTests {

  protected abstract FieldValuesParser createProcessor(ProcessingEnvironment env);

  @Test
  void getFieldValues() throws Exception {
    TestProcessor processor = new TestProcessor();
    TestCompiler compiler = TestCompiler.forSystem()
            .withProcessors(processor)
            .withSources(SourceFile.forTestClass(FieldValues.class));
    compiler.compile((compiled) -> {
    });
    Map<String, Object> values = processor.getValues();
    assertThat(values.get("stringNone")).isNull();
    assertThat(values.get("stringConst")).isEqualTo("c");
    assertThat(values.get("bool")).isEqualTo(true);
    assertThat(values.get("boolNone")).isEqualTo(false);
    assertThat(values.get("boolConst")).isEqualTo(true);
    assertThat(values.get("boolObject")).isEqualTo(true);
    assertThat(values.get("boolObjectNone")).isNull();
    assertThat(values.get("boolObjectConst")).isEqualTo(true);
    assertThat(values.get("integer")).isEqualTo(1);
    assertThat(values.get("integerNone")).isEqualTo(0);
    assertThat(values.get("integerConst")).isEqualTo(2);
    assertThat(values.get("integerObject")).isEqualTo(3);
    assertThat(values.get("integerObjectNone")).isNull();
    assertThat(values.get("integerObjectConst")).isEqualTo(4);
    assertThat(values.get("charset")).isEqualTo("US-ASCII");
    assertThat(values.get("charsetConst")).isEqualTo("UTF-8");
    assertThat(values.get("mimeType")).isEqualTo("text/html");
    assertThat(values.get("mimeTypeConst")).isEqualTo("text/plain");
    assertThat(values.get("object")).isEqualTo(123);
    assertThat(values.get("objectNone")).isNull();
    assertThat(values.get("objectConst")).isEqualTo("c");
    assertThat(values.get("objectInstance")).isNull();
    assertThat(values.get("stringArray")).isEqualTo(new Object[] { "FOO", "BAR" });
    assertThat(values.get("stringArrayNone")).isNull();
    assertThat(values.get("stringEmptyArray")).isEqualTo(new Object[0]);
    assertThat(values.get("stringArrayConst")).isEqualTo(new Object[] { "OK", "KO" });
    assertThat(values.get("stringArrayConstElements")).isEqualTo(new Object[] { "c" });
    assertThat(values.get("integerArray")).isEqualTo(new Object[] { 42, 24 });
    assertThat(values.get("unknownArray")).isNull();
    assertThat(values.get("durationNone")).isNull();
    assertThat(values.get("durationNanos")).isEqualTo("5ns");
    assertThat(values.get("durationMillis")).isEqualTo("10ms");
    assertThat(values.get("durationSeconds")).isEqualTo("20s");
    assertThat(values.get("durationMinutes")).isEqualTo("30m");
    assertThat(values.get("durationHours")).isEqualTo("40h");
    assertThat(values.get("durationDays")).isEqualTo("50d");
    assertThat(values.get("durationZero")).isEqualTo(0);
    assertThat(values.get("dataSizeNone")).isNull();
    assertThat(values.get("dataSizeBytes")).isEqualTo("5B");
    assertThat(values.get("dataSizeKilobytes")).isEqualTo("10KB");
    assertThat(values.get("dataSizeMegabytes")).isEqualTo("20MB");
    assertThat(values.get("dataSizeGigabytes")).isEqualTo("30GB");
    assertThat(values.get("dataSizeTerabytes")).isEqualTo("40TB");
    assertThat(values.get("periodNone")).isNull();
    assertThat(values.get("periodDays")).isEqualTo("3d");
    assertThat(values.get("periodWeeks")).isEqualTo("2w");
    assertThat(values.get("periodMonths")).isEqualTo("10m");
    assertThat(values.get("periodYears")).isEqualTo("15y");
    assertThat(values.get("periodZero")).isEqualTo(0);
  }

  @SupportedAnnotationTypes({ "cn.taketoday.context.properties.sample.ConfigurationProperties" })
  @SupportedSourceVersion(SourceVersion.RELEASE_6)
  private class TestProcessor extends AbstractProcessor {

    private FieldValuesParser processor;

    private final Map<String, Object> values = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment env) {
      this.processor = createProcessor(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      for (TypeElement annotation : annotations) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
          if (element instanceof TypeElement typeElement) {
            try {
              this.values.putAll(this.processor.getFieldValues(typeElement));
            }
            catch (Exception ex) {
              throw new IllegalStateException(ex);
            }
          }
        }
      }
      return false;
    }

    Map<String, Object> getValues() {
      return this.values;
    }

  }

}
