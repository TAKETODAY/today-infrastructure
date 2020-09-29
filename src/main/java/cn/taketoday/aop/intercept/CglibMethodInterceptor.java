/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.aop.intercept;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.taketoday.aop.proxy.TargetSource;
import cn.taketoday.context.cglib.proxy.MethodProxy;
import cn.taketoday.context.utils.OrderUtils;

/**
 * @author TODAY <br>
 *
 *         2018-11-06 19:14
 */
//@Slf4j
public class CglibMethodInterceptor implements cn.taketoday.context.cglib.proxy.MethodInterceptor {

  private final Object target;
  private final HashMap<Method, MethodInterceptor[]> aspectMappings;

  public CglibMethodInterceptor(TargetSource targetSource) {
    this.target = targetSource.getTarget();
    Map<Method, List<MethodInterceptor>> aspectMappings_ = targetSource.getAspectMappings();
    aspectMappings = new HashMap<>(aspectMappings_.size());

    for (Entry<Method, List<MethodInterceptor>> advices : aspectMappings_.entrySet()) {
      final List<MethodInterceptor> interceptors = advices.getValue();
      interceptors.sort(Comparator.comparingInt(OrderUtils::getOrder).reversed());
      aspectMappings.put(advices.getKey(), interceptors.toArray(new MethodInterceptor[interceptors.size()]));
    }
  }

  @Override
  public Object intercept(final Object obj,
                          final Method method,
                          final Object[] args,
                          final MethodProxy proxy) throws Throwable //
  {
    final MethodInterceptor[] advices = aspectMappings.get(method);
    if (advices == null) {
      return proxy.invoke(target, args);
    }
    // log.debug("Intercept method: [{}]", method.getName());
    return new DefaultMethodInvocation(target, method, proxy, args, advices).proceed();
  }

}
