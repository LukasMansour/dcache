package dmg.cells.nucleus;


public class CellLock {
    Object _object = null;
    CellMessageAnswerable _callback = null;
    long _timeout = 0;
    boolean _sync = true;
    CellMessage _message = null;

    public CellLock(CellMessage msg, CellMessageAnswerable callback,
            long timeout) {
        if (callback == null)
            throw new IllegalArgumentException("Null callback not permitted");
        _callback = callback;
        _timeout = System.currentTimeMillis() + timeout;
        _sync = false;
        _message = msg;
    }

    public CellLock() {
    }

    public void setObject(Object o) {
        _object = o;
    }

    public Object getObject() {
        return _object;
    }

    public CellMessageAnswerable getCallback() {
        return _callback;
    }

    public boolean isSync() {
        return _sync;
    }

    public CellMessage getMessage() {
        return _message;
    }

    public long getTimeout() {
        return _timeout;
    }

}
