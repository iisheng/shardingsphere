/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.binder.segment.select.projection.engine;

import org.apache.shardingsphere.infra.binder.segment.select.projection.Projection;
import org.apache.shardingsphere.infra.binder.segment.select.projection.impl.AggregationDistinctProjection;
import org.apache.shardingsphere.infra.binder.segment.select.projection.impl.AggregationProjection;
import org.apache.shardingsphere.infra.binder.segment.select.projection.impl.ColumnProjection;
import org.apache.shardingsphere.infra.binder.segment.select.projection.impl.ExpressionProjection;
import org.apache.shardingsphere.infra.binder.segment.select.projection.impl.ParameterMarkerProjection;
import org.apache.shardingsphere.infra.binder.segment.select.projection.impl.ShorthandProjection;
import org.apache.shardingsphere.infra.database.DefaultDatabase;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.metadata.database.schema.decorator.model.ShardingSphereSchema;
import org.apache.shardingsphere.sql.parser.sql.common.constant.AggregationType;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.BinaryOperationExpression;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.FunctionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.complex.CommonExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.item.AggregationDistinctProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.item.AggregationProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.item.ColumnProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.item.ExpressionProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.item.ShorthandProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.AliasSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.OwnerSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.JoinTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.TableNameSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.value.identifier.IdentifierValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ProjectionEngineTest {
    
    @Mock
    private ShardingSphereSchema schema;
    
    @Mock
    private DatabaseType databaseType;
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentNotMatched() {
        assertFalse(new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), null).isPresent());
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfShorthandProjectionSegment() {
        ShorthandProjectionSegment shorthandProjectionSegment = new ShorthandProjectionSegment(0, 0);
        shorthandProjectionSegment.setOwner(new OwnerSegment(0, 0, new IdentifierValue("tbl")));
        Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), shorthandProjectionSegment);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(ShorthandProjection.class));
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfShorthandProjectionSegmentAndDuplicateTableSegment() {
        SimpleTableSegment table = new SimpleTableSegment(new TableNameSegment(0, 0, new IdentifierValue("t_order")));
        when(schema.getAllColumnNames("t_order")).thenReturn(Arrays.asList("order_id", "content"));
        Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(table, new ShorthandProjectionSegment(0, 0));
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(ShorthandProjection.class));
        assertThat(((ShorthandProjection) actual.get()).getActualColumns().size(), is(2));
        Map<String, ColumnProjection> actualColumns = new LinkedHashMap<>();
        actualColumns.put("t_order.order_id", new ColumnProjection("t_order", "order_id", null));
        actualColumns.put("t_order.content", new ColumnProjection("t_order", "content", null));
        assertThat(((ShorthandProjection) actual.get()).getActualColumns(), is(actualColumns));
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfColumnProjectionSegment() {
        ColumnProjectionSegment columnProjectionSegment = new ColumnProjectionSegment(new ColumnSegment(0, 10, new IdentifierValue("name")));
        columnProjectionSegment.setAlias(new AliasSegment(0, 0, new IdentifierValue("alias")));
        Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), columnProjectionSegment);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(ColumnProjection.class));
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfExpressionProjectionSegment() {
        List<ExpressionSegment> list = getExpressionSegments();
        list.forEach(expressionSegment -> {
            ExpressionProjectionSegment expressionProjectionSegment = new ExpressionProjectionSegment(0, 10, "text", expressionSegment);
            Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                    Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), expressionProjectionSegment);
            assertTrue(actual.isPresent());
            assertThat(actual.get(), instanceOf(ExpressionProjection.class));
            if (null != expressionSegment) {
                ExpressionProjection actualExpressionProjection = (ExpressionProjection) actual.get();
                assertTrue(actualExpressionProjection.getParameters().size() > 0);
            }
        });
    }
    
    private List<ExpressionSegment> getExpressionSegments() {
        List<ExpressionSegment> result = new ArrayList<>();
        FunctionSegment functionSegment = new FunctionSegment(7, 28, "IFNULL", "IFNULL(MAX(status), 0)");
        AggregationProjectionSegment parameter = new AggregationProjectionSegment(14, 24, AggregationType.MAX, "(status)");
        functionSegment.getParameters().add(parameter);
        result.add(functionSegment);
        ExpressionSegment right = new LiteralExpressionSegment(25, 25, 1);
        ExpressionSegment binaryOperationExpression = new BinaryOperationExpression(7, 25, parameter, right, "+", "IFNULL(status, 0)+1");
        result.add(binaryOperationExpression);
        result.add(null);
        return result;
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfAggregationDistinctProjectionSegment() {
        AggregationDistinctProjectionSegment aggregationDistinctProjectionSegment = new AggregationDistinctProjectionSegment(0, 10, AggregationType.COUNT, "(1)", "distinctExpression");
        Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), aggregationDistinctProjectionSegment);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(AggregationDistinctProjection.class));
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfAggregationProjectionSegment() {
        AggregationProjectionSegment aggregationProjectionSegment = new AggregationProjectionSegment(0, 10, AggregationType.COUNT, "(1)");
        Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), aggregationProjectionSegment);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(AggregationProjection.class));
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfAggregationDistinctProjectionSegmentAndAggregationTypeIsAvg() {
        AggregationDistinctProjectionSegment aggregationDistinctProjectionSegment = new AggregationDistinctProjectionSegment(0, 10, AggregationType.AVG, "(1)", "distinctExpression");
        Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), aggregationDistinctProjectionSegment);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(AggregationDistinctProjection.class));
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfAggregationProjectionSegmentAndAggregationTypeIsAvg() {
        AggregationProjectionSegment aggregationProjectionSegment = new AggregationProjectionSegment(0, 10, AggregationType.AVG, "(1)");
        Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), aggregationProjectionSegment);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(AggregationProjection.class));
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfParameterMarkerExpressionSegment() {
        ParameterMarkerExpressionSegment parameterMarkerExpressionSegment = new ParameterMarkerExpressionSegment(7, 7, 0);
        parameterMarkerExpressionSegment.setAlias(new AliasSegment(0, 0, new IdentifierValue("alias")));
        Optional<Projection> actual = new ProjectionEngine(DefaultDatabase.LOGIC_NAME,
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(mock(TableSegment.class), parameterMarkerExpressionSegment);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(ParameterMarkerProjection.class));
        assertThat(actual.get().getAlias().orElse(null), is("alias"));
    }
    
    @Test
    public void assertCreateProjectionWhenProjectionSegmentInstanceOfShorthandProjectionSegmentAndJoinTableSegment() {
        SimpleTableSegment ordersTableSegment = new SimpleTableSegment(new TableNameSegment(0, 0, new IdentifierValue("t_order")));
        when(schema.getAllColumnNames("t_order")).thenReturn(Arrays.asList("order_id", "customer_id"));
        SimpleTableSegment customersTableSegment = new SimpleTableSegment(new TableNameSegment(0, 0, new IdentifierValue("t_customer")));
        when(schema.getAllColumnNames("t_customer")).thenReturn(Collections.singletonList("customer_id"));
        JoinTableSegment table = new JoinTableSegment();
        table.setLeft(ordersTableSegment);
        table.setRight(customersTableSegment);
        table.setCondition(new CommonExpressionSegment(0, 0, "t_order.customer_id=t_customer.customer_id"));
        ShorthandProjectionSegment shorthandProjectionSegment = new ShorthandProjectionSegment(0, 10);
        Optional<Projection> actual = new ProjectionEngine(
                DefaultDatabase.LOGIC_NAME, Collections.singletonMap(DefaultDatabase.LOGIC_NAME, schema), databaseType).createProjection(table, shorthandProjectionSegment);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), instanceOf(ShorthandProjection.class));
        Map<String, ColumnProjection> actualColumns = ((ShorthandProjection) actual.get()).getActualColumns();
        assertThat(actualColumns.size(), is(3));
        assertThat(actualColumns.get("t_order.order_id"), is(new ColumnProjection("t_order", "order_id", null)));
        assertThat(actualColumns.get("t_order.customer_id"), is(new ColumnProjection("t_order", "customer_id", null)));
        assertThat(actualColumns.get("t_customer.customer_id"), is(new ColumnProjection("t_customer", "customer_id", null)));
    }
}
