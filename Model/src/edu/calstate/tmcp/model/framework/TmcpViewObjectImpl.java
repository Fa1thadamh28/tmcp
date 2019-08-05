package edu.calstate.tmcp.model.framework;

import java.sql.Types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.jbo.AttributeDef;
import oracle.jbo.ViewCriteria;
import oracle.jbo.ViewCriteriaRow;
import oracle.jbo.server.ViewObjectImpl;

public class TmcpViewObjectImpl
  extends ViewObjectImpl
{
  public class TmcmViewObjectImpl extends ViewObjectImpl {
      public String getViewCriteriaClause(boolean forQuery) {
          ViewCriteria viewCriteria = getViewCriteria();
          if (viewCriteria != null) {
              AttributeDef[] attrs = viewCriteria.getViewObject().getAttributeDefs();
              StringBuffer columnNameListBuff = new StringBuffer();
              for (AttributeDef attr : attrs) {
                  if (attr.isQueriable()) {
                      columnNameListBuff.append(attr.getColumnName().toUpperCase());
                      columnNameListBuff.append("|");
                  }
              }
              String columnNameList = columnNameListBuff.substring(0, columnNameListBuff.length() - 1);
              ViewCriteriaRow vcr = (ViewCriteriaRow) viewCriteria.first();
              while (vcr != null) {
                  for (AttributeDef attr : attrs) {
                      int index = attr.getIndex();
                      String criteria = (String) vcr.getAttribute(index);
                      if (criteria != null) {
                          boolean restricted = true;
                          switch (attr.getSQLType()) {
                          case (Types.INTEGER):
                          case (Types.NUMERIC):
                          case (Types.DECIMAL):
                          case (Types.DATE):
                          case (Types.TIMESTAMP):
                              {
                                  restricted = false;
                              }
                              break;

                          } // end of switch
                          String newCriteria = detectInjection(criteria, columnNameList, restricted);
                          vcr.setAttribute(index, newCriteria);
                      } //end of if (criteria != null)
                  } // end of for (AttributeDef attr: attrs)
                  vcr = (ViewCriteriaRow) viewCriteria.next();
              } // end of while (vcr != null)
          } // end if (viewCriteria != null)
          return super.getViewCriteriaClause(forQuery);
      }

      protected String detectInjection(String criteria, String columnNames, boolean restrictive) {
          boolean reject = false;
          String testPattern;
          if (restrictive) {
              testPattern = "^(>=|<=|<|>|<>|!=|=|BETWEEN|IN|LIKE|IS)";
          } else {
              StringBuffer constructLooseTestPattern = new StringBuffer("(^(=|!=|LIKE))|(");
              constructLooseTestPattern.append(columnNames);
              constructLooseTestPattern.append(")");
              testPattern = constructLooseTestPattern.toString();
          }
          String testCriteria = criteria.trim().toUpperCase();
          if (testCriteria != null && testCriteria.length() > 0) {
              Pattern sqliPattern = Pattern.compile(testPattern);
              Matcher matcher = sqliPattern.matcher(testCriteria);
              if (matcher.find()) {
                  reject = true;
              }
          }
          // note that the ?: operator can be used in place of simple if statement, for example
          // int a = 1;
          // int b = 2;
          // int minVal = (a < b) ? a : b;
          return reject ? null : criteria;
      }
}
}
