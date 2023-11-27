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

package cn.taketoday.aop.aspectj;

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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.IntroductionAwareMethodMatcher;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.aop.framework.autoproxy.ProxyCreationContext;
import cn.taketoday.aop.interceptor.ExposeInvocationInterceptor;
import cn.taketoday.aop.support.AbstractExpressionPointcut;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.BeanFactoryAnnotationUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Framework {@link cn.taketoday.aop.Pointcut} implementation
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

  private static final Logger log = LoggerFactory.getLogger(AspectJExpressionPointcut.class);

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

  @Nullable
  private Class<?> pointcutDeclarationScope;

  private String[] pointcutParameterNames = Constant.EMPTY_STRING_ARRAY;

  private Class<?>[] pointcutParameterTypes = Constant.EMPTY_CLASSES;

  @Nullable
  private BeanFactory beanFactory;

  @Nullable
  private transient ClassLoader pointcutClassLoader;

  @Nullable
  private transient PointcutExpression pointcutExpression;

  private transient ConcurrentHashMap<Method, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(32);

  /**
   * Create a new default AspectJExpressionPointcut.
   */
  public AspectJExpressionPointcut() { }

  /**
   * Create a new AspectJExpressionPointcut with the given settings.
   *
   * @param declarationScope the declaration scope for the pointcut
   * @param paramNames the parameter names for the pointcut
   * @param paramTypes the parameter types for the pointcut
   */
  public AspectJExpressionPointcut(@Nullable Class<?> declarationScope, String[] paramNames, Class<?>[] paramTypes) {
    this.pointcutDeclarationScope = declarationScope;
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
  public void setPointcutDeclarationScope(@Nullable Class<?> pointcutDeclarationScope) {
    this.pointcutDeclarationScope = pointcutDeclarationScope;
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
  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public ClassFilter getClassFilter() {
    obtainPointcutExpression();
    return this;
  }

  @Override
  public MethodMatcher getMethodMatcher() {
    obtainPointcutExpression();
    return this;
  }

  /**
   * Check whether this pointcut is ready to match,
   * lazily building the underlying AspectJ pointcut expression.
   */
  private PointcutExpression obtainPointcutExpression() {
    if (getExpression() == null) {
      throw new IllegalStateException("Must set property 'expression' before attempting to match");
    }
    if (this.pointcutExpression == null) {
      this.pointcutClassLoader = determinePointcutClassLoader();
      this.pointcutExpression = buildPointcutExpression(this.pointcutClassLoader);
    }
    return this.pointcutExpression;
  }

  /**
   * Determine the ClassLoader to use for pointcut evaluation.
   */
  @Nullable
  private ClassLoader determinePointcutClassLoader() {
    if (beanFactory instanceof ConfigurableBeanFactory cft) {
      return cft.getBeanClassLoader();
    }
    if (pointcutDeclarationScope != null) {
      return pointcutDeclarationScope.getClassLoader();
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
   * write {@code and} as "&amp;&amp;" (though &amp;&amp; will work).
   * We also allow {@code and} between two pointcut sub-expressions.
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
    PointcutExpression pointcutExpression = obtainPointcutExpression();
    try {
      try {
        return pointcutExpression.couldMatchJoinPointsInType(targetClass);
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
    catch (Throwable ex) {
      log.debug("PointcutExpression matching rejected target class", ex);
    }
    return false;
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
    obtainPointcutExpression();
    ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);

    // Special handling for this, target, @this, @target, @annotation
    // in Framework - we can optimize since we know we have exactly this class,
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
      // we say this is not a match as in Framework there will never be a different
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
    obtainPointcutExpression();
    // todo targetClass
    ShadowMatch shadowMatch = getTargetShadowMatch(method, invocation.getThis().getClass());

    // Bind Framework AOP proxy to AspectJ "this" and Framework AOP target to AspectJ target,
    // consistent with return of MethodInvocationProceedingJoinPoint
    ProxyMethodInvocation pmi = null;
    Object targetObject = null;
    Object thisObject = null;
    try {
      MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
      targetObject = mi.getThis();
      if (!(mi instanceof ProxyMethodInvocation)) {
        throw new IllegalStateException("MethodInvocation is not a Framework ProxyMethodInvocation: " + mi);
      }
      pmi = (ProxyMethodInvocation) mi;
      thisObject = pmi.getProxy();
    }
    catch (IllegalStateException ex) {
      // No current invocation...
      if (log.isDebugEnabled()) {
        log.debug("Could not access current invocation - matching with limited context: {}", ex.toString());
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
       * <p>See SPR-2979 for the original bug.
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
      log.debug("Failed to evaluate join point for arguments {} - falling back to non-match", Arrays.asList(invocation.getArguments()), ex);
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
    if (shadowMatch instanceof DefensiveShadowMatch) {
      return new RuntimeTestWalker(((DefensiveShadowMatch) shadowMatch).primary);
    }
    return new RuntimeTestWalker(shadowMatch);
  }

  private void bindParameters(ProxyMethodInvocation invocation, JoinPointMatch jpm) {
    // Note: Can't use JoinPointMatch.getClass().getName() as the key, since
    // Framework AOP does all the matching at a join point, and then all the invocations
    // under this scenario, if we just use JoinPointMatch as the key, then
    // 'last man wins' which is not what we want at all.
    // Using the expression is guaranteed to be safe, since 2 identical expressions
    // are guaranteed to bind in exactly the same way.
    invocation.setAttribute(resolveExpression(), jpm);
  }

  private ShadowMatch getTargetShadowMatch(Method method, Class<?> targetClass) {
    Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
    if (targetMethod.getDeclaringClass().isInterface()) {
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
    // Avoid lock contention for known Methods through concurrent access...
    ShadowMatch shadowMatch = this.shadowMatchCache.get(targetMethod);
    if (shadowMatch == null) {
      synchronized(this.shadowMatchCache) {
        // Not found - now check again with full lock...
        PointcutExpression fallbackExpression = null;
        shadowMatch = this.shadowMatchCache.get(targetMethod);
        if (shadowMatch == null) {
          Method methodToMatch = targetMethod;
          try {
            try {
              shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
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
            if (targetMethod != originalMethod && (shadowMatch == null || (Proxy.isProxyClass(targetMethod.getDeclaringClass())
                    && (shadowMatch.neverMatches() || containsAnnotationPointcut())))) {
              // Fall back to the plain original method in case of no resolvable match or a
              // negative match on a proxy class (which doesn't carry any annotations on its
              // redeclared methods), as well as for annotation pointcuts.
              methodToMatch = originalMethod;
              try {
                shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
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
          this.shadowMatchCache.put(targetMethod, shadowMatch);
        }
      }
    }
    return shadowMatch;
  }

  private boolean containsAnnotationPointcut() {
    return resolveExpression().contains("@annotation");
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

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization, just initialize state after deserialization.
    ois.defaultReadObject();

    // Initialize transient fields.
    // pointcutExpression will be initialized lazily by checkReadyToMatch()
    this.shadowMatchCache = new ConcurrentHashMap<>(32);
  }

  /**
   * Handler for the Framework-specific {@code bean()} pointcut designator
   * extension to AspectJ.
   * <p>This handler must be added to each pointcut object that needs to
   * handle the {@code bean()} PCD. Matching context is obtained
   * automatically by examining a thread local variable and therefore a matching
   * context need not be set on the pointcut.
   */
  private class BeanPointcutDesignatorHandler implements PointcutDesignatorHandler {

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
  @SuppressWarnings("rawtypes")
  private class BeanContextMatcher implements ContextBasedMatcher {

    private final NamePattern expressionPattern;

    public BeanContextMatcher(String expression) {
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

  private record DefensiveShadowMatch(ShadowMatch primary, ShadowMatch other) implements ShadowMatch {

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

}
