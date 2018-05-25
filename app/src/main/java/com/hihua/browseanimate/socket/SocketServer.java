package com.hihua.browseanimate.socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hihua.browseanimate.util.UtilLog;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by hihua on 17/10/26.
 */

public class SocketServer {
    private ServerSocket mSocketServer;
    private Map<String, ServerClient> mSocketClients = new HashMap<String, ServerClient>();
    private Thread threadAccpet;
    private Thread threadSend;
    private final ByteArrayOutputStream mStreamHeader = new ByteArrayOutputStream();
    private final ByteArrayOutputStream mStreamBuffer = new ByteArrayOutputStream();
    private final NotifyServerSocket mNotify;

    public SocketServer(HandleServerSocket handleServer) {
        mNotify = new NotifyServerSocket(handleServer);
    }

    public boolean startListen(final int port) {
        try {
            mSocketServer = new ServerSocket(port);
            mSocketServer.setReuseAddress(true);

            InetAddress inetAddress = mSocketServer.getInetAddress();
            final String hostAddress = inetAddress.getHostAddress();

            threadAccpet = new Thread(new Runnable() {
                @Override
                public void run() {
                    startSend();
                    mNotify.onListen(hostAddress, port);

                    while (!threadAccpet.isInterrupted()) {
                        try {
                            Socket socket = mSocketServer.accept();

                            InetAddress inetAddress = socket.getInetAddress();
                            String hostName = inetAddress.getHostName();
                            String hostAddress = inetAddress.getHostAddress();

                            ServerClient serverClient = new ServerClient(socket);
                            if (serverClient.setSocket()) {
                                serverClient.startReceive(hostAddress, port);

                                mSocketClients.put(hostAddress, serverClient);
                                mNotify.onAccept(hostAddress);
                            } else {
                                socket.close();
                                socket = null;
                            }
                        } catch (IOException e) {
                            break;
                        }
                    }

                    mNotify.onClose();
                    UtilLog.writeDebug(getClass(), "thread listen quit");
                }
            });

            threadAccpet.start();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void startSend() {
        threadSend = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!threadSend.isInterrupted()) {
                    byte[] data = null;

                    synchronized (mStreamBuffer) {
                        data = mStreamBuffer.toByteArray();
                        mStreamBuffer.reset();
                    }

                    if (data != null && data.length > 0)
                        sendClient(data);

                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                synchronized (mStreamHeader) {
                    try {
                        mStreamHeader.close();
                    } catch (IOException e) {

                    }
                }

                synchronized (mStreamBuffer) {
                    try {
                        mStreamBuffer.close();
                    } catch (IOException e) {

                    }
                }

                UtilLog.writeDebug(getClass(), "thread send quit");
            }
        });

