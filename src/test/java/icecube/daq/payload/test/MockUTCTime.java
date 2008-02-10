package icecube.daq.payload.test;

import icecube.daq.payload.IUTCTime;
import icecube.util.Poolable;

public class MockUTCTime
    extends Poolable
    implements IUTCTime
{
    private long time;

    public MockUTCTime(long time)
    {
        this.time = time;
    }

    public int compareTo(Object obj)
    {
        if (obj == null) {
            return 1;
        } else if (!(obj instanceof IUTCTime)) {
            return getClass().getName().compareTo(obj.getClass().getName());
        }

        final long val = ((IUTCTime) obj).getUTCTimeAsLong();
        if (time < val) {
            return -1;
        } else if (time > val) {
            return 1;
        }

        return 0;
    }

    public Object deepCopy()
    {
        return new MockUTCTime(time);
    }

    /**
     * Object is able to dispose of itself.
     * This means it is able to return itself to the pool from
     * which it came.
     */
    public void dispose()
    {
        // do nothing
    }

    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    public IUTCTime getOffsetUTCTime(double x0)
    {
        throw new Error("Unimplemented");
    }

    /**
     * Get an object from the pool in a non-static context.
     *
     * @return object of this type from the object pool.
     */
    public Poolable getPoolable()
    {
        return new MockSourceID(-1);
    }

    public long getUTCTimeAsLong()
    {
        return time;
    }

    /**
     * Object knows how to recycle itself
     */
    public void recycle()
    {
        // do nothing
    }

    public long timeDiff(IUTCTime x0)
    {
        throw new Error("Unimplemented");
    }

    public double timeDiff_ns(IUTCTime x0)
    {
        throw new Error("Unimplemented");
    }
}
