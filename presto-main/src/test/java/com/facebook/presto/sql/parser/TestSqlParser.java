package com.facebook.presto.sql.parser;

import com.facebook.presto.sql.tree.Statement;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.antlr.runtime.tree.CommonTree;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.facebook.presto.sql.parser.TreePrinter.treeToString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class TestSqlParser
{
    @Test
    public void testStatementBuilder()
            throws Exception
    {
        printStatement("select * from foo");

        printStatement("select * from foo a (x, y, z)");

        printStatement("select show from foo");
        printStatement("select extract(day from x), extract(dow from x) from y");

        printStatement("" +
                "select * from foo a " +
                "join bar b using (bar_id) " +
                "left join zoo on (a.x = zoo.y) " +
                "cross join (d natural inner join e)");

        printStatement("select * from information_schema.tables");

        printStatement("show tables");
        printStatement("show tables from information_schema");

        for (int i = 1; i <= 22; i++) {
            if (i != 15) {
                printStatement(getTpchQuery(i));
            }
        }
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:1: no viable alternative at character '@'")
    public void testTokenizeErrorStartOfLine()
    {
        SqlParser.createStatement("@select");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:25: no viable alternative at character '@'")
    public void testTokenizeErrorMiddleOfLine()
    {
        SqlParser.createStatement("select * from foo where @what");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:20: mismatched character '<EOF>' expecting '''")
    public void testTokenizeErrorIncompleteToken()
    {
        SqlParser.createStatement("select * from 'oops");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 3:1: mismatched input 'from' expecting EOF")
    public void testParseErrorStartOfLine()
    {
        SqlParser.createStatement("select *\nfrom x\nfrom");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 3:7: no viable alternative at input 'from'")
    public void testParseErrorMiddleOfLine()
    {
        SqlParser.createStatement("select *\nfrom x\nwhere from");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:14: no viable alternative at input '<EOF>'")
    public void testParseErrorEndOfInput()
    {
        SqlParser.createStatement("select * from");
    }

    @Test(expectedExceptions = ParsingException.class, expectedExceptionsMessageRegExp = "line 1:16: no viable alternative at input '<EOF>'")
    public void testParseErrorEndOfInputWhitespace()
    {
        SqlParser.createStatement("select * from  ");
    }

    @Test
    public void testParsingExceptionPositionInfo()
    {
        try {
            SqlParser.createStatement("select *\nfrom x\nwhere from");
            fail("expected exception");
        }
        catch (ParsingException e) {
            assertEquals(e.getMessage(), "line 3:7: no viable alternative at input 'from'");
            assertEquals(e.getErrorMessage(), "no viable alternative at input 'from'");
            assertEquals(e.getLineNumber(), 3);
            assertEquals(e.getCharPositionInLine(), 7);
        }
    }

    private static void printStatement(String sql)
    {
        println(sql.trim());
        println("");

        CommonTree tree = SqlParser.parseStatement(sql);
        println(treeToString(tree));
        println("");

        Statement statement = SqlParser.createStatement(tree);
        println(statement.toString());
        println("");
    }

    private static void println(String s)
    {
        if (Boolean.parseBoolean(System.getProperty("printParse"))) {
            System.out.println(s);
        }
    }

    private static String getTpchQuery(int q)
            throws IOException
    {
        return fixTpchQuery(readResource("tpch/queries/" + q + ".sql"));
    }

    private static String readResource(String name)
            throws IOException
    {
        return Resources.toString(Resources.getResource(name), Charsets.UTF_8);
    }

    private static String fixTpchQuery(String s)
    {
        s = s.replaceFirst("(?m);$", "");
        s = s.replaceAll("(?m)^:[xo]$", "");
        s = s.replaceAll("(?m)^:n -1$", "");
        s = s.replaceAll("(?m)^:n ([0-9]+)$", "LIMIT $1");
        s = s.replaceAll("([^']):([0-9]+)", "$1$2");
        return s;
    }
}
