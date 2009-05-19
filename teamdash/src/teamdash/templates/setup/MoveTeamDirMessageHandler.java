package teamdash.templates.setup;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.msg.MessageEvent;
import net.sourceforge.processdash.msg.MessageHandler;
import net.sourceforge.processdash.util.NetworkDriveList;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MoveTeamDirMessageHandler implements MessageHandler {

    private String processID;

    private List<String> indivRootTemplateIDs;

    private DashboardContext ctx;

    private static final Logger logger = Logger
            .getLogger(MoveTeamDirMessageHandler.class.getName());

    public void setConfigElement(Element xml, String attrName) {
        this.processID = xml.getAttribute("processID");

        this.indivRootTemplateIDs = new ArrayList<String>();
        NodeList nl = xml.getElementsByTagName("include");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            this.indivRootTemplateIDs.add(e.getAttribute("templateID"));
        }
    }

    public void setDashboardContext(DashboardContext ctx) {
        this.ctx = ctx;
    }

    public String[] getMessageTypes() {
        return new String[] { processID + "/moveTeamDir" };
    }

    public void handle(MessageEvent message) {
        Element xml = message.getMessageXml();
        String projectId = getString(xml, PROJECT_ID_ATTR);
        if (!StringUtils.hasValue(projectId))
            return;

        String path = findProject(PropertyKey.ROOT, projectId);
        if (path == null)
            return;

        String directory = getString(xml, DIR_ATTR);
        String directoryUNC = getString(xml, DIR_UNC_ATTR);
        String url = getString(xml, URL_ATTR);

        NetworkDriveList dl = new NetworkDriveList();
        if (dl.wasSuccessful()) {
            if (StringUtils.hasValue(directoryUNC)) {
                if (directory == null || !directory.startsWith("\\\\")) {
                    String newDir = dl.fromUNCName(directoryUNC);
                    if (StringUtils.hasValue(newDir))
                        directory = newDir;
                }
            } else if (StringUtils.hasValue(directory)) {
                String newUNC = dl.toUNCName(directory);
                if (StringUtils.hasValue(newUNC))
                    directoryUNC = newUNC;
            }
        }

        logger.info("Moving team data directory for project '" + path + "' to:\n"
            + "\tdirectory=" + directory + "\n"
            + "\tdirectoryUNC=" + directoryUNC + "\n"
            + "\turl=" + url);

        DataContext data = ctx.getData().getSubcontext(path);
        saveString(data, TeamDataConstants.TEAM_DIRECTORY, directory);
        saveString(data, TeamDataConstants.TEAM_DIRECTORY_UNC, directoryUNC);
        saveString(data, TeamDataConstants.TEAM_DATA_DIRECTORY_URL, url);
        RepairImportInstruction.maybeRepairForIndividual(data);
    }

    private String findProject(PropertyKey node, String id) {
        String templateID = ctx.getHierarchy().getID(node);

        if (indivRootTemplateIDs.contains(templateID)) {
            String path = node.path();
            String idDataName = DataRepository.createDataName(path,
                TeamDataConstants.PROJECT_ID);
            SimpleData idValue = ctx.getData().getSimpleValue(idDataName);
            if (idValue != null && id.equals(idValue.format()))
                return path;
            else
                return null;
        }

        for (int i = ctx.getHierarchy().getNumChildren(node); i-- > 0;) {
            PropertyKey child = ctx.getHierarchy().getChildKey(node, i);
            String result = findProject(child, id);
            if (result != null)
                return result;
        }

        return null;
    }

    private String getString(Element xml, String tagName) {
        NodeList nl = xml.getElementsByTagName(tagName);
        if (nl.getLength() == 1)
            return XMLUtils.getTextContents((Element) nl.item(0));
        else
            return null;
    }

    private void saveString(DataContext data, String name, String value) {
        SimpleData d;
        if (StringUtils.hasValue(value))
            d = StringData.create(value);
        else
            d = null;
        data.putValue(name, d);
    }

    static final String PROJECT_ID_ATTR = "projectID";

    static final String DIR_ATTR = "directory";

    static final String DIR_UNC_ATTR = "directoryUNC";

    static final String URL_ATTR = "url";

}
