/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.aspectj;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.weaver.patterns.NamePattern;
import org.aspectj.weaver.reflect.ReflectionWorld.ReflectionWorldException;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.ContextBasedMatcher;
import org.aspectj.weaver.tools.FuzzyBoolean;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.aspectj.weaver.tools.MatchingContext;
import org.aspectj.weaver.tools.PointcutDesignatorHandler;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParameter;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.aspectj.weaver.tools.ShadowMatch;
import org.aspectj.weaver.tools.UnsupportedPointcutPrimitiveException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;

import infra.aop.ClassFilter;
import infra.aop.IntroductionAwareMethodMatcher;
import infra.aop.MethodMatcher;
import infra.aop.ProxyMethodInvocation;
import infra.aop.framework.autoproxy.ProxyCreationContext;
import infra.aop.interceptor.ExposeInvocationInterceptor;
import infra.aop.support.AbstractExpressionPointcut;
import infra.aop.support.AopUtils;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.annotation.BeanFactoryAnnotationUtils;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

/**
 * Framework {@link infra.aop.Pointcut} implementation
 * that uses the AspectJ weaver to evaluate a pointcut expression.
 *
 * <p>The pointcut expression value is an AspectJ expression. This can
 * reference other pointcuts and use composition and other operations.
 *
 * <p>Naturally, as this is to be processed by Framework AOP's proxy-based model,
 * only method execution pointcuts are supported.
 *
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AspectJExpressionPointcut extends AbstractExpressionPointcut
        implements ClassFilter, IntroductionAwareMethodMatcher, BeanFactoryAware {

  private static final String AJC_MAGIC = "ajc$";

  private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = Set.of(
          PointcutPrimitive.EXECUTION,
          PointcutPrimitive.ARGS,
          PointcutPrimitive.REFERENCE,
          PointcutPrimitive.THIS,
          PointcutPrimitive.TARGET,
          PointcutPrimitive.WITHIN,
          PointcutPrimitive.AT_ANNOTATION,
          PointcutPrimitive.AT_WITHIN,
          PointcutPrimitive.AT_ARGS,
          PointcutPrimitive.AT_TARGET
  );

  private static final Logger log = LoggerFactory.getLogger(AspectJExpressionPointcut.class);

  @Nullable
  private Class<?> pointcutDeclarationScope;

  private boolean aspectCompiledByAjc;

  private String[] pointcutParameterNames = Constant.EMPTY_STRING_ARRAY;

  private Class<?>[] pointcutParameterTypes = Constant.EMPTY_CLASSES;

  @Nullable
  private BeanFactory beanFactory;

  @Nullable
  private transient volatile ClassLoader pointcutClassLoader;

  @Nullable
  private transient volatile PointcutExpression pointcutExpression;

  private transient volatile boolean pointcutParsingFailed = false;

  /**
   * Create a new default AspectJExpressionPointcut.
   */
  public AspectJExpressionPointcut() {

  }

  /**
   * Create a new AspectJExpressionPointcut with the given settings.
   *
   * @param declarationScope the declaration scope for the pointcut
   * @param paramNames the parameter names for the pointcut
   * @param paramTypes the parameter types for the pointcut
   */
  public AspectJExpressionPointcut(Class<?> declarationScope, String[] paramNames, Class<?>[] paramTypes) {
    setPointcutDeclarationScope(declarationScope);
    if (paramNames.length != paramTypes.length) {
      throw new IllegalStateException(
              "Number of pointcut parameter names must match number of pointcut parameter types");
    }
    this.pointcutParameterNames = paramNames;
    this.pointcutParameterTypes = paramTypes;
  }

  /**
   * Set the declaration scope for the pointcut.
   */
  public void setPointcutDeclarationScope(Class<?> pointcutDeclarationScope) {
    this.pointcutDeclarationScope = pointcutDeclarationScope;
    this.aspectCompiledByAjc = compiledByAjc(pointcutDeclarationScope);
  }

  /**
   * Set the parameter names for the pointcut.
   */
  public void setParameterNames(String... names) {
    this.pointcutParameterNames = names;
  }

  /**
   * Set the parameter types for the pointcut.
   */
  public void setParameterTypes(Class<?>... types) {
    this.pointcutParameterTypes = types;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public ClassFilter getClassFilter() {
    checkExpression();
    return this;
  }

  @Override
  public MethodMatcher getMethodMatcher() {
    checkExpression();
    return this;
  }

  /**
   * Check whether this pointcut is ready to match.
   */
  private void checkExpression() {
    if (getExpression() == null) {
      throw new IllegalStateException("Must set property 'expression' before attempting to match");
    }
  }

  /**
   * Lazily build the underlying AspectJ pointcut expression.
   */
  private PointcutExpression obtainPointcutExpression() {
    PointcutExpression pointcutExpression = this.pointcutExpression;
    if (pointcutExpression == null) {
      ClassLoader pointcutClassLoader = determinePointcutClassLoader();
      pointcutExpression = buildPointcutExpression(pointcutClassLoader);
      this.pointcutClassLoader = pointcutClassLoader;
      this.pointcutExpression = pointcutExpression;
    }
    return pointcutExpression;
  }

  /**
   * Determine the ClassLoader to use for pointcut evaluation.
   */
  @Nullable
  private ClassLoader determinePointcutClassLoader() {
    if (this.beanFactory instanceof ConfigurableBeanFactory cbf) {
      return cbf.getBeanClassLoader();
    }
    if (this.pointcutDeclarationScope != null) {
      return this.pointcutDeclarationScope.getClassLoader();
    }
    return ClassUtils.getDefaultClassLoader();
  }

  /**
   * Build the underlying AspectJ pointcut expression.
   */
  private PointcutExpression buildPointcutExpression(@Nullable ClassLoader classLoader) {
    PointcutParser parser = initializePointcutParser(classLoader);
    PointcutParameter[] pointcutParameters = new PointcutParameter[this.pointcutParameterNames.length];
    for (int i = 0; i < pointcutParameters.length; i++) {
      pointcutParameters[i] = parser.createPointcutParameter(
              this.pointcutParameterNames[i], this.pointcutParameterTypes[i]);
    }
    return parser.parsePointcutExpression(replaceBooleanOperators(resolveExpression()),
            this.pointcutDeclarationScope, pointcutParameters);
  }

  private String resolveExpression() {
    String expression = getExpression();
    Assert.state(expression != null, "No expression set");
    return expression;
  }

  /**
   * Initialize the underlying AspectJ pointcut parser.
   */
  private PointcutParser initializePointcutParser(@Nullable ClassLoader classLoader) {
    PointcutParser parser = PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(
            SUPPORTED_PRIMITIVES, classLoader);
    parser.registerPointcutDesignatorHandler(new BeanPointcutDesignatorHandler());
    return parser;
  }

  /**
   * If a pointcut expression has been specified in XML, the user cannot
   * write "and" as "&&" (though {@code &amp;&amp;} will work).
   * <p>We also allow "and" between two pointcut sub-expressions.
   * <p>This method converts back to {@code &&} for the AspectJ pointcut parser.
   */
  private String replaceBooleanOperators(String pcExpr) {
    String result = StringUtils.replace(pcExpr, " and ", " && ");
    result = StringUtils.replace(result, " or ", " || ");
    result = StringUtils.replace(result, " not ", " ! ");
    return result;
  }

  /**
   * Return the underlying AspectJ pointcut expression.
   */
  public PointcutExpression getPointcutExpression() {
    return obtainPointcutExpression();
  }

  @Override
  public boolean matches(Class<?> targetClass) {
    if (this.pointcutParsingFailed) {
      // Pointcut parsing failed before below -> avoid trying again.
      return false;
    }
    if (this.aspectCompiledByAjc && compiledByAjc(targetClass)) {
      // ajc-compiled aspect class for ajc-compiled target class -> already weaved.
      return false;
    }

    try {
      try {
        return obtainPointcutExpression().couldMatchJoinPointsInType(targetClass);
      }
      catch (ReflectionWorldException ex) {
        log.debug("PointcutExpression matching rejected target class - trying fallback expression", ex);
        // Actually this is still a "maybe" - treat the pointcut as dynamic if we don't know enough yet
        PointcutExpression fallbackExpression = getFallbackPointcutExpression(targetClass);
        if (fallbackExpression != null) {
          return fallbackExpression.couldMatchJoinPointsInType(targetClass);
        }
      }
    }
    catch (IllegalArgumentException | IllegalStateException | UnsupportedPointcutPrimitiveException ex) {
      this.pointcutParsingFailed = true;
      if (log.isDebugEnabled()) {
        log.debug("Pointcut parser rejected expression [" + getExpression() + "]: " + ex);
      }
    }
    catch (Throwable ex) {
      log.debug("PointcutExpression matching rejected target class", ex);
    }
    return false;
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
    ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);

    // Special handling for this, target, @this, @target, @annotation
    // in Infra - we can optimize since we know we have exactly this class,
    // and there will never be matching subclass at runtime.
    if (shadowMatch.alwaysMatches()) {
      return true;
    }
    else if (shadowMatch.neverMatches()) {
      return false;
    }
    else {
      // the maybe case
      if (hasIntroductions) {
        return true;
      }
      // A match test returned maybe - if there are any subtype sensitive variables
      // involved in the test (this, target, at_this, at_target, at_annotation) then
      // we say this is not a match as in Infra there will never be a different
      // runtime subtype.
      RuntimeTestWalker walker = getRuntimeTestWalker(shadowMatch);
      return (!walker.testsSubtypeSensitiveVars() || walker.testTargetInstanceOfResidue(targetClass));
    }
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    return matches(method, targetClass, false);
  }

  @Override
  public boolean isRuntime() {
    return obtainPointcutExpression().mayNeedDynamicTest();
  }

  @Override
  public boolean matches(MethodInvocation invocation) {
    Method method = invocation.getMethod();
    ShadowMatch shadowMatch = getTargetShadowMatch(method, invocation.getThis().getClass());

    // Bind Infra AOP proxy to AspectJ "this" and Infra AOP target to AspectJ target,
    // consistent with return of MethodInvocationProceedingJoinPoint
    ProxyMethodInvocation pmi = null;
    Object targetObject = null;
    Object thisObject = null;
    try {
      MethodInvocation curr = ExposeInvocationInterceptor.currentInvocation();
      if (curr.getMethod() == method) {
        targetObject = curr.getThis();
        if (!(curr instanceof ProxyMethodInvocation currPmi)) {
          throw new IllegalStateException("MethodInvocation is not a Infra ProxyMethodInvocation: " + curr);
        }
        pmi = currPmi;
        thisObject = pmi.getProxy();
      }
    }
    catch (IllegalStateException ex) {
      // No current invocation...
      if (log.isDebugEnabled()) {
        log.debug("Could not access current invocation - matching with limited context: " + ex);
      }
    }

    try {
      JoinPointMatch joinPointMatch = shadowMatch.matchesJoinPoint(thisObject, targetObject, invocation.getArguments());

      /*
       * Do a final check to see if any this(TYPE) kind of residue match. For
       * this purpose, we use the original method's (proxy method's) shadow to
       * ensure that 'this' is correctly checked against. Without this check,
       * we get incorrect match on this(TYPE) where TYPE matches the target
       * type but not 'this' (as would be the case of JDK dynamic proxies).
       */
      if (pmi != null && thisObject != null) {  // there is a current invocation
        RuntimeTestWalker originalMethodResidueTest = getRuntimeTestWalker(getShadowMatch(method, method));
        if (!originalMethodResidueTest.testThisInstanceOfResidue(thisObject.getClass())) {
          return false;
        }
        if (joinPointMatch.matches()) {
          bindParameters(pmi, joinPointMatch);
        }
      }

      return joinPointMatch.matches();
    }
    catch (Throwable ex) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to evaluate join point for arguments {} - falling back to non-match",
                Arrays.asList(invocation.getArguments()), ex);
      }
      return false;
    }
  }

  @Nullable
  protected String getCurrentProxiedBeanName() {
    return ProxyCreationContext.getCurrentProxiedBeanName();
  }

  /**
   * Get a new pointcut expression based on a target class's loader rather than the default.
   */
  @Nullable
  private PointcutExpression getFallbackPointcutExpression(Class<?> targetClass) {
    try {
      ClassLoader classLoader = targetClass.getClassLoader();
      if (classLoader != null && classLoader != this.pointcutClassLoader) {
        return buildPointcutExpression(classLoader);
      }
    }
    catch (Throwable ex) {
      log.debug("Failed to create fallback PointcutExpression", ex);
    }
    return null;
  }

  private RuntimeTestWalker getRuntimeTestWalker(ShadowMatch shadowMatch) {
    if (shadowMatch instanceof DefensiveShadowMatch defensiveShadowMatch) {
      return new RuntimeTestWalker(defensiveShadowMatch.primary);
    }
    return new RuntimeTestWalker(shadowMatch);
  }

  private void bindParameters(ProxyMethodInvocation invocation, JoinPointMatch jpm) {
    // Note: Can't use JoinPointMatch.getClass().getName() as the key, since
    // Infra AOP does all the matching at a join point, and then all the invocations
    // under this scenario, if we just use JoinPointMatch as the key, then
    // 'last man wins' which is not what we want at all.
    // Using the expression is guaranteed to be safe, since 2 identical expressions
    // are guaranteed to bind in exactly the same way.
    invocation.setAttribute(resolveExpression(), jpm);
  }

  private ShadowMatch getTargetShadowMatch(Method method, Class<?> targetClass) {
    Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
    if (targetMethod.getDeclaringClass().isInterface()
            && targetMethod.getDeclaringClass() != targetClass
            && obtainPointcutExpression().getPointcutExpression().contains("." + targetMethod.getName() + "(")) {
      // Try to build the most specific interface possible for inherited methods to be
      // considered for sub-interface matches as well, in particular for proxy classes.
      // Note: AspectJ is only going to take Method.getDeclaringClass() into account.
      Set<Class<?>> ifcs = ClassUtils.getAllInterfacesForClassAsSet(targetClass);
      if (ifcs.size() > 1) {
        try {
          Class<?> compositeInterface = ClassUtils.createCompositeInterface(
                  ClassUtils.toClassArray(ifcs), targetClass.getClassLoader());
          targetMethod = ReflectionUtils.getMostSpecificMethod(targetMethod, compositeInterface);
        }
        catch (IllegalArgumentException ex) {
          // Implemented interfaces probably expose conflicting method signatures...
          // Proceed with original target method.
        }
      }
    }
    return getShadowMatch(targetMethod, method);
  }

  private ShadowMatch getShadowMatch(Method targetMethod, Method originalMethod) {
    ShadowMatchKey key = new ShadowMatchKey(this, targetMethod);
    ShadowMatch shadowMatch = ShadowMatchUtils.getShadowMatch(key);
    if (shadowMatch == null) {
      PointcutExpression pointcutExpression = obtainPointcutExpression();
      synchronized(pointcutExpression) {
        shadowMatch = ShadowMatchUtils.getShadowMatch(key);
        if (shadowMatch != null) {
          return shadowMatch;
        }
        PointcutExpression fallbackExpression = null;
        Method methodToMatch = targetMethod;
        try {
          try {
            shadowMatch = pointcutExpression.matchesMethodExecution(methodToMatch);
          }
          catch (ReflectionWorldException ex) {
            // Failed to introspect target method, probably because it has been loaded
            // in a special ClassLoader. Let's try the declaring ClassLoader instead...
            try {
              fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
              if (fallbackExpression != null) {
                shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
              }
            }
            catch (ReflectionWorldException ex2) {
              fallbackExpression = null;
            }
          }
          if (targetMethod != originalMethod && (shadowMatch == null ||
                  (Proxy.isProxyClass(targetMethod.getDeclaringClass()) &&
                          (shadowMatch.neverMatches() || containsAnnotationPointcut())))) {
            // Fall back to the plain original method in case of no resolvable match or a
            // negative match on a proxy class (which doesn't carry any annotations on its
            // redeclared methods), as well as for annotation pointcuts.
            methodToMatch = originalMethod;
            try {
              shadowMatch = pointcutExpression.matchesMethodExecution(methodToMatch);
            }
            catch (ReflectionWorldException ex) {
              // Could neither introspect the target class nor the proxy class ->
              // let's try the original method's declaring class before we give up...
              try {
                fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
                if (fallbackExpression != null) {
                  shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
                }
              }
              catch (ReflectionWorldException ex2) {
                fallbackExpression = null;
              }
            }
          }
        }
        catch (Throwable ex) {
          // Possibly AspectJ 1.8.10 encountering an invalid signature
          log.debug("PointcutExpression matching rejected target method", ex);
          fallbackExpression = null;
        }
        if (shadowMatch == null) {
          shadowMatch = new ShadowMatchImpl(org.aspectj.util.FuzzyBoolean.NO, null, null, null);
        }
        else if (shadowMatch.maybeMatches() && fallbackExpression != null) {
          shadowMatch = new DefensiveShadowMatch(shadowMatch,
                  fallbackExpression.matchesMethodExecution(methodToMatch));
        }
        shadowMatch = ShadowMatchUtils.setShadowMatch(key, shadowMatch);
      }
    }
    return shadowMatch;
  }

  private boolean containsAnnotationPointcut() {
    return resolveExpression().contains("@annotation");
  }

  private static boolean compiledByAjc(Class<?> clazz) {
    for (Field field : clazz.getDeclaredFields()) {
      if (field.getName().startsWith(AJC_MAGIC)) {
        return true;
      }
    }
    Class<?> superclass = clazz.getSuperclass();
    return (superclass != null && compiledByAjc(superclass));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof AspectJExpressionPointcut that &&
            ObjectUtils.nullSafeEquals(getExpression(), that.getExpression()) &&
            ObjectUtils.nullSafeEquals(this.pointcutDeclarationScope, that.pointcutDeclarationScope) &&
            ObjectUtils.nullSafeEquals(this.pointcutParameterNames, that.pointcutParameterNames) &&
            ObjectUtils.nullSafeEquals(this.pointcutParameterTypes, that.pointcutParameterTypes)));
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHash(getExpression(), this.pointcutDeclarationScope,
            this.pointcutParameterNames, this.pointcutParameterTypes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("AspectJExpressionPointcut: (");
    for (int i = 0; i < this.pointcutParameterTypes.length; i++) {
      sb.append(this.pointcutParameterTypes[i].getName());
      sb.append(' ');
      sb.append(this.pointcutParameterNames[i]);
      if ((i + 1) < this.pointcutParameterTypes.length) {
        sb.append(", ");
      }
    }
    sb.append(") ");
    if (getExpression() != null) {
      sb.append(getExpression());
    }
    else {
      sb.append("<pointcut expression not set>");
    }
    return sb.toString();
  }

  /**
   * Handler for the Infra-specific {@code bean()} pointcut designator
   * extension to AspectJ.
   * <p>This handler must be added to each pointcut object that needs to
   * handle the {@code bean()} PCD. Matching context is obtained
   * automatically by examining a thread local variable and therefore a matching
   * context need not be set on the pointcut.
   */
  private final class BeanPointcutDesignatorHandler implements PointcutDesignatorHandler {

    private static final String BEAN_DESIGNATOR_NAME = "bean";

    @Override
    public String getDesignatorName() {
      return BEAN_DESIGNATOR_NAME;
    }

    @Override
    public ContextBasedMatcher parse(String expression) {
      return new BeanContextMatcher(expression);
    }
  }

  /**
   * Matcher class for the BeanNamePointcutDesignatorHandler.
   * <p>Dynamic match tests for this matcher always return true,
   * since the matching decision is made at the proxy creation time.
   * For static match tests, this matcher abstains to allow the overall
   * pointcut to match even when negation is used with the bean() pointcut.
   */
  private final class BeanContextMatcher implements ContextBasedMatcher {

    private final NamePattern expressionPattern;

    private BeanContextMatcher(String expression) {
      this.expressionPattern = new NamePattern(expression);
    }

    @Override
    public boolean couldMatchJoinPointsInType(Class someClass) {
      return (contextMatch(someClass) == FuzzyBoolean.YES);
    }

    @Override
    public boolean couldMatchJoinPointsInType(Class someClass, MatchingContext context) {
      return (contextMatch(someClass) == FuzzyBoolean.YES);
    }

    @Override
    public boolean matchesDynamically(MatchingContext context) {
      return true;
    }

    @Override
    public FuzzyBoolean matchesStatically(MatchingContext context) {
      return contextMatch(null);
    }

    @Override
    public boolean mayNeedDynamicTest() {
      return false;
    }

    private FuzzyBoolean contextMatch(@Nullable Class<?> targetType) {
      String advisedBeanName = getCurrentProxiedBeanName();
      if (advisedBeanName == null) {  // no proxy creation in progress
        // abstain; can't return YES, since that will make pointcut with negation fail
        return FuzzyBoolean.MAYBE;
      }
      if (BeanFactoryUtils.isGeneratedBeanName(advisedBeanName)) {
        return FuzzyBoolean.NO;
      }
      if (targetType != null) {
        boolean isFactory = FactoryBean.class.isAssignableFrom(targetType);
        return FuzzyBoolean.fromBoolean(
                matchesBean(isFactory ? BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName : advisedBeanName));
      }
      else {
        return FuzzyBoolean.fromBoolean(matchesBean(advisedBeanName) ||
                matchesBean(BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName));
      }
    }

    private boolean matchesBean(String advisedBeanName) {
      return BeanFactoryAnnotationUtils.isQualifierMatch(
              this.expressionPattern::matches, advisedBeanName, beanFactory);
    }
  }

  private static final class DefensiveShadowMatch implements ShadowMatch {

    private final ShadowMatch primary;

    private final ShadowMatch other;

    public DefensiveShadowMatch(ShadowMatch primary, ShadowMatch other) {
      this.primary = primary;
      this.other = other;
    }

    @Override
    public boolean alwaysMatches() {
      return this.primary.alwaysMatches();
    }

    @Override
    public boolean maybeMatches() {
      return this.primary.maybeMatches();
    }

    @Override
    public boolean neverMatches() {
      return this.primary.neverMatches();
    }

    @Override
    public JoinPointMatch matchesJoinPoint(Object thisObject, Object targetObject, Object[] args) {
      try {
        return this.primary.matchesJoinPoint(thisObject, targetObject, args);
      }
      catch (ReflectionWorldException ex) {
        return this.other.matchesJoinPoint(thisObject, targetObject, args);
      }
    }

    @Override
    public void setMatchingContext(MatchingContext aMatchContext) {
      this.primary.setMatchingContext(aMatchContext);
      this.other.setMatchingContext(aMatchContext);
    }
  }

  private record ShadowMatchKey(AspectJExpressionPointcut expression, Method method) {
  }

}
