/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.aop;

import infra.aop.framework.autoproxy.AbstractAutoProxyCreator;

/**
 * Marker interface that indicates a bean that is part of AOP infrastructure. In
 * particular, this implies that any such bean is not subject to auto-proxying,
 * even if a pointcut would match.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 21:31
 * @see AbstractAutoProxyCreator
 * @since 3.0
 */
public interface AopInfrastructureBean {

}
