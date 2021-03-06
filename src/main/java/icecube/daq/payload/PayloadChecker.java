package icecube.daq.payload;

import icecube.daq.payload.impl.SourceID;
import icecube.daq.payload.impl.UTCTime;
import icecube.daq.util.JAXPUtil;
import icecube.daq.util.JAXPUtilException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Run configuration.
 */
class RunConfig
{
    /** Trigger configuration name. */
    private String trigCfg;

    /**
     * Run configuration
     * @param configName configuration name
     * @param doc XML tree describing run configuration
     */
    RunConfig(String configName, Node doc)
    {
        NodeList cfgNodes;
        try {
            cfgNodes = JAXPUtil.extractNodeList(doc, "runConfig/triggerConfig");
        } catch (JAXPUtilException jux) {
            throw new Error(jux);
        }

        if (cfgNodes.getLength() == 0) {
            throw new Error("No trigger configuration found in " + configName);
        } else if (cfgNodes.getLength() > 1) {
            throw new Error("Multiple trigger configurations found in " +
                            configName);
        }

        trigCfg = cfgNodes.item(0).getTextContent();
    }

    /**
     * Return trigger configuration name.
     * @return name
     */
    String getTriggerConfig()
    {
        return trigCfg;
    }
}

/**
 * Trigger configuration entry
 */
class TriggerConfigEntry
{
    /** Log object. */
    private static final Logger LOG =
        Logger.getLogger(TriggerConfigEntry.class);

    /**
     * Map of configuration names to list of parameters
     */
    private static final HashMap<String, String[]> triggerParams =
        new HashMap<String, String[]>() {
            {
                put("AmandaM18Trigger", new String[0]);
                put("AmandaM24Trigger", new String[0]);
                put("AmandaMFrag20Trigger", new String[0]);
                put("AmandaRandomTrigger", new String[0]);
                put("AmandaStringTrigger", new String[0]);
                put("AmandaVolumeTrigger", new String[0]);
                put("CalibrationTrigger", new String[] { "hitType", "domSet" });
                put("ClusterTrigger",
                    new String[] {
                        "coherenceLength", "multiplicity", "timeWindow",
                        "domSet",
                    });
                put("CylinderTrigger",
                    new String[] {
                        "multiplicity", "simpleMultiplicity", "radius", "height",
                        "timeWindow", "domSet",
                    });
                put("FixedRateTrigger", new String[] { "interval" });
                put("MinBiasTrigger", new String[] { "prescale" });
                put("MultiplicityStringTrigger",
                    new String[] {
                        "maxLength", "numberOfVetoTopDoms", "string",
                        "threshold", "timeWindow",
                    });
                put("PhysicsMinBiasTrigger",
                    new String[] { "deadtime", "prescale" });
                put("SimpleMajorityTrigger",
                    new String[] { "threshold", "timeWindow", "domSet" });
                put("SlowMPTrigger",
                    new String[] { "t_proximity", "t_min", "t_max",
                                   "alpha_min", "dc_algo", "rel_v",
                                   "min_n_tuples", "max_event_length", });
                put("ThroughputTrigger", new String[0]);
                put("TrigBoardTrigger", new String[] { "prescale" });
                put("VolumeTrigger",
                    new String[] {
                        "timeWindow", "multiplicity", "coherenceLength",
                        "domSet",
                    });
            }
        };

    /*
    private static final String[] bogusTypeList = new String[] {
        "AmandaRandomTrigger",
        "ClusterTrigger",
        "MultiplicityStringTrigger",
        "PhysicsMinBiasTrigger",
    };
    */

    private int type = -1;
    private int id = -1;
    private int srcId = -1;
    private String name;
    private HashMap<String, Object> params = new HashMap<String, Object>();

    /**
     * Trigger configuration entry
     */
    TriggerConfigEntry(Node top)
    {
        for (Node node = top.getFirstChild(); node != null;
             node = node.getNextSibling())
        {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element elem = (Element) node;
            final String tagName = elem.getTagName();

            if (tagName.equals("triggerType")) {
                type = Integer.parseInt(elem.getTextContent());
            } else if (tagName.equals("triggerConfigId")) {
                id = Integer.parseInt(elem.getTextContent());
            } else if (tagName.equals("sourceId")) {
                srcId = Integer.parseInt(elem.getTextContent());
            } else if (tagName.equals("triggerName")) {
                name = elem.getTextContent();
            } else if (tagName.equals("parameterConfig")) {
                parseTriggerParameter(elem);
            } else if (!tagName.equals("readoutConfig")) {
                throw new Error("Unknown trigger " +
                                (name != null ? name + " " :
                                 (id != 0 ? "#" + id + " " :
                                  "")) + " attribute \"" + tagName + "\"");
            }
        }

        validate();
    }

