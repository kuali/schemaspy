/*
 * This file is a part of the SchemaSpy project (http://schemaspy.sourceforge.net).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sourceforge.schemaspy.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.Table.ByColumnIdComparator;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.model.TableIndex;
import net.sourceforge.schemaspy.util.LineWriter;

/**
 * The page that lists all of the columns in the schema,
 * allowing the end user to sort by column's attributes.
 *
 * @author John Currier
 */
public class HtmlColumnsPage extends HtmlFormatter {
    private static HtmlColumnsPage instance = new HtmlColumnsPage();

    /**
     * Singleton: Don't allow instantiation
     */
    private HtmlColumnsPage() {
    }

    /**
     * Singleton accessor
     *
     * @return the singleton instance
     */
    public static HtmlColumnsPage getInstance() {
        return instance;
    }

    /**
     * Returns details about the columns that are displayed on this page.
     *
     * @return
     */
    public Map<String, ColumnInfo> getColumnInfos()
    {
        // build a collection of all possible column details
        Map<String, ColumnInfo> avails = new HashMap<String, ColumnInfo>();
        avails.put("id", new ColumnInfo("Id", new ByColumnIdComparator()));
        avails.put("table", new ColumnInfo("Table", new ByTableComparator()));
        avails.put("column", new ColumnInfo("Column", new ByColumnComparator()));
        avails.put("type", new ColumnInfo("Type", new ByTypeComparator()));
        avails.put("size", new ColumnInfo("Size", new BySizeComparator()));
        avails.put("nulls", new ColumnInfo("Nulls", new ByNullableComparator()));
        avails.put("auto", new ColumnInfo("Auto", new ByAutoUpdateComparator()));
        avails.put("default", new ColumnInfo("Default", new ByDefaultValueComparator()));
        avails.put("children", new ColumnInfo("Children", new ByChildrenComparator()));
        avails.put("parents", new ColumnInfo("Parents", new ByParentsComparator()));
        avails.put("comments", new ColumnInfo("Comments", new ByCommentsComparator()));

        // now put the ones requested in the order requested
        // LinkedHashMap maintains insertion order
        Map<String, ColumnInfo> infos = new LinkedHashMap<String, ColumnInfo>();

        for (String detail : Config.getInstance().getColumnDetails()) {
            ColumnInfo info = avails.get(detail);

            if (info == null)
                throw new IllegalArgumentException("Undefined column detail requested: '" + detail + "'. Valid details: " + avails.keySet());
            infos.put(detail, info);
        }

        return infos;
    }

    public class ColumnInfo
    {
        private final String columnName;
        private final Comparator<TableColumn> comparator;

        private ColumnInfo(String columnName, Comparator<TableColumn> comparator)
        {
            this.columnName = columnName;
            this.comparator = comparator;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getLocation() {
            return getLocation(columnName);
        }

        public String getLocation(String colName) {
            return "columns.by" + colName + ".html";
        }

        private Comparator<TableColumn> getComparator() {
            return comparator;
        }

        @Override
        public String toString() {
            return getLocation();
        }
    }

    public void write(Database database, Collection<Table> tables, ColumnInfo columnInfo, LineWriter html) throws IOException {
        Set<TableColumn> columns = new TreeSet<TableColumn>(columnInfo.getComparator());
        Set<TableColumn> primaryColumns = new HashSet<TableColumn>();
        Set<TableColumn> indexedColumns = new HashSet<TableColumn>();

        for (Table table : tables) {
            columns.addAll(table.getColumns());

            primaryColumns.addAll(table.getPrimaryColumns());
            for (TableIndex index : table.getIndexes()) {
                indexedColumns.addAll(index.getColumns());
            }
        }

        writeHeader(database, columns.size(), columnInfo, html);

        HtmlTablePage formatter = HtmlTablePage.getInstance();

        for (TableColumn column : columns) {
            formatter.writeColumn(column, column.getTable().getName(), primaryColumns, indexedColumns, true, false, html);
        }

        writeFooter(html);
    }

