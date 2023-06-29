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

package cn.taketoday.web.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.http.MediaTypeEditor;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/29 13:00
 */
class WebUtilRuntimeHintsTests {

  private final RuntimeHints hints = new RuntimeHints();

  @BeforeEach
  void setup() {
    TodayStrategies.forLocation("META-INF/config/aot.factories")
            .load(RuntimeHintsRegistrar.class)
            .forEach(registrar -> registrar.registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
  }

  @Test
  void mediaTypeEditorHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(MediaTypeEditor.class)
            .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.hints);
  }

}