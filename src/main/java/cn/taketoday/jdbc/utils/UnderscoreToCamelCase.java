package cn.taketoday.jdbc.utils;

import cn.taketoday.core.utils.StringUtils;

/**
 * Takes a string formatted like: 'my_string_variable' and returns it as:
 * 'myStringVariable'
 *
 * @author ryancarlson
 * @author dimzon - complete rewrite
 */
public class UnderscoreToCamelCase {

  public static String convert(String underscore) {
    if (StringUtils.isEmpty(underscore)) {
      return underscore;
    }

    final char[] chars = underscore.toCharArray();
    int write = -1;
    boolean upper = false;
    for (final char c : chars) {
      if ('_' == c) {
        upper = true;
        continue;
      }
      if (upper) {
        upper = false;
        chars[++write] = Character.toUpperCase(c);
      }
      else {
        chars[++write] = Character.toLowerCase(c);
      }
    }
    return new String(chars, 0, ++write);
  }
}
