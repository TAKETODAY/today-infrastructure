/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package org.test;

import org.test.SampleApplication.SampleApplicationRuntimeHints;

import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.framework.Application;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportRuntimeHints;
import cn.taketoday.stereotype.Service;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(SampleApplicationRuntimeHints.class)
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}

	static class SampleApplicationRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			// Force creation of at least one JDK proxy
			hints.proxies().registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(Service.class));
		}
	}

}
