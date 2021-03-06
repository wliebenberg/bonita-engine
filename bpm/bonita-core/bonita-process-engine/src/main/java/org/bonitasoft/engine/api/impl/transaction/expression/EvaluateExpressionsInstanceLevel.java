/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.api.impl.transaction.expression;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.core.expression.control.api.ExpressionResolverService;
import org.bonitasoft.engine.core.expression.control.model.SExpressionContext;
import org.bonitasoft.engine.core.process.definition.model.builder.ServerModelConvertor;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.model.SExpression;

/**
 * @author Zhao Na
 */
public class EvaluateExpressionsInstanceLevel extends AbstractEvaluateExpressionsInstance implements TransactionContentWithResult<Map<String, Serializable>> {

    private final Map<Expression, Map<String, Serializable>> expressions;

    private final long containerId;

    private final long processDefinitionId;

    private final String containerType;

    private final ExpressionResolverService expressionResolver;

    private final Map<String, Serializable> results = new HashMap<String, Serializable>(0);

    public EvaluateExpressionsInstanceLevel(final Map<Expression, Map<String, Serializable>> expressions, final long containerId, final String containerType,
            final long processDefinitionId, final ExpressionResolverService expressionService) {
        this.expressions = expressions;
        this.containerId = containerId;
        expressionResolver = expressionService;
        this.processDefinitionId = processDefinitionId;
        this.containerType = containerType;
    }

    @Override
    public void execute() throws SBonitaException {
        // FIXME: call the appropriate method(s) from the right service(s):
        if (expressions != null && !expressions.isEmpty()) {
            final SExpressionContext context = new SExpressionContext();
            context.setContainerId(containerId);
            context.setContainerType(containerType);
            context.setProcessDefinitionId(processDefinitionId);

            final Set<Expression> exps = expressions.keySet();
            for (Expression exp : exps) {
                final Map<String, Serializable> partialContext = expressions.get(exp);
                context.setSerializableInputValues(partialContext);
                final SExpression sexp = ServerModelConvertor.convertExpression(exp);
                final Serializable res = (Serializable) expressionResolver.evaluate(sexp, context);
                results.put(buildName(exp), res);// MAYBE instead of exp.getNAME
            }
        }

    }

    @Override
    public Map<String, Serializable> getResult() {
        return results;
    }
}
