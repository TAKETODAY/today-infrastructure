/*
 * Copyright 2022-2024 the original author or authors.
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

package infra.shell.jline.tui.component.support;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.AttributedString;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp.Capability;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import infra.context.ResourceLoaderAware;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.shell.jline.tui.component.context.ComponentContext;
import infra.shell.jline.tui.style.TemplateExecutor;
import infra.util.Assert;
import infra.util.FileCopyUtils;
import infra.util.StringUtils;

/**
 * Base class for components.
 *
 * @author Janne Valkealahti
 * @author Piotr Olaszewski
 */
public abstract class AbstractComponent<T extends ComponentContext<T>> implements ResourceLoaderAware {

  private static final Logger log = LoggerFactory.getLogger(AbstractComponent.class);

  public static final String OPERATION_EXIT = "EXIT";

  public static final String OPERATION_BACKSPACE = "BACKSPACE";

  public static final String OPERATION_CHAR = "CHAR";

  public static final String OPERATION_UNICODE = "UNICODE";

  public static final String OPERATION_SELECT = "SELECT";

  public static final String OPERATION_DOWN = "DOWN";

  public static final String OPERATION_UP = "UP";

  private final Terminal terminal;

  private final BindingReader bindingReader;

  private final KeyMap<String> keyMap = new KeyMap<>();

  private final List<Consumer<T>> preRunHandlers = new ArrayList<>();

  private final List<Consumer<T>> postRunHandlers = new ArrayList<>();

  private @Nullable Function<T, List<AttributedString>> renderer;

  private boolean printResults = true;

  private @Nullable String templateLocation;

  private @Nullable TemplateExecutor templateExecutor;

  private @Nullable ResourceLoader resourceLoader;