        threadSend.start();
    }

    public void putHeader(byte[] data, int dataSize) {
        byte[] header = setLength(dataSize);
        if (header != null) {
            synchronized (mStreamHeader) {
                mStreamHeader.reset();
                mStreamHeader.write(header, 0, header.length);
                mStreamHeader.write(data, 0, dataSize);
            }
        }

        /*
        synchronized (mStreamHeader) {
            mStreamHeader.reset();
            mStreamHeader.write(data, 0, dataSize);
        }
        */
    }

    public void putData(byte[] data, int dataSize) {
        byte[] header = setLength(dataSize);
        if (header != null) {
            synchronized (mStreamBuffer) {
                mStreamBuffer.write(header, 0, header.length);
                mStreamBuffer.write(data, 0, dataSize);
            }
        }

        /*
        synchronized (mStreamBuffer) {
            mStreamBuffer.write(data, 0, dataSize);
        }
        */
    }

    private void closeListen() {
        if (mSocketServer != null) {
            try {
                mSocketServer.close();
            } catch (IOException e) {

            }

            mSocketServer = null;
        }
    }

    public void close() {
        closeClient();
        closeListen();

        if (threadSend != null) {
            threadSend.interrupt();

            try {
                threadSend.join();
            } catch (InterruptedException e) {
                threadSend.interrupt();
            }

            threadSend = null;
        }

        if (threadAccpet != null) {
            threadAccpet.interrupt();

            try {
                threadAccpet.join();
            } catch (InterruptedException e) {
                threadAccpet.interrupt();
            }

            threadAccpet = null;
        }
    }

    private void sendClient(byte[] data) {
        synchronized (mSocketClients) {
            List<String> address = new Vector<String>();

            for (Map.Entry<String, ServerClient> entry : mSocketClients.entrySet()) {
                String key = entry.getKey();
                ServerClient serverClient = entry.getValue();
                if (!serverClient.sendData(data))
                    address.add(key);
            }

            for (String addr : address)
                closeClient(addr);
        }
    }

    public void closeClient() {
        synchronized (mSocketClients) {
            for (Map.Entry<String, ServerClient> entry : mSocketClients.entrySet()) {
                String key = entry.getKey();
                ServerClient serverClient = entry.getValue();
                serverClient.close();
            }

            mSocketClients.clear();
        }
    }

    public void closeClient(String addr) {
        synchronized (mSocketClients) {
            for (Map.Entry<String, ServerClient> entry : mSocketClients.entrySet()) {
                String key = entry.getKey();
                ServerClient serverClient = entry.getValue();

                if (key.equals(addr)) {
                    serverClient.close();
                    mSocketClients.remove(key);

                    break;
                }
            }
        }
    }

    public void removeClient(String addr) {
        synchronized (mSocketClients) {
            mSocketClients.remove(addr);
        }
    }

    private byte[] setLength(byte[] data) {
        int size = data.length;
        if (size > 0) {
            //byte[] header = {(byte) (size >> 0), (byte) (size >> 8), (byte) (size >> 16), (byte) (size >> 24)};
            byte[] header = { (byte) (size >> 24), (byte) (size >> 16), (byte) (size >> 8), (byte) (size >> 0) };

            return header;
        }

        return null;
    }

    private byte[] setLength(int size) {
        if (size > 0) {
            //byte[] header = {(byte) (size >> 0), (byte) (size >> 8), (byte) (size >> 16), (byte) (size >> 24)};
            byte[] header = { (byte) (size >> 24), (byte) (size >> 16), (byte) (size >> 8), (byte) (size >> 0) };

            return header;
        }

        return null;
    }

    public class ServerClient {
        private final Socket mSocket;
        private DataInputStream mInputStream;
        private DataOutputStream mOutputStream;
        private SocketBuffer mSocketBuffer = new SocketBuffer();
        private boolean mSendHeader = false;

        public ServerClient(Socket socket) {
            mSocket = socket;
        }

        public boolean setSocket() {
            try {
                mSocket.setTcpNoDelay(true);
                mSocket.setReuseAddress(true);
                mSocket.setSoLinger(true, 0);

                InputStream inputStream = mSocket.getInputStream();
                OutputStream outputStream = mSocket.getOutputStream();
                mInputStream = new DataInputStream(inputStream);
                mOutputStream = new DataOutputStream(outputStream);

                return true;
            } catch (IOException e) {
                return false;
            }
        }

        private int send(byte[] data) {
            if (mOutputStream == null)
                return 0;

            try {
                mOutputStream.write(data);
                //UtilLog.writeDebug(getClass(), "send length: " + data.length);
                return data.length;
            } catch (IOException e) {
                return -1;
            }
        }

        public boolean sendString(String content) {
            try {
                byte[] bytes = content.getBytes(Charset.defaultCharset());
                mOutputStream.writeInt(bytes.length);
                mOutputStream.write(bytes);
                return true;
            } catch (IOException e) {
                UtilLog.writeError(getClass(), e);
                return false;
            }
        }

        public boolean sendData(byte[] data) {
            if (!mSendHeader) {
                byte[] header = mStreamHeader.toByteArray();
                if (header != null && header.length > 0) {
                    int ret = send(header);
                    if (ret > 0)
                        mSendHeader = true;
                    else
                        return false;
                } else
                    return true;
            }

            if (mSendHeader) {
                int ret = send(data);
                if (ret > 0)
                    return true;
                else
                    return false;
            } else
                return false;
        }

        public void close() {
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
            }
        }

        private int receive(int size) {
            try {
                return mSocketBuffer.writeBuffer(mInputStream, size);
            } catch (Exception e) {
                return -1;
            }
        }

        private int receive() {
            if (mInputStream != null) {
                try {
                    return mInputStream.readInt();
                } catch (IOException e) {

                }
            }

            return -1;
        }

        public void startReceive(final String addr, final int port) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
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
                                mNotify.onReceive(ServerClient.this, mSocketBuffer);

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

                    UtilLog.writeDebug(getClass(), String.format("%s:%d thread recieve quit", addr, port));

                    close();

                    removeClient(addr);

                    mNotify.onDisconnect(addr, mSocketClients.isEmpty());

                    if (mSocketClients.isEmpty()) {
                        synchronized (mStreamBuffer) {
                            mStreamBuffer.reset();
                        }
                    }
                }
            });

            thread.start();
        }
    }

    class NotifyServerSocket extends Handler {
        private final WeakReference<HandleServerSocket> mWeakListener;

        private final int MSG_ON_LISTEN = 0;
        private final int MSG_ON_ACCEPT = 1;
        private final int MSG_ON_CLOSE = 2;
        private final int MSG_ON_DISCONNECT = 3;

        public NotifyServerSocket(HandleServerSocket handle) {
            mWeakListener = new WeakReference<HandleServerSocket>(handle);
        }

        public HandleServerSocket getHandleServer() {
            return mWeakListener.get();
        }

        public void onListen(String address, int port) {
            Bundle bundle = new Bundle();
            bundle.putString("address", address);
            bundle.putInt("port", port);

            Message msg = new Message();
            msg.what = MSG_ON_LISTEN;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onAccept(String addr) {
            Bundle bundle = new Bundle();
            bundle.putString("addr", addr);

            Message msg = new Message();
            msg.what = MSG_ON_ACCEPT;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onClose() {
            sendEmptyMessage(MSG_ON_CLOSE);
        }

        public void onDisconnect(String addr, boolean empty) {
            Bundle bundle = new Bundle();
            bundle.putString("addr", addr);
            bundle.putBoolean("empty", empty);

            Message msg = new Message();
            msg.what = MSG_ON_DISCONNECT;
            msg.setData(bundle);

            sendMessage(msg);
        }

        public void onReceive(ServerClient serverClient, SocketBuffer socketBuffer) {
            HandleServerSocket handle = mWeakListener.get();
            if (handle == null)
                return;

            handle.onReceive(serverClient, socketBuffer);
        }

        @Override
        public void handleMessage(Message msg) {
            HandleServerSocket handle = mWeakListener.get();
            if (handle == null)
                return;

            switch (msg.what) {
                case MSG_ON_LISTEN: {
                    Bundle bundle = msg.getData();
                    String address = bundle.getString("address", null);
                    int port = bundle.getInt("port");

                    handle.onListen(address, port);
                }
                break;

                case MSG_ON_ACCEPT: {
                    Bundle bundle = msg.getData();
                    String addr = bundle.getString("addr");

                    handle.onAccept(addr);
                }
                break;

                case MSG_ON_CLOSE: {
                    handle.onClose();
                }
                break;

                case MSG_ON_DISCONNECT: {
                    Bundle bundle = msg.getData();
                    String addr = bundle.getString("addr");
                    boolean empty = bundle.getBoolean("empty");

                    handle.onDisconnect(addr, empty);
                }
                break;
            }
        }
    }

    public interface HandleServerSocket {
        public void onListen(String address, int port);
        public void onAccept(String address);
        public void onClose();
        public void onDisconnect(String address, boolean empty);
        public void onReceive(ServerClient serverClient, SocketBuffer socketBuffer);
    }
}
