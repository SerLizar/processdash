package teamdash.wbs;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList;

public class TeamProjectBottomUp extends TeamProject {

    private Map subprojects;

    private FileWatcher watcher;

    private boolean mergeSimilarNodes;

    private Set ignoredSubprojects;

    public TeamProjectBottomUp(String location, String projectName) {
        this(ImportDirectoryFactory.getInstance().get(location), projectName,
                true, true, null);
    }

    protected TeamProjectBottomUp(File directory, String projectName,
            boolean mergeSimilar, boolean autoReload, Set ignoredSubprojects) {
        this(ImportDirectoryFactory.getInstance().get(directory), projectName,
                mergeSimilar, autoReload, ignoredSubprojects);
    }

    private TeamProjectBottomUp(ImportDirectory importDir, String projectName,
            boolean mergeSimilar, boolean autoReload, Set ignoredSubprojects) {
        super(importDir.getDirectory(), projectName);
        super.setReadOnly(true);
        super.setImportDirectory(importDir);
        this.mergeSimilarNodes = mergeSimilar;
        this.ignoredSubprojects = (ignoredSubprojects != null
                ? ignoredSubprojects : Collections.EMPTY_SET);

        reloadBottomUpData();
        if (autoReload)
            watcher = new FileWatcher();
    }

    public void dispose() {
        watcher.quit();
    }

    public String getProjectName() {
        return super.getProjectName() + " (Bottom Up)";
    }

    public void setReadOnly(boolean readOnly) {
        // do nothing.  We don't want to allow anyone to make this
        // project editable.
    }

    public boolean save() {
        // do nothing.  The read-only status of the parent project
        // ought to have the same effect, but we don't want to take
        // any chances.
        return true;
    }

    public void reload() {
        if (getWBS() == null)
            // if the work breakdown structure is still null, then we are
            // loading data on behalf of our superclass's constructor.
            // It isn't safe to perform any of our own initializations yet.
            super.reload();
        else {
            // if the work breakdown structure is not null, then someone else
            // is calling us, requesting data to be reloaded.
            maybeReload();
        }
    }

    public boolean maybeReload() {
        refreshImportDirectory();
        openProjectSettings();
        return reloadBottomUpData();
    }

    private boolean reloadBottomUpData() {
        if (refreshSubprojects()) {
            recalculateBottomUpData();
            return true;
        } else {
            return false;
        }
    }

    private boolean refreshSubprojects() {
        boolean result = false;

        if (subprojects == null) {
            subprojects = new LinkedHashMap();
            result = true;
        }

        Set subprojectChecklist = new HashSet(subprojects.keySet());

        Element settings = getProjectSettings();
        NodeList nl = settings.getElementsByTagName("subproject");
        for (int i = 0; i < nl.getLength(); i++) {
            Element subprojectElem = (Element) nl.item(i);
            String projectID = subprojectElem.getAttribute("projectID");
            if (ignoredSubprojects.contains(projectID))
                continue;

            String shortName = subprojectElem.getAttribute("shortName");

            TeamProject subproject = (TeamProject) subprojects.get(shortName);
            if (subproject == null) {
                File dir = getProjectDataDirectory(subprojectElem, true);
                if (dir != null) {
                    ImportDirectory iDir = ImportDirectoryFactory.getInstance()
                            .get(dir);
                    subproject = new TeamProject(iDir.getDirectory(), shortName);
                    subproject.setImportDirectory(iDir);
                    subprojects.put(shortName, subproject);
                    result = true;
                }
            } else {
                subprojectChecklist.remove(shortName);
                if (subproject.maybeReload())
                    result = true;
            }
        }

        for (Iterator i = subprojectChecklist.iterator(); i.hasNext();) {
            String subprojectName = (String) i.next();
            subprojects.remove(subprojectName);
            result = true;
        }

        return result;
    }

    private void recalculateBottomUpData() {
        WBSModel newWbs = makeStartingWBS();
        TeamMemberList newTeam = new TeamMemberList();

        for (Iterator i = subprojects.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String shortName = (String) e.getKey();
            TeamProject subproject = (TeamProject) e.getValue();

            String subprojInitials = TeamMember.convertToInitials(shortName);
            addWBSItems(newWbs, shortName, subproject, subprojInitials);
            double totalSubprojectTime = sumUpTime(newWbs, subprojInitials);
            addTeamMember(newTeam, shortName, subproject, totalSubprojectTime);
        }

        newWbs.copyNodeExpansionStates(getWBS(),
                MasterWBSUtil.NODE_ID_COMPARATOR);
        getWBS().copyFrom(newWbs);

        getTeamMemberList().copyFrom(newTeam);
    }

