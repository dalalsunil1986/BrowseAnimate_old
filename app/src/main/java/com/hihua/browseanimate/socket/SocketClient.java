package com.hihua.browseanimate.socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hihua.browseanimate.util.UtilLog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;

/**
 * Created by hihua on 17/10/26.
 */

public class SocketClient {
    private Socket mSocket;
    private DataInputStream mInputStream;
    private DataOutputStream mOutputStream;
    private Thread threadReceive;
    private Thread threadSend;
    private final NotifyClientSocket mNotify;
    private SocketBuffer mSocketBuffer = new SocketBuffer();

    public SocketClient(HandleClientSocket handleClient) {
        mNotify = new NotifyClientSocket(handleClient);
    }

    private boolean setSocket() {
        try {
            mSocket.setTcpNoDelay(true);
            mSocket.setReuseAddress(true);
            mSocket.setOOBInline(true);

            InputStream inputStream = mSocket.getInputStream();
            OutputStream outputStream = mSocket.getOutputStream();
            mInputStream = new DataInputStream(inputStream);
            mOutputStream = new DataOutputStream(outputStream);

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void closeSocket() {
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {

            }

            mInputStream = null;
        }

        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {

            }

            mOutputStream = null;
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {

            }

            mSocket = null;
        }
    }

    public void close() {
        closeSocket();

        if (threadSend != null) {
            threadSend.interrupt();

            try {
                threadSend.join();
            } catch (InterruptedException e) {
                threadSend.interrupt();
            }

            threadSend = null;
        }

        if (threadReceive != null) {
            threadReceive.interrupt();

            try {
                threadReceive.join();
            } catch (InterruptedException e) {
                threadReceive.interrupt();
            }

            threadReceive = null;
        }
    }

