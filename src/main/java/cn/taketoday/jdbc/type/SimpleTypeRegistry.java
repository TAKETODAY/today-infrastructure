/**
 * Copyright 2009-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.jdbc.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

/**
 * @author Clinton Begin
 * @author TODAY
 */
public abstract class SimpleTypeRegistry {

  static final HashSet<Class<?>> primitiveTypes = new HashSet<>(32);

  static {
    primitiveTypes.add(String.class);
    primitiveTypes.add(Byte.class);
    primitiveTypes.add(Short.class);
    primitiveTypes.add(Character.class);
    primitiveTypes.add(Integer.class);
    primitiveTypes.add(Long.class);
    primitiveTypes.add(Float.class);
    primitiveTypes.add(Double.class);
    primitiveTypes.add(Boolean.class);
    primitiveTypes.add(Date.class);
    primitiveTypes.add(Class.class);
    primitiveTypes.add(BigInteger.class);
    primitiveTypes.add(BigDecimal.class);

    Collections.addAll(primitiveTypes, //
                       boolean.class, byte.class, char.class, int.class,
                       long.class, double.class, float.class, short.class,
                       boolean[].class, byte[].class, char[].class, double[].class,
                       float[].class, int[].class, long[].class, short[].class,
                       float[].class, int[].class, long[].class, short[].class
    );
  }

  /*
   * Tells us if the class passed in is a known common type
   *
   * @param clazz The class to check
   * @return True if the class is known
   */
  public static boolean isSimpleType(Class<?> clazz) {
    return primitiveTypes.contains(clazz);
  }

}
