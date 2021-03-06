package icecube.daq.payload.test;

import icecube.daq.payload.IEventHitRecord;
import icecube.daq.payload.IHitPayload;
import icecube.daq.util.IDOMRegistry;

import java.nio.ByteBuffer;

public class MockDeltaHitRecord
    implements IEventHitRecord
{
    private byte flags;
    private short chanId;
    private long time;
    private short pedFlag;
    private int word0;
    private int word2;
    private byte[] data;

    public MockDeltaHitRecord(byte flags, short chanId, long time,
                              short pedFlag, int word0, int word2, byte[] data)
    {
        this.flags = flags;
        this.chanId = chanId;
        this.time = time;
        this.pedFlag = pedFlag;
        this.word0 = word0;
        this.word2 = word2;
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    @Override
    public short getChannelID()
    {
        return chanId;
    }

    @Override
    public long getHitTime()
    {
        return time;
    }

    @Override
    public int length()
    {
        return 20 + data.length;
    }

    @Override
    public boolean matches(IDOMRegistry domRegistry, IHitPayload hit)
    {
        if (time != hit.getUTCTime()) {
            return false;
        }

        final long mbid = hit.getDOMID().longValue();
        return (chanId == domRegistry.getChannelId(mbid));
    }

    @Override
    public int writeRecord(ByteBuffer buf, int offset, long baseTime)
    {
        final int len = length();
        if (buf.capacity() < offset + len) {
            throw new Error("MockDeltaHitRecord requires " + len +
                            " bytes, but only " + (buf.capacity() - offset) +
                            " (of " + buf.capacity() + ") are available");
        }

        buf.putShort(offset + 0, (short) length());
        buf.put(offset + 2, (byte) 1);
        buf.put(offset + 3, flags);
        buf.putShort(offset + 4, chanId);
        buf.putInt(offset + 6, (int) (time - baseTime));
        buf.putShort(offset + 10, pedFlag);
        buf.putInt(offset + 12, word0);
        buf.putInt(offset + 16, word2);

        int oldPos = buf.position();
        buf.position(offset + 20);
        buf.put(data);
        buf.position(oldPos);

        return 20 + data.length;
    }

    @Override
    public String toString()
    {
        return "MockDeltaHitRecord[flags " + flags + " chan " + chanId +
            " time " + time + " pedFlg " + pedFlag + " w0 " + word0 +
            " w2 " + word2 + " data*" + data.length + "]";
    }
}
