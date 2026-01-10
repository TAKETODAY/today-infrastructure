/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

import infra.lang.Constant;

/**
 * {@link ParameterNameDiscoverer} implementation which uses JDK 8's reflection facilities
 * for introspecting parameter names (based on the "-parameters" compiler flag).
 *
 * @author TODAY 2021/9/10 22:44
 * @see Parameter#getName()
 * @since 4.0
 */
public class ReflectiveParameterNameDiscoverer extends ParameterNameDiscoverer {

  @Override
  public String @Nullable [] getParameterNames(Executable executable) {
    final Parameter[] parameters = executable.getParameters();
    if (parameters.length == 0) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    int i = 0;
    String[] parameterNames = null;
    for (final Parameter parameter : parameters) {
      if (parameter.isNamePresent()) {
        if (parameterNames == null) {
          parameterNames = new String[parameters.length];
        }
        parameterNames[i++] = parameter.getName();
      }
      else {
        return null;
      }
    }
    return parameterNames;
  }

}
