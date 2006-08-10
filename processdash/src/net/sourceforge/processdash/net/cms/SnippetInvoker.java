// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.net.http.HTMLPreprocessor;
import net.sourceforge.processdash.net.http.TinyCGI;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

/** Object which can invoke snippets, and capture the results.
 */
public class SnippetInvoker implements SnippetEnvironment {

    /** Status indicating that a snippet has not yet been invoked. */
    public static final int STATUS_NOT_RUN = -1;

    /** Status indicating that a snippet completed successfully */
    public static final int STATUS_OK = 0;

    /** Status indicating that a snippet could not be invoked, because no
     * definition could be found. */
    public static final int STATUS_NO_DEFINITION = 1;

    /** Status indicating that a snippet was not invoked, because it did not
     * match the context of this invoker. */
    public static final int STATUS_CONTEXT_MISMATCH = 2;

    /** Status indicating that a snippet was not invoked, because it did not
     * support the active mode of this invoker. */
    public static final int UNSUPPORTED_MODE = 3;

    /** Status indicating that an error was encountered during the invocation
     * of this snippet. */
    public static final int STATUS_INTERNAL_ERROR = 4;

    /** Canonical key components to use for the statuses above when retrieving
     * data from a resource bundle. */
    public static final String[] STATUS_RESOURCE_KEYS = { "OK",
            "No_Definition", "Context_Mismatch", "Unsupported_Mode",
            "Internal_Error" };



    private Map parentEnv;

    private Map parentParameters;

    private String prefix;

    private DataContext dataContext;

    private String mode;

    private String action;

    private String queryString;


    /** Construct a snippet invoker to use in support of a parent request */
    public SnippetInvoker(Map parentEnv, Map parentParameters, String prefix,
            DataContext dataContext) {
        this.parentEnv = parentEnv;
        this.parentParameters = parentParameters;
        this.prefix = prefix;
        this.dataContext = dataContext;

        this.mode = (String) parentParameters.get("mode");
        this.action = (String) parentParameters.get("action");

        StringBuffer qStr = new StringBuffer();
        for (int i = 0; i < PROPAGATED_PARAMS.length; i++)
            appendParam(qStr, PROPAGATED_PARAMS[i]);
        queryString = qStr.toString();
    }

    /** Invoke a single snippet.
     *
     * @param snippet the snippet to run
     * @return the content generated by the snippet
     * @throws IOException if an error was encountered
     */
    public String invoke(SnippetInstanceTO snippet) throws IOException {

        SnippetDefinition defn = snippet.getDefinition();
        if (defn == null) {
            snippet.setStatus(SnippetInvoker.STATUS_NO_DEFINITION);
            return null;
        } else if (!defn.matchesContext(dataContext)) {
            snippet.setStatus(SnippetInvoker.STATUS_CONTEXT_MISMATCH);
            return null;
        }

        if (XMLUtils.hasValue(mode)
                && !"view".equalsIgnoreCase(mode)
                && !defn.getModes().contains(mode)) {
            snippet.setStatus(SnippetInvoker.UNSUPPORTED_MODE);
            return null;
        }

        String uri = defn.getUri(mode, action);
        if (uri == null) {
            snippet.setStatus(SnippetInvoker.UNSUPPORTED_MODE);
            return null;
        }

        String namespace = snippet.getNamespace();

        Map extraEnvironment = new HashMap();
        extraEnvironment.put(SNIPPET_ID, snippet.getSnippetID());
        extraEnvironment.put(SNIPPET_VERSION, snippet.getSnippetVersion());
        extraEnvironment.put(PERSISTED_TEXT, snippet.getPersistedText());
        extraEnvironment.put(RESOURCES, defn.getResources());
        extraEnvironment.put(HTMLPreprocessor.REPLACEMENTS_PARAM, Collections
                .singletonMap("$$$_", namespace));

        StringBuffer queryString = new StringBuffer(this.queryString);
        addNamespacedParameters(parentParameters, namespace, queryString);
        if (defn.shouldParsePersistedText())
            addParsedParameters(snippet.getPersistedText(), queryString);

        StringBuffer fullUri = new StringBuffer();
        fullUri.append(WebServer.urlEncodePath(prefix)).append("/").append(uri);
        HTMLUtils.appendQuery(fullUri, queryString.toString());

        WebServer webServer = (WebServer) parentEnv
                .get(TinyCGI.TINY_WEB_SERVER);
        try {
            String results = webServer.getRequestAsString(fullUri.toString(),
                    extraEnvironment);
            snippet.setStatus(SnippetInvoker.STATUS_OK);
            snippet.setUri(fullUri.toString());
            return results;
        } catch (IOException ioe) {
            snippet.setStatus(SnippetInvoker.STATUS_INTERNAL_ERROR);
            snippet.setInvocationException(ioe);
            throw ioe;
        }
    }

    protected static void addNamespacedParameters(Map params,
            String namespace, StringBuffer queryString) {
        for (Iterator i = params.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String name = (String) e.getKey();
            if (name.startsWith(namespace) && name.endsWith("_ALL")) {
                appendParam(queryString, name, (String[]) e.getValue());
                appendParam(queryString, name.substring(namespace.length()),
                        (String[]) e.getValue());
            }
        }
    }

    private void addParsedParameters(String persistedText,
            StringBuffer queryString) {
        if (persistedText != null && persistedText.length() > 0) {
            String params = PARAM_PERSISTER.getQueryString(persistedText);
            HTMLUtils.appendQuery(queryString, params);
        }
    }

    private void appendParam(StringBuffer query, String name) {
        String value = (String) parentParameters.get(name);
        appendParam(query, name, value);
    }

    protected static void appendParam(StringBuffer query, String name,
            String[] values) {
        if (values != null) {
            if (name.endsWith("_ALL"))
                name = name.substring(0, name.length() - 4);
            for (int i = 0; i < values.length; i++)
                appendParam(query, name, values[i]);
        }
    }

    protected static void appendParam(StringBuffer query, String name,
            String value) {
        if (XMLUtils.hasValue(value))
            HTMLUtils.appendQuery(query, name, value);
    }

    /** Query parameters that should be propagated from the parent HTTP request
     * to the snippet */
    private static final String[] PROPAGATED_PARAMS = { "mode", "action",
            "EXPORT", "defaults", PAGE_FILENAME_PARAM, PAGE_TITLE_PARAM,
            LOCALIZED_PREFIX_PARAM, CURRENT_FRAME_URI, FULL_PAGE_URI,
            FULL_PAGE_TARGET };


    private static final ParamDataPersister PARAM_PERSISTER =
        new XmlParamDataPersisterV1();
}
