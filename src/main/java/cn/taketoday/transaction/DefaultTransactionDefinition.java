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
package cn.taketoday.transaction;

import java.io.Serializable;

import cn.taketoday.context.AnnotationAttributes;

/**
 * @author TODAY <br>
 *         2018-10-09 11:57
 */
public class DefaultTransactionDefinition implements TransactionDefinition, Serializable {

    private static final long serialVersionUID = 1L;

    /** Prefix for the propagation constants defined in TransactionDefinition */
    public static final String PREFIX_PROPAGATION = "PROPAGATION_";

    /** Prefix for the isolation constants defined in TransactionDefinition */
    public static final String PREFIX_ISOLATION = "ISOLATION_";

    /** Prefix for transaction timeout values in description strings */
    public static final String PREFIX_TIMEOUT = "timeout_";

    /** Marker for read-only transactions in description strings */
    public static final String READ_ONLY_MARKER = "readOnly";

    /** Constants instance for TransactionDefinition */
    public static final Constants constants = new Constants(TransactionDefinition.class);

    private int propagationBehavior = PROPAGATION_REQUIRED;

    private int isolationLevel = ISOLATION_DEFAULT;

    private int timeout = TIMEOUT_DEFAULT;

    private boolean readOnly = false;

    private String name;

    private String qualifier;
    private Class<?>[] rollbackOn;

    public DefaultTransactionDefinition() {

    }

    public DefaultTransactionDefinition(AnnotationAttributes attributes) {
        setTimeout(attributes.getNumber("timeout").intValue());

        setReadOnly(attributes.getBoolean(READ_ONLY_MARKER));
        setQualifier(attributes.getString("txManager"));

        final Class<?>[] rollbackOn = attributes.getAttribute("rollbackOn", Class[].class);
        setRollbackOn(rollbackOn);

        final Isolation isolation = attributes.getEnum("isolation");
        setIsolationLevel(isolation.value());

        final Propagation propagation = attributes.getEnum("propagation");
        setPropagationBehavior(propagation.value());
    }

    public DefaultTransactionDefinition(DefaultTransactionDefinition other) {
        this.setName(other.getName());
        this.setTimeout(other.getTimeout());
        this.setReadOnly(other.isReadOnly());
        this.setQualifier(other.getQualifier());
        this.setRollbackOn(other.getRollbackOn());
        this.setIsolationLevel(other.getIsolationLevel());
        this.setPropagationBehavior(other.getPropagationBehavior());
    }

    public DefaultTransactionDefinition(int propagationBehavior) {
        this.propagationBehavior = propagationBehavior;
    }

    /**
     * Set the propagation behavior by the name of the corresponding constant in
     * TransactionDefinition, e.g. "PROPAGATION_REQUIRED".
     * 
     * @param constantName
     *            name of the constant
     * @throws IllegalArgumentException
     *             if the supplied value is not resolvable to one of the
     *             {@code PROPAGATION_} constants or is {@code null}
     * @see #setPropagationBehavior
     * @see #PROPAGATION_REQUIRED
     */
    public final DefaultTransactionDefinition setPropagationBehaviorName(String constantName) {
        if (!constantName.startsWith(PREFIX_PROPAGATION)) {
            throw new IllegalArgumentException("Only propagation constants allowed");
        }
        setPropagationBehavior(constants.asNumber(constantName).intValue());
        return this;
    }

    /**
     * Set the propagation behavior. Must be one of the propagation constants in the
     * TransactionDefinition interface. Default is PROPAGATION_REQUIRED.
     * <p>
     * Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
     * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
     * transactions. Consider switching the "validateExistingTransactions" flag to
     * "true" on your transaction manager if you'd like isolation level declarations
     * to get rejected when participating in an existing transaction with a
     * different isolation level.
     * <p>
     * Note that a transaction manager that does not support custom isolation levels
     * will throw an exception when given any other level than
     * {@link #ISOLATION_DEFAULT}.
     * 
     * @throws IllegalArgumentException
     *             if the supplied value is not one of the {@code PROPAGATION_}
     *             constants
     * @see #PROPAGATION_REQUIRED
     */
    public final DefaultTransactionDefinition setPropagationBehavior(int propagationBehavior) {
        if (!constants.getValues(PREFIX_PROPAGATION).contains(propagationBehavior)) {
            throw new IllegalArgumentException("Only values of propagation constants allowed");
        }
        this.propagationBehavior = propagationBehavior;
        return this;
    }

    @Override
    public final int getPropagationBehavior() {
        return this.propagationBehavior;
    }

    /**
     * Set the isolation level by the name of the corresponding constant in
     * TransactionDefinition, e.g. "ISOLATION_DEFAULT".
     * 
     * @param constantName
     *            name of the constant
     * @throws IllegalArgumentException
     *             if the supplied value is not resolvable to one of the
     *             {@code ISOLATION_} constants or is {@code null}
     * @see #setIsolationLevel
     * @see #ISOLATION_DEFAULT
     */
    public final DefaultTransactionDefinition setIsolationLevelName(String constantName) throws IllegalArgumentException {
        if (!constantName.startsWith(PREFIX_ISOLATION)) {
            throw new IllegalArgumentException("Only isolation constants allowed");
        }
        setIsolationLevel(constants.asNumber(constantName).intValue());
        return this;
    }

