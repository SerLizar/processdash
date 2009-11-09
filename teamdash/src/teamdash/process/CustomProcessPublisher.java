package teamdash.process;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import net.sourceforge.processdash.net.http.ContentSource;
import net.sourceforge.processdash.net.http.HTMLPreprocessor;
import net.sourceforge.processdash.process.PhaseUtil;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.ui.lib.ProgressDialog;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CustomProcessPublisher {

    private static final String EXT_FILE_PREFIX = "extfile:";

    private static final String PARAM_ITEM = "param";

    private static final String VALUE = "value";

    public static void publish(CustomProcess process, File destFile,
            ContentSource contentSource) throws IOException {

        publish(process, destFile, contentSource, null);
    }

    public static void publish(CustomProcess process, OutputStream output,
            ContentSource contentSource) throws IOException {

        publish(process, output, contentSource, null);
    }

    public static void publish(CustomProcess process, File destFile,
            ContentSource contentSource, URL extBase) throws IOException {

        CustomProcessPublisher pub = new CustomProcessPublisher(contentSource,
                extBase);
        FileOutputStream fos = new FileOutputStream(destFile);
        pub.publish(process, fos);
        pub.close();
    }

    public static void publish(CustomProcess process, OutputStream output,
            ContentSource contentSource, URL extBase) throws IOException {

        CustomProcessPublisher pub = new CustomProcessPublisher(contentSource,
                extBase);
        pub.setHeadless(true);
        pub.publish(process, output);
        pub.close();
    }

    JarOutputStream zip;

    Writer out;

    ContentSource contentSource;

    HTMLPreprocessor processor;

    HashMap customParams, parameters;

    URL extBase;

    boolean headless;

    protected CustomProcessPublisher(ContentSource contentSource, URL extBase)
            throws IOException {
        this.contentSource = contentSource;
        this.extBase = extBase;
        parameters = new HashMap();
        customParams = new HashMap();
        processor = new HTMLPreprocessor(contentSource, null, null, "",
                customParams, parameters);
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    protected synchronized void publish(CustomProcess process,
            OutputStream output) throws IOException {
        initProcess(process);

        Document script = loadScript(process.getGeneratorScript());
        openStreams(process, script, output);

        writeXMLSettings(process);
        runGenerationScript(script);
    }

    protected Document loadScript(String scriptName) throws IOException {
        try {
            return XMLUtils.parse(getFile(scriptName));
        } catch (SAXException se) {
            System.err.print(se);
            se.printStackTrace();
            throw new IOException("Invalid XML file");
        }
    }

    protected void openStreams(CustomProcess process, Document script,
            OutputStream output) throws IOException {

        String scriptVers = script.getDocumentElement().getAttribute("version");
        String scriptReqt = script.getDocumentElement().getAttribute(
                "requiresDashboard");
        String scriptStartingJar = script.getDocumentElement().getAttribute(
                "startingJar");

        Manifest mf = new Manifest();
        JarInputStream startingJarIn = null;
        if (scriptStartingJar != null) {
            startingJarIn = openStartingJar(scriptStartingJar);
            if (startingJarIn != null)
                mf = startingJarIn.getManifest();
        }

        Attributes attrs = mf.getMainAttributes();
        attrs.putValue("Manifest-Version", "1.0");
        String packageName = (String) parameters.get("Dash_Package_Name");
        if (packageName == null)
            packageName = (String) parameters.get("Full_Name");
        attrs.putValue(DashPackage.NAME_ATTRIBUTE, packageName);
        attrs.putValue(DashPackage.ID_ATTRIBUTE, process.getProcessID());
        attrs.putValue(DashPackage.VERSION_ATTRIBUTE, scriptVers + "."
                + TIMESTAMP_FORMAT.format(new Date()));
        if (scriptReqt != null)
            attrs.putValue(DashPackage.REQUIRE_ATTRIBUTE, scriptReqt);

        zip = new JarOutputStream(output, mf);
        out = new OutputStreamWriter(zip);

        if (startingJarIn != null)
            copyFilesFromStartingJar(startingJarIn);
    }

    protected void close() throws IOException {
        out.flush();
        zip.closeEntry();
        zip.close();
    }

    private JarInputStream openStartingJar(String scriptStartingJar)
            throws IOException {
        byte[] contents = getRawFileBytes(scriptStartingJar);
        if (contents == null)
            return null;

        ByteArrayInputStream bytesIn = new ByteArrayInputStream(contents);
        return new JarInputStream(bytesIn);
    }

    private void copyFilesFromStartingJar(JarInputStream startingJarIn)
            throws IOException {
        ZipEntry entry;
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((entry = startingJarIn.getNextEntry()) != null) {
            ZipEntry outEntry = cloneZipEntry(entry);
            zip.putNextEntry(outEntry);
            while ((bytesRead = startingJarIn.read(buffer)) != -1)
                zip.write(buffer, 0, bytesRead);
            zip.closeEntry();
        }
    }

    private ZipEntry cloneZipEntry(ZipEntry entry) {
        ZipEntry result = new ZipEntry(entry.getName());

        if (entry.getComment() != null)
            result.setComment(entry.getComment());
        if (entry.getExtra() != null)
            result.setExtra(entry.getExtra());
        if (entry.getTime() != -1)
            result.setTime(entry.getTime());

        return result;
    }

    protected void initProcess(CustomProcess process) {
        String processName = process.getName();
        String versionNum = process.getVersion();
        String fullName, versionString;

        if (versionNum == null || versionNum.length() == 0)
            versionNum = versionString = "";
        else
            versionString = " (v" + versionNum + ")";
        fullName = processName + versionString;

        setParam("Process_ID", process.getProcessID());
        setParam("Process_Name", processName);
        setParam("Version_Num", versionNum);
        setParam("Version_String", versionString);
        setParam("Full_Name", fullName);

        for (Iterator iter = process.getItemTypes().iterator(); iter.hasNext();) {
            String type = (String) iter.next();
            handleItemList(process, type);
        }

        // parameters.put("USE_TO_DATE_DATA", "t");
    }

    private void handleItemList(CustomProcess process, String itemType) {
        List processItems = process.getItemList(itemType);
        String[] itemList = new String[processItems.size()];
        String itemPrefix = CustomProcess.bouncyCapsToUnderlines(itemType);

        Iterator i = processItems.iterator();
        int itemNum = 0;
        lastItemID = null;
        while (i.hasNext()) {
            CustomProcess.Item item = (CustomProcess.Item) i.next();
            // String itemID = itemType + itemNum;
            String itemID = getItemID(itemPrefix, itemNum);
            itemList[itemNum] = itemID;

            if (CustomProcess.PHASE_ITEM.equals(itemType))
                initPhase(item, itemID);
            else if (PARAM_ITEM.equals(itemType))
                initParam(item);

            setupItem(item, itemID, itemNum);

            itemNum++;
            lastItemID = itemID;
        }
        parameters.put(itemPrefix + "_List_ALL", itemList);
    }

    private String lastItemID;

    private String getItemID(String itemPrefix, int pos) {
        String phasePos = "" + pos;
        while (phasePos.length() < 3)
            phasePos = "0" + phasePos;
        String id = itemPrefix + phasePos;
        setParam(id + "_Pos", phasePos);
        return id;
    }

    private void setupItem(CustomProcess.Item phase, String id, int pos) {
        Iterator iter = phase.getAttributes().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry) iter.next();
            String attrName = CustomProcess.bouncyCapsToUnderlines((String) e
                    .getKey());
            String attrValue = (String) e.getValue();

            if (attrName.startsWith("Is_")) {
                // normalize boolean attributes
                if (attrValue != null && attrValue.length() > 0
                        && "tTyY".indexOf(attrValue.charAt(0)) != -1)
                    attrValue = "t";
                else
                    attrValue = "";
            }

            setParam(id + "_" + attrName, attrValue);

            if (attrName.endsWith("Filename") || attrName.endsWith("File_Name")) {
                enhanceFilenameAttribute(id + "_" + attrName, attrValue);
            }
        }

        if (lastItemID != null) {
            setParam(lastItemID + "_Next_Sibling", id);
            setParam(id + "_Prev_Sibling", lastItemID);
        }
    }

    private void enhanceFilenameAttribute(String attrName, String filename) {
        String directory = "";
        String baseName = filename;

        Matcher m = FILENAME_PATTERN.matcher(filename);
        if (m.matches()) {
            directory = m.group(1);
            baseName = m.group(2);
        }

        setParam(attrName + "_Directory", directory);
        setParam(attrName + "_Basename", baseName);
    }

    private static Pattern FILENAME_PATTERN = Pattern
            .compile("(.*[/\\\\]|)([^/\\\\]+)");

    protected void initPhase(CustomProcess.Item phase, String id) {
        String phaseName = phase.getAttr(CustomProcess.NAME);
        String phaseID = CustomProcess.makeUltraSafe(phaseName);
        setParam(id + "_ID", phaseID);

        String phaseType = phase.getAttr(CustomProcess.TYPE);
        if (phaseType != null && phaseType.trim().length() != 0)
            phaseType = phaseType.trim().toUpperCase();
        else
            phaseType = "DEVELOP";

        if (PhaseUtil.isAppraisalPhaseType(phaseType)) {
            setParam(id + "_Is_Appraisal", "t");
            setParam(id + "_Is_Quality", "t");
            if (phaseType.endsWith("INSP"))
                setParam(id + "_Is_Inspection", "t");
        } else if (PhaseUtil.isFailurePhaseType(phaseType)) {
            setParam(id + "_Is_Failure", "t");
            setParam(id + "_Is_Quality", "t");
        } else if (PhaseUtil.isDevelopmentPhaseType(phaseType)) {
            setParam(id + "_Is_Development", "t");
        } else if (PhaseUtil.isOverheadPhaseType(phaseType)) {
            setParam(id + "_Is_Overhead", "t");
        }
        if ("plan".equalsIgnoreCase(phaseType)
                || !PhaseUtil.isOverheadPhaseType(phaseType)) {
            setParam(id + "_Is_Defect_Injection", "t");
            setParam(id + "_Is_Defect_Removal", "t");
        }
        if ("at".equalsIgnoreCase(phaseType)
                || "pl".equalsIgnoreCase(phaseType))
            setParam(id + "_Is_After_Development", "t");

        if (PSP_PHASE_NAMES.contains(phaseName.toLowerCase()))
            setParam(id + "_Is_PSP", "t");
    }
    private static Set PSP_PHASE_NAMES = Collections.unmodifiableSet(
            new HashSet(Arrays.asList(new String[] { "planning", "design",
                    "design review", "code", "code review", "compile", "test",
                    "postmortem" })));

    private void initParam(CustomProcess.Item item) {
        String name = item.getAttr(CustomProcess.NAME);
        String value = item.getAttr(VALUE);
        if (value == null)
            value = "t";
        setParam(CustomProcess.bouncyCapsToUnderlines(name), value);
    }

    protected void setParam(String parameter, String value) {
        parameters.put(parameter, value);
    }

    protected void writeXMLSettings(CustomProcess process) throws IOException {
        startFile(CustomProcess.SETTINGS_FILENAME);
        process.writeXMLSettings(out);
    }

    protected void startFile(String filename) throws IOException {
        out.flush();
        if (filename.startsWith("/"))
            filename = filename.substring(1);
        zip.putNextEntry(new ZipEntry(filename));
    }

    protected String getFile(String filename) throws IOException {
        return processContent(getRawFile(filename));
    }

    protected String getRawFile(String filename) throws IOException {
        byte[] rawContent = getRawFileBytes(filename);
        if (rawContent == null)
            return null;
        return new String(rawContent);
    }

    protected byte[] getRawFileBytes(String filename) throws IOException {
        if (filename != null && filename.startsWith(EXT_FILE_PREFIX))
            return getRawBytesFromExternalFile(filename
                    .substring(EXT_FILE_PREFIX.length()));
        else
            return contentSource.getContent("/", filename, true);
    }

    private byte[] getRawBytesFromExternalFile(String filename)
            throws IOException {
        if (extBase == null)
            return null;
        URL extFile = new URL(extBase, filename);
        URLConnection conn = extFile.openConnection();
        return FileUtils.slurpContents(conn.getInputStream(), true);
    }

    protected String processContent(String content) throws IOException {
        if (content == null)
            return null;
        return processor.preprocess(content);
    }

    protected void runGenerationScript(Document script) throws IOException {
        NodeList files = script.getElementsByTagName("file");
        String defaultInDir = script.getDocumentElement().getAttribute(
                "inDirectory");
        String defaultOutDir = script.getDocumentElement().getAttribute(
                "outDirectory");
        ProgressDialog progressDialog = null;
        if (!headless)
            progressDialog = new ProgressDialog((java.awt.Frame) null,
                    "Saving", "Saving Custom Process...");
        for (int i = 0; i < files.getLength(); i++) {
            FileGenerator task = new FileGenerator((Element) files.item(i),
                    defaultInDir, defaultOutDir);
            if (headless)
                task.run();
            else
                progressDialog.addTask(task);
        }
        if (!headless)
            progressDialog.run();
    }

    private class FileGenerator implements Runnable {
        Element file;

        String defaultInDir, defaultOutDir;

        public FileGenerator(Element f, String inDir, String outDir) {
            file = f;
            defaultInDir = inDir;
            defaultOutDir = outDir;
        }

        public void run() {
            try {
                generateFile(file, defaultInDir, defaultOutDir);
            } catch (FileNotFoundException fnfe) {
                System.err.println("Warning: could not find file "
                        + fnfe.getMessage() + " - skipping");
            } catch (IOException ioe) {
                System.err
                        .println("While processing " + file.getAttribute("in")
                                + ", caught exception " + ioe);
            }
        }
    }

    private String maybeDefaultDir(String file, String dir) {
        if (file == null || file.startsWith("/")
                || file.startsWith(EXT_FILE_PREFIX))
            return file;
        return dir + "/" + file;
    }

    protected void generateFile(Element file, String defaultInDir,
            String defaultOutDir) throws IOException {
        String inputFile = file.getAttribute("in");
        String outputFile = file.getAttribute("out");
        if (!XMLUtils.hasValue(inputFile))
            return;
        if (!XMLUtils.hasValue(outputFile))
            outputFile = inputFile;
        inputFile = maybeDefaultDir(inputFile, defaultInDir);
        outputFile = maybeDefaultDir(outputFile, defaultOutDir);

        String encoding = file.getAttribute("encoding");

        if ("binary".equals(encoding)) {
            byte[] contents = getRawFileBytes(inputFile);
            if (contents == null)
                throw new FileNotFoundException(inputFile);
            startFile(outputFile);
            zip.write(contents);
            return;
        }

        processor.setDefaultEchoEncoding(encoding);

        String contents = getRawFile(inputFile);
        if (contents == null)
            throw new FileNotFoundException(inputFile);

        customParams.clear();
        NodeList params = file.getElementsByTagName("param");
        String name, val;
        if (params != null)
            for (int i = 0; i < params.getLength(); i++) {
                Element param = (Element) params.item(i);
                customParams.put(name = param.getAttribute("name"), val = param
                        .getAttribute("value"));
                if (XMLUtils.hasValue(param.getAttribute("replace")))
                    contents = StringUtils.findAndReplace(contents, name, val);
            }

        contents = processContent(contents);
        contents = StringUtils.findAndReplace(contents, "[!--#", "<!--#");
        contents = StringUtils.findAndReplace(contents, "--]", "-->");

        if (outputFile.endsWith("#properties")) {
            Properties p = new Properties();
            p.load(new ByteArrayInputStream(contents.getBytes("8859_1")));
            parameters.putAll(p);
        } else {
            startFile(outputFile);
            zip.write(contents.getBytes());
        }
    }

    private static final SimpleDateFormat TIMESTAMP_FORMAT =
        new SimpleDateFormat("yyyyMMddHHmmss");
}
