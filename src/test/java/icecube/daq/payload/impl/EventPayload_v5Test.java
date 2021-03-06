package icecube.daq.payload.impl;

import icecube.daq.payload.IEventHitRecord;
import icecube.daq.payload.PayloadChecker;
import icecube.daq.payload.PayloadRegistry;
import icecube.daq.payload.test.LoggingCase;
import icecube.daq.payload.test.MockDOMRegistry;
import icecube.daq.payload.test.MockDeltaHitRecord;
import icecube.daq.payload.test.MockHitData;
import icecube.daq.payload.test.MockReadoutRequest;
import icecube.daq.payload.test.MockSourceID;
import icecube.daq.payload.test.MockTriggerRequest;
import icecube.daq.payload.test.MockUTCTime;
import icecube.daq.payload.test.TestUtil;
import icecube.daq.util.IDOMRegistry;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class EventPayload_v5Test
    extends LoggingCase
{
    /** Get the current year */
    private static final short YEAR =
        (short) (new GregorianCalendar()).get(GregorianCalendar.YEAR);

    /**
     * Constructs an instance of this test.
     *
     * @param name the name of the test.
     */
    public EventPayload_v5Test(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(EventPayload_v5Test.class);
    }

    public void testCreate()
        throws Exception
    {
        final int uid = 12;
        final long firstTime = 1111L;
        final long lastTime = 2222L;
        final int runNum = 4444;
        final int subrunNum = 5555;

        final int trigCfgId = 6666;
        final int trigType = 7777;
        final int trigSrcId = 8888;

        final int rrType = 100;
        final long rrDomId = 103;
        final int rrSrcId = 104;

        final long hitTime1 = 1122L;
        final int hitType1 = 23;
        final int hitCfgId1 = 24;
        final int hitSrcId1 = 25;
        final long hitDomId1 = 1126L;
        final int hitMode1 = 27;
        final short hitChanId1 = 28;

        MockDeltaHitRecord hitRec =
            new MockDeltaHitRecord((byte) 0, hitChanId1, hitTime1, (short) 34,
                                   56, 78, new byte[0]);

        MockReadoutRequest mockReq =
            new MockReadoutRequest(uid, trigSrcId);
        mockReq.addElement(rrType, firstTime, lastTime, rrDomId, rrSrcId);

        MockTriggerRequest trigReq =
            new MockTriggerRequest(firstTime, uid, trigType, trigCfgId,
                                   trigSrcId, firstTime, lastTime, null,
                                   mockReq);

        ArrayList<IEventHitRecord> hitRecList =
            new ArrayList<IEventHitRecord>();
        hitRecList.add(hitRec);

        EventPayload_v5 evt =
            new EventPayload_v5(uid, new MockUTCTime(firstTime),
                                new MockUTCTime(lastTime), YEAR, runNum,
                                subrunNum, trigReq, hitRecList);

        assertTrue("Bad event", PayloadChecker.validateEvent(evt, true));

        assertEquals("Bad UID", uid, evt.getUID());
        assertEquals("Bad first UTC time",
                     firstTime, evt.getFirstTimeUTC().longValue());
        assertEquals("Bad last UTC time",
                     lastTime, evt.getLastTimeUTC().longValue());
        assertEquals("Bad year", YEAR, evt.getYear());
        assertEquals("Bad run number", runNum, evt.getRunNumber());
        assertEquals("Bad subrun number", subrunNum, evt.getSubrunNumber());

        MockDOMRegistry domRegistry = new MockDOMRegistry();
        domRegistry.addChannelId(hitDomId1, hitChanId1);

        ByteBuffer buf =
            TestUtil.createEventv5(uid, firstTime, lastTime, YEAR, runNum,
                                   subrunNum, trigReq, hitRecList, domRegistry);

        assertEquals("Bad payload length", buf.capacity(), evt.length());

        evt.recycle();
    }

    public void testCreateFromBuffer()
        throws Exception
    {
        final int uid = 12;
        final long firstTime = 1111L;
        final long lastTime = 2222L;
        final int runNum = 444;
        final int subrunNum = 555;

        final int trigUID = 666;
        final int trigType = 777;
        final int trigCfgId = 888;
        final int trigSrcId = 999;
        final long trigFirstTime = firstTime + 1;
        final long trigLastTime = lastTime - 1;

        final long halfTime = firstTime + (lastTime - firstTime) / 2L;

        final int type1 = 100;
        final long firstTime1 = firstTime + 5;
        final long lastTime1 = halfTime - 1;
        final long domId1 = 103;
        final int srcId1 = 104;

        final int type2 = 200;
        final long firstTime2 = halfTime + 1;
        final long lastTime2 = lastTime - 5;
        final long domId2 = -1;
        final int srcId2 = -1;

        final long hitTime1 = halfTime - 5;
        final int hitType1 = 23;
        final int hitCfgId1 = 24;
        final int hitSrcId1 = 25;
        final long hitDomId1 = 1126L;
        final int hitMode1 = 27;
        final short hitChanId1 = 28;

        final long hitTime2 = halfTime + 5;
        final int hitType2 = 33;
        final int hitCfgId2 = 34;
        final int hitSrcId2 = 35;
        final long hitDomId2 = 2109L;
        final int hitMode2 = 37;
        final short hitChanId2 = 38;

        ArrayList hitList = new ArrayList();
        hitList.add(new MockHitData(hitTime1, hitType1, hitCfgId1, hitSrcId1,
                                    hitDomId1, hitMode1));
        hitList.add(new MockHitData(hitTime2, hitType2, hitCfgId2, hitSrcId2,
                                    hitDomId2, hitMode2));

        MockReadoutRequest mockReq =
            new MockReadoutRequest(trigUID, trigSrcId);
        mockReq.addElement(type1, firstTime1, lastTime1, domId1, srcId1);
        mockReq.addElement(type2, firstTime2, lastTime2, domId2, srcId2);

        MockTriggerRequest trigReq =
            new MockTriggerRequest(trigFirstTime, trigUID, trigType, trigCfgId,
                                   trigSrcId, trigFirstTime, trigLastTime,
                                   hitList, mockReq);

        List<IEventHitRecord> hitRecList = new ArrayList<IEventHitRecord>();
        hitRecList.add(new MockDeltaHitRecord((byte) 1, hitChanId1, hitTime1,
                                              (short) 45, 67, 89,
                                              new byte[] { (byte) 123 }));
        hitRecList.add(new MockDeltaHitRecord((byte) 2, hitChanId2, hitTime2,
                                              (short) 56, 78, 90,
                                              new byte[] { (byte) 45,
                                                           (byte) 5 }));

        MockDOMRegistry domRegistry = new MockDOMRegistry();
        domRegistry.addChannelId(hitDomId1, hitChanId1);
        domRegistry.addChannelId(hitDomId2, hitChanId2);

        ByteBuffer buf =
            TestUtil.createEventv5(uid, firstTime, lastTime, YEAR, runNum,
                                   subrunNum, trigReq, hitRecList, domRegistry);

        EventPayload_v5 evt = new EventPayload_v5(buf, 0);
        evt.loadPayload();

        assertTrue("Bad event", PayloadChecker.validateEvent(evt, true));

        assertEquals("Bad payload length", buf.capacity(), evt.length());

        assertEquals("Bad UID", uid, evt.getUID());
        assertEquals("Bad first UTC time",
                     firstTime, evt.getFirstTimeUTC().longValue());
        assertEquals("Bad last UTC time",
                     lastTime, evt.getLastTimeUTC().longValue());
        assertEquals("Bad year", YEAR, evt.getYear());
        assertEquals("Bad run number", runNum, evt.getRunNumber());
        assertEquals("Bad subrun number", subrunNum, evt.getSubrunNumber());

        evt.recycle();
    }

    public void testWriteByteBuffer()
        throws Exception
    {
        final int uid = 12;
        final long firstTime = 1111L;
        final long lastTime = 2222L;
        final int runNum = 444;
        final int subrunNum = 555;

        final int trigUID = 666;
        final int trigType = 777;
        final int trigCfgId = 888;
        final int trigSrcId = 999;
        final long trigFirstTime = firstTime + 1;
        final long trigLastTime = lastTime - 1;

        final long halfTime = firstTime + (lastTime - firstTime) / 2L;

        final int type1 = 100;
        final long firstTime1 = firstTime + 11;
        final long lastTime1 = halfTime - 2;
        final long domId1 = 103;
        final int srcId1 = 104;

        final int type2 = 200;
        final long firstTime2 = halfTime + 2;
        final long lastTime2 = lastTime - 11;
        final long domId2 = -1;
        final int srcId2 = -1;

        final long hitTime1 = halfTime - 7;
        final int hitType1 = -1;
        final int hitCfgId1 = 24;
        final int hitSrcId1 = 25;
        final long hitDomId1 = 1126L;
        final int hitMode1 = 27;
        final short hitChanId1 = 28;

        final long hitTime2 = halfTime + 7;
        final int hitType2 = -1;
        final int hitCfgId2 = 34;
        final int hitSrcId2 = 35;
        final long hitDomId2 = 2109L;
        final int hitMode2 = 37;
        final short hitChanId2 = 38;

        ArrayList hitList = new ArrayList();
        hitList.add(new MockHitData(hitTime1, hitType1, hitCfgId1, hitSrcId1,
                                    hitDomId1, hitMode1));
        hitList.add(new MockHitData(hitTime2, hitType2, hitCfgId2, hitSrcId2,
                                    hitDomId2, hitMode2));

        MockReadoutRequest mockReq =
            new MockReadoutRequest(trigUID, trigSrcId);
        mockReq.addElement(type1, firstTime1, lastTime1, domId1, srcId1);
        mockReq.addElement(type2, firstTime2, lastTime2, domId2, srcId2);

        MockTriggerRequest trigReq =
            new MockTriggerRequest(trigFirstTime, trigUID, trigType, trigCfgId,
                                   trigSrcId, trigFirstTime, trigLastTime,
                                   hitList, mockReq);

        List<IEventHitRecord> hitRecList = new ArrayList<IEventHitRecord>();
        hitRecList.add(new MockDeltaHitRecord((byte) 1, hitChanId1, hitTime1,
                                              (short) 45, 67, 89,
                                              new byte[] { (byte) 123 }));
        hitRecList.add(new MockDeltaHitRecord((byte) 2, hitChanId2, hitTime2,
                                              (short) 56, 78, 90,
                                              new byte[] { (byte) 45,
                                                           (byte) 5 }));

        MockDOMRegistry domRegistry = new MockDOMRegistry();
        domRegistry.addChannelId(hitDomId1, hitChanId1);
        domRegistry.addChannelId(hitDomId2, hitChanId2);

        ByteBuffer buf =
            TestUtil.createEventv5(uid, firstTime, lastTime, YEAR, runNum,
                                   subrunNum, trigReq, hitRecList, domRegistry);

        EventPayload_v5 evt = new EventPayload_v5(buf, 0);
        evt.loadPayload();

        evt.setDOMRegistry(new MockDOMRegistry());

        assertTrue("Bad event", PayloadChecker.validateEvent(evt, true));

        ByteBuffer newBuf = ByteBuffer.allocate(buf.limit());
        for (int b = 0; b < 2; b++) {
            final boolean loaded = (b == 1);
            final int written = evt.writePayload(loaded, 0, newBuf);

            assertEquals("Bad number of bytes written", buf.limit(), written);

            for (int i = 0; i < buf.limit(); i++) {
                assertEquals("Bad " + (loaded ? "loaded" : "copied") +
                             " byte #" + i, buf.get(i), newBuf.get(i));
            }
        }
    }

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }
}