  public AbstractComponent(Terminal terminal) {
    Assert.notNull(terminal, "terminal must be set");
    this.terminal = terminal;
    this.bindingReader = new BindingReader(terminal.reader());
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * Gets a {@link Terminal}.
   *
   * @return a terminal
   */
  public Terminal getTerminal() {
    return terminal;
  }

  /**
   * Sets a display renderer.
   *
   * @param renderer the display renderer function
   */
  public void setRenderer(@Nullable Function<T, List<AttributedString>> renderer) {
    this.renderer = renderer;
  }

  /**
   * Render to be shows content of a display with set display renderer using a given
   * context.
   *
   * @param context the context
   * @return list of attributed strings
   */
  public List<AttributedString> render(T context) {
    log.debug(
            String.format("Rendering with context [%s] as class [%s] in [%s]", context, context.getClass(), this));
    return renderer == null ? List.of() : renderer.apply(context);
  }

  /**
   * Adds a pre-run handler.
   *
   * @param handler the handler
   */
  public void addPreRunHandler(Consumer<T> handler) {
    this.preRunHandlers.add(handler);
  }

  /**
   * Adds a post-run handler.
   *
   * @param handler the handler
   */
  public void addPostRunHandler(Consumer<T> handler) {
    this.postRunHandlers.add(handler);
  }

  /**
   * Sets if results should be printed into a console, Defaults to {@code true}.
   *
   * @param printResults flag setting if results are printed
   */
  public void setPrintResults(boolean printResults) {
    this.printResults = printResults;
  }

  /**
   * Runs a component logic with a given context and returns updated context.
   *
   * @param context the context
   * @return a context
   */
  public final T run(ComponentContext<?> context) {
    bindKeyMap(keyMap);
    context = runPreRunHandlers(getThisContext(context));
    T run = runInternal(getThisContext(context));
    context = runPostRunHandlers(getThisContext(context));
    // if there's no tty don't try to print results as it'd be pointless
    if (printResults && hasTty()) {
      printResults(context);
    }
    return run;
  }

  /**
   * Gets a template executor.
   *
   * @return a template executor
   */
  public @Nullable TemplateExecutor getTemplateExecutor() {
    return templateExecutor;
  }

  /**
   * Sets a template executor.
   *
   * @param templateExecutor the template executor
   */
  public void setTemplateExecutor(TemplateExecutor templateExecutor) {
    this.templateExecutor = templateExecutor;
  }

  /**
   * Sets a template location.
   *
   * @param templateLocation the template location
   */
  public void setTemplateLocation(String templateLocation) {
    this.templateLocation = templateLocation;
  }

  /**
   * Checks if this component has an existing {@code tty}.
   *
   * @return true if component has tty
   */
  protected boolean hasTty() {
    boolean hasTty = true;
    if (this.terminal instanceof DumbTerminal) {
      if (this.terminal.getSize().getRows() == 0) {
        hasTty = false;
      }
    }
    log.debug(String.format("Terminal is %s with size %s, marking hasTty as %s", this.terminal,
            this.terminal.getSize(), hasTty));
    return hasTty;
  }

  /**
   * Render a given template with attributes.
   *
   * @param attributes the attributes
   * @return rendered content as attributed strings
   */
  protected List<AttributedString> renderTemplateResource(Map<String, @Nullable Object> attributes) {
    Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
    Assert.notNull(templateLocation, "'templateLocation' must not be null");
    Assert.notNull(templateExecutor, "'templateExecutor' must not be null");

    String templateResource = resourceAsString(resourceLoader.getResource(templateLocation));
    log.debug("Rendering template: " + templateResource);
    log.debug("Rendering template attributes: " + attributes);
    AttributedString rendered;
    if (templateLocation.endsWith(".stg")) {
      rendered = templateExecutor.renderGroup(templateResource, attributes);
    }
    else {
      rendered = templateExecutor.render(templateResource, attributes);
    }
    log.debug("Template executor result: [" + rendered + "]");
    List<AttributedString> rows = rendered.columnSplitLength(Integer.MAX_VALUE);
    // remove last if empty as columnsplit adds it
    int lastIndex = rows.size() - 1;
    if (lastIndex > 0 && rows.get(lastIndex).length() == 0) {
      rows.remove(lastIndex);
    }
    return rows;
  }

  /**
   * Gets a real component context using common this trick.
   *
   * @param context the context
   * @return a component context
   */
  public abstract T getThisContext(@Nullable ComponentContext<?> context);

  /**
   * Read input.
   *
   * @param bindingReader the binding reader
   * @param keyMap the key map
   * @param context the context
   * @return true if read is complete, false to stop
   */
  protected abstract boolean read(BindingReader bindingReader, KeyMap<String> keyMap, T context);

  /**
   * Run internal logic called from public run method.
   *
   * @param context the context
   * @return a context
   */
  protected abstract T runInternal(T context);

  /**
   * Bind key map.
   *
   * @param keyMap the key bindings to configure
   */
  protected abstract void bindKeyMap(KeyMap<String> keyMap);

  /**
   * Enter into read loop. This should be called from a component.
   *
   * @param context the context
   */
  protected void loop(ComponentContext<?> context) {
    Display display = new Display(terminal, false);
    Attributes attr = terminal.enterRawMode();
    Size size = new Size();

    try {
      terminal.puts(Capability.keypad_xmit);
      terminal.puts(Capability.cursor_invisible);
      terminal.writer().flush();
      size.copy(terminal.getSize());
      display.clear();
      display.reset();

      while (true) {
        display.resize(size.getRows(), size.getColumns());
        display.update(render(getThisContext(context)), 0);
        boolean exit = read(bindingReader, keyMap, getThisContext(context));
        if (exit) {
          break;
        }
      }
    }
    finally {
      terminal.setAttributes(attr);
      terminal.puts(Capability.keypad_local);
      terminal.puts(Capability.cursor_normal);
      display.update(Collections.emptyList(), 0);
    }
  }

  /**
   * Run pre-run handlers
   *
   * @param context the context
   * @return a context
   */
  protected T runPreRunHandlers(T context) {
    this.preRunHandlers.stream().forEach(c -> c.accept(context));
    return context;
  }

  /**
   * Run post-run handlers
   *
   * @param context the context
   * @return a context
   */
  protected T runPostRunHandlers(T context) {
    this.postRunHandlers.stream().forEach(c -> c.accept(context));
    return context;
  }

  private void printResults(ComponentContext<?> context) {
    log.debug(String.format("About to write result with incoming context [%s] as class [%s] in [%s]", context,
            context.getClass(), this));
    String out = render(getThisContext(context)).stream().map(as -> as.toAnsi()).collect(Collectors.joining("\n"));
    log.debug(String.format("Writing result [%s] in [%s]", out, this));
    if (StringUtils.hasText(out)) {
      terminal.writer().println(out);
      terminal.writer().flush();
    }
  }

  private static String resourceAsString(Resource resource) {
    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      return FileCopyUtils.copyToString(reader);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
