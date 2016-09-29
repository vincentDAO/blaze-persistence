package com.blazebit.persistence.impl.query;

import com.blazebit.persistence.impl.AbstractCommonQueryBuilder;
import com.blazebit.persistence.impl.CustomSQLQuery;
import com.blazebit.persistence.impl.CustomSQLTypedQuery;
import com.blazebit.persistence.impl.plan.CustomSelectQueryPlan;
import com.blazebit.persistence.impl.plan.SelectQueryPlan;
import com.blazebit.persistence.spi.OrderByElement;
import com.blazebit.persistence.spi.SetOperationType;

import javax.persistence.Query;
import java.util.*;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class SetOperationQuerySpecification extends CustomQuerySpecification {

    private final Query leftMostQuery;
    private final List<Query> setOperands;
    private final SetOperationType operator;
    private final List<? extends OrderByElement> orderByElements;
    private final boolean nested;

    public SetOperationQuerySpecification(AbstractCommonQueryBuilder<?, ?, ?, ?, ?> commonQueryBuilder, Query leftMostQuery, Query baseQuery, List<Query> setOperands, SetOperationType operator,
                                          List<? extends OrderByElement> orderByElements, boolean nested, Set<String> parameterListNames, String limit, String offset,
                                          List<String> keyRestrictedLeftJoinAliases, List<EntityFunctionNode> entityFunctionNodes, boolean recursive, List<CTENode> ctes, boolean shouldRenderCteNodes) {
        super(commonQueryBuilder, baseQuery, parameterListNames, limit, offset, keyRestrictedLeftJoinAliases, entityFunctionNodes, recursive, ctes, shouldRenderCteNodes);
        this.leftMostQuery = leftMostQuery;
        this.setOperands = setOperands;
        this.operator = operator;
        this.orderByElements = orderByElements;
        this.nested = nested;
    }

    @Override
    public Query getBaseQuery() {
        return baseQuery;
    }

    @Override
    protected void initialize() {
        String sqlQuery;
        List<Query> participatingQueries = new ArrayList<Query>();

        if (leftMostQuery instanceof CustomSQLQuery) {
            CustomSQLQuery customQuery = (CustomSQLQuery) leftMostQuery;
            List<Query> customQueryParticipants = customQuery.getParticipatingQueries();
            participatingQueries.addAll(customQueryParticipants);
            sqlQuery = customQuery.getSql();
        } else if (leftMostQuery instanceof CustomSQLTypedQuery<?>) {
            CustomSQLTypedQuery<?> customQuery = (CustomSQLTypedQuery<?>) leftMostQuery;
            List<Query> customQueryParticipants = customQuery.getParticipatingQueries();
            participatingQueries.addAll(customQueryParticipants);
            sqlQuery = customQuery.getSql();
        } else {
            participatingQueries.add(baseQuery);
            sqlQuery = extendedQuerySupport.getSql(em, baseQuery);
        }

        int size = sqlQuery.length() + 10;
        List<String> setOperands = new ArrayList<String>();
        setOperands.add(sqlQuery);

        for (Query q : this.setOperands) {
            String setOperandSql;

            if (q instanceof CustomSQLQuery) {
                CustomSQLQuery customQuery = (CustomSQLQuery) q;
                List<Query> customQueryParticipants = customQuery.getParticipatingQueries();
                participatingQueries.addAll(customQueryParticipants);

                setOperandSql = customQuery.getSql();
            } else if (q instanceof CustomSQLTypedQuery<?>) {
                CustomSQLTypedQuery<?> customQuery = (CustomSQLTypedQuery<?>) q;
                List<Query> customQueryParticipants = customQuery.getParticipatingQueries();
                participatingQueries.addAll(customQueryParticipants);

                setOperandSql = customQuery.getSql();
            } else {
                setOperandSql = extendedQuerySupport.getSql(em, q);
                participatingQueries.add(q);
            }

            setOperands.add(setOperandSql);
            size += setOperandSql.length() + 30;
        }

        StringBuilder sqlSb = new StringBuilder(size);

        dbmsDialect.appendSet(sqlSb, operator, nested, setOperands, orderByElements, limit, offset);
        StringBuilder withClause = applyCtes(sqlSb, baseQuery, participatingQueries);
        Map<String, String> addedCtes = applyExtendedSql(sqlSb, false, false, withClause, null, null);

        this.sql = sqlSb.toString();
        this.participatingQueries = participatingQueries;
        this.addedCtes = addedCtes;
        this.dirty = false;
    }

    @Override
    public SelectQueryPlan createSelectPlan(int firstResult, int maxResults) {
        final String sql = getSql();
        return new CustomSelectQueryPlan(extendedQuerySupport, serviceProvider, baseQuery, participatingQueries, sql, firstResult, maxResults);
    }

}