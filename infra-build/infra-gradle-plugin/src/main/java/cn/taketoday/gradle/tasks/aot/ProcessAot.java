/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.gradle.tasks.aot;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom {@link JavaExec} task for ahead-of-time processing of a Infra application.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@CacheableTask
public abstract class ProcessAot extends AbstractAot {

  public ProcessAot() {
    getMainClass().set("cn.taketoday.framework.ApplicationAotProcessor");
  }

  /**
   * Returns the main class of the application that is to be processed ahead-of-time.
   *
   * @return the application main class property
   */
  @Input
  public abstract Property<String> getApplicationMainClass();

  @Override
  @TaskAction
  public void exec() {
    List<String> args = new ArrayList<>();
    args.add(getApplicationMainClass().get());
    args.addAll(processorArgs());
    setArgs(args);
    super.exec();
  }

}
