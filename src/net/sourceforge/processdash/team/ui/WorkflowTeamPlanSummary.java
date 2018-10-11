// Copyright (C) 2018 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.team.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryRunner;
import net.sourceforge.processdash.tool.db.QueryUtils;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

public class WorkflowTeamPlanSummary extends TinyCGIBase {

    private static final Resources resources = Resources
            .getDashBundle("Analysis.Workflow.Analysis");


    @Override
    protected void writeContents() throws IOException {
        List<Object[]> enactments = queryEnactments();
        Map<String, List<String>> workflows = summarizeWorkflows(enactments);

        String baseUrl = getSummaryBaseUrl();
        if (workflows.isEmpty()) {
            writeNoWorkflowsFound();
        } else if (workflows.size() == 1) {
            writeSingleWorkflowReport(baseUrl, workflows);
        } else {
            writeWorkflowSelector(baseUrl, workflows);
        }
    }



    /**
     * Find a list of process enactments that match the current project
     * filtering criteria
     */
    private List<Object[]> queryEnactments() {
        // Normally we would automate the steps below by using the PDashQuery
        // object. Unfortunately, that object would apply an incorrect
        // constraint to the ProcessEnactment table (requiring enactment roots
        // to match the active project filter, rather than matching against
        // included plan items). To achieve the results we want, we must apply
        // the filtering logic manually. (The code below is copied from the
        // DbAbstractFunction class.)

        // retrieve the database query runner
        DataRepository data = getDataRepository();
        DatabasePlugin db = QueryUtils.getDatabasePlugin(data, true);
        QueryRunner queryRunner = db == null ? null
                : db.getObject(QueryRunner.class);
        if (queryRunner == null)
            return Collections.EMPTY_LIST;

        // build the effective query and associated argument list
        StringBuilder queryHql = new StringBuilder(ENACTMENT_QUERY);
        List queryArgs = new ArrayList();
        ListData criteria = ListData.asListData(
            data.getInheritableValue(getPrefix(), "DB_Filter_Criteria"));
        if (criteria != null && criteria.test())
            QueryUtils.addCriteriaToHql(queryHql, "ts", queryArgs,
                criteria.asList());

        // if we know that the query won't return any result, don't bother
        // running it against the database.
        if (queryHql.indexOf(QueryUtils.IMPOSSIBLE_CONDITION) != -1)
            return Collections.EMPTY_LIST;

        // run the query
        return queryRunner.queryHql(queryHql.toString(), queryArgs.toArray());
    }

    // query to retrieve the enactments that match the current project filter
    // criteria. The TaskStatusFact table is included in the query, to enable
    // an active "User Group" filter to influence the result.
    private static final String ENACTMENT_QUERY = "select distinct "
            + "pe.rootItem.identifier, pe.process.identifier, pe.process.name "
            + "from ProcessEnactment pe, TaskStatusFact ts "
            + "where pe.includesItem.key = ts.planItem.key "
            + "and ts.versionInfo.current = 1 "
            + "order by pe.process.identifier desc";



    /**
     * Take a list of enactments found by the {@link #queryEnactments()} method,
     * and gather them into common workflows.
     * 
     * This method will return a Map whose keys are user-displayable workflow
     * names. The values are lists whose first item is the ID of the workflow,
     * and whose subsequent items are the plan item IDs of enactment roots.
     * 
     * When the enactment query was run against a Team Project, the list of
     * workflows it returned will all come from that project, so the key/name
     * mapping will be unique. On a Master Project, two workflows could have the
     * same name; for now we just choose the one associated with the most
     * recently created team project.
     */
    private Map<String, List<String>> summarizeWorkflows(
            List<Object[]> enactments) {
        Map<String, List<String>> result = new TreeMap<String, List<String>>();

        for (Object[] enactment : enactments) {
            String rootItemID = (String) enactment[0];
            String processID = (String) enactment[1];
            String processName = (String) enactment[2];

            List<String> processData = result.get(processName);
            if (processData == null) {
                processData = new ArrayList<String>();
                processData.add(processID);
                result.put(processName, processData);
            }

            processData.add(rootItemID);
        }

        return result;
    }



    private String getSummaryBaseUrl() {
        String baseUrl = (String) env.get("SCRIPT_PATH");
        int slashPos = baseUrl.lastIndexOf('/');
        baseUrl = baseUrl.substring(0, slashPos + 1) + "workflowSummary"
                + "?project=t";
        return baseUrl;
    }

    private String getWorkflowSummaryUrl(String baseUrl,
            List<String> workflowData) {
        StringBuffer url = new StringBuffer(baseUrl);
        Iterator<String> i = workflowData.iterator();
        HTMLUtils.appendQuery(url, "workflow", i.next());
        while (i.hasNext())
            HTMLUtils.appendQuery(url, "root", i.next());
        return url.toString();
    }



    private void writeNoWorkflowsFound() {
        out.write("<html><body>");
        out.write(resources.getHTML("No_Workflows_Message_Component"));
        out.write("</body></html>");
    }


    private void writeSingleWorkflowReport(String baseUrl,
            Map<String, List<String>> workflows) {
        // FIXME
        writeWorkflowSelector(baseUrl, workflows);
    }


    private void writeWorkflowSelector(String baseUrl,
            Map<String, List<String>> workflows) {
        out.write("<html><body>");
        out.write("<p>");
        out.write(resources.getHTML("Choose_Workflow_Prompt"));
        out.write("</p>\n<ul>\n");

        for (Entry<String, List<String>> e : workflows.entrySet()) {
            String workflowName = e.getKey();
            List<String> workflowData = e.getValue();
            String url = getWorkflowSummaryUrl(baseUrl, workflowData);

            out.write("<li><a href=\"" + url + "\">");
            out.write(HTMLUtils.escapeEntities(workflowName));
            out.write("</a>");
            out.write(" - " + workflowData);
            out.write("</li>\n");
        }

        out.write("</ul>\n");
        out.write("</body></html>");
    }

}
