package icecube.daq.payload.impl;

import icecube.daq.payload.IEventHitRecord;
import icecube.daq.payload.IEventPayload;
import icecube.daq.payload.IEventTriggerRecord;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IReadoutDataPayload;
import icecube.daq.payload.ISourceID;
import icecube.daq.payload.ITriggerRequestPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.payload.PayloadException;
import icecube.daq.payload.PayloadRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Event version 4
 */
public class EventPayload_v4
    extends BasePayload
    implements IEventPayload
{
    /** Internal event record type */
    public static final short RECORD_TYPE = 11;

    /** OFfset of record type field */
    private static final int OFFSET_RECTYPE = 0;
    /** Offset of unique ID field */
    private static final int OFFSET_UID = 2;
    /** Offset of source ID field */
    private static final int OFFSET_SOURCEID = 6;
    /** Offset of starting time field */
    private static final int OFFSET_FIRSTTIME = 10;
    /** Offset of ending time field */
    private static final int OFFSET_LASTTIME = 18;
    /** Offset of year field */
    private static final int OFFSET_YEAR = 26;
    /** Offset of unused field */
    //private static final int OFFSET_UNUSED = 28;
    /** Offset of run number field */
    private static final int OFFSET_RUNNUMBER = 30;
    /** Offset of subrun number field */
    private static final int OFFSET_SUBRUNNUMBER = 34;

    /** Offset of composite data section */
    private static final int OFFSET_COMPOSITE = 38;

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

    /** unique ID */
    private int uid;
    /** source ID */
    private int srcId;
    /** starting time */
    private long firstTime;
    /** ending time */
    private long lastTime;
    /** year */
    private short year;
    /** run number */
    private int runNum;
    /** subrun number */
    private int subrunNum;
    /** trigger request */
    private ITriggerRequestPayload trigReq;
    /** composite data */
    private List<IPayload> dataList;

    /** cached source ID object */
    private SourceID srcObj;
    /** cached starting time object */
    private UTCTime firstTimeObj;
    /** cached ending time object */
    private UTCTime lastTimeObj;

    /**
     * Create an event
     * @param buf byte buffer
     * @param offset index of first byte
     * @throws PayloadException if there is a problem
     */
    public EventPayload_v4(ByteBuffer buf, int offset)
        throws PayloadException
    {
        super(buf, offset);
    }

    /**
     * Event constructor for PayloadFactory.
     * @param buf byte buffer
     * @param offset index of first byte
     * @param len total number of bytes
     * @param utcTime payload time (UTC)
     * @throws PayloadException if there is a problem
     */
    EventPayload_v4(ByteBuffer buf, int offset, int len, long utcTime)
        throws PayloadException
    {
        super(buf, offset, len, utcTime);
    }

    /**
     * Create an event
     * @param uid unique ID
     * @param srcId source ID
     * @param firstTime starting time
     * @param lastTime ending time
     * @param year year
     * @param runNum run number
     * @param subrunNum subrun number
     * @param trigReq trigger request
     * @param dataList readout data payloads
     */
    public EventPayload_v4(int uid, ISourceID srcId, IUTCTime firstTime,
                           IUTCTime lastTime, short year, int runNum,
                           int subrunNum, ITriggerRequestPayload trigReq,
                           List<IPayload> dataList)
    {
        super(firstTime.longValue());

        this.uid = uid;
        this.srcId = srcId.getSourceID();
        this.firstTime = firstTime.longValue();
        this.lastTime = lastTime.longValue();
        this.year = year;
        this.runNum = runNum;
        this.subrunNum = subrunNum;
        this.trigReq = trigReq;
        this.dataList = new ArrayList<IPayload>(dataList);
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

        int len = LEN_PAYLOAD_HEADER + OFFSET_COMPOSITE + OFFSET_COMPDATA +
            trigReq.length();

        for (IPayload datum : dataList) {
            len += datum.length();
        }

        return len;
    }

    /**
     * Unimplemented
     * @return Error
     */
    @Override
    public Object deepCopy()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get event configuration ID
     * @return -1
     * @deprecated
     */
    @Override
    public int getEventConfigID()
    {
        return -1;
    }

    /**
     * Get event type
     * @return -1
     * @deprecated
     */
    @Override
    public int getEventType()
    {
        return -1;
    }

    /**
     * Get event version
     * @return <tt>4</tt>
     */
    @Override
    public int getEventVersion()
    {
        return 4;
    }

    /**
     * Get event starting time
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
     * Unimplemented
     * @return Error
     */
    @Override
    public Iterable<IEventHitRecord> getHitRecords()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get event ending time
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
     * Get the name of this payload.
     * @return name
     */
    @Override
    public String getPayloadName()
    {
        return "EventV4";
    }

    /**
     * Get the payload registry type
     * @return type
     */
    @Override
    public int getPayloadType()
    {
        return PayloadRegistry.PAYLOAD_ID_EVENT_V4;
    }

    /**
     * Get the list of readout data payloads for this event
     * @return readout data payload list
     */
    @Override
    public List getReadoutDataPayloads()
    {
        return dataList;
    }

    /**
     * Get the run number
     * @return run number
     */
    @Override
    public int getRunNumber()
    {
        return runNum;
    }

    /**
     * Get the source ID object
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
     * Get the subrun number
     * @return subrun number
     */
    @Override
    public int getSubrunNumber()
    {
        return subrunNum;
    }

    /**
     * Unimplemented
     * @return Error
     */
    @Override
    public int getTriggerConfigID()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Unimplemented
     * @return Error
     */
    @Override
    public Iterable<IEventTriggerRecord> getTriggerRecords()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get the trigger request
     * @return trigger request
     */
    @Override
    public ITriggerRequestPayload getTriggerRequestPayload()
    {
        return trigReq;
    }

    /**
     * Unimplemented
     * @return Error
     */
    @Override
    public int getTriggerType()
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get unique ID
     * @return unique ID
     */
    @Override
    public int getUID()
    {
        return uid;
    }

    /**
     * Get the year in which this event took place
     * @return year
     */
    @Override
    public short getYear()
    {
        return year;
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
        srcId = buf.getInt(pos + OFFSET_SOURCEID);
        firstTime = buf.getLong(pos + OFFSET_FIRSTTIME);
        lastTime = buf.getLong(pos + OFFSET_LASTTIME);
        year = buf.getShort(pos + OFFSET_YEAR);
        runNum = buf.getInt(pos + OFFSET_RUNNUMBER);
        subrunNum = buf.getInt(pos + OFFSET_SUBRUNNUMBER);

        if (firstTime != utcTime) {
            throw new PayloadException("Trigger request first time " +
                                       firstTime +
                                       " differs from header time " + utcTime);
        }

        int compPos = pos + OFFSET_COMPOSITE;

        final int compLen = buf.getInt(compPos + OFFSET_COMPLEN);
/*
        if (compLen != len - ((rrPos + OFFSET_COMPLEN) - offset)) {
            throw new PayloadException("Composite length should be " +
                                       (len - ((rrPos + OFFSET_COMPLEN) -
                                               offset)) + ", not " + compLen);
        }
*/

        final int numComp = (int) buf.getShort(compPos + OFFSET_COMPNUM);

        List<IPayload> compList = new ArrayList<IPayload>();
        int loadedBytes = loadCompositeData(buf, compPos + OFFSET_COMPDATA,
                                            numComp, compList);

        if (compLen != OFFSET_COMPDATA + loadedBytes) {
            throw new PayloadException("Composite should contain " + compLen +
                                       " bytes, but " +
                                       (loadedBytes + OFFSET_COMPDATA) +
                                       " were read");
        }

        if (compList.size() == 0) {
            throw new PayloadException("Composite is empty");
        }

        trigReq = (ITriggerRequestPayload) compList.remove(0);
        dataList = compList;

        srcObj = null;
        firstTimeObj = null;
        lastTimeObj = null;

        return (compPos - pos) + LEN_COMPOSITE_HEADER + loadedBytes;
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
    private int loadCompositeData(ByteBuffer buf, int offset, int numData,
                                  List<IPayload> compList)
        throws PayloadException
    {
        int totLen = 0;

        PayloadFactory factory = getPayloadFactory();
        for (int i = 0; i < numData; i++) {
            IPayload pay = factory.getPayload(buf, offset + totLen);

            try {
                ((IPayload) pay).loadPayload();
            } catch (IOException ioe) {
                throw new PayloadException("Couldn't load composite payload #" +
                                           i, ioe);
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
        // do nothing
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
        buf.putInt(offset + OFFSET_SOURCEID, srcId);
        buf.putLong(offset + OFFSET_FIRSTTIME, firstTime);
        buf.putLong(offset + OFFSET_LASTTIME, lastTime);
        buf.putShort(offset + OFFSET_YEAR, year);
        buf.putInt(offset + OFFSET_RUNNUMBER, runNum);
        buf.putInt(offset + OFFSET_SUBRUNNUMBER, subrunNum);

        List<IPayload> compList = new ArrayList<IPayload>();
        compList.add(trigReq);
        compList.addAll(dataList);

        int compPos = offset + OFFSET_COMPOSITE;

        // composite header
        // length is filled in below
        buf.putInt(compPos + OFFSET_COMPLEN, -1);
        // composite type is deprecated
        buf.putShort(compPos + OFFSET_COMPTYPE, (short) 0);
        buf.putShort(compPos + OFFSET_COMPNUM, (short) compList.size());

        int totLen = 0;
        for (IPayload pay : compList) {
            final int expLen = pay.length();

            int len;
            try {
                len = pay.writePayload(false, compPos + LEN_COMPOSITE_HEADER +
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

        buf.putInt(compPos + OFFSET_COMPLEN, LEN_COMPOSITE_HEADER + totLen);

        return (compPos + LEN_COMPOSITE_HEADER + totLen) - offset;
    }

    /**
     * Clear out any cached data.
     */
    @Override
    public void recycle()
    {
        super.recycle();

        uid = -1;
        srcId = -1;
        firstTime = -1L;
        lastTime = -1L;
        year = -1;
        runNum = -1;
        subrunNum = -1;

        if (trigReq != null) {
            trigReq.recycle();
            trigReq = null;
        }

        if (dataList != null) {
            for (IPayload pay : dataList) {
                pay.recycle();
            }
            dataList = null;
        }

        srcObj = null;
        firstTimeObj = null;
        lastTimeObj = null;
    }

    /**
     * Get a debugging string representing this object.
     * @return debugging string
     */
    @Override
    public String toString()
    {
        String subStr;
        if (subrunNum == 0) {
            subStr = "";
        } else {
            subStr = " sub " + subrunNum;
        }

        int totData;
        int totHits;
        if (dataList == null) {
            totData = 0;
            totHits = 0;
        } else {
            totData = dataList.size();

            totHits = 0;
            for (IPayload pay : dataList) {
                IReadoutDataPayload rdout = (IReadoutDataPayload) pay;
                totHits += rdout.getNumHits();
            }
        }

        return "EventPayload_v4[#" + uid + " src " + getSourceID() + " [" +
            firstTime + "-" + lastTime + "] yr " + year + " run " + runNum +
            subStr + " trig " + trigReq + " data*" + totData +
            " hits*" + totHits + "]";
    }
}
