/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.test.system;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;

/**
 * JUnit {@code @Rule} to capture output from System.out and System.err.
 * <p>
 * To use add as a {@link Rule @Rule}:
 *
 * <pre class="code">
 * public class MyTest {
 *
 *     &#064;Rule
 *     public OutputCaptureRule output = new OutputCaptureRule();
 *
 *     &#064;Test
 *     public void test() {
 *         assertThat(output).contains("ok");
 *     }
 *
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0
 */
public class OutputCaptureRule implements TestRule, CapturedOutput {

  private final OutputCapture delegate = new OutputCapture();

  private final List<Matcher<? super String>> matchers = new ArrayList<>();

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        OutputCaptureRule.this.delegate.push();
        try {
          base.evaluate();
        }
        finally {
          try {
            if (!OutputCaptureRule.this.matchers.isEmpty()) {
              String output = OutputCaptureRule.this.delegate.toString();
              MatcherAssert.assertThat(output, allOf(OutputCaptureRule.this.matchers));
            }
          }
          finally {
            OutputCaptureRule.this.delegate.pop();
          }
        }
      }
    };
  }

  @Override
  public String getAll() {
    return this.delegate.getAll();
  }

  @Override
  public String getOut() {
    return this.delegate.getOut();
  }

  @Override
  public String getErr() {
    return this.delegate.getErr();
  }

  @Override
  public String toString() {
    return this.delegate.toString();
  }

  /**
   * Verify that the output is matched by the supplied {@code matcher}. Verification is
   * performed after the test method has executed.
   *
   * @param matcher the matcher
   */
  public void expect(Matcher<? super String> matcher) {
    this.matchers.add(matcher);
  }

}