    public void connect(final String addr, final int port, final int timeout) {
        threadReceive = new Thread(new Runnable() {
            @Override
            public void run() {
                SocketAddress socketAddress = new InetSocketAddress(addr, port);

                while (!threadReceive.isInterrupted()) {
                    try {
                        mSocket = new Socket();
                        mSocket.connect(socketAddress, timeout);
                        if (setSocket()) {
                            mNotify.onConnect(addr, port);
                            receives();
                        } else
                            closeSocket();
                    } catch (IOException e) {
                        UtilLog.writeError(getClass(), e);
                    }

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });

        threadReceive.start();
    }

    public boolean connect(final String addr, final int port) {
        SocketAddress socketAddress = new InetSocketAddress(addr, port);
        mSocket = new Socket();

        try {
            mSocket.connect(socketAddress);
            if (setSocket())
                return true;
            else
                closeSocket();
        } catch (IOException e) {
            UtilLog.writeError(getClass(), e);
        }

        return false;
    }

    private int receive(int size) {
        try {
            return mSocketBuffer.writeBuffer(mInputStream, size);
        } catch (Exception e) {
            return -1;
        }
    }

    private int receive() {
        if (mInputStream == null)
            return -1;

        try {
            return mInputStream.readInt();
        } catch (IOException e) {
            return -1;
        }
    }

    /*
    private void startReceive() {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                int size = 4096;
                mSocketBuffer.newBuffer(size);

                while (true) {
                    int len = receive(size);
                    if (len > 0) {
                        mNotify.onReceive(mSocketBuffer);
                        mSocketBuffer.reset();
                    } else
                        break;

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                String addr = mSocket.getInetAddress().getHostAddress();
                int port = mSocket.getPort();

                close();
                mNotify.onDisconnect(addr, port);
            }
        });

        thread.start();
    }
    */

    private void receives() {
        String addr = mSocket.getInetAddress().getHostAddress();
        int port = mSocket.getPort();
        int dataSize = 0;

        while (true) {
            if (dataSize > 0) {
                int len = 0;

                while (dataSize > 0) {
                    len = receive(dataSize);
                    if (len > 0)
                        dataSize -= len;
                    else
                        break;
                }

                if (dataSize == 0) {
                    mNotify.onReceive(mSocketBuffer);
                }

                if (len < 1)
                    break;
            } else {
                dataSize = receive();
                if (dataSize > 0) {
                    if (dataSize > mSocketBuffer.getBufferLength())
                        mSocketBuffer.newBuffer(dataSize);
                    else
                        mSocketBuffer.reset();
                } else
                    break;
            }
        }

        closeSocket();
        mNotify.onDisconnect(addr, port);
    }

    /*
    private void startReceive() {
        threadReceive = new Thread(new Runnable() {
            @Override
            public void run() {
                String addr = mSocket.getInetAddress().getHostAddress();
                int port = mSocket.getPort();
                int dataSize = 0;

                while (true) {
                    if (dataSize > 0) {
                        int len = 0;

                        while (dataSize > 0) {
                            len = receive(dataSize);
                            if (len > 0)
                                dataSize -= len;
                            else
                                break;
                        }

                        if (dataSize == 0)
                            mNotify.onReceive(mSocketBuffer);

                        if (len < 1)
                            break;
                    } else {
                        dataSize = receive();
                        if (dataSize > 0) {
                            if (dataSize > mSocketBuffer.getBufferLength())
                                mSocketBuffer.newBuffer(dataSize);
                            else
                                mSocketBuffer.reset();
                        } else
                            break;
                    }

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                closeSocket();
                mNotify.onDisconnect(addr, port);
            }
        });

        threadReceive.start();
    }
    */

    public void sendContent(final String content) {
        if (mSocket != null && mOutputStream != null) {
            threadSend = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] bytes = content.getBytes(Charset.defaultCharset());
                        mOutputStream.writeInt(bytes.length);
                        mOutputStream.write(bytes);
                        mNotify.onSend(true, content);
                    } catch (IOException e) {
                        mNotify.onSend(false, content);
                    }
                }
            });

            threadSend.start();
        } else
            mNotify.onSend(false, content);
    }

    public boolean sendString(final String content) {
        if (mSocket != null && mOutputStream != null) {
            byte[] bytes = content.getBytes(Charset.defaultCharset());

            try {
                mOutputStream.writeInt(bytes.length);
                mOutputStream.write(bytes);
                return true;
            } catch (IOException e) {
                UtilLog.writeError(getClass(), e);
            }
        }

        return false;
    }

    class NotifyClientSocket extends Handler {
        private final WeakReference<HandleClientSocket> mWeakListener;

        private final int MSG_ON_CONNECT = 0;
        private final int MSG_ON_DISCONNECT = 1;
        private final int MSG_ON_SEND = 2;

        public NotifyClientSocket(HandleClientSocket handle) {
            mWeakListener = new WeakReference<HandleClientSocket>(handle);
        }

        public HandleClientSocket getHandleClient() {
            return mWeakListener.get();
        }

        public void onConnect(String addr, int port) {
            Bundle bundle = new Bundle();
            bundle.putString("addr", addr);
            bundle.putInt("port", port);

            Message msg = new Message();
            msg.what = MSG_ON_CONNECT;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onDisconnect(String addr, int port) {
            Bundle bundle = new Bundle();
            bundle.putString("addr", addr);
            bundle.putInt("port", port);

            Message msg = new Message();
            msg.what = MSG_ON_DISCONNECT;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onSend(boolean success, String content) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("success", success);
            bundle.putString("content", content);

            Message msg = new Message();
            msg.what = MSG_ON_SEND;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onReceive(SocketBuffer socketBuffer) {
            HandleClientSocket handle = mWeakListener.get();
            if (handle == null)
                return;

            handle.onReceive(socketBuffer);
        }

        @Override
        public void handleMessage(Message msg) {
            HandleClientSocket handle = mWeakListener.get();
            if (handle == null)
                return;

            switch (msg.what) {
                case MSG_ON_CONNECT: {
                    Bundle bundle = msg.getData();
                    String addr = bundle.getString("addr");
                    int port = bundle.getInt("port");

                    handle.onConnect(addr, port);
                }
                break;

                case MSG_ON_DISCONNECT: {
                    Bundle bundle = msg.getData();
                    String addr = bundle.getString("addr");
                    int port = bundle.getInt("port");

                    handle.onDisconnect(addr, port);
                }
                break;

                case MSG_ON_SEND: {
                    Bundle bundle = msg.getData();
                    boolean success = bundle.getBoolean("success");
                    String content = bundle.getString("content");

                    handle.onSend(success, content);
                }
                break;
            }
        }
    }

    public interface HandleClientSocket {
        public void onConnect(String addr, int port);
        public void onDisconnect(String addr, int port);
        public void onSend(boolean success, String content);
        public void onReceive(SocketBuffer socketBuffer);
    }
}
