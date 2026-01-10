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

package infra.aop.framework;

import infra.aop.support.AopUtils;
import infra.beans.factory.annotation.Autowired;
import infra.lang.Assert;
import infra.stereotype.Component;

/**
 * @author Oliver Gierke
 */
@Component
public class ClassWithComplexConstructor {

  private final Dependency dependency;

  ClassWithComplexConstructor selfReference;

  @Autowired
  public ClassWithComplexConstructor(Dependency dependency) {
    Assert.notNull(dependency, "No Dependency bean injected");
    this.dependency = dependency;
  }

  public Dependency getDependency() {
    return this.dependency;
  }

  @Autowired
  public void setSelfReference(ClassWithComplexConstructor selfReference) {
    this.selfReference = selfReference;
  }

  public void method() {
    Assert.state(this.selfReference != this && AopUtils.isCglibProxy(this.selfReference),
            "Self reference must be a CGLIB proxy");
    this.dependency.method();
  }

}