    private void parseTriggerParameter(Node top)
    {
        String pName = null;
        Object pVal = null;

        for (Node node = top.getFirstChild(); node != null;
             node = node.getNextSibling())
        {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element elem = (Element) node;
            final String tagName = elem.getTagName();

            if (tagName.equals("parameterName")) {
                pName = elem.getTextContent();
            } else if (tagName.equals("parameterValue")) {
                try {
                    pVal = Long.parseLong(elem.getTextContent());
                } catch (Exception ex) {
                    try {
                        pVal = Boolean.parseBoolean(elem.getTextContent());
                    } catch (Exception ex2) {
                        throw new Error("Bad value for \"" + pName + "\"", ex);
                    }
                }
            }
        }

        params.put(pName, pVal);
    }

    int getId()
    {
        return id;
    }

    String getName()
    {
        return name;
    }

    Object getParameter(String key)
    {
        if (!params.containsKey(key)) {
            return null;
        }

        return params.get(key);
    }

    int getSourceID()
    {
        return srcId;
    }

    int getType()
    {
        return type;
    }

    boolean isType(int type)
    {
        return type == this.type;
    }

    void validate()
    {
        if (name == null || srcId < 0 || type < 0) {
            throw new Error("Trigger configuration \"" + toString() +
                            " was not properly initialized");
        }

        if (id < 0 && srcId != SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) {
            throw new Error("Trigger configuration \"" + toString() +
                            " ID should be set for source " +
                            SourceIdRegistry.getDAQNameFromSourceID(srcId));
        }

/*
        boolean isBogusType = false;
        for (String b : bogusTypeList) {
            if (name.equals(b)) {
                isBogusType = true;
                break;
            }
        }

        int expType = TriggerRegistry.getTriggerType(name);
        if (type != expType) {
            String errMsg = "Trigger \"" + name + "\" should have type #" +
                expType + ", not #" + type;

            if (!isBogusType) {
                throw new Error(errMsg);
            }

            LOG.error(errMsg);
        }
*/

        if (!triggerParams.containsKey(name)) {
            LOG.error("Not validating trigger \"" + name + "\"");
            return;
        }

        String[] expParams = triggerParams.get(name);

        for (String p : params.keySet()) {
            boolean found = false;
            for (String x : expParams) {
                if (p.equals(x)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new Error("Trigger \"" + name + "\" has unknown" +
                                " parameter \"" + p + "\"");
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(name);
        buf.append('#').append(id);
        buf.append('@').append(srcId);
        buf.append('*').append(type);

        if (params.size() > 0) {
            buf.append("{");
            boolean needComma = false;
            for (String pName : params.keySet()) {
                if (needComma) {
                    buf.append(',');
                } else {
                    needComma = true;
                }
                buf.append(pName).append(':').append(params.get(pName));
            }
            buf.append('}');
        }

        return buf.toString();
    }
}

/**
 * Trigger configuration
 */
class TriggerConfig
{
    private String name;
    private List<TriggerConfigEntry> entries =
        new ArrayList<TriggerConfigEntry>();

    /**
     * Trigger configuration
     * @param name configuration name
     */
    TriggerConfig(String name)
    {
        this.name = name;
    }

    /**
     * Add a configuration entry.
     * @param entry configuration entry
     */
    void add(TriggerConfigEntry entry)
    {
        entries.add(entry);
    }

    /**
     * Get the list of entries
     * @return list of entries
     */
    List<TriggerConfigEntry> entries()
    {
        return entries;
    }

    /**
     * Get the configuration name.
     * @return name
     */
    String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name + "*" + entries.size();
    }
}

/**
 * Check that payload contents are valid.
 */
public abstract class PayloadChecker
{
    /** Log object. */
    private static final Logger LOG =
        Logger.getLogger(PayloadChecker.class);

    private static final int SMT_TYPE = -1;
    // = TriggerRegistry.getTriggerType("SimpleMajorityTrigger");

    /** Get the current year */
    private static final short YEAR =
        (short) (new GregorianCalendar()).get(GregorianCalendar.YEAR);

    /**
     * Should we ignore readout request elements which fall outside the
     * request bounds in non-global trigger request?
     */
    private static final boolean IGNORE_NONGLOBAL_RREQS = true;

    /** Trigger configuration data. */
    private static TriggerConfig triggerConfig;

    /** Has an expected run number been set? */
    private static boolean hasRunNumber;
    /** Expected run number */
    private static int runNumber;

    /**
     * Cannot create an instance of a utility class
     */
    private PayloadChecker()
    {
    }

    /**
     * Unset run number
     */
    public static void clearRunNumber()
    {
        hasRunNumber = false;
    }

    /**
     * Load trigger configuration data.
     *
     * @param configDir directory holding run configuration files
     * @param configName name of run configuration file
     */
    public static void configure(File configDir, String configName)
    {
        String trigCfgName = getTriggerConfigName(configDir, configName);

        File trigCfgDir = new File(configDir, "trigger");

        triggerConfig = loadTriggerConfig(trigCfgDir, trigCfgName);

        setTriggerNames();
    }

    private static void setTriggerNames()
    {
        int max = 0;
        for (TriggerConfigEntry entry : triggerConfig.entries()) {
            if (max < entry.getType()) {
                max = entry.getType();
            }
        }

        String[] typeNames = new String[max + 1];
        for (TriggerConfigEntry entry : triggerConfig.entries()) {
            typeNames[entry.getType()] = entry.getName();
        }

        icecube.daq.payload.impl.TriggerRequest.setTypeNames(typeNames);
    }

    /**
     * Get string representation of a DOM ID.
     *
     * @param dom DOM ID
     *
     * @return DOM ID string
     */
    private static String getDOMString(IDOMID dom)
    {
        if (dom == null || dom.longValue() == 0xffffffffffffffffL) {
            return "";
        }

        return dom.toString();
    }

    /**
     * Get string representation of an event.
     *
     * @param evt event
     *
     * @return event string
     */
    private static String getEventString(IEventPayload evt)
    {
        return "event #" + evt.getUID();
    }

    /**
     * Get string representation of a hit.
     *
     * @param hit hit
     *
     * @return hit string
     */
    private static String getHitString(IHitPayload hit)
    {
        return "hit@" + hit.getUTCTime();
    }

    /**
     * Get string representation of a readout data payload.
     *
     * @param rdp readout data payload
     *
     * @return readout data payload string
     */
    private static String getReadoutDataString(IReadoutDataPayload rdp)
    {
        return "RDP #" + rdp.getUID();
    }

    /**
     * Get string representation of a readout request element.
     *
     * @param elem readout request element
     *
     * @return readout request element string
     */
    private static String getRRElementString(IReadoutRequestElement elem)
    {
        String domStr = getDOMString(elem.getDOMID());
        String srcStr = getSourceString(elem.getSourceID());

        return "rrElem[" + getReadoutType(elem.getReadoutType()) +
            (domStr.length() == 0 ? "" : " dom " + domStr) +
            (srcStr.length() == 0 ? "" : " src " + srcStr) +
            "]";
    }

    /**
     * Get string representation of a readout request element.
     *
     * @param elem readout request element
     *
     * @return readout request element string
     */
    private static String getReadoutType(int rdoutType)
    {
        switch (rdoutType) {
        case IReadoutRequestElement.READOUT_TYPE_GLOBAL:
            return "global";
        case IReadoutRequestElement.READOUT_TYPE_II_GLOBAL:
            return "inIceGlobal";
        case IReadoutRequestElement.READOUT_TYPE_IT_GLOBAL:
            return "icetopGlobal";
        case IReadoutRequestElement.READOUT_TYPE_II_STRING:
            return "string";
        case IReadoutRequestElement.READOUT_TYPE_II_MODULE:
            return "dom";
        case IReadoutRequestElement.READOUT_TYPE_IT_MODULE:
            return "module";
        default:
            break;
        }

        return "unknownRdoutType#" + rdoutType;
    }

    /**
     * Get the threshold for this SimpleMajorityTrigger trigger request?
     *
     * @param tr trigger request
     *
     * @return threshold value
     */
    private static int getSMTThreshold(ITriggerRequestPayload tr)
    {
        loadPayload(tr);

        if (triggerConfig == null) {
            if (tr.getSourceID().getSourceID() ==
                SourceIdRegistry.INICE_TRIGGER_SOURCE_ID &&
                tr.getTriggerType() == SMT_TYPE)
            {
                return 8;
            }
        } else {
            for (TriggerConfigEntry cfg : triggerConfig.entries()) {
                if (cfg.getId() == tr.getTriggerConfigID() &&
                    cfg.getType() == tr.getTriggerType() &&
                    cfg.getSourceID() == tr.getSourceID().getSourceID() &&
                    cfg.getName().equals("SimpleMajorityTrigger"))
                {
                    Object obj = cfg.getParameter("threshold");
                    if (obj != null) {
                        return ((Long) obj).intValue();
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Get string representation of a source ID.
     *
     * @param src source ID
     *
     * @return source ID string
     */
    private static String getSourceString(int src)
    {
        return getSourceString(new SourceID(src));
    }

    /**
     * Get string representation of a source ID.
     *
     * @param src source ID
     *
     * @return source ID string
     */
    private static String getSourceString(ISourceID src)
    {
        if (src == null || src.getSourceID() < 0) {
            return "";
        }

        if (SourceIdRegistry.getDAQIdFromSourceID(src.getSourceID()) == 0) {
            return SourceIdRegistry.getDAQNameFromSourceID(src.getSourceID());
        }

        return src.toString();
    }

    private static List<IHitPayload> getTrigReqHits(ITriggerRequestPayload tr)
    {
        return getTrigReqHits(tr, new ArrayList<IHitPayload>());
    }

    private static List<IHitPayload> getTrigReqHits(ITriggerRequestPayload tr,
                                                    List<IHitPayload> hitList)
    {
        Collection<IPayload> payList;
        try {
            payList = tr.getPayloads();
        } catch (PayloadFormatException pfe) {
            LOG.error("Couldn't get list of payloads from " + tr, pfe);
            payList = null;
        }

        int numHits = 0;

        if (payList != null) {
            long trigStart = tr.getFirstTimeUTC().longValue();
            long trigFinish = tr.getLastTimeUTC().longValue();

            for (IPayload pay : payList) {
                if (pay instanceof ITriggerRequestPayload) {
                    getTrigReqHits((ITriggerRequestPayload) pay, hitList);
                } else if (pay instanceof IHitPayload) {
                    long hitTime = ((IHitPayload) pay).getUTCTime();
                    if (hitTime >= trigStart && hitTime <= trigFinish) {
                        hitList.add((IHitPayload) pay);
                        numHits++;
                    } else {
                        LOG.error("Trigger contains bogus hit " + pay);
                    }
                } else {
                    LOG.error("Unrecognized payload " + pay +
                              " in " + tr);
                }
            }
        }

        if (isSimpleMajorityTrigger(tr)) {
            final int threshold = getSMTThreshold(tr);

            if (numHits < threshold) {
                LOG.error(getTriggerRequestString(tr) + " contains " + numHits +
                          " hits, but should have at least " + threshold);
            }
        }

        return hitList;
    }

    /**
     * Read the trigger configuration file name from the run configuration file.
     *
     * @param configDir directory holding run configuration files
     * @param runConfig name of run configuration file
     *
     * @return name of trigger configuration file
     */
    public static String getTriggerConfigName(File configDir, String runConfig)
    {
        Document doc;
        try {
            doc = JAXPUtil.loadXMLDocument(configDir, runConfig);
        } catch (JAXPUtilException jux) {
            throw new Error(jux);
        }

        RunConfig runCfg = new RunConfig(runConfig, doc);
        return runCfg.getTriggerConfig();
    }

    /**
     * Get string representation of a trigger record.
     *
     * @param tr trigger record
     *
     * @return trigger record string
     */
    private static String getTriggerRecordString(IEventTriggerRecord tr)
    {
        return "trigRec[" + getSourceString(tr.getSourceID()) + "[" +
            tr.getFirstTime() + "-" + tr.getLastTime() + "]]";
    }

    /**
     * Get string representation of a trigger request.
     *
     * @param tr trigger request
     *
     * @return trigger request string
     */
    private static String getTriggerRequestString(ITriggerRequestPayload tr)
    {
        return "trigReq #" + tr.getUID() + "[" +
            getTriggerTypeString(tr.getTriggerType()) + "-" +
            tr.getTriggerConfigID() + "/" +
            getSourceString(tr.getSourceID()) +
            "]";
    }

    /** List of trigger types */
    private static String[] trigTypes = new String[] {
        "SimpMaj", "Calib", "MinBias", "Thruput", "FixedRt", "SyncBrd",
        "TrigBrd", "AmMFrag20", "AmVol", "AmM18", "AmM24", "AmStr",
        "AmSpase", "AmRand", "AmCalT0", "AmCalLaser",
    };

    /**
     * Get string representation of trigger type.
     *
     * @param trigType trigger type
     *
     * @return trigger type string
     */
    private static String getTriggerTypeString(int trigType)
    {
        if (triggerConfig != null) {
            for (TriggerConfigEntry cfg : triggerConfig.entries()) {
                if (cfg.isType(trigType)) {
                    return cfg.getName();
                }
            }
        } else if (trigType >= 0 && trigType < trigTypes.length) {
            return trigTypes[trigType];
        }

        return "unknownTrigType#" + trigType;
    }

    /**
     * Is the second pair of times contained within the first set of
     * times?
     *
     * @param descr0 description of first pair of times
     * @param first0 first time
     * @param last0 last time
     * @param descr1 description of second pair of times
     * @param first1 first time to be checked
     * @param last1 last time to be checked
     * @param verbose <tt>true</tt> if log message should be written when
     *                second pair of times is not within the first pair
     *
     * @return <tt>true</tt> if second pair of times are within the first pair
     */
    private static boolean isIntervalContained(String descr0, IUTCTime first0,
                                               IUTCTime last0, String descr1,
                                               IUTCTime first1, IUTCTime last1,
                                               boolean verbose)
    {
        boolean isPoint = first1.longValue() == last1.longValue();

        String intvl1;
        if (isPoint) {
            intvl1 = "value " + first1.longValue();
        } else {
            intvl1 = "interval [" + first1 + "-" + last1 + "]";
        }

        if (first0.longValue() > last0.longValue()) {
            LOG.error("Invalid " + descr0 + " interval [" + first0 + "-" +
                      last0 + "]");
            return false;
        } else if (first1.longValue() > last1.longValue()) {
            LOG.error("Invalid " + descr1 + " " + intvl1);
            return false;
        }

        if (verbose) {
            long firstDiff =
                first0.longValue() - first1.longValue();
            long lastDiff = last0.longValue() - last1.longValue();

            if (first1.longValue() > last0.longValue()) {
                LOG.error(descr0 + " interval [" + first0 + "-" + last0 +
                          "] is greater than " + descr1 + " " + intvl1 +
                          " diff [" + firstDiff + "-" + lastDiff + "]");
            } else if (last1.longValue() < first0.longValue()) {
                LOG.error(descr0 + " interval [" + first0 + "-" + last0 +
                          "] is less than " + descr1 + " " + intvl1 +
                          " diff [" + firstDiff + "-" + lastDiff + "]");
            } else if (first1.longValue() < first0.longValue()) {
                if (last1.longValue() >= last0.longValue()) {
                    LOG.error(descr0 + " interval [" + first0 + "-" + last0 +
                              "] is encapsulated by " + descr1 + " " + intvl1 +
                              " diff [" + firstDiff + "-" + lastDiff + "]");
                } else {
                    LOG.error(descr0 + " interval [" + first0 + "-" + last0 +
                              "] overlaps the lower end of " + descr1 +
                              " " + intvl1 + " diff [" + firstDiff + "-" +
                              lastDiff + "]");
                }
            } else if (last1.longValue() > last0.longValue()) {
                LOG.error(descr0 + " interval [" + first0 + "-" + last0 +
                          "] overlaps the upper end of " + descr1 +
                          " " + intvl1 + " diff [" + firstDiff + "-" +
                          lastDiff + "]");
            }
        }

        return first0.longValue() <= first1.longValue() &&
            last0.longValue() >= last1.longValue();
    }

    /**
     * Is this a SimpleMajorityTrigger trigger request?
     *
     * @param tr trigger request
     *
     * @return <tt>true</tt> if the trigger request is a SimpleMajorityTrigger
     */
    private static boolean isSimpleMajorityTrigger(ITriggerRequestPayload tr)
    {
        loadPayload(tr);

        if (triggerConfig == null) {
            return (tr.getSourceID().getSourceID() ==
                    SourceIdRegistry.INICE_TRIGGER_SOURCE_ID &&
                    tr.getTriggerType() == SMT_TYPE);
        }

        for (TriggerConfigEntry cfg : triggerConfig.entries()) {
            if (cfg.getId() == tr.getTriggerConfigID() &&
                cfg.getType() == tr.getTriggerType() &&
                cfg.getSourceID() == tr.getSourceID().getSourceID() &&
                cfg.getName().equals("SimpleMajorityTrigger"))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Are the source IDs equal?
     *
     * @param firstSrc first Source ID
     * @param secondSrc second Source ID
     *
     * @return <tt>true</tt> if the Source IDs are equal
     */
    private static boolean isSourceEqual(String descr0, ISourceID src0,
                                         String descr1, ISourceID src1,
                                         boolean verbose)
    {
        final boolean isEqual = (src0 == null && src1 == null) ||
            (src0 != null && src1 != null && src0.equals(src1));

        if (!isEqual && verbose) {
            LOG.error(descr0 + " source " + src0 +
                      " is not equal to " + descr1 + " source " + src1);
        }

        return isEqual;
    }

    /**
     * Load the payload data.
     *
     * @param pay payload
     *
     * @return <tt>false</tt> if payload could not be loaded
     */
    private static boolean loadPayload(IPayload pay)
    {
        try {
            pay.loadPayload();
        } catch (IOException ioe) {
            LOG.error("Couldn't load payload", ioe);
            return false;
        } catch (PayloadFormatException pfe) {
            LOG.error("Couldn't load payload", pfe);
            return false;
        }

        return true;
    }

    /**
     * Read individual trigger configuration entries
     * from trigger configuration file.
     *
     * @param trigCfgDir directory holding trigger configuration files
     * @param trigConfig name of trigger configuration file
     *
     * @return configuration entries
     */
    public static TriggerConfig loadTriggerConfig(File trigCfgDir,
                                                  String trigConfig)
    {
        Document doc;
        try {
            doc = JAXPUtil.loadXMLDocument(trigCfgDir, trigConfig);
        } catch (JAXPUtilException jux) {
            throw new Error(jux);
        }

        NodeList cfgNodes;
        try {
            cfgNodes =
                JAXPUtil.extractNodeList(doc, "activeTriggers/triggerConfig");
        } catch (JAXPUtilException jux) {
            throw new Error(jux);
        }

        TriggerConfig tmpConfig = new TriggerConfig(trigConfig);

        for (int i = 0; i < cfgNodes.getLength(); i++) {
            tmpConfig.add(new TriggerConfigEntry((Element) cfgNodes.item(i)));
        }

        return tmpConfig;
    }

    /**
     * Set run number.
     *
     * @param runNum run number
     */
    public static void setRunNumber(int runNum)
    {
        hasRunNumber = true;
        runNumber = runNum;
    }

    private static String toHexString(ByteBuffer bb)
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bb.limit(); i++) {
            String str = Integer.toHexString(bb.get(i));
            buf.append("(byte)0x");
            if (str.length() < 2) {
                buf.append('0').append(str);
            } else if (str.length() > 2) {
                buf.append(str.substring(str.length() - 2));
            } else {
                buf.append(str);
            }
            buf.append(", ");
        }

        // lose trailing whitespace
        while (buf.length() > 0 && buf.charAt(buf.length() - 1) == ' ') {
            buf.setLength(buf.length() - 1);
        }

        return buf.toString();
    }

    /**
     * Return a Java definition of the byte array (named <tt>payBytes</tt>
     * which describes the payload.
     * @param pay payload to dump
     * @return string dump of payload bytes
     */
    public static String dumpPayloadBytes(IPayload pay)
    {
        return dumpPayloadBytes(pay, "payBytes");
    }

    /**
     * Return a Java definition of the byte array which describes the payload.
     * @param pay payload to dump
     * @param name name  of byte array
     * @return string dump of payload bytes
     */
    public static String dumpPayloadBytes(IPayload pay, String name)
    {
        ByteBuffer buf = ByteBuffer.allocate(pay.length());
        try {
            pay.writePayload(false, 0, buf);
        } catch (IOException ioe) {
            System.err.println("Couldn't dump payload " + pay);
            ioe.printStackTrace();
            buf = null;
        }
        if (buf == null) {
            return null;
        }

        return "byte[] " + name + " = new byte[] { " + toHexString(buf) + " };";
    }

    /**
     * Return a short description of the payload.
     *
     * @param pay payload
     *
     * @return short description string
     */
    public static String toString(IPayload pay)
    {
        if (pay instanceof IEventPayload) {
            return getEventString((IEventPayload) pay);
        } else if (pay instanceof IHitPayload) {
            return getHitString((IHitPayload) pay);
        } else if (pay instanceof IReadoutDataPayload) {
            return getReadoutDataString((IReadoutDataPayload) pay);
        } else if (pay instanceof ITriggerRequestPayload) {
            return getTriggerRequestString((ITriggerRequestPayload) pay);
        } else {
            return pay.toString();
        }
    }

    /**
     * Validate all subcomponents of an event payload.
     *
     * @param evt event
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if event is valid
     */
    public static boolean validateEvent(IEventPayload evt, boolean verbose)
    {
        loadPayload(evt);

        String evtDesc = getEventString(evt);
        if (!validateInterval(evtDesc, evt.getFirstTimeUTC(),
                              evt.getLastTimeUTC(), verbose))
        {
            return false;
        }

        if (!validateEventYear(evt.getYear(), verbose)) {
            return false;
        }

        boolean valid;
        if (evt.getEventVersion() >= 5) {
            valid = validateEventRecords(evt, evtDesc, verbose);
        } else {
            valid = validateEventTrigReqAndHits(evt, evtDesc, verbose);
        }

        if (valid && hasRunNumber) {
            valid |= evt.getRunNumber() == runNumber;
        }

        return valid;
    }

    private static boolean validateEventRdoutData(IReadoutDataPayload rdp,
                                                  String trDesc,
                                                  IUTCTime trFirst,
                                                  IUTCTime trLast,
                                                  boolean verbose)
    {
        String rdpDesc = getReadoutDataString(rdp);
        IUTCTime rdpFirst = rdp.getFirstTimeUTC();
        IUTCTime rdpLast = rdp.getLastTimeUTC();

        if (!validateInterval(rdpDesc, rdpFirst, rdpLast, verbose)) {
            return false;
        }

        if (!isIntervalContained(trDesc, trFirst, trLast,
                                 rdpDesc, rdpFirst, rdpLast, verbose))
        {
            return false;
        }

        return validateReadoutDataPayload(rdp, verbose);
    }

    private static boolean validateEventRecords(IEventPayload evt,
                                                String evtDesc, boolean verbose)
    {
        for (IEventTriggerRecord trigRec : evt.getTriggerRecords()) {
            String trDesc = getTriggerRecordString(trigRec);
            IUTCTime trFirst = new UTCTime(trigRec.getFirstTime());
            IUTCTime trLast = new UTCTime(trigRec.getLastTime());
            if (!validateInterval(trDesc, trFirst, trLast, verbose)) {
                return false;
            }

            if (!isIntervalContained(evtDesc, evt.getFirstTimeUTC(),
                                     evt.getLastTimeUTC(),
                                     trDesc, trFirst, trLast, verbose))
            {
                return false;
            }

        }

        final long evtFirst = evt.getFirstTimeUTC().longValue();
        final long evtLast = evt.getLastTimeUTC().longValue();

        for (IEventHitRecord hitRec : evt.getHitRecords()) {
            final long hitTime = hitRec.getHitTime();
            if (hitTime < evtFirst || hitTime > evtLast) {
                LOG.error(evtDesc + " interval [" + evtFirst + "-" + evtLast +
                          " does not contain hit@" + hitTime);
                return false;
            }
        }

        return true;
    }

    private enum Container { UNKNOWN, READOUT_DATA, HIT_REC_LIST };

    private static boolean validateEventTrigReqAndHits(IEventPayload evt,
                                                       String evtDesc,
                                                       boolean verbose)
    {
        ITriggerRequestPayload trigReq = evt.getTriggerRequestPayload();
        loadPayload(trigReq);

        String trDesc = getTriggerRequestString(trigReq);
        IUTCTime trFirst = trigReq.getFirstTimeUTC();
        IUTCTime trLast = trigReq.getLastTimeUTC();
        if (!validateInterval(trDesc, trFirst, trLast, verbose)) {
            return false;
        }

        if (!isIntervalContained(evtDesc, evt.getFirstTimeUTC(),
                                 evt.getLastTimeUTC(),
                                 trDesc, trFirst, trLast, verbose))
        {
            return false;
        }

        if (!validateTriggerRequest(trigReq, verbose)) {
            return false;
        }

        List<IHitPayload> trigHits = getTrigReqHits(trigReq);

        EnumSet<Container> container = EnumSet.noneOf(Container.class);

        ArrayList<IHitDataPayload> evtHits = new ArrayList<IHitDataPayload>();
        for (Object obj : evt.getReadoutDataPayloads()) {
            if (obj instanceof IReadoutDataPayload) {
                IReadoutDataPayload rdp = (IReadoutDataPayload) obj;
                loadPayload(rdp);

                if (!validateEventRdoutData(rdp, trDesc, trFirst, trLast,
                                            verbose))
                {
                    return false;
                }

                List<IHitData> rdpHits = rdp.getHitList();
                if (rdpHits != null) {
                    for (IHitData rhit : rdpHits) {
                        evtHits.add((IHitDataPayload) rhit);
                    }
                }

                container.add(Container.READOUT_DATA);
            } else if (obj instanceof IHitRecordList) {
                IHitRecordList recList = (IHitRecordList) obj;

                LOG.error("Event #" + evt.getUID() + " contains" +
                          " hit record list #" + recList.getUID() +
                          " instead of readout data payload");
                return false;
            } else {
                LOG.error("Not validating " + obj);

                container.add(Container.UNKNOWN);
            }
        }

        boolean valid = true;
        for (IHitPayload tHit : trigHits) {
            boolean found = false;
            for (IHitDataPayload eHit : evtHits) {
                if (tHit.getDOMID().equals(eHit.getDOMID()) &&
                    tHit.getPayloadTimeUTC().equals(eHit.getPayloadTimeUTC()))
                {
                    found = true;
                    break;
                }
            }

            if (!found) {
                StringBuilder hBuf = new StringBuilder();
                for (IHitDataPayload eHit : evtHits) {
                    if (hBuf.length() == 0) {
                        hBuf.append('[');
                    } else {
                        hBuf.append(' ');
                    }
                    hBuf.append(eHit.toString());
                }
                hBuf.append(']');

                StringBuilder csBuf = new StringBuilder();
                if (container.contains(Container.READOUT_DATA)) {
                    if (csBuf.length() > 0) {
                        csBuf.append('|');
                    }
                    csBuf.append("readout data");
                }
                if (container.contains(Container.HIT_REC_LIST)) {
                    if (csBuf.length() > 0) {
                        csBuf.append('|');
                    }
                    csBuf.append("hit record list");
                }
                if (container.contains(Container.UNKNOWN)) {
                    if (csBuf.length() > 0) {
                        csBuf.append('|');
                    }
                    csBuf.append("??unknown object??");
                }

                LOG.error("Couldn't find trigger hit " + tHit +
                          " in " + csBuf.toString() +
                          " hits " + hBuf.toString());
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Validate the year value for this event.
     *
     * @param evtYear year value for this event
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if event year is valid
     */
    public static boolean validateEventYear(short evtYear, boolean verbose)
    {
        if (evtYear != YEAR && evtYear != -1) {
            if (verbose) {
                LOG.error("Expected event year to be " + YEAR +
                          " (or -1), not " + evtYear);
            }

            return false;
        }

        return true;
    }

    /**
     * Ensure that interval times are non-null and that first time is not
     * greater than second time.
     *
     * @param descr description of payload which contains the interval
     * @param first first time
     * @param last last time
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if interval is valid
     */
    private static boolean validateInterval(String descr, IUTCTime first,
                                            IUTCTime last, boolean verbose)
    {
        if (first == null || last == null) {
            if (verbose) {
                LOG.error("Cannot get interval for " + descr);
            }

            return false;
        }

        if (first.longValue() > last.longValue()) {
            if (verbose) {
                LOG.error("Bad " + descr + " interval [" + first + "-" + last +
                          "]");
            }

            return false;
        }

        return true;
    }

    /**
     * Validate all subcomponents of a payload.
     *
     * @param pay payload
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if payload is valid
     */
    public static boolean validatePayload(IPayload pay, boolean verbose)
    {
        boolean rtnVal;

        if (pay == null) {
            rtnVal = false;
        } else if (pay instanceof IEventPayload) {
            rtnVal = validateEvent((IEventPayload) pay, verbose);
        } else if (pay instanceof ITriggerRequestPayload) {
            rtnVal = validateTriggerRequest((ITriggerRequestPayload) pay,
                                            verbose);
        } else if (pay instanceof IReadoutDataPayload) {
            rtnVal = validateReadoutDataPayload((IReadoutDataPayload) pay,
                                                verbose);
        } else if (pay instanceof IHitPayload) {
            // hits have nothing to validate
            rtnVal = true;
        } else if (pay instanceof IHitRecordList) {
            // XXX fill this in!
            LOG.error("Not validating IHitRecordList objects");
            rtnVal = true;
        } else {
            LOG.error("Unknown payload type " + pay.getClass().getName());
            rtnVal = false;
        }

        return rtnVal;
    }

    /**
     * Validate all subcomponents of a readout data payload.
     *
     * @param rdp readout data payload
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if readout data payload is valid
     */
    public static boolean validateReadoutDataPayload(IReadoutDataPayload rdp,
                                                     boolean verbose)
    {
        String rdpDesc = getReadoutDataString(rdp);
        int rdpCfg = rdp.getTriggerConfigID();
        ISourceID rdpSrc = rdp.getSourceID();
        IUTCTime rdpFirst = rdp.getFirstTimeUTC();
        IUTCTime rdpLast = rdp.getLastTimeUTC();

        List dataList = rdp.getDataPayloads();
        if (dataList != null) {
            for (Object obj : dataList) {
                IHitDataPayload hit = (IHitDataPayload) obj;
                loadPayload(hit);

                ISourceID hitSrc = hit.getSourceID();
                String hitDesc = getHitString(hit);
                IUTCTime time = hit.getPayloadTimeUTC();

                if (!isIntervalContained(rdpDesc, rdpFirst, rdpLast,
                                         hitDesc, time, time, verbose))
                {
                    return false;
                }

                if (!isSourceEqual(rdpDesc, rdpSrc, hitDesc, hitSrc, verbose)) {
                    return false;
                }

                if (rdpCfg != -1) {
                    if (verbose) {
                        LOG.error(rdpDesc + " config ID " + rdpCfg +
                                  " should be -1");
                    }

                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validate all subcomponents of a trigger request.
     *
     * @param tr trigger request
     * @param verbose <tt>true</tt> if errors should be logged
     *
     * @return <tt>true</tt> if trigger request is valid
     */
    public static boolean validateTriggerRequest(ITriggerRequestPayload tr,
                                                 boolean verbose)
    {
        loadPayload(tr);

        String trDesc = getTriggerRequestString(tr);
        IUTCTime trFirst = tr.getFirstTimeUTC();
        IUTCTime trLast = tr.getLastTimeUTC();
        if (!validateInterval(trDesc, trFirst, trLast, verbose)) {
            return false;
        }

        IReadoutRequest rReq = tr.getReadoutRequest();

        if (rReq != null) {
            List elemList = rReq.getReadoutRequestElements();

            for (Object obj : elemList) {
                IReadoutRequestElement elem = (IReadoutRequestElement) obj;

                String elemDesc = getRRElementString(elem);

                IUTCTime elemFirst = elem.getFirstTimeUTC();
                IUTCTime elemLast = elem.getLastTimeUTC();
                if (!validateInterval(elemDesc, elemFirst, elemLast, verbose)) {
                    return false;
                }

                /* XXX - only validate readout requests for global triggers */
                if ((!IGNORE_NONGLOBAL_RREQS ||
                     tr.getSourceID().getSourceID() ==
                     SourceIdRegistry.GLOBAL_TRIGGER_SOURCE_ID) &&
                    !isIntervalContained(trDesc, trFirst, trLast, elemDesc,
                                         elemFirst, elemLast, verbose))
                {
                    return false;
                }
            }
        }

        Collection<IPayload> payList;
        try {
            payList = tr.getPayloads();
        } catch (PayloadFormatException pfe) {
            LOG.error("Couldn't fetch payloads for " + trDesc, pfe);
            return false;
        }

        if (payList != null) {
            for (IPayload pay : payList) {
                if (pay instanceof ITriggerRequestPayload) {
                    ITriggerRequestPayload subTR = (ITriggerRequestPayload) pay;
                    loadPayload(subTR);

                    String subDesc = getTriggerRequestString(subTR);
                    IUTCTime subFirst = subTR.getFirstTimeUTC();
                    IUTCTime subLast = subTR.getLastTimeUTC();
                    if (!validateInterval(subDesc, subFirst, subLast,
                                          verbose))
                    {
                        return false;
                    }

                    if (!isIntervalContained(trDesc, trFirst, trLast, subDesc,
                                             subFirst, subLast, verbose))
                    {
                        return false;
                    }

                    if (!validateTriggerRequest(subTR, verbose)) {
                        return false;
                    }
                } else if (pay instanceof IHitPayload) {
                    IHitPayload hit = (IHitPayload) pay;
                    loadPayload(hit);

                    IUTCTime time = hit.getPayloadTimeUTC();
                    String hitDesc = "hit@" + time;

                    if (!isIntervalContained(trDesc, trFirst, trLast,
                                             hitDesc, time, time, verbose))
                    {
                        return false;
                    }
                } else {
                    LOG.error("Unknown payload type " +
                              pay.getClass().getName() + " in " + trDesc);
                    return false;
                }
            }
        }

        return true;
    }
}