    private void writeHeader(Database db, int numberOfColumns, ColumnInfo selectedColumn, LineWriter html) throws IOException {
        writeHeader(db, null, "Columns", html);

        html.writeln("<table width='100%'>");
        html.writeln("<tr><td class='container'>");
        writeGeneratedOn(db.getConnectTime(), html);
        html.writeln("</td><td class='container' rowspan='2' align='right' valign='top'>");
        writeLegend(false, false, html);
        html.writeln("</td></tr>");
        html.writeln("<tr valign='top'><td class='container' align='left' valign='top'>");
        html.writeln("<p>");
        html.writeln("<form name='options' action=''>");
        if (Config.getInstance().getColumnDetails().contains("comments"))
            html.writeln(" <input type=checkbox id='showComments'><label for='showComments'>Comments</label>");
        html.writeln(" <input type=checkbox checked id='showLegend'><label for='showLegend'>Legend</label>");
        html.writeln("</form>");
        html.writeln("</table>");

        html.writeln("<div class='indent'>");
        html.write("<b>");
        html.write(db.getName());
        if (db.getSchema() != null) {
            html.write('.');
            html.write(db.getSchema());
        } else if (db.getCatalog() != null) {
            html.write('.');
            html.write(db.getCatalog());
        }
        html.write(" contains ");
        html.write(String.valueOf(numberOfColumns));
        html.write(" columns</b> - click on heading to sort:");
        Collection<Table> tables = db.getTables();
        boolean hasTableIds = tables.size() > 0 && tables.iterator().next().getId() != null;
        writeMainTableHeader(hasTableIds, selectedColumn, html);
        html.writeln("<tbody valign='top'>");
    }

    public void writeMainTableHeader(boolean hasTableIds, ColumnInfo selectedColumn, LineWriter out) throws IOException {
        boolean onColumnsPage = selectedColumn != null;
        List<String> details = null;
        out.writeln("<a name='columns'></a>");
        out.writeln("<table id='columns' class='dataTable' rules='groups'>");

        if (onColumnsPage) {
            details = new ArrayList<String>(Config.getInstance().getColumnDetails());
            if (!hasTableIds)
                details.remove("id");   // simplify subsequent logic by yanking it now

            for (String detail : details) {
                if (detail.equals("comments"))
                    out.writeln("<colgroup class='comment'>");
                else
                    out.writeln("<colgroup>");
            }
        } else {
            int numCols = hasTableIds ? 9 : 8;
            for (int i = 0; i < numCols; ++i) {
                out.writeln("<colgroup>");
            }
            out.writeln("<colgroup class='comment'>");
        }

        out.writeln("<thead align='left'>");
        out.writeln("<tr>");
        if (onColumnsPage) {
            Map<String, String> headings = new HashMap<String, String>();
            headings.put("id", getTH(selectedColumn, "ID", null, "right"));
            headings.put("table", getTH(selectedColumn, "Table", null, null));
            headings.put("column", getTH(selectedColumn, "Column", null, null));
            headings.put("type", getTH(selectedColumn, "Type", null, null));
            headings.put("size", getTH(selectedColumn, "Size", null, null));
            headings.put("nulls", getTH(selectedColumn, "Nulls", "Are nulls allowed?", null));
            headings.put("auto", getTH(selectedColumn, "Auto", "Is column automatically updated?", null));
            headings.put("default", getTH(selectedColumn, "Default", "Default value", null));
            headings.put("children", getTH(selectedColumn, "Children", "Columns in tables that reference this column", null));
            headings.put("parents", getTH(selectedColumn, "Parents", "Columns in tables that are referenced by this column", null));
            headings.put("comments", "<th title='Comments' class='comment'><span class='notSortedByColumn'>Comments</span></th>");

            // output the headings in the order specified
            if (details != null) {  // redundant, but keeps compiler happy
                for (String detail : details) {
                    out.writeln(headings.get(detail));
                }
            }
        } else {
            if (hasTableIds)
                out.writeln(getTH(selectedColumn, "ID", null, "right"));
            out.writeln(getTH(selectedColumn, "Column", null, null));
            out.writeln(getTH(selectedColumn, "Type", null, null));
            out.writeln(getTH(selectedColumn, "Size", null, null));
            out.writeln(getTH(selectedColumn, "Nulls", "Are nulls allowed?", null));
            out.writeln(getTH(selectedColumn, "Auto", "Is column automatically updated?", null));
            out.writeln(getTH(selectedColumn, "Default", "Default value", null));
            out.writeln(getTH(selectedColumn, "Children", "Columns in tables that reference this column", null));
            out.writeln(getTH(selectedColumn, "Parents", "Columns in tables that are referenced by this column", null));
            out.writeln("  <th title='Comments' class='comment'><span class='notSortedByColumn'>Comments</span></th>");
        }

        out.writeln("</tr>");
        out.writeln("</thead>");
    }

