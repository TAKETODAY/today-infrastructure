package cn.taketoday.jdbc.utils;

import cn.taketoday.core.Constant;
import cn.taketoday.core.utils.StringUtils;

/**
 * @author TODAY 2021/1/8 18:24
 */
public class CamelCaseToUnderscore {

  /**
   * Convert a name in camelCase to an underscored name in lower case. Any upper
   * case letters are converted to lower case with a preceding underscore.
   *
   * @param name
   *         the original name
   *
   * @return the converted name
   */
  public static String convert(String name) {

    if (StringUtils.isEmpty(name)) {
      return Constant.BLANK;
    }

    final int length = name.length();
    final StringBuilder ret = new StringBuilder();
    for (int i = 0; i < length; i++) {
      final char c = name.charAt(i);
      if (c > 0x40 && c < 0x5b) {
        ret.append('_').append((char) (c | 0x20));
      }
      else {
        ret.append(c);
      }
    }
    return ret.toString();
  }

}

