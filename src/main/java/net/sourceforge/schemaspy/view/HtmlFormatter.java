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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.Revision;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.HtmlEncoder;
import net.sourceforge.schemaspy.util.LineWriter;

public class HtmlFormatter {
    protected final boolean encodeComments = Config.getInstance().isEncodeCommentsEnabled();
    protected final boolean showAnomalies = Config.getInstance().isAnomaliesEnabled();
    private   final boolean isMetered = Config.getInstance().isMeterEnabled();
    protected final boolean displayNumRows = Config.getInstance().isNumRowsEnabled();
    protected final boolean showDBName = Config.getInstance().isDBNameEnabled();
    protected final String googleAnalyticsID = Config.getInstance().getGoogleAnalyticsID();

    protected HtmlFormatter() {
    }

    protected void writeHeader(Database db, Table table, String text, List<String> javascript, LineWriter out) throws IOException {
        out.writeln("<!DOCTYPE html>");
        out.writeln("<!--[if IE 9]><html class=\"lt-ie10\" lang=\"en\" > <![endif]-->");
        out.writeln("<html>");
        out.writeln("<head>");
        out.writeln("  <!-- SchemaSpy rev " + new Revision() + " -->");
        out.write("  <title>");
        out.write(getDescription(db, table, text, false));
        out.writeln("</title>");

        out.write("  <link rel=stylesheet href='");
        if (table != null)
            out.write("../");
        out.writeln("schemaSpy.css' type='text/css'>");

        out.write("  <link rel=stylesheet href='");
        if (table != null)
            out.write("../");
        out.writeln("schemaSpy-print.css' type='text/css'>");

        out.write("  <link rel=stylesheet href='");
        if (table != null)
            out.write("../");
        out.writeln("css/normalize.css' type='text/css'>");

        out.write("  <link rel=stylesheet href='");
        if (table != null)
            out.write("../");
        out.writeln("css/foundation.css' type='text/css'>");

        out.writeln("  <meta HTTP-EQUIV='Content-Type' CONTENT='text/html; charset=" + Config.getInstance().getCharset() + "'>");
        out.writeln("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript' SRC='" + (table == null ? "" : "../") + "js/vendor/jquery.js'></SCRIPT>");
        out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript' SRC='" + (table == null ? "" : "../") + "js/schemaspy/schemaSpy.js'></SCRIPT>");
        out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript' SRC='http://code.jquery.com/jquery-migrate-1.2.1.js'></SCRIPT>");
        if (table != null) {
            out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript'>");
            out.writeln("    table='" + table + "';");
            out.writeln("  </SCRIPT>");
        }
        if (javascript != null) {
            out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript'>");
            for (String line : javascript)
                out.writeln("    " + line);
            out.writeln("  </SCRIPT>");
        }
        if((googleAnalyticsID != null) && (googleAnalyticsID.length() > 0))
        {
          out.writeln("<script language=\"JavaScript\" type=\"text/javascript\">(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');  ga('create', '" + googleAnalyticsID + "', 'auto');  ga('send', 'pageview');</script>");
        }
        out.writeln("</head>");
        out.writeln("<body class=\"antialiased\">");
        writeTableOfContents(out);
        out.writeln("<div class='inner-wrap'>");
        out.writeln("<div>");
        out.writeln("<div class=\"header\">");
        out.write("<h2>");
        if (table == null)
            out.write("Analysis of ");
        out.write(getDescription(db, table, text, true));
        out.write("</h2>");
        if (table == null && db.getDescription() != null)
            out.write("<h3>" + db.getDescription().replace("\\=", "=") + "</h3>");

        String comments = table == null ? null : table.getComments();
        if (comments != null) {
            out.write("<div style='padding: 0px 4px;'>");
            if (encodeComments)
                for (int i = 0; i < comments.length(); ++i)
                    out.write(HtmlEncoder.encodeToken(comments.charAt(i)));
            else
                out.write(comments);
            out.writeln("</div><p>");
        }
        out.writeln("</div>");
    }

    /**
     * Convenience method for all those formatters that don't deal with JavaScript
     */
    protected void writeHeader(Database db, Table table, String text, LineWriter out) throws IOException {
        writeHeader(db, table, text, null, out);
    }

    protected void writeGeneratedOn(String connectTime, LineWriter html) throws IOException {
        html.write("<span class='container'>");
        html.write("Generated on ");
        html.write(connectTime);
        html.writeln("</span>");
    }

