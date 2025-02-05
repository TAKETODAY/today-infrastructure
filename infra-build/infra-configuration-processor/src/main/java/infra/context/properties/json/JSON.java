/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.properties.json;

final class JSON {

  private JSON() {

  }

  static double checkDouble(double d) throws JSONException {
    if (Double.isInfinite(d) || Double.isNaN(d)) {
      throw new JSONException("Forbidden numeric value: " + d);
    }
    return d;
  }

  static Boolean toBoolean(Object value) {
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof String stringValue) {
      if ("true".equalsIgnoreCase(stringValue)) {
        return true;
      }
      if ("false".equalsIgnoreCase(stringValue)) {
        return false;
      }
    }
    return null;
  }

  static Double toDouble(Object value) {
    if (value instanceof Double) {
      return (Double) value;
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    if (value instanceof String) {
      try {
        return Double.valueOf((String) value);
      }
      catch (NumberFormatException ignored) {
      }
    }
    return null;
  }

  static Integer toInteger(Object value) {
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value instanceof String) {
      try {
        return (int) Double.parseDouble((String) value);
      }
      catch (NumberFormatException ignored) {
      }
    }
    return null;
  }

  static Long toLong(Object value) {
    if (value instanceof Long) {
      return (Long) value;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    if (value instanceof String) {
      try {
        return (long) Double.parseDouble((String) value);
      }
      catch (NumberFormatException ignored) {
      }
    }
    return null;
  }

  static String toString(Object value) {
    if (value instanceof String) {
      return (String) value;
    }
    if (value != null) {
      return String.valueOf(value);
    }
    return null;
  }

  public static JSONException typeMismatch(Object indexOrName, Object actual, String requiredType)
          throws JSONException {
    if (actual == null) {
      throw new JSONException("Value at " + indexOrName + " is null.");
    }
    throw new JSONException("Value " + actual + " at " + indexOrName + " of type " + actual.getClass().getName()
            + " cannot be converted to " + requiredType);
  }

  public static JSONException typeMismatch(Object actual, String requiredType) throws JSONException {
    if (actual == null) {
      throw new JSONException("Value is null.");
    }
    throw new JSONException("Value " + actual + " of type " + actual.getClass().getName()
            + " cannot be converted to " + requiredType);
  }

}
