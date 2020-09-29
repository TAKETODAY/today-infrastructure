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
package cn.taketoday.context.cglib.core;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * @author TODAY <br>
 * 2019-09-02 19:24
 */
public class RejectModifierPredicate implements Predicate<Method> {

  private final int rejectMask;

  public RejectModifierPredicate(int rejectMask) {
    this.rejectMask = rejectMask;
  }

  @Override
  public boolean test(Method arg) {
    return (arg.getModifiers() & rejectMask) == 0;
  }
}
