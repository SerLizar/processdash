// Copyright (C) 2017 Tuma Solutions, LLC
// REST API Add-on for the Process Dashboard
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

package net.sourceforge.processdash.rest.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.rest.rs.HttpException;
import net.sourceforge.processdash.rest.to.RestProject;
import net.sourceforge.processdash.rest.to.RestTask;
import net.sourceforge.processdash.util.StringUtils;

public class RestTaskService {

    private static RestTaskService svc;

    public static RestTaskService get() {
        if (svc == null)
            svc = new RestTaskService();
        return svc;
    }


    private DashboardContext ctx;

    private DashHierarchy hier;

    private RestProjectService projects;

    private RestTaskService() {
        ctx = RestDashContext.get();
        hier = ctx.getHierarchy();
        projects = RestProjectService.get();
        DashController.assignHierarchyNodeIDs();
    }

    public RestTask byID(String nodeID) {
        return byKey(hier.findKeyByNodeID(nodeID));
    }

    public RestTask byPath(String taskPath) {
        return byKey(hier.findExistingKey(taskPath));
    }

    public RestTask byKey(PropertyKey key) {
        String nodeID = hier.pget(key).getNodeID();
        if (!StringUtils.hasValue(nodeID))
            return null;

        String fullPath = key.path();
        RestProject proj = projects.containingPath(fullPath);
        if (fullPath.equals(proj.getFullName()))
            return null;

        String taskName = fullPath.substring(proj.getFullName().length() + 1);
        return new RestTask(nodeID, taskName, proj);
    }

    public List<RestTask> allLeaves() {
        return leavesUnder(PropertyKey.ROOT);
    }

    public List<RestTask> leavesUnder(String parentPath) {
        return leavesUnder(hier.findExistingKey(parentPath));
    }

    public List<RestTask> leavesUnder(PropertyKey parent) {
        return leavesUnder(parent, false);
    }

    public List<RestTask> forProject(RestProject project) {
        PropertyKey projectKey = hier.findExistingKey(project.getFullName());
        return leavesUnder(projectKey, true);
    }

    private List<RestTask> leavesUnder(PropertyKey parent,
            boolean pruneTeamProjects) {
        List<RestTask> result = new ArrayList<RestTask>();
        if (parent != null)
            enumLeafTasks(result, parent, true, pruneTeamProjects);
        return result;
    }

    private void enumLeafTasks(List<RestTask> result, PropertyKey node,
            boolean isStartingNode, boolean pruneTeamProjects) {
        // if pruneTeamProjects is true, and this is a team project, abort
        if (pruneTeamProjects && !isStartingNode
                && projects.byPath(node.path()) != null)
            return;

        int numKids = hier.getNumChildren(node);
        if (numKids == 0) {
            RestTask task = byKey(node);
            if (task != null)
                result.add(task);
        } else {
            for (int i = 0; i < numKids; i++)
                enumLeafTasks(result, hier.getChildKey(node, i), false,
                    pruneTeamProjects);
        }
    }


    public static final int TASK_COMPLETION_DATE = 1;

    public static final int TASK_TIMES = 2;

    public RestTask loadData(RestTask task) {
        return loadData(task, TASK_COMPLETION_DATE + TASK_TIMES);
    }

    public RestTask loadData(RestTask task, int fieldMask) {
        if (task == null)
            return null;
        String fullPath = task.getFullPath();
        SimpleData sd;

        if ((fieldMask & TASK_COMPLETION_DATE) > 0) {
            // load the task completion date
            sd = ctx.getData().getSimpleValue(fullPath + COMPLETED);
            if (sd instanceof DateData)
                task.setCompletionDate(((DateData) sd).getValue());
        }

        if ((fieldMask & TASK_TIMES) > 0) {
            // load the estimated time for the task
            sd = ctx.getData().getSimpleValue(fullPath + EST_TIME);
            if (sd instanceof DoubleData)
                task.setEstimatedTime(((DoubleData) sd).getDouble());

            // load the actual time for the task
            sd = ctx.getData().getSimpleValue(fullPath + ACT_TIME);
            if (sd instanceof DoubleData)
                task.setActualTime(((DoubleData) sd).getDouble());
        }

        return task;
    }

    public void saveEstimatedTime(RestTask task, Double estimatedTime) {
        ensureLeaf(task);

        // if this task has a read-only time estimate, don't change it.
        String dataName = task.getFullPath() + EST_TIME;
        SimpleData sd = ctx.getData().getSimpleValue(dataName);
        if (sd != null && !sd.isEditable())
            return;

        // store the new time estimate
        DoubleData dd = (estimatedTime == null ? null
                : new DoubleData(estimatedTime.doubleValue(), true));
        ctx.getData().userPutValue(dataName, dd);
    }

    public void saveCompletionDate(RestTask task, Date completionDate) {
        ensureLeaf(task);
        DateData dd = (completionDate == null ? null
                : new DateData(completionDate, true));
        ctx.getData().userPutValue(task.getFullPath() + COMPLETED, dd);
    }

    public void ensureLeaf(RestTask task) {
        if (task == null)
            throw HttpException.notFound();
        String path = task.getFullPath();
        PropertyKey key = hier.findExistingKey(path);
        if (key == null || hier.getNumChildren(key) > 0)
            throw HttpException.badRequest();
    }


    private static final String EST_TIME = "/Estimated Time";

    private static final String ACT_TIME = "/Time";

    private static final String COMPLETED = "/Completed";

}
