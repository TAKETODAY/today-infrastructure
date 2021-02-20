/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aop.listener.AspectsDestroyListener;
import cn.taketoday.aop.proxy.AspectAutoProxyCreator;
import cn.taketoday.aop.proxy.DefaultAutoProxyCreator;
import cn.taketoday.aop.target.TargetSourceCreator;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * Enable Aspect Oriented Programming
 *
 * @author TODAY <br>
 * 2020-02-06 20:02
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Import(AutoProxyConfiguration.class)
public @interface EnableAspectAutoProxy {

}

class AutoProxyConfiguration {

  @MissingBean
  DefaultAutoProxyCreator defaultAutoProxyCreator(TargetSourceCreator[] sourceCreators) {
    final DefaultAutoProxyCreator proxyCreator = new DefaultAutoProxyCreator();

    if(ObjectUtils.isNotEmpty(sourceCreators)) {
      proxyCreator.setTargetSourceCreators(sourceCreators);
    }
    return proxyCreator;
  }

  @MissingBean
  AspectAutoProxyCreator aspectAutoProxyCreator() {
    return new AspectAutoProxyCreator();
  }

  @MissingBean
  AspectsDestroyListener aspectsDestroyListener() {
    return new AspectsDestroyListener();
  }

}