    private String getTH(ColumnInfo selectedColumn, String columnName, String title, String align) {
        StringBuilder buf = new StringBuilder("  <th");

        if (align != null) {
            buf.append(" align='");
            buf.append(align);
            buf.append("'");
        }

        if (title != null) {
            buf.append(" title='");
            buf.append(title);
            buf.append("'");
        }

        if (selectedColumn != null) {
            if (selectedColumn.getColumnName().equals(columnName)) {
                buf.append(" class='sortedByColumn'>");
                buf.append(columnName);
            } else {
                buf.append(" class='notSortedByColumn'>");
                buf.append("<a href='");
                buf.append(selectedColumn.getLocation(columnName));
                buf.append("#columns'><span class='notSortedByColumn'>");
                buf.append(columnName);
                buf.append("</span></a>");
            }
        } else {
            buf.append('>');
            buf.append(columnName);
        }
        buf.append("</th>");

        return buf.toString();
    }

    @Override
    protected void writeFooter(LineWriter html) throws IOException {
        html.writeln("</tbody>");
        html.writeln("</table>");
        html.writeln("</div>");
        super.writeFooter(html);
    }

    @Override
    protected boolean isColumnsPage() {
        return true;
    }

    private class ByColumnComparator implements Comparator<TableColumn> {
        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getName().compareToIgnoreCase(column2.getName());
            if (rc == 0)
                rc = column1.getTable().compareTo(column2.getTable());
            return rc;
        }
    }

    private class ByTableComparator implements Comparator<TableColumn> {
        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getTable().compareTo(column2.getTable());
            if (rc == 0)
                rc = column1.getName().compareToIgnoreCase(column2.getName());
            return rc;
        }
    }

    private class ByTypeComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> bySize = new BySizeComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getTypeName().compareToIgnoreCase(column2.getTypeName());
            if (rc == 0) {
                rc = bySize.compare(column1, column2);
            }
            return rc;
        }
    }

    private class ByCommentsComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byType = new ByTypeComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            String comment1 = column1.getComments();
            if (comment1 == null)
                comment1 = "";
            String comment2 = column1.getComments();
            if (comment2 == null)
                comment2 = "";

            int rc = comment1.compareToIgnoreCase(comment2);
            if (rc == 0) {
                rc = byType.compare(column1, column2);
            }
            return rc;
        }
    }

    private class BySizeComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getLength() - column2.getLength();
            if (rc == 0) {
                rc = column1.getDecimalDigits() - column2.getDecimalDigits();
                if (rc == 0)
                    rc = byColumn.compare(column1, column2);
            }
            return rc;
        }
    }

    private class ByNullableComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.isNullable() == column2.isNullable() ? 0 : column1.isNullable() ? -1 : 1;
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByAutoUpdateComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.isAutoUpdated() == column2.isAutoUpdated() ? 0 : column1.isAutoUpdated() ? -1 : 1;
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByDefaultValueComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byNullable = new ByNullableComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            String value1 = String.valueOf(column1.getDefaultValue());
            String value2 = String.valueOf(column2.getDefaultValue());

            int rc = value1.compareToIgnoreCase(value2);
            if (rc == 0)
                rc = byNullable.compare(column1, column2);
            return rc;
        }
    }

    private class ByChildrenComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            Set<String> childTables1 = new TreeSet<String>();
            Set<String> childTables2 = new TreeSet<String>();

            for (TableColumn column : column1.getChildren()) {
                if (!column.getParentConstraint(column1).isImplied())
                    childTables1.add(column.getTable().getName());
            }

            for (TableColumn column : column2.getChildren()) {
                if (!column.getParentConstraint(column2).isImplied())
                    childTables2.add(column.getTable().getName());
            }

            int rc = childTables1.toString().compareToIgnoreCase(childTables2.toString());
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByParentsComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            Set<String> parentTables1 = new TreeSet<String>();
            Set<String> parentTables2 = new TreeSet<String>();

            for (TableColumn column : column1.getParents()) {
                if (!column.getChildConstraint(column1).isImplied())
                    parentTables1.add(column.getTable().getName() + '.' + column.getTable().getSchema());
            }

            for (TableColumn column : column2.getParents()) {
                if (!column.getChildConstraint(column2).isImplied())
                    parentTables2.add(column.getTable().getName() + '.' + column.getTable().getSchema());
            }

            int rc = parentTables1.toString().compareToIgnoreCase(parentTables2.toString());
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }
}
