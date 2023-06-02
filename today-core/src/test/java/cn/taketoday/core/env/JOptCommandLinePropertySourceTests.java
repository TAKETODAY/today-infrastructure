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

package cn.taketoday.core.env;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JOptCommandLinePropertySource}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 */
class JOptCommandLinePropertySourceTests {

  @Test
  void withRequiredArg_andArgIsPresent() {
    OptionParser parser = new OptionParser();
    parser.accepts("foo").withRequiredArg();
    OptionSet options = parser.parse("--foo=bar");

    PropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void withOptionalArg_andArgIsMissing() {
    OptionParser parser = new OptionParser();
    parser.accepts("foo").withOptionalArg();
    OptionSet options = parser.parse("--foo");

    PropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.containsProperty("foo")).isTrue();
    assertThat(ps.getProperty("foo")).isEqualTo("");
  }

  @Test
    // gh-24464
  void withOptionalArg_andArgIsEmpty() {
    OptionParser parser = new OptionParser();
    parser.accepts("foo").withOptionalArg();
    OptionSet options = parser.parse("--foo=");

    PropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.containsProperty("foo")).isTrue();
    assertThat(ps.getProperty("foo")).isEqualTo("");
  }

  @Test
  void withNoArg() {
    OptionParser parser = new OptionParser();
    parser.accepts("o1");
    parser.accepts("o2");
    OptionSet options = parser.parse("--o1");

    PropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.containsProperty("o1")).isTrue();
    assertThat(ps.containsProperty("o2")).isFalse();
    assertThat(ps.getProperty("o1")).isEqualTo("");
    assertThat(ps.getProperty("o2")).isNull();
  }

  @Test
  void withRequiredArg_andMultipleArgsPresent_usingDelimiter() {
    OptionParser parser = new OptionParser();
    parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
    OptionSet options = parser.parse("--foo=bar,baz,biz");

    CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.getOptionValues("foo")).containsExactly("bar", "baz", "biz");
    assertThat(ps.getProperty("foo")).isEqualTo("bar,baz,biz");
  }

  @Test
  void withRequiredArg_andMultipleArgsPresent_usingRepeatedOption() {
    OptionParser parser = new OptionParser();
    parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
    OptionSet options = parser.parse("--foo=bar", "--foo=baz", "--foo=biz");

    CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.getOptionValues("foo")).containsExactly("bar", "baz", "biz");
    assertThat(ps.getProperty("foo")).isEqualTo("bar,baz,biz");
  }

  @Test
  void withMissingOption() {
    OptionParser parser = new OptionParser();
    parser.accepts("foo").withRequiredArg().withValuesSeparatedBy(',');
    OptionSet options = parser.parse(); // <-- no options whatsoever

    PropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.getProperty("foo")).isNull();
  }

  @Test
  void withDottedOptionName() {
    OptionParser parser = new OptionParser();
    parser.accepts("infra.profiles.active").withRequiredArg();
    OptionSet options = parser.parse("--infra.profiles.active=p1");

    CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.getProperty("infra.profiles.active")).isEqualTo("p1");
  }

  @Test
  void withDefaultNonOptionArgsNameAndNoNonOptionArgsPresent() {
    OptionParser parser = new OptionParser();
    parser.acceptsAll(Arrays.asList("o1", "option1")).withRequiredArg();
    parser.accepts("o2");
    OptionSet optionSet = parser.parse("--o1=v1", "--o2");
    EnumerablePropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);

    assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
    assertThat(ps.containsProperty("o1")).isTrue();
    assertThat(ps.containsProperty("o2")).isTrue();

    assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
    assertThat(ps.getProperty("nonOptionArgs")).isNull();
    assertThat(ps.getPropertyNames()).hasSize(2);
  }

  @Test
  void withDefaultNonOptionArgsNameAndNonOptionArgsPresent() {
    OptionParser parser = new OptionParser();
    parser.accepts("o1").withRequiredArg();
    parser.accepts("o2");
    OptionSet optionSet = parser.parse("--o1=v1", "noa1", "--o2", "noa2");
    PropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);

    assertThat(ps.containsProperty("nonOptionArgs")).isTrue();
    assertThat(ps.containsProperty("o1")).isTrue();
    assertThat(ps.containsProperty("o2")).isTrue();

    assertThat(ps.getProperty("nonOptionArgs")).isEqualTo("noa1,noa2");
  }

  @Test
  void withCustomNonOptionArgsNameAndNoNonOptionArgsPresent() {
    OptionParser parser = new OptionParser();
    parser.accepts("o1").withRequiredArg();
    parser.accepts("o2");
    OptionSet optionSet = parser.parse("--o1=v1", "noa1", "--o2", "noa2");
    CommandLinePropertySource<?> ps = new JOptCommandLinePropertySource(optionSet);
    ps.setNonOptionArgsPropertyName("NOA");

    assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
    assertThat(ps.containsProperty("NOA")).isTrue();
    assertThat(ps.containsProperty("o1")).isTrue();
    assertThat(ps.containsProperty("o2")).isTrue();
    String nonOptionArgs = ps.getProperty("NOA");
    assertThat(nonOptionArgs).isEqualTo("noa1,noa2");
  }

  @Test
  void withRequiredArg_ofTypeEnum() {
    OptionParser parser = new OptionParser();
    parser.accepts("o1").withRequiredArg().ofType(OptionEnum.class);
    OptionSet options = parser.parse("--o1=VAL_1");

    PropertySource<?> ps = new JOptCommandLinePropertySource(options);
    assertThat(ps.getProperty("o1")).isEqualTo("VAL_1");
  }

  public enum OptionEnum {
    VAL_1
  }

}
