package com.hihua.browseanimate.socket;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by hihua on 17/10/27.
 */

public class SocketBuffer {
    private int bufferLength = 0;
    private int dataLength = 0;
    private int writeIndex = 0;
    private int readIndex = 0;
    private byte[] buffer = null;

    public int getBufferLength() {
        return bufferLength;
    }

    private void setBufferLength(int bufferLength) {
        this.bufferLength = bufferLength;
    }

    public int getDataLength() {
        return dataLength;
    }

    private void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public int getWriteIndex() {
        return writeIndex;
    }

    private void setWriteIndex(int writeIndex) {
        this.writeIndex = writeIndex;
    }

    public int getReadIndex() {
        return readIndex;
    }

    private void setReadIndex(int readIndex) {
        this.readIndex = readIndex;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    private void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public void newBuffer(int bufferLength) {
        buffer = new byte[bufferLength];
        setBufferLength(bufferLength);
        reset();
    }

    public void reset() {
        setDataLength(0);
        setWriteIndex(0);
        setReadIndex(0);
    }

    public int writeBuffer(InputStream inputStream, int size) throws Exception {
        if (inputStream != null && getBufferLength() >= getWriteIndex() + size) {
            int len = inputStream.read(getBuffer(), getWriteIndex(), size);
            if (len > 0) {
                setDataLength(getDataLength() + len);
                setWriteIndex(getWriteIndex() + len);
            }

            return len;
        }

        return -1;
    }

    public String toString() {
        if (buffer != null)
            return new String(buffer, 0, dataLength, Charset.defaultCharset());
        else
            return null;
    }
}
