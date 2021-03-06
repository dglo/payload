package icecube.daq.payload.impl;

import icecube.daq.payload.IManagedObject;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutRequest;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadException;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.splicer.Spliceable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Trigger request
 */
public class TriggerRequest
    extends BasePayload
    implements ITriggerRequestPayload, Spliceable
{
    /** Internal trigger request record type */
    public static final short RECORD_TYPE = 4;

    /** OFfset of record type field */
    private static final int OFFSET_RECTYPE = 0;
    /** Offset of unique ID field */
    private static final int OFFSET_UID = 2;
    /** Offset of trigger type field */
    private static final int OFFSET_TRIGTYPE = 6;
    /** Offset of trigger configuration ID field */
    private static final int OFFSET_CONFIGID = 10;
    /** Offset of source ID field */
    private static final int OFFSET_SOURCEID = 14;
    /** Offset of starting time field */
    private static final int OFFSET_FIRSTTIME = 18;
    /** Offset of ending time field */
    private static final int OFFSET_LASTTIME = 26;

    /** Offset of readout request field */
    private static final int OFFSET_RDOUTREQ = 34;

    /** Offset of composite length (within composite data section) */
    private static final int OFFSET_COMPLEN = 0;
    /** Offset of composite type (within composite data section) */
    private static final int OFFSET_COMPTYPE = 4;
    /** Offset of number of composites (within composite data section) */
    private static final int OFFSET_COMPNUM = 6;
    /** Offset of composite data (within composite data section) */
    public static final int OFFSET_COMPDATA = 8;

    /** Length of composite header */
    public static final int LEN_COMPOSITE_HEADER = 8;

    /** list of trigger names */
    private static String[] TYPE_NAMES = new String[] {
        "SMT", "Calib", "MinBias", "Thruput", "Cluster", "SyncBrd",
        "TrigBrd", "AmMFrag20", "AmVol", "AmM18", "AmM24", "AmStr", "AmRand",
        "PhysMBT", "Cluster", "Volume", "??Trig16??", "MplcityStr",
        "??Trig18??", "??Trig19??", "Volume", "Cylinder", "SlowMP",
        "FixedRate", "SlowMP", "??Trig25??", "??Trig26??", "??Trig27??",
        "??Trig28??", "??Trig29??", "FixedRate"
    };

    /** unique ID */
    private int uid;
    /** trigger type */
    private int trigType;
    /** configuration ID */
    private int cfgId;
    /** component source ID */
    private int srcId;
    /** starting time */
    private long firstTime;
    /** ending time */
    private long lastTime;

    /** readout request */
    private IReadoutRequest rdoutReq;

    /** list of composite payloads */
    private Collection<IPayload> compList;

    /** cached source ID object */
    private SourceID srcObj;
    /** cached starting time object */
    private UTCTime firstTimeObj;
    /** cached ending time object */
    private UTCTime lastTimeObj;

    /**
     * Create a trigger request
     * @param buf byte buffer
     * @param offset index of first byte
     * @throws PayloadException if there is a problem
     */
    public TriggerRequest(ByteBuffer buf, int offset)
        throws PayloadException
    {
        super(buf, offset);
    }

    /**
     * Constructor for PayloadFactory.
     * @param buf byte buffer
     * @param offset index of first byte
     * @param len total number of bytes
     * @param utcTime payload time (UTC)
     * @throws PayloadException if there is a problem
     */
    TriggerRequest(ByteBuffer buf, int offset, int len, long utcTime)
        throws PayloadException
    {
        super(buf, offset, len, utcTime);
    }

    /**
     * Create a trigger request
     * @param uid unique ID
     * @param trigType trigger type
     * @param cfgId trigger configuration ID
     * @param srcId source ID
     * @param firstTime starting time
     * @param lastTime ending time
     * @param rdoutReq readout request
     * @param compList composite payload list
     */
    public TriggerRequest(int uid, int trigType, int cfgId, int srcId,
                          long firstTime, long lastTime,
                          IReadoutRequest rdoutReq,
                          Collection<IPayload> compList)
    {
        super(firstTime);

        if (firstTime > lastTime) {
            throw new Error("Illegal time interval [" + firstTime + "-" +
                            lastTime + "]");
        } else if (rdoutReq == null) {
            throw new Error("Readout request cannot be null");
        }

        this.uid = uid;
        this.trigType = trigType;
        this.cfgId = cfgId;
        this.srcId = srcId;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.rdoutReq = rdoutReq;
        if (compList == null) {
            this.compList = new ArrayList<IPayload>();
        } else {
            this.compList = new ArrayList<IPayload>(compList);
        }
    }

    /**
     * Compare two payloads for the splicer.
     * NOTE: Make sure all compared fields have been loaded by
     * preloadSpliceableFields()
     * @param spliceable object being compared
     * @return -1, 0, or 1
     */
    @Override
    public int compareSpliceable(Spliceable spliceable)
    {
        if (!(spliceable instanceof IPayload)) {
            final String className = spliceable.getClass().getName();
            return getClass().getName().compareTo(className);
        }

        long payTime = ((IPayload) spliceable).getUTCTime();
        if (firstTime < payTime) {
            return -1;
        } else if (firstTime > payTime) {
            return 1;
        }

        return 0;
    }

    /**
     * Compute the number of bytes needed to save this payload to a byte buffer
     * @return number of bytes
     */
    @Override
    public int computeBufferLength()
    {
        if (!isLoaded()) {
            throw new Error(getPayloadName() + " has not been loaded");
        }

        int bufLen = OFFSET_RDOUTREQ + rdoutReq.length() +
            LEN_COMPOSITE_HEADER;

        for (IPayload comp : compList) {
            bufLen += comp.length();
        }

        return bufLen;
    }

    /**
     * Return a copy of this object.
     * @return copied object
     */
    @Override
    public Object deepCopy()
    {
        if (!isLoaded()) {
            throw new Error(getPayloadName() + " has not been loaded");
        }

        if (rdoutReq == null) {
            throw new Error("Cannot deepCopy " + toString() +
                            "; readout request is null");
        }

        IReadoutRequest newRReq =
            (IReadoutRequest) ((IPayload) rdoutReq).deepCopy();

        Collection<IPayload> newList = new ArrayList<IPayload>();
        for (IPayload comp : compList) {
            newList.add((IPayload) comp.deepCopy());
        }

        return new TriggerRequest(uid, trigType, cfgId, srcId, firstTime,
                                  lastTime, newRReq, newList);
    }

    /**
     * Get request starting time
     * @return starting time object
     */
    @Override
    public IUTCTime getFirstTimeUTC()
    {
        if (firstTimeObj == null) {
            firstTimeObj = new UTCTime(firstTime);
        }

        return firstTimeObj;
    }

    /**
     * Get request ending time
     * @return ending time object
     */
    @Override
    public IUTCTime getLastTimeUTC()
    {
        if (lastTimeObj == null) {
            lastTimeObj = new UTCTime(lastTime);
        }

        return lastTimeObj;
    }

    /**
     * Unimplemented
     * @return Error
     */
    public int getNumData()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get the name of this payload.
     * @return name
     */
    @Override
    public String getPayloadName()
    {
        return "TriggerRequest";
    }

    /**
     * Get the payload registry type
     * @return type
     */
    @Override
    public int getPayloadType()
    {
        return PayloadRegistry.PAYLOAD_ID_TRIGGER_REQUEST;
    }

    /**
     * Get the list of composite payloads included in this trigger request
     * @return list of payloads
     */
    @Override
    public Collection<IPayload> getPayloads()
    {
        return compList;
    }

    /**
     * Get the readout request associated with this trigger request
     * @return readout request
     */
    @Override
    public IReadoutRequest getReadoutRequest()
    {
        return rdoutReq;
    }

    /**
     * Get the source ID
     * @return source ID object
     */
    @Override
    public ISourceID getSourceID()
    {
        if (srcObj == null) {
            srcObj = new SourceID(srcId);
        }

        return srcObj;
    }

    /**
     * Get the trigger configuration ID
     * @return trigger config ID
     */
    @Override
    public int getTriggerConfigID()
    {
        return cfgId;
    }

    /**
     * Get the trigger type
     * @return trigger type
     */
    @Override
    public int getTriggerType()
    {
        return trigType;
    }

    /**
     * Get the trigger name for the specified trigger type.
     *
     * @return trigger name
     */
    public static String getTriggerName(int type, int cfgId, int uid)
    {
        if (type == -1 && cfgId == -1 && uid >= 0) {
            return "Merged";
        }

        if (TYPE_NAMES == null || type < 0 || type >= TYPE_NAMES.length ||
            TYPE_NAMES[type] == null)
        {
            return "#" + type;
        }

        return TYPE_NAMES[type];
    }

    /**
     * Get the trigger name for the trigger type.
     *
     * @return trigger name
     */
    @Override
    public String getTriggerName()
    {
        return getTriggerName(trigType, cfgId, uid);
    }

    /**
     * Get the trigger name for the specified trigger type.
     *
     * @return trigger name
     */
    public static int getTriggerType(String name)
    {
        if (name != null) {
            if (name.equals("Merged")) {
                return -1;
            }

            if (TYPE_NAMES != null) {
                for (int i = 0; i < TYPE_NAMES.length; i++) {
                    if (name.equals(TYPE_NAMES[i])) {
                        return i;
                    }
                }
            }
        }

        return Integer.MIN_VALUE;
    }

    /**
     * Get the unique ID
     * @return unique ID
     */
    @Override
    public int getUID()
    {
        return uid;
    }

    /**
     * Is this a merged request?
     *
     * @return <tt>true</tt> if this is a merged request
     */
    @Override
    public boolean isMerged()
    {
        return trigType == -1;
    }

    /**
     * Load the payload data
     * @param buf byte buffer
     * @param offset index of first byte
     * @param utcTime payload time
     * @param isEmbedded <tt>true</tt> if this payload is embedded in another
     * @return number of bytes loaded
     * @throws PayloadException if there is a problem
     */
    @Override
    public int loadBody(ByteBuffer buf, int offset, long utcTime,
                        boolean isEmbedded)
        throws PayloadException
    {
        int pos;
        if (isEmbedded) {
            pos = offset;
        } else {
            pos = offset + LEN_PAYLOAD_HEADER;
        }

        final short recType = buf.getShort(pos + OFFSET_RECTYPE);
        if (recType != RECORD_TYPE) {
            throw new PayloadException("Record type should be " + RECORD_TYPE +
                                       ", not " + recType);
        }

        uid = buf.getInt(pos + OFFSET_UID);
        trigType = buf.getInt(pos + OFFSET_TRIGTYPE);
        cfgId = buf.getInt(pos + OFFSET_CONFIGID);
        srcId = buf.getInt(pos + OFFSET_SOURCEID);
        firstTime = buf.getLong(pos + OFFSET_FIRSTTIME);
        lastTime = buf.getLong(pos + OFFSET_LASTTIME);

        if (firstTime != utcTime) {
            throw new PayloadException("Trigger request first time " +
                                       firstTime +
                                       " differs from header time " + utcTime);
        }

        rdoutReq = new ReadoutRequest(buf, pos + OFFSET_RDOUTREQ, firstTime);
        try {
            ((IPayload) rdoutReq).loadPayload();
        } catch (IOException ioe) {
            throw new PayloadException("Cannot load readout request", ioe);
        }

        int rrPos = pos + OFFSET_RDOUTREQ + rdoutReq.getEmbeddedLength();

        final int compLen = buf.getInt(rrPos + OFFSET_COMPLEN);
/*
        if (compLen != len - ((rrPos + OFFSET_COMPLEN) - offset)) {
            throw new PayloadException("Composite length should be " +
                                       (len - ((rrPos + OFFSET_COMPLEN) -
                                               offset)) + ", not " + compLen);
        }
*/

        final int numComp = (int) buf.getShort(rrPos + OFFSET_COMPNUM);

        compList = new ArrayList<IPayload>();

        int loadedBytes = loadCompositeData(buf, rrPos + OFFSET_COMPLEN +
                                            LEN_COMPOSITE_HEADER, numComp);

        if (compLen != LEN_COMPOSITE_HEADER + loadedBytes) {
            throw new PayloadException("Composite should contain " + compLen +
                                       " bytes, but " +
                                       (LEN_COMPOSITE_HEADER + loadedBytes) +
                                       " were read");
        }

        srcObj = null;
        firstTimeObj = null;
        lastTimeObj = null;

        return (rrPos - pos) + LEN_COMPOSITE_HEADER + loadedBytes;
    }

    /**
     * Load this payload's composite data
     * @param buf byte buffer
     * @param offset index of first byte
     * @param numData number of composite data payloads
     * @param compList list in which new payloads are stored
     * @return number of bytes loaded
     * @throws PayloadException if there is a problem
     */
    private int loadCompositeData(ByteBuffer buf, int offset, int numData)
        throws PayloadException
    {
        int totLen = 0;

        PayloadFactory factory = getPayloadFactory();
        for (int i = 0; i < numData; i++) {
            IPayload pay = factory.getPayload(buf, offset + totLen);
            try {
                ((IPayload) pay).loadPayload();
            } catch (IOException ioe) {
                throw new PayloadException("Cannot load composite#" + i, ioe);
            }
            compList.add(pay);
            totLen += pay.length();
        }

        return totLen;
    }

    /**
     * Preload any essential fields so splicer can sort unloaded payloads.
     * @param buf byte buffer
     * @param offset index of first byte
     * @param len total number of bytes
     * @throws PayloadException if the essential fields cannot be preloaded
     */
    @Override
    public void preloadSpliceableFields(ByteBuffer buf, int offset, int len)
        throws PayloadException
    {
        if (isLoaded()) {
            return;
        }

        // make sure we can load the field(s) needed in compareSpliceable()
        final int bodyOffset;
        if (offset == 0) {
            bodyOffset = OFFSET_PAYLOAD;
        } else {
            bodyOffset = 0;
        }

        if (bodyOffset + OFFSET_LASTTIME + 8 > len) {
            throw new PayloadException("Cannot load field at offset " +
                                       (bodyOffset + OFFSET_LASTTIME) +
                                       " from " + len + "-byte buffer");
        }

        uid = buf.getInt(offset + bodyOffset + OFFSET_UID);
        trigType = buf.getInt(offset + bodyOffset + OFFSET_TRIGTYPE);
        firstTime = buf.getLong(offset + bodyOffset + OFFSET_FIRSTTIME);
        lastTime = buf.getLong(offset + bodyOffset + OFFSET_LASTTIME);
    }

    /**
     * Write this payload's data to the byte buffer
     * @param buf byte buffer
     * @param offset index of first byte
     * @return number of bytes written
     * @throws PayloadException if there is a problem
     */
    @Override
    public int putBody(ByteBuffer buf, int offset)
        throws PayloadException
    {
        // trigger request
        buf.putShort(offset + OFFSET_RECTYPE, RECORD_TYPE);
        buf.putInt(offset + OFFSET_UID, uid);
        buf.putInt(offset + OFFSET_TRIGTYPE, trigType);
        buf.putInt(offset + OFFSET_CONFIGID, cfgId);
        buf.putInt(offset + OFFSET_SOURCEID, srcId);
        buf.putLong(offset + OFFSET_FIRSTTIME, firstTime);
        buf.putLong(offset + OFFSET_LASTTIME, lastTime);

        final int expRRLen = rdoutReq.length() - LEN_PAYLOAD_HEADER;

        int rrLen = rdoutReq.putBody(buf, offset + OFFSET_RDOUTREQ);
        if (rrLen != expRRLen) {
            throw new PayloadException("Readout request body length should " +
                                       "be " + expRRLen + " bytes, not " +
                                       rrLen);
        }

        int pos = offset + OFFSET_RDOUTREQ + rrLen;

        int expPos = OFFSET_RDOUTREQ + rrLen;
        if (pos - offset != expPos) {
            throw new PayloadException("Expected composite data to start " +
                                       "at byte " + expPos + ", not " +
                                       (pos - offset));
        }

        // composite header
        // length is filled in below
        buf.putInt(pos + OFFSET_COMPLEN, -1);
         // composite type is deprecated
        buf.putShort(pos + OFFSET_COMPTYPE, (short) 1);
        buf.putShort(pos + OFFSET_COMPNUM, (short) compList.size());

        int totLen = 0;
        for (IPayload pay : compList) {
            final int expLen = pay.length();

            int len;
            try {
                len = pay.writePayload(false, pos + LEN_COMPOSITE_HEADER +
                                       totLen, buf);
            } catch (IOException ioe) {
                throw new PayloadException("Cannot write payload " + pay, ioe);
            }

            if (len != expLen) {
                throw new PayloadException("Expected to write " + expLen +
                                           " bytes, but wrote " + len);
            }
            totLen += len;
        }

        buf.putInt(pos + OFFSET_COMPLEN, LEN_COMPOSITE_HEADER + totLen);

        return (pos + LEN_COMPOSITE_HEADER + totLen) - offset;
    }

    /**
     * Clear out any cached data.
     */
    @Override
    public void recycle()
    {
        super.recycle();

        uid = -1;
        trigType = -1;
        cfgId = -1;
        srcId = -1;
        firstTime = -1L;
        lastTime = -1L;

        if (rdoutReq != null) {
            ((IManagedObject) rdoutReq).recycle();
            rdoutReq = null;
        }

        if (compList != null) {
            for (IPayload pay : compList) {
                pay.recycle();
            }
            compList = null;
        }

        srcObj = null;
        firstTimeObj = null;
        lastTimeObj = null;
    }

    public static void setTypeNames(String[] names)
    {
        TYPE_NAMES = names;
    }

    /**
     * Set the universal ID for global requests which will become events.
     *
     * @param uid new UID
     */
    @Override
    public void setUID(int uid)
    {
        if (rdoutReq == null) {
            throw new Error("Cannot set UID for " + toString() +
                            "; readout request is null");
        }

        this.uid = uid;
        rdoutReq.setUID(uid);
    }

    /**
     * Unimplemented
     * @param buf ignored
     * @param offset ignored
     * @return Error
     * @throws PayloadException if there is a problem
     */
    public int writeDataBytes(ByteBuffer buf, int offset)
        throws PayloadException
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get a debugging string representing this object.
     * @return debugging string
     */
    @Override
    public String toString()
    {
        String szStr;
        if (compList == null) {
            szStr = "null";
        } else {
            szStr = Integer.toString(compList.size());
        }

        return "TriggerRequest[" + getSourceID() + " " + getTriggerName() +
            "-" + cfgId + " uid " + uid + " [" + firstTime + "-" + lastTime +
            "] rReq " + rdoutReq + " composites*" + szStr + "]";
    }
}
