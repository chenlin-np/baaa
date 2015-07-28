package com.northpoint.basics.dataimport.impl;

import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.text.csv.Csv;
import com.northpoint.basics.dataimport.DataImporter;
import com.northpoint.basics.exception.NorthpointException;

public class CsvDataImporter implements DataImporter {
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SX";

    private static Logger log = LoggerFactory.getLogger(CsvDataImporter.class);

    private Reader reader;
    private ResourceResolver rr;
    private Session session;
    private String confPath;
    private String destPath;

    private List<String[]> fields;
    private List<String[]> defaultFields;
    private List<Object[]> parentFields;
    private String primaryType;
    private boolean isNameFromField;
    private String nameFromField;
    private String nameScript;
    private String[] nameScriptFields;
    private int[] nameFieldIndexes;
    private String dryRunPath;
    private boolean dryRunSuccess;

    public CsvDataImporter(Reader reader, ResourceResolver rr, String confPath,
            String destPath) throws NorthpointException {
        this.reader = reader;
        this.confPath = confPath;
        this.destPath = destPath;
        this.dryRunPath = null;
        this.rr = rr;
        this.session = rr.adaptTo(Session.class);
        this.dryRunSuccess = false;

        initConf();
    }

    private void initConf() throws NorthpointException {
        try {
            Node confNode = rr.resolve(confPath).adaptTo(Node.class);
            if (confNode == null) {
                throw new NorthpointException(null, "Conf path not found: "
                        + confPath);
            }

            // Read primary type
            if (confNode.hasProperty("primaryType")) {
                primaryType = confNode.getProperty("primaryType").getString();
            } else {
                primaryType = "nt:unstructured";
            }

            // Read name gen conf
            Node nameGenNode = confNode.getNode("nameGen");
            if (nameGenNode.hasProperty("fromField")) {
                isNameFromField = true;
                nameFromField = nameGenNode.getProperty("fromField")
                        .getString();
            } else {
                isNameFromField = false;
                nameScript = nameGenNode.getProperty("script").getString();
                nameScriptFields = nameGenNode.getProperty("scriptFields")
                        .getString().split(",");
            }

            // Prepare nameFieldIndexes
            if (isNameFromField) {
                nameFieldIndexes = new int[1];
            } else {
                nameFieldIndexes = new int[nameScriptFields.length];
            }

            // Read fields
            fields = new ArrayList<String[]>();
            Node fieldsNode = confNode.getNode("fields");
            NodeIterator nodeIter = fieldsNode.getNodes();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.nextNode();
                String key = node.getName();
                String name = node.hasProperty("name") ? node.getProperty(
                        "name").getString() : key;
                String type = node.hasProperty("type") ? node.getProperty(
                        "type").getString() : "string";
                String script = node.hasProperty("script") ? node.getProperty(
                        "script").getString() : null;

                String[] confArr = new String[4];
                confArr[0] = name;
                confArr[1] = type;
                confArr[2] = script;
                confArr[3] = key;
                fields.add(confArr);

                // Remember the indexes of script fields
                if (!isNameFromField) {
                    for (int i = 0; i < nameScriptFields.length; i++) {
                        if (nameScriptFields[i].equals(key)) {
                            nameFieldIndexes[i] = fields.size() - 1;
                        }
                    }
                } else {
                    if (nameFromField.equals(key)) {
                        nameFieldIndexes[0] = fields.size() - 1;
                    }
                }
            }

            // Read parent fields
            parentFields = new ArrayList<Object[]>();
            if (confNode.hasNode("parent-fields")) {
                Node parentFieldsNode = confNode.getNode("parent-fields");
                nodeIter = parentFieldsNode.getNodes();
                while (nodeIter.hasNext()) {
                    Node node = nodeIter.nextNode();
                    String key = node.getName();
                    String name = node.hasProperty("name") ? node.getProperty(
                            "name").getString() : key;
                    String script = node.hasProperty("script") ? node
                            .getProperty("script").getString() : null;
                    Object[] confArr = new Object[3];
                    confArr[0] = name;

                    confArr[1] = 0;
                    for (int i = 0; i < fields.size(); i++) {
                        String[] field = fields.get(i);
                        if (field[3].equals(key)) {
                            confArr[1] = i;
                            break;
                        }
                    }
                    confArr[2] = script;
                    parentFields.add(confArr);
                }
            }

            // Read default fields
            defaultFields = new ArrayList<String[]>();
            Node defaultFieldsNode = confNode.getNode("defaultFields");
            if (defaultFieldsNode != null) {
                nodeIter = defaultFieldsNode.getNodes();
                while (nodeIter.hasNext()) {
                    Node node = nodeIter.nextNode();
                    String key = node.getName();
                    String type = node.hasProperty("type") ? node.getProperty(
                            "type").getString() : "string";
                    String value = node.getProperty("value").getString();
                    String[] confArr = new String[3];
                    confArr[0] = key;
                    confArr[1] = type;
                    confArr[2] = value;
                    defaultFields.add(confArr);
                }
            }
        } catch (RepositoryException e) {
            throw new NorthpointException(e,
                    "Repository Exception while reading import configuration: "
                            + confPath + ". Reason: " + e.getMessage());
        }
    }

    public List<String[]> getFields() {
        return this.fields;
    }

    public String getDryRunPath() throws NorthpointException {
        if (this.dryRunPath == null) {
            throw new NorthpointException(null, "Dry run never executed.");
        }
        return this.dryRunPath;
    }

    public String[] doDryRun() throws NorthpointException {
        this.dryRunSuccess = true;
        List<String> errors = new ArrayList<String>();
        Iterator<String[]> lineIter;
        try {
            lineIter = new Csv().read(reader);
        } catch (IOException e) {
            this.dryRunSuccess = false;
            throw new NorthpointException(e,
                    "IO Exception while importing CSV.");
        }

        // Generate tmp folder
        String tmpName = Long.toString(System.currentTimeMillis()) + "-"
                + Integer.toString(new Random().nextInt(1000));
        this.dryRunPath = DataImporter.TMP_ROOT + "/" + tmpName;
        try {
            Node tmpRootNode = rr.resolve(DataImporter.TMP_ROOT).adaptTo(
                    Node.class);
            if (tmpRootNode == null) {
                tmpRootNode = JcrUtil.createPath(DataImporter.TMP_ROOT,
                        "nt:unstructured", session);
            }
            tmpRootNode.addNode(tmpName, "nt:unstructured");
        } catch (RepositoryException e) {
            this.dryRunSuccess = false;
            throw new NorthpointException(e, "Cannot create tmp folder: "
                    + dryRunPath);
        }

        int lineCount = 0;
        while (lineIter.hasNext()) {
            lineCount++;
            String[] cols = lineIter.next();
            try {
                List<Object> result = readLine(cols);
                String nodeName = getName(cols);

                String actualPath = destPath + "/" + nodeName;
                Resource res = rr.resolve(actualPath);
                if (res != null
                        && !res.getResourceType().equals("sling:nonexisting")) {
                    throw new NorthpointException(null, "Node already exists: "
                            + actualPath);
                }

                String tmpPath = dryRunPath + "/" + nodeName;
                saveNode(tmpPath, result);
            } catch (NorthpointException e) {
                this.dryRunSuccess = false;
                errors.add("Error on line: " + lineCount + ": " + e.getReason());
            }
        }
        try {
            this.session.save();
        } catch (RepositoryException e) {
            this.dryRunSuccess = false;
            throw new NorthpointException(e,
                    "Repository Exception while saving temp nodes.");
        }
        return errors.toArray(new String[errors.size()]);
    }

    private void saveNode(String path, List<Object> values)
            throws NorthpointException {
        try {
            // Remove special chars. In JCR, it means namespace.
            // TODO: use some library here.
            path = path.replaceAll(":", "");
            path = path.replaceAll("\\'", "");

            // Create parent node first
            String parentPath = path.substring(0, path.lastIndexOf('/'));
            if (rr.resolve(parentPath).adaptTo(Node.class) == null) {
                Node parentNode = JcrUtil.createPath(parentPath, primaryType,
                        session);
                if (primaryType.equals("cq:Page")) {
                    parentNode = parentNode.addNode("jcr:content",
                            "cq:PageContent");
                }

                for (Object[] field : parentFields) {
                    String name = (String) field[0];
                    int index = (Integer) field[1];
                    String script = (String) field[2];
                    Object value = values.get(index);
                    String type = fields.get(index)[1];
                    if (script != null && !script.isEmpty()) {
                        value = executeJavaScript(script, value.toString());
                        type = "string";
                    }
                    saveProperty(parentNode, name, value, type);
                }
            }

            Node node = JcrUtil.createUniquePath(path, primaryType, session);
            if (primaryType.equals("cq:Page")) {
                node = node.addNode("jcr:content", "cq:PageContent");
            }

            for (String[] field : defaultFields) {
                saveProperty(node, field[0], field[2], field[1]);
            }

            int i = 0;
            try {
                for (String[] field : fields) {
                    saveProperty(node, field[0], values.get(i), field[1]);
                    i++;
                }
            } catch (Exception e) {
                log.error("Skipping field for path " + path);
            }
        } catch (RepositoryException e) {
            throw new NorthpointException(e, "Error while saving node: " + path);
        }
    }

    private String getName(String[] cols) throws NorthpointException {
        if (isNameFromField) {
            return (String) cols[nameFieldIndexes[0]];
        } else {
            String[] scriptParams = new String[nameFieldIndexes.length];
            for (int i = 0; i < scriptParams.length; i++) {
                scriptParams[i] = cols[nameFieldIndexes[i]];
            }
            String name = executeJavaScript(nameScript, scriptParams);
            
            // Omit special characters and unnecessary dashes
            name = name.replaceAll("[^a-zA-Z0-9\\-\\/]", "")
                    .replaceAll("-+", "-")
                    .replaceAll("^-", "")
                    .replaceAll("-$", "");
            return name;
        }
    }

    private String executeJavaScript(String nameScript, String... value)
            throws NorthpointException {
        // Escape apostrophe

        for (int i = 0; i < value.length; i++) {
            if (value[i] != null) {
                value[i] = value[i].replaceAll("\\'", "\\\\'");
            } else {
                value[i] = "";
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length - 1; i++) {
            sb.append("'").append(value[i]).append("',");
        }
        if (value.length > 0) {
            sb.append("'").append(value[value.length - 1]).append("'");
        }
        String args = sb.toString();

        try {
            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects();
            String script = "var getName = " + nameScript + "; getName(" + args
                    + ");";
            Object result = cx.evaluateString(scope, script, "script", 1, null);
            return (String) result;
        } catch (EvaluatorException e) {
            throw new NorthpointException(e, "Error executing javascript: "
                    + e.getMessage());
        }
    }

    private List<Object> readLine(String[] cols) throws NorthpointException {
        if (cols.length < fields.size()) {
            throw new NorthpointException(null,
                    "Too Few columns. There should be " + fields.size()
                            + " columns");
        }
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < fields.size(); i++) {
            String type = fields.get(i)[1];
            String script = fields.get(i)[2];
            String value = cols[i];

            // Execute script
            if (script != null) {
                value = executeJavaScript(script, value);
            }

            if (type.startsWith("string")) {
                result.add(value);
            } else if (type.equals("boolean")) {
                result.add(new Boolean(value.equalsIgnoreCase("true") ? true
                        : false));
            } else if (type.startsWith("date")) {
                String formatStr = type.substring("date".length() + 1,
                        type.length() - 1);
                if (formatStr.isEmpty()) {
                    formatStr = CsvDataImporter.DEFAULT_DATE_FORMAT;
                }
                SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
                Date date = null;
                if (value != null) {
                    try {
                        date = sdf.parse(value);
                    } catch (Exception e) {
                        throw new NorthpointException(e,
                                "Error parsing date \"" + value
                                        + "\" using format \"" + formatStr
                                        + "\"");
                    }
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(date);
                    result.add(cal);
                }
            }
        }
        return result;
    }

    private void saveProperty(Node node, String key, Object value, String type)
            throws NorthpointException {
        try {
            if (key.contains("/")) {
                String relPath = key.substring(0, key.lastIndexOf('/'));
                String absolutePath = node.getPath() + "/" + relPath;
                key = key.substring(key.lastIndexOf('/') + 1);
                node = JcrUtil.createPath(absolutePath, "nt:unstructured",
                        session);
            }
            if (type.equals("string")) {
                node.setProperty(key, (String) value);
            } else if (type.equals("boolean")) {
                node.setProperty(key, ((String) value).equalsIgnoreCase("true"));
            } else if (type.indexOf("date") == 0) {
                try {
                    node.setProperty(key, (Calendar) value);
                } catch (ClassCastException cce) {
                    System.out.println("Unable to convert " + value
                            + " to calendar object.");
                }
            } else if (type.equals("string[]")) {
                List<String> strings = new ArrayList<String>();
                if (node.hasProperty(key)) {
                    Value[] values = node.getProperty(key).getValues();
                    for (Value singleValue : values) {
                        strings.add(singleValue.getString());
                    }
                }
                String[] stringsToAdd = ((String) value).split(",");
                for (int i = 0; i < stringsToAdd.length; i++) {
                    strings.add(stringsToAdd[i]);
                }
                node.setProperty(key,
                        strings.toArray(new String[strings.size()]));
            }
        } catch (RepositoryException e) {
            throw new NorthpointException(e,
                    "Repository Exception while setting property: " + key);
        }
    }

    public boolean isDryRunSuccess() {
        return this.dryRunSuccess;
    }
}
