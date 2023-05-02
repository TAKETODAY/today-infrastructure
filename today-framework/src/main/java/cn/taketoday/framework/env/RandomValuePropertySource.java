/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.env;

import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.DigestUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link PropertySource} that returns a random value for any property that starts with
 * {@literal "random."}. Where the "unqualified property name" is the portion of the
 * requested property name beyond the "random." prefix, this {@link PropertySource}
 * returns:
 * <ul>
 * <li>When {@literal "int"}, a random {@link Integer} value, restricted by an optionally
 * specified range.</li>
 * <li>When {@literal "long"}, a random {@link Long} value, restricted by an optionally
 * specified range.</li>
 * <li>When {@literal "uuid"}, a random {@link UUID} value.</li>
 * <li>Otherwise, a {@code byte[]}.</li>
 * </ul>
 * The {@literal "random.int"} and {@literal "random.long"} properties supports a range
 * suffix whose syntax is:
 * <p>
 * {@code OPEN value (,max) CLOSE} where the {@code OPEN,CLOSE} are any character and
 * {@code value,max} are integers. If {@code max} is not provided, then 0 is used as the
 * lower bound and {@code value} is the upper bound. If {@code max} is provided then
 * {@code value} is the minimum value and {@code max} is the maximum (exclusive).
 *
 * @author Dave Syer
 * @author Matt Benson
 * @author Madhura Bhave
 * @since 4.0
 */
public class RandomValuePropertySource extends PropertySource<Random> {

  /**
   * Name of the random {@link PropertySource}.
   */
  public static final String RANDOM_PROPERTY_SOURCE_NAME = "random";

  private static final String PREFIX = "random.";

  private static final Logger logger = LoggerFactory.getLogger(RandomValuePropertySource.class);

  public RandomValuePropertySource() {
    this(RANDOM_PROPERTY_SOURCE_NAME);
  }

  public RandomValuePropertySource(String name) {
    super(name, new Random());
  }

  @Override
  public Object getProperty(String name) {
    if (!name.startsWith(PREFIX)) {
      return null;
    }
    logger.trace("Generating random property for '{}'", name);
    return getRandomValue(name.substring(PREFIX.length()));
  }

  private Object getRandomValue(String type) {
    if (type.equals("int")) {
      return getSource().nextInt();
    }
    if (type.equals("long")) {
      return getSource().nextLong();
    }
    String range = getRange(type, "int");
    if (range != null) {
      return getNextIntInRange(Range.of(range, Integer::parseInt));
    }
    range = getRange(type, "long");
    if (range != null) {
      return getNextLongInRange(Range.of(range, Long::parseLong));
    }
    if (type.equals("uuid")) {
      return UUID.randomUUID().toString();
    }
    return getRandomBytes();
  }

  private String getRange(String type, String prefix) {
    if (type.startsWith(prefix)) {
      int startIndex = prefix.length() + 1;
      if (type.length() > startIndex) {
        return type.substring(startIndex, type.length() - 1);
      }
    }
    return null;
  }

  private int getNextIntInRange(Range<Integer> range) {
    OptionalInt first = getSource().ints(1, range.getMin(), range.getMax()).findFirst();
    assertPresent(first.isPresent(), range);
    return first.getAsInt();
  }

  private long getNextLongInRange(Range<Long> range) {
    OptionalLong first = getSource().longs(1, range.getMin(), range.getMax()).findFirst();
    assertPresent(first.isPresent(), range);
    return first.getAsLong();
  }

  private void assertPresent(boolean present, Range<?> range) {
    if (!present) {
      throw new IllegalStateException("Could not get random number for range '" + range + "'");
    }
  }

  private Object getRandomBytes() {
    byte[] bytes = new byte[32];
    getSource().nextBytes(bytes);
    return DigestUtils.md5DigestAsHex(bytes);
  }

  public static void addToEnvironment(ConfigurableEnvironment environment) {
    PropertySources sources = environment.getPropertySources();
    PropertySource<?> existing = sources.get(RANDOM_PROPERTY_SOURCE_NAME);
    if (existing != null) {
      logger.trace("RandomValuePropertySource already present");
      return;
    }
    RandomValuePropertySource randomSource = new RandomValuePropertySource(RANDOM_PROPERTY_SOURCE_NAME);
    if (sources.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) != null) {
      sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, randomSource);
    }
    else {
      sources.addLast(randomSource);
    }
    logger.trace("RandomValuePropertySource add to Environment");
  }

  static final class Range<T extends Number> {

    private final String value;

    private final T min;

    private final T max;

    private Range(String value, T min, T max) {
      this.value = value;
      this.min = min;
      this.max = max;
    }

    T getMin() {
      return this.min;
    }

    T getMax() {
      return this.max;
    }

    @Override
    public String toString() {
      return this.value;
    }

    static <T extends Number & Comparable<T>> Range<T> of(String value, Function<String, T> parse) {
      T zero = parse.apply("0");
      String[] tokens = StringUtils.commaDelimitedListToStringArray(value);
      T min = parse.apply(tokens[0]);
      if (tokens.length == 1) {
        Assert.isTrue(min.compareTo(zero) > 0, "Bound must be positive.");
        return new Range<>(value, zero, min);
      }
      T max = parse.apply(tokens[1]);
      Assert.isTrue(min.compareTo(max) < 0, "Lower bound must be less than upper bound.");
      return new Range<>(value, min, max);
    }

  }

}
