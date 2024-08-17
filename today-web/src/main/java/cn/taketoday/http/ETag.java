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

package cn.taketoday.http;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * Represents an ETag for HTTP conditional requests.
 *
 * @param tag the unquoted tag value
 * @param weak whether the entity tag is for weak or strong validation
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7232">RFC 7232</a>
 * @since 5.0
 */
public record ETag(String tag, boolean weak) {

  private static final Logger logger = LoggerFactory.getLogger(ETag.class);

  private static final ETag WILDCARD = new ETag("*", false);

  /**
   * Whether this a wildcard tag matching to any entity tag value.
   */
  public boolean isWildcard() {
    return (this == WILDCARD);
  }

  /**
   * Return the fully formatted tag including "W/" prefix and quotes.
   */
  public String formattedTag() {
    if (isWildcard()) {
      return "*";
    }
    return (this.weak ? "W/" : "") + "\"" + this.tag + "\"";
  }

  @Override
  public String toString() {
    return formattedTag();
  }

  /**
   * Parse entity tags from an "If-Match" or "If-None-Match" header.
   *
   * @param source the source string to parse
   * @return the parsed ETags
   */
  public static List<ETag> parse(String source) {

    List<ETag> result = new ArrayList<>();
    State state = State.BEFORE_QUOTES;
    int startIndex = -1;
    boolean weak = false;

    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);

      if (state == State.IN_QUOTES) {
        if (c == '"') {
          String tag = source.substring(startIndex, i);
          if (StringUtils.hasText(tag)) {
            result.add(new ETag(tag, weak));
          }
          state = State.AFTER_QUOTES;
          startIndex = -1;
          weak = false;
        }
        continue;
      }

      if (Character.isWhitespace(c)) {
        continue;
      }

      if (c == ',') {
        state = State.BEFORE_QUOTES;
        continue;
      }

      if (state == State.BEFORE_QUOTES) {
        if (c == '*') {
          result.add(WILDCARD);
          state = State.AFTER_QUOTES;
          continue;
        }
        if (c == '"') {
          state = State.IN_QUOTES;
          startIndex = i + 1;
          continue;
        }
        if (c == 'W' && source.length() > i + 2) {
          if (source.charAt(i + 1) == '/' && source.charAt(i + 2) == '"') {
            state = State.IN_QUOTES;
            i = i + 2;
            startIndex = i + 1;
            weak = true;
            continue;
          }
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Unexpected char at index " + i);
      }
    }

    if (state != State.IN_QUOTES && logger.isDebugEnabled()) {
      logger.debug("Expected closing '\"'");
    }

    return result;
  }

  private enum State {

    BEFORE_QUOTES, IN_QUOTES, AFTER_QUOTES

  }

}
