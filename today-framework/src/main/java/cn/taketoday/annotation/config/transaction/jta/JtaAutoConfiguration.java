/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.annotation.config.transaction.jta;

import cn.taketoday.annotation.config.jpa.HibernateJpaAutoConfiguration;
import cn.taketoday.annotation.config.transaction.TransactionAutoConfiguration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnProperty;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JTA.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Nishant Raut
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@AutoConfiguration(before = { HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class })
@ConditionalOnClass(jakarta.transaction.Transaction.class)
@ConditionalOnProperty(prefix = "infra.jta", value = "enabled", matchIfMissing = true)
@Import(JndiJtaConfiguration.class)
public class JtaAutoConfiguration {

}
