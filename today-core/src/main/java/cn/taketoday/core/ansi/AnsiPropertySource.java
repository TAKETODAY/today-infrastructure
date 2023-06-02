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

package cn.taketoday.core.ansi;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.util.StringUtils;

/**
 * {@link PropertyResolver} for {@link AnsiStyle}, {@link AnsiColor},
 * {@link AnsiBackground} and {@link Ansi8BitColor} elements. Supports properties of the
 * form {@code AnsiStyle.BOLD}, {@code AnsiColor.RED} or {@code AnsiBackground.GREEN}.
 * Also supports a prefix of {@code Ansi.} which is an aggregation of everything (with
 * background colors prefixed {@code BG_}).
 * <p>
 * ANSI 8-bit color codes can be used with {@code AnsiColor} and {@code AnsiBackground}.
 * For example, {@code AnsiColor.208} will render orange text.
 * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">Wikipedia</a> has a complete
 * list of the 8-bit color codes that can be used.
 *
 * @author Phillip Webb
 * @author Toshiaki Maki
 * @since 4.0
 */
public class AnsiPropertySource extends PropertySource<AnsiElement> {

  private static final List<Mapping> MAPPINGS = List.of(
          new EnumMapping<>("AnsiStyle.", AnsiStyle.class),
          new EnumMapping<>("AnsiColor.", AnsiColor.class),
          new Ansi8BitColorMapping("AnsiColor.", Ansi8BitColor::foreground),
          new EnumMapping<>("AnsiBackground.", AnsiBackground.class),
          new Ansi8BitColorMapping("AnsiBackground.", Ansi8BitColor::background),
          new EnumMapping<>("Ansi.", AnsiStyle.class),
          new EnumMapping<>("Ansi.", AnsiColor.class),
          new EnumMapping<>("Ansi.BG_", AnsiBackground.class)
  );

  private final boolean encode;

  /**
   * Create a new {@link AnsiPropertySource} instance.
   *
   * @param name the name of the property source
   * @param encode if the output should be encoded
   */
  public AnsiPropertySource(String name, boolean encode) {
    super(name);
    this.encode = encode;
  }

  @Override
  public Object getProperty(String name) {
    if (StringUtils.isNotEmpty(name)) {
      for (Mapping mapping : MAPPINGS) {
        String prefix = mapping.getPrefix();
        if (name.startsWith(prefix)) {
          String postfix = name.substring(prefix.length());
          AnsiElement element = mapping.getElement(postfix);
          if (element != null) {
            return encode ? AnsiOutput.encode(element) : element;
          }
        }
      }
    }
    return null;
  }

  /**
   * Mapping between a name and the pseudo property source.
   */
  private abstract static class Mapping {

    private final String prefix;

    Mapping(String prefix) {
      this.prefix = prefix;
    }

    String getPrefix() {
      return this.prefix;
    }

    abstract AnsiElement getElement(String postfix);

  }

  /**
   * {@link Mapping} for {@link AnsiElement} enums.
   */
  private static class EnumMapping<E extends Enum<E> & AnsiElement> extends Mapping {

    private final Set<E> enums;

    EnumMapping(String prefix, Class<E> enumType) {
      super(prefix);
      this.enums = EnumSet.allOf(enumType);
    }

    @Override
    AnsiElement getElement(String postfix) {
      for (Enum<?> candidate : this.enums) {
        if (candidate.name().equals(postfix)) {
          return (AnsiElement) candidate;
        }
      }
      return null;
    }

  }

  /**
   * {@link Mapping} for {@link Ansi8BitColor}.
   */
  private static class Ansi8BitColorMapping extends Mapping {

    private final IntFunction<Ansi8BitColor> factory;

    Ansi8BitColorMapping(String prefix, IntFunction<Ansi8BitColor> factory) {
      super(prefix);
      this.factory = factory;
    }

    @Override
    AnsiElement getElement(String postfix) {
      if (containsOnlyDigits(postfix)) {
        try {
          return this.factory.apply(Integer.parseInt(postfix));
        }
        catch (IllegalArgumentException ignored) { }
      }
      return null;
    }

    private boolean containsOnlyDigits(String postfix) {
      for (int i = 0; i < postfix.length(); i++) {
        if (!Character.isDigit(postfix.charAt(i))) {
          return false;
        }
      }
      return !postfix.isEmpty();
    }

  }

}