    protected void writeTableOfContents(LineWriter html) throws IOException {
        // don't forget to modify HtmlMultipleSchemasIndexPage with any changes to 'header' or 'headerHolder'
        Config config = Config.getInstance();
        String path = getPathToRoot();
        html.writeln("<nav class=\"top-bar\" data-topbar role=\"navigation\">");
        html.writeln("<section class=\"top-bar-section\">");
        html.writeln("<ul class=\"left\">");
        //html.writeln("<dt>Navigation:</dt>");

        if (config.isOneOfMultipleSchemas())
            html.writeln("  <li><a href='" + path + "../index.html' title='All Schemas Evaluated'>Schemas</a></li>");
        html.writeln("  <li" + (isMainIndex() ? " class='active'" : "") + "><a href='" + path + "index.html' title='All tables and views in the schema'>Tables</a></li>");
        html.writeln("  <li" + (isRelationshipsPage() ? " class='active'" : "") + "><a href='" + path + "relationships.html' title='Diagram of table relationships'>Relationships</a></li>");
        if (config.hasOrphans())
            html.writeln("  <li" + (isOrphansPage() ? " class='active'" : "") + "><a href='" + path + "utilities.html' title='View of tables with neither parents nor children'>Utility&nbsp;Tables</a></li>");
        html.writeln("  <li" + (isConstraintsPage() ? " class='active'" : "") + "><a href='" + path + "constraints.html' title='Useful for diagnosing error messages that just give constraint name or number'>Constraints</a></li>");
        if(showAnomalies)
          html.writeln("  <li" + (isAnomaliesPage() ? " class='active'" : "") + "><a href='" + path + "anomalies.html' title=\"Things that might not be quite right\">Anomalies</a></li>");
        html.writeln("  <li" + (isColumnsPage() ? " class='active'" : "") + "><a href='" + path + HtmlColumnsPage.getInstance().getColumnInfos().get("column") + "' title=\"All of the columns in the schema\">Columns</a></li>");
        if (config.hasRoutines())
            html.writeln("  <li" + (isRoutinesPage() ? " id='current'" : "") + "><a href='" + path + "routines.html' title='Stored Procedures / Functions'>Routines</a></li>");
        html.writeln(" </ul>");
        html.writeln(" </section>");
        html.writeln(" </nav>");

    }

    protected String getDescription(Database db, Table table, String text, boolean hoverHelp) {
        StringBuilder description = new StringBuilder();
        if (table != null) {
            if (table.isView())
                description.append("View ");
            else
                description.append("Table ");
        }
        if(showDBName) {
          if (hoverHelp)
              description.append("<span title='Database'>");
          description.append(db.getName());
          if (hoverHelp)
              description.append("</span>");
        }

        if (db.getSchema() != null) {
            if(showDBName)
                description.append('.');
            if (hoverHelp)
                description.append("<span title='Schema'>");
            description.append(db.getSchema());
            if (hoverHelp)
                description.append("</span>");
        } else if (db.getCatalog() != null) {
            description.append('.');
            if (hoverHelp)
                description.append("<span title='Catalog'>");
            description.append(db.getCatalog());
            if (hoverHelp)
                description.append("</span>");
        }
        if (table != null) {
            description.append('.');
            if (hoverHelp)
                description.append("<span title='Table'>");
            description.append(table.getName());
            if (hoverHelp)
                description.append("</span>");
        }
        if (text != null) {
            description.append(" - ");
            description.append(text);
        }

        return description.toString();
    }

    protected boolean sourceForgeLogoEnabled() {
        return Config.getInstance().isLogoEnabled();
    }

    protected void writeLegend(boolean tableDetails, LineWriter out) throws IOException {
        writeLegend(tableDetails, true, out);
    }

