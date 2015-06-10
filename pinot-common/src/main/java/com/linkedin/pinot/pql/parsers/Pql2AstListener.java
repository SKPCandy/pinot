/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.pql.parsers;

import com.linkedin.pinot.pql.parsers.pql2.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import org.antlr.v4.runtime.misc.NotNull;


/**
 * TODO Document me!
 *
 * @author jfim
 */
public class Pql2AstListener extends PQL2BaseListener {
  Stack<AstNode> _nodeStack = new Stack<AstNode>();
  AstNode _rootNode = null;

  private void pushNode(AstNode node) {
    if (_rootNode == null) {
      _rootNode = node;
    }

    AstNode parentNode = null;

    if (!_nodeStack.isEmpty()) {
      parentNode = _nodeStack.peek();
    }

    if (parentNode != null) {
      parentNode.addChild(node);
    }

    node.setParent(parentNode);

    _nodeStack.push(node);
  }

  private void popNode() {
    AstNode topNode = _nodeStack.pop();

    // We clone the children list as it can be mutated by the doneProcessingSiblings call
    if (topNode.hasChildren()) {
      List<AstNode> originalChildrenList = topNode.getChildren();
      List<AstNode> children = new ArrayList<AstNode>(originalChildrenList);
      Collections.copy(children, originalChildrenList);

      for (AstNode child : children) {
        child.doneProcessingSiblings();
      }
    }

    topNode.doneProcessingChildren();
  }

  public AstNode getRootNode() {
    return _rootNode;
  }

  @Override
  public void enterSelect(@NotNull PQL2Parser.SelectContext ctx) {
    pushNode(new SelectAstNode());
  }

  @Override
  public void exitSelect(@NotNull PQL2Parser.SelectContext ctx) {
    popNode();
  }

  @Override
  public void enterTableName(@NotNull PQL2Parser.TableNameContext ctx) {
    pushNode(new TableNameAstNode(ctx.getText()));
  }

  @Override
  public void exitTableName(@NotNull PQL2Parser.TableNameContext ctx) {
    popNode();
  }

  @Override
  public void enterStarColumnList(@NotNull PQL2Parser.StarColumnListContext ctx) {
    pushNode(new StarColumnListAstNode());
  }

  @Override
  public void exitStarColumnList(@NotNull PQL2Parser.StarColumnListContext ctx) {
    popNode();
  }

  @Override
  public void enterOutputColumnList(@NotNull PQL2Parser.OutputColumnListContext ctx) {
    // TODO
    pushNode(new OutputColumnListAstNode());
  }

  @Override
  public void exitOutputColumnList(@NotNull PQL2Parser.OutputColumnListContext ctx) {
    popNode();
  }

  @Override
  public void enterIsPredicate(@NotNull PQL2Parser.IsPredicateContext ctx) {
    // TODO
    pushNode(new IsPredicateAstNode());
  }

  @Override
  public void exitIsPredicate(@NotNull PQL2Parser.IsPredicateContext ctx) {
    popNode();
  }

  @Override
  public void enterPredicateParenthesisGroup(@NotNull PQL2Parser.PredicateParenthesisGroupContext ctx) {
    // TODO
    pushNode(new PredicateParenthesisGroupAstNode());
  }

  @Override
  public void exitPredicateParenthesisGroup(@NotNull PQL2Parser.PredicateParenthesisGroupContext ctx) {
    popNode();
  }

  @Override
  public void enterComparisonPredicate(@NotNull PQL2Parser.ComparisonPredicateContext ctx) {
    pushNode(new ComparisonPredicateAstNode(ctx.getChild(0).getChild(1).getText()));
  }

  @Override
  public void exitComparisonPredicate(@NotNull PQL2Parser.ComparisonPredicateContext ctx) {
    popNode();
  }

  @Override
  public void enterExpressionParenthesisGroup(@NotNull PQL2Parser.ExpressionParenthesisGroupContext ctx) {
    // TODO
    pushNode(new ExpressionParenthesisGroupAstNode());
  }

  @Override
  public void exitExpressionParenthesisGroup(@NotNull PQL2Parser.ExpressionParenthesisGroupContext ctx) {
    popNode();
  }

  @Override
  public void enterOutputColumn(@NotNull PQL2Parser.OutputColumnContext ctx) {
    // TODO
    pushNode(new OutputColumnAstNode());
  }

  @Override
  public void exitOutputColumn(@NotNull PQL2Parser.OutputColumnContext ctx) {
    popNode();
  }

  @Override
  public void enterIdentifier(@NotNull PQL2Parser.IdentifierContext ctx) {
    pushNode(new IdentifierAstNode(ctx.getText()));
  }

  @Override
  public void exitIdentifier(@NotNull PQL2Parser.IdentifierContext ctx) {
    popNode();
  }

  @Override
  public void enterStarExpression(@NotNull PQL2Parser.StarExpressionContext ctx) {
    pushNode(new StarExpressionAstNode());
  }

  @Override
  public void exitStarExpression(@NotNull PQL2Parser.StarExpressionContext ctx) {
    popNode();
  }

