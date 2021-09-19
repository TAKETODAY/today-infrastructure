/*
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.core;

import java.lang.reflect.Member;
import java.util.function.Function;

public class MethodInfoTransformer implements Function<Member, MethodInfo> {

  private static final MethodInfoTransformer INSTANCE = new MethodInfoTransformer();

  public static MethodInfoTransformer getInstance() {
    return INSTANCE;
  }

  @Override
  public MethodInfo apply(Member value) {
    return MethodInfo.from(value);
  }

}