    /**
     * Set the isolation level. Must be one of the isolation constants in the
     * TransactionDefinition interface. Default is ISOLATION_DEFAULT.
     * <p>
     * Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
     * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
     * transactions. Consider switching the "validateExistingTransactions" flag to
     * "true" on your transaction manager if you'd like isolation level declarations
     * to get rejected when participating in an existing transaction with a
     * different isolation level.
     * <p>
     * Note that a transaction manager that does not support custom isolation levels
     * will throw an exception when given any other level than
     * {@link #ISOLATION_DEFAULT}.
     * 
     * @throws IllegalArgumentException
     *             if the supplied value is not one of the {@code ISOLATION_}
     *             constants
     * @see #ISOLATION_DEFAULT
     */
    public final DefaultTransactionDefinition setIsolationLevel(int isolationLevel) {
        if (!constants.getValues(PREFIX_ISOLATION).contains(isolationLevel)) {
            throw new IllegalArgumentException("Only values of isolation constants allowed");
        }
        this.isolationLevel = isolationLevel;
        return this;
    }

    @Override
    public final int getIsolationLevel() {
        return this.isolationLevel;
    }

    /**
     * Set the timeout to apply, as number of seconds. Default is TIMEOUT_DEFAULT
     * (-1).
     * <p>
     * Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
     * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
     * transactions.
     * <p>
     * Note that a transaction manager that does not support timeouts will throw an
     * exception when given any other timeout than {@link #TIMEOUT_DEFAULT}.
     * 
     * @see #TIMEOUT_DEFAULT
     */
    public final DefaultTransactionDefinition setTimeout(int timeout) {
        if (timeout < TIMEOUT_DEFAULT) {
            throw new IllegalArgumentException("Timeout must be a positive integer or TIMEOUT_DEFAULT");
        }
        this.timeout = timeout;
        return this;
    }

    @Override
    public final int getTimeout() {
        return this.timeout;
    }

    /**
     * Set whether to optimize as read-only transaction. Default is "false".
     * <p>
     * The read-only flag applies to any transaction context, whether backed by an
     * actual resource transaction ({@link #PROPAGATION_REQUIRED}/
     * {@link #PROPAGATION_REQUIRES_NEW}) or operating non-transactionally at the
     * resource level ({@link #PROPAGATION_SUPPORTS}). In the latter case, the flag
     * will only apply to managed resources within the application, such as a
     * Hibernate {@code Session}.
     * <p>
     * This just serves as a hint for the actual transaction subsystem; it will
     * <i>not necessarily</i> cause failure of write access attempts. A transaction
     * manager which cannot interpret the read-only hint will <i>not</i> throw an
     * exception when asked for a read-only transaction.
     */
    public final DefaultTransactionDefinition setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    @Override
    public final boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * Set the name of this transaction. Default is none.
     * <p>
     * This will be used as transaction name to be shown in a transaction monitor,
     * if applicable (for example, WebLogic's).
     */
    public final DefaultTransactionDefinition setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    /**
     * This implementation compares the {@code toString()} results.
     * 
     * @see #toString()
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof TransactionDefinition && toString().equals(other.toString())));
    }

    /**
     * This implementation returns {@code toString()}'s hash code.
     * 
     * @see #toString()
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Return an identifying description for this transaction definition.
     * <p>
     * The format matches the one used by
     * {@link org.springframework.transaction.interceptor.TransactionAttributeEditor},
     * to be able to feed {@code toString} results into bean properties of type
     * {@link org.springframework.transaction.interceptor.TransactionAttribute}.
     * <p>
     * Has to be overridden in subclasses for correct {@code equals} and
     * {@code hashCode} behavior. Alternatively, {@link #equals} and
     * {@link #hashCode} can be overridden themselves.
     */
    @Override
    public String toString() {
        return getDefinitionDescription().toString();
    }

    /**
     * Return an identifying description for this transaction definition.
     * <p>
     * Available to subclasses, for inclusion in their {@code toString()} result.
     */
    protected final StringBuilder getDefinitionDescription() {
        StringBuilder result = new StringBuilder();
        result.append(constants.toCode(this.propagationBehavior, PREFIX_PROPAGATION));
        result.append(',');
        result.append(constants.toCode(this.isolationLevel, PREFIX_ISOLATION));
        if (this.timeout != TIMEOUT_DEFAULT) {
            result.append(',');
            result.append(PREFIX_TIMEOUT).append(this.timeout);
        }
        if (this.readOnly) {
            result.append(',');
            result.append(READ_ONLY_MARKER);
        }
        return result;
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    @Override
    public boolean rollbackOn(Throwable ex) {
        return ex instanceof RuntimeException || ex instanceof Error;
    }

    public DefaultTransactionDefinition setQualifier(String qualifier) {
        this.qualifier = qualifier;
        return this;
    }

    public Class<?>[] getRollbackOn() {
        return rollbackOn;
    }

    public void setRollbackOn(Class<?>... rollbackOn) {
        this.rollbackOn = rollbackOn;
    }

}