  @Override
  public void enterFunctionCall(@NotNull PQL2Parser.FunctionCallContext ctx) {
    pushNode(new FunctionCallAstNode(ctx.getChild(0).getText()));
  }

  @Override
  public void exitFunctionCall(@NotNull PQL2Parser.FunctionCallContext ctx) {
    popNode();
  }

  @Override
  public void enterIntegerLiteral(@NotNull PQL2Parser.IntegerLiteralContext ctx) {
    pushNode(new IntegerLiteralAstNode(Integer.parseInt(ctx.getText())));
  }

  @Override
  public void exitIntegerLiteral(@NotNull PQL2Parser.IntegerLiteralContext ctx) {
    popNode();
  }

  @Override
  public void enterOrderBy(@NotNull PQL2Parser.OrderByContext ctx) {
    // TODO
    pushNode(new OrderByAstNode());
  }

  @Override
  public void exitOrderBy(@NotNull PQL2Parser.OrderByContext ctx) {
    popNode();
  }

  @Override
  public void enterGroupBy(@NotNull PQL2Parser.GroupByContext ctx) {
    // TODO
    pushNode(new GroupByAstNode());
  }

  @Override
  public void exitGroupBy(@NotNull PQL2Parser.GroupByContext ctx) {
    popNode();
  }

  @Override
  public void enterBetweenPredicate(@NotNull PQL2Parser.BetweenPredicateContext ctx) {
    // TODO
    pushNode(new BetweenPredicateAstNode());
  }

  @Override
  public void exitBetweenPredicate(@NotNull PQL2Parser.BetweenPredicateContext ctx) {
    popNode();
  }

  @Override
  public void enterBinaryMathOp(@NotNull PQL2Parser.BinaryMathOpContext ctx) {
    pushNode(new BinaryMathOpAstNode(ctx.getChild(1).getText()));
  }

  @Override
  public void exitBinaryMathOp(@NotNull PQL2Parser.BinaryMathOpContext ctx) {
    popNode();
  }

  @Override
  public void enterInPredicate(@NotNull PQL2Parser.InPredicateContext ctx) {
    // TODO
    pushNode(new InPredicateAstNode());
  }

  @Override
  public void exitInPredicate(@NotNull PQL2Parser.InPredicateContext ctx) {
    popNode();
  }

  @Override
  public void enterHaving(@NotNull PQL2Parser.HavingContext ctx) {
    // TODO
    pushNode(new HavingAstNode());
  }

  @Override
  public void exitHaving(@NotNull PQL2Parser.HavingContext ctx) {
    popNode();
  }

  @Override
  public void enterStringLiteral(@NotNull PQL2Parser.StringLiteralContext ctx) {
    // TODO Test this with escaped strings (ie. 'O''clock')
    String text = ctx.getText();
    int textLength = text.length();
    pushNode(new StringLiteralAstNode(text.substring(1, textLength - 1)));
  }

  @Override
  public void exitStringLiteral(@NotNull PQL2Parser.StringLiteralContext ctx) {
    popNode();
  }

  @Override
  public void enterFloatingPointLiteral(@NotNull PQL2Parser.FloatingPointLiteralContext ctx) {
    // TODO
    pushNode(new FloatingPointLiteralAstNode());
  }

  @Override
  public void exitFloatingPointLiteral(@NotNull PQL2Parser.FloatingPointLiteralContext ctx) {
    popNode();
  }

  @Override
  public void enterLimit(@NotNull PQL2Parser.LimitContext ctx) {
    // Can either be LIMIT <maxRows> or LIMIT <offset>, <maxRows> (the second is a MySQL syntax extension)
    if (ctx.getChild(0).getChildCount() == 2)
      pushNode(new LimitAstNode(Integer.parseInt(ctx.getChild(0).getChild(1).getText())));
    else
      pushNode(new LimitAstNode(
          Integer.parseInt(ctx.getChild(0).getChild(3).getText()),
          Integer.parseInt(ctx.getChild(0).getChild(1).getText())
      ));
  }

  @Override
  public void exitLimit(@NotNull PQL2Parser.LimitContext ctx) {
    popNode();
  }

  @Override
  public void enterWhere(@NotNull PQL2Parser.WhereContext ctx) {
    pushNode(new WhereAstNode());
  }

  @Override
  public void exitWhere(@NotNull PQL2Parser.WhereContext ctx) {
    popNode();
  }

  @Override
  public void enterTopClause(@NotNull PQL2Parser.TopClauseContext ctx) {
    pushNode(new TopAstNode(Integer.parseInt(ctx.getChild(1).getText())));
  }

  @Override
  public void exitTopClause(@NotNull PQL2Parser.TopClauseContext ctx) {
    popNode();
  }

  @Override
  public void enterBooleanPredicateOp(@NotNull PQL2Parser.BooleanPredicateOpContext ctx) {
    pushNode(new BooleanPredicateOpAstNode(ctx.getChild(1).getText()));
  }

  @Override
  public void exitBooleanPredicateOp(@NotNull PQL2Parser.BooleanPredicateOpContext ctx) {
    popNode();
  }
}