    protected void writeLegend(boolean tableDetails, boolean diagramDetails, LineWriter out) throws IOException {
        out.writeln(" <table class='legend'>");
        out.writeln("  <tr>");
        out.writeln("   <td class='dataTable' valign='bottom'>Legend:</td>");
        if (sourceForgeLogoEnabled())
            out.writeln("   <td class='container' align='right' valign='top'><a href='http://sourceforge.net' target='_blank'><img src='http://sourceforge.net/sflogo.php?group_id=137197&amp;type=1' alt='SourceForge.net' border='0' height='31' width='88'></a></td>");
        out.writeln("  </tr>");
        out.writeln("  <tr><td class='container' colspan='2'>");
        out.writeln("   <table class='dataTable'>");
        out.writeln("    <tbody>");
        out.writeln("    <tr><td class='primaryKey'>Primary key columns</td></tr>");
        out.writeln("    <tr><td class='indexedColumn'>Columns with indexes</td></tr>");
        if (tableDetails)
            out.writeln("    <tr class='impliedRelationship'><td class='detail'><span class='impliedRelationship'>Implied relationships</span></td></tr>");
        // comment this out until I can figure out a clean way to embed image references
        //out.writeln("    <tr><td class='container'>Arrows go from children (foreign keys)" + (tableDetails ? "<br>" : " ") + "to parents (primary keys)</td></tr>");
        if (diagramDetails) {
            out.writeln("    <tr><td class='excludedColumn'>Excluded column relationships</td></tr>");
            if (!tableDetails)
                out.writeln("    <tr class='impliedRelationship'><td class='legendDetail'>Dashed lines show implied relationships</td></tr>");
            out.writeln("    <tr><td class='legendDetail'>&lt; <em>n</em> &gt; number of related tables</td></tr>");
        }
        out.writeln("    </tbody>");
        out.writeln("   </table>");
        out.writeln("  </td></tr>");
        out.writeln(" </table>");
        out.writeln("&nbsp;");
    }

    protected void writeExcludedColumns(Set<TableColumn> excludedColumns, Table table, LineWriter html) throws IOException {
        Set<TableColumn> notInDiagram;

        // diagram INCLUDES relationships directly connected to THIS table's excluded columns
        if (table == null) {
            notInDiagram = excludedColumns;
        } else {
            notInDiagram = new HashSet<TableColumn>();
            for (TableColumn column : excludedColumns) {
                if (column.isAllExcluded() || !column.getTable().equals(table)) {
                    notInDiagram.add(column);
                }
            }
        }

        if (notInDiagram.size() > 0) {
            html.writeln("<span class='excludedRelationship'>");
            html.writeln("<br>Excluded from diagram's relationships: ");
            for (TableColumn column : notInDiagram) {
                if (!column.getTable().equals(table)) {
                    html.write("<a href=\"" + getPathToRoot() + "tables/");
                    html.write(urlEncode(column.getTable().getName()));
                    html.write(".html\">");
                    html.write(column.getTable().getName());
                    html.write(".");
                    html.write(column.getName());
                    html.writeln("</a>&nbsp;");
                }
            }
            html.writeln("</span>");
        }
    }

    protected void writeInvalidGraphvizInstallation(LineWriter html) throws IOException {
        html.writeln("<br>SchemaSpy was unable to generate a diagram of table relationships.");
        html.writeln("<br>SchemaSpy requires Graphviz " + Dot.getInstance().getSupportedVersions().substring(4) + " from <a href='http://www.graphviz.org' target='_blank'>www.graphviz.org</a>.");
    }

    protected void writeFooter(LineWriter html) throws IOException {
        html.writeln("<footer><p class=\"credit\">Generated by <a href=\"http://schemaspy.sourceforge.net/\">SchemaSpy</a></p></footer>");
        html.writeln("</div>"); // for the div class 'row'
        html.writeln("</div>"); // for the div class 'inner-wrap'
        html.writeln("</body>");
        html.writeln("</html>");
    }

    /**
     * Override if your output doesn't live in the root directory.
     * If non blank must end with a trailing slash.
     *
     * @return String
     */
    protected String getPathToRoot() {
        return "";
    }

    /**
     * Override and return true if you're the main index page.
     *
     * @return boolean
     */
    protected boolean isMainIndex() {
        return false;
    }

    /**
     * Override and return true if you're the relationships page.
     *
     * @return boolean
     */
    protected boolean isRelationshipsPage() {
        return false;
    }

    /**
     * Override and return true if you're the orphans page.
     *
     * @return boolean
     */
    protected boolean isOrphansPage() {
        return false;
    }

    /**
     * Override and return true if you're the constraints page
     *
     * @return boolean
     */
    protected boolean isConstraintsPage() {
        return false;
    }

    /**
     * Override and return true if you're the anomalies page
     *
     * @return boolean
     */
    protected boolean isAnomaliesPage() {
        return false;
    }

    /**
     * Override and return true if you're the columns page
     *
     * @return boolean
     */
    protected boolean isColumnsPage() {
        return false;
    }

    /**
     * Override and return true if you're the routines page
     *
     * @return boolean
     */
    protected boolean isRoutinesPage() {
        return false;
    }

    /**
     * Encode the specified string
     *
     * @param string
     * @return
     */
    static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, Config.DOT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            Logger logger = Logger.getLogger(HtmlFormatter.class.getName());
            logger.info("Error trying to urlEncode string [" + string + "] with encoding [" + Config.DOT_CHARSET + "]");
            return string;
        }
    }
}