    private WBSModel makeStartingWBS() {
        WBSModel result = new WBSModel(getProjectName());
        WBSNode[] descendants = result.getDescendants(result.getRoot());
        result.deleteNodes(Arrays.asList(descendants));
        MasterWBSUtil.mergeFromMaster(super.readWBS(), getProjectID(), result);
        MasterWBSUtil.visitWBS(result, MASTER_WBS_CLEANER);
        return result;
    }

    private void addTeamMember(TeamMemberList newTeam, String shortName,
            TeamProject subproject, double totalSubprojectTime) {
        // create a new row in the team list, and set the name/initials.
        newTeam.maybeAddEmptyRow();
        int pos = newTeam.getRowCount() - 1;
        newTeam.setValueAt(shortName, pos, TeamMemberList.NAME_COLUMN);
        newTeam.setValueAt(shortName, pos, TeamMemberList.INITIALS_COLUMN);

        // retrieve the newly added pseudo-team-member. (Note that we couldn't
        // do this until the name was set above, because the getTeamMembers()
        // method only returns nonempty individuals.)
        TeamMember newMember = (TeamMember) newTeam.getTeamMembers().get(pos);

        // find out when the subteam is starting, and set that as the start
        // date for our new pseudo-team-member.
        TeamMemberList subTeam = subproject.getTeamMemberList();
        Date subteamStartDate = subTeam.getDateForEffort(0);
        newMember.setStartDate(subteamStartDate);

        // now, find out when the subteam plans to finish.  Then, create an
        // artificial schedule that causes our pseudo-team-member's to finish
        // on that date.
        long teamEndTime = subteamStartDate.getTime();
        for (Iterator i = subTeam.getTeamMembers().iterator(); i.hasNext();) {
            TeamMember m = (TeamMember) i.next();
            double memberTime = sumUpTime(subproject.getWBS(), m.getInitials());
            Date memberEndDate = m.getSchedule().getDateForEffort(memberTime);
            if (memberEndDate != null)
                teamEndTime = Math.max(teamEndTime, memberEndDate.getTime());
        }
        Date teamEndDate = new Date(teamEndTime);
        int teamEndWeek = newMember.getSchedule().dateToWeekValue(teamEndDate);
        newMember.getSchedule().setHoursPerWeek(0.01);
        newMember.getSchedule().setWeekData(teamEndWeek,
            new Double(Integer.MAX_VALUE));
    }

    private void addWBSItems(WBSModel newWbs, String shortName,
            TeamProject subproject, String subprojInitials) {
        List teamMemberInitials = getTeamMemberInitials(subproject);
        String projectID = subproject.getProjectID();
        WBSModel subprojectWBS = subproject.getWBS();

        MasterWBSUtil.mergeFromSubproject(subprojectWBS, projectID, shortName,
                subprojInitials, teamMemberInitials,
                mergeSimilarNodes == false, newWbs);
    }

    private List getTeamMemberInitials(TeamProject proj) {
        List result = new LinkedList();
        List teamMembers = proj.getTeamMemberList().getTeamMembers();
        for (Iterator i = teamMembers.iterator(); i.hasNext();) {
            TeamMember t = (TeamMember) i.next();
            result.add(t.getInitials());
        }
        return result;
    }

    private double sumUpTime(WBSModel wbs, String name) {
        SumAttribute sum = new SumAttribute(name + "-Time (Top Down)");
        MasterWBSUtil.visitWBS(wbs, sum);
        return sum.getTotal();
    }

    private static class SumAttribute implements WBSNodeVisitor {
        private double total = 0;
        private String attrName;
        public SumAttribute(String attrName) {
            this.attrName = attrName;
        }
        public void visit(WBSNode parent, WBSNode child) {
            double val = child.getNumericAttribute(attrName);
            if (!Double.isNaN(val))
                total += val;
        }
        public double getTotal() {
            return total;
        }
    }

    private static class MasterWBSCleaner implements WBSNodeVisitor {

        public void visit(WBSNode parent, WBSNode child) {
            child.setReadOnly(false);
            child.setAttribute("Time (Top Down)", null);
        }

    }
    private static final WBSNodeVisitor MASTER_WBS_CLEANER = new MasterWBSCleaner();



    private class FileWatcher extends Thread {
        public volatile boolean isRunning;

        public FileWatcher() {
            super(FileWatcher.class.getName());
            setDaemon(true);
            isRunning = true;
            start();
        }

        public void run() {
            while (isRunning) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
                maybeReload();
            }
        }

        public void quit() {
            isRunning = false;
        }
    }

}
