// UsbConnectionManager.java (已修正編譯錯誤的終極備案版)
package com.example.test;

import android.content.Context;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UsbConnectionManager {
    private static final String TAG = "UsbConnectionManager";
    private static UsbConnectionManager instance;
    private final Context appContext;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private volatile boolean isConnecting = false;
    private String foundIpAddress = null;

    private static final int RASPBERRY_PI_PORT = 8080;

    public static class ConnectionResult {
        public final boolean success;
        public final String message;
        public ConnectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        public static ConnectionResult success(String ip) { return new ConnectionResult(true, "連接成功: " + ip); }
        public static ConnectionResult failure(String message) { return new ConnectionResult(false, message); }
    }

    private static class UsbInterfaceInfo {
        final NetworkInterface networkInterface;
        final InetAddress localIpAddress;

        UsbInterfaceInfo(NetworkInterface networkInterface, InetAddress localIpAddress) {
            this.networkInterface = networkInterface;
            this.localIpAddress = localIpAddress;
        }
    }

    private UsbConnectionManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized UsbConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new UsbConnectionManager(context.getApplicationContext());
        }
        return instance;
    }

    public CompletableFuture<ConnectionResult> connect(Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            if (isConnected()) return ConnectionResult.success(foundIpAddress);
            if (isConnecting) return ConnectionResult.failure("已在連接中");

            isConnecting = true;
            disconnectNetwork();

            progressCallback.accept("正在查找 USB 網路介面...");
            UsbInterfaceInfo usbInfo = findUsbInterfaceAndLocalIp();
            if (usbInfo == null) {
                isConnecting = false;
                return ConnectionResult.failure("找不到 USB 網路共享介面。\n請確認已在系統設定中啟用。");
            }

            String localIpStr = usbInfo.localIpAddress.getHostAddress();
            int lastDot = localIpStr.lastIndexOf('.');
            String subnet = localIpStr.substring(0, lastDot + 1);

            Log.d(TAG, "connect: 找到介面 " + usbInfo.networkInterface.getName() + ", 本機 IP: " + localIpStr);
            progressCallback.accept("正在掃描樹莓派 (" + subnet + "x)...");

            for (int i = 1; i <= 254; i++) {
                if (localIpStr.endsWith("." + i)) continue;

                String host = subnet + i;
                progressCallback.accept(String.format("正在掃描: %s (%d/254)", host, i));

                try (Socket tempSocket = new Socket()) {
                    tempSocket.bind(new InetSocketAddress(usbInfo.localIpAddress, 0));
                    tempSocket.connect(new InetSocketAddress(host, RASPBERRY_PI_PORT), 75);

                    Log.d(TAG, "connect: ✅ 找到伺服器! IP 位址是: " + host);
                    if (establishPermanentConnection(host, usbInfo.localIpAddress)) {
                        return ConnectionResult.success(host);
                    } else {
                        // [已修正] 現在只傳入一個參數，符合方法定義
                        return ConnectionResult.failure("找到伺服器但建立永久連接失敗");
                    }
                } catch (Exception e) {
                    // 掃描中，失敗是正常的
                }
            }

            isConnecting = false;
            Log.e(TAG, "connect: 掃描網段 " + subnet + "x 結束，未找到伺服器。");
            return ConnectionResult.failure("已掃描網段 " + subnet + "x，\n但未找到伺服器。\n請確認樹莓派程式是否運行？");
        });
    }

    private UsbInterfaceInfo findUsbInterfaceAndLocalIp() {
        Log.d(TAG, "====== 開始查找 USB 網路介面 ======");
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                String interfaceName = intf.getName().toLowerCase();
                boolean isUp = intf.isUp();
                boolean isUsbKeywordMatch = interfaceName.contains("rndis") || interfaceName.contains("usb")
                        || interfaceName.contains("tether");

                // Log 顯示每個正在檢查的介面詳細資訊
                Log.d(TAG, "正在檢查介面: '" + intf.getName() + "', 是否啟用 (isUp): " + isUp +
                        ", 是否為目標 (isUsbKeywordMatch): " + isUsbKeywordMatch);

                // 條件判斷：介面必須是啟用狀態，並且名稱包含關鍵字
                if (isUp && isUsbKeywordMatch) {
                    Log.i(TAG, "-> 找到符合條件的候選介面: '" + intf.getName() + "'。正在查找其 IPv4 位址...");
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        // Log 顯示該介面下的每個 IP 位址
                        Log.d(TAG, "   -- 檢查到 IP: " + addr.getHostAddress() + ", 是否為 IPv4: " +
                                (addr instanceof Inet4Address) + ", 是否為本地迴環: " + addr.isLoopbackAddress());

                        // 條件判斷：位址必須是 IPv4 且不能是本地迴環位址 (127.0.0.1)
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            Log.i(TAG, "✅✅✅ 成功！已鎖定 USB 網路介面！");
                            Log.i(TAG, "-> 介面名稱: " + intf.getName());
                            Log.i(TAG, "-> 本機 IP 位址: " + addr.getHostAddress());
                            Log.d(TAG, "====== 查找結束 ======");
                            return new UsbInterfaceInfo(intf, addr);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "findUsbInterfaceAndLocalIp: 查找過程中發生例外錯誤", ex);
        }

        Log.w(TAG, "⚠️ 未能找到任何符合條件的 USB 網路共享介面。");
        Log.d(TAG, "====== 查找結束 ======");
        return null;
    }

    private boolean establishPermanentConnection(String serverIp, InetAddress localIp) {
        try {
            Log.d(TAG, "establishPermanentConnection: 正在建立到 " + serverIp + " 的永久連接...");
            socket = new Socket();
            socket.bind(new InetSocketAddress(localIp, 0));
            socket.connect(new InetSocketAddress(serverIp, RASPBERRY_PI_PORT), 7000);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            foundIpAddress = serverIp;
            isConnecting = false;
            Log.d(TAG, "establishPermanentConnection: ✅ 永久連接建立成功！");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "establishPermanentConnection: ❌ 建立永久連接失敗", e);
            disconnectNetwork();
            return false;
        }
    }


    // --- 以下為您原本的 Public 方法，無需修改 ---
    public boolean isConnected() { return socket != null && socket.isConnected() && !socket.isClosed(); }
    public String getConnectedDeviceName() { return isConnected() ? "Raspberry Pi (IP: " + foundIpAddress + ")" : "N/A"; }
    public String getConnectionStatus() { return isConnected() ? "已透過掃描連接" : "未連接"; }

    public void sendUsbCommand(String command) {
        if (!isConnected() || outputStream == null) {
            Log.e(TAG, "sendUsbCommand: 無法發送命令，Socket 未連接。");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String commandToSend = command.endsWith("\n") ? command : command + "\n";
                outputStream.write(commandToSend.getBytes("UTF-8"));
                outputStream.flush();
                Log.d(TAG, "sendUsbCommand: 命令發送成功: " + command.trim());
            } catch (Exception e) {
                Log.e(TAG, "sendUsbCommand: 命令發送失敗", e);
                disconnectNetwork();
            }
        });
    }

    public String receiveUsbResponse() {
        if (!isConnected() || inputStream == null) {
            Log.e(TAG, "receiveUsbResponse: 無法接收回應，Socket 未連接。");
            return null;
        }
        try {
            byte[] buffer = new byte[4096];
            socket.setSoTimeout(10000);
            int bytesRead = inputStream.read(buffer);
            socket.setSoTimeout(0);

            if (bytesRead > 0) {
                return new String(buffer, 0, bytesRead, "UTF-8");
            }
            disconnectNetwork();
            return null;
        } catch (java.net.SocketTimeoutException e) {
            Log.w(TAG, "receiveUsbResponse: 接收回應超時");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "receiveUsbResponse: 接收回應失敗", e);
            disconnectNetwork();
            return null;
        }
    }

    public void disconnectNetwork() {
        isConnecting = false;
        try { if (inputStream != null) inputStream.close(); } catch (Exception e) { /* ignore */ }
        try { if (outputStream != null) outputStream.close(); } catch (Exception e) { /* ignore */ }
        try { if (socket != null) socket.close(); } catch (Exception e) { /* ignore */ }
        inputStream = null;
        outputStream = null;
        socket = null;
        foundIpAddress = null;
    }

    public InputStream getSocketInputStream() {
        if (socket != null && socket.isConnected()) {
            try {
                return socket.getInputStream();
            } catch (java.io.IOException e) {
                Log.e(TAG, "getSocketInputStream: 獲取輸入流失敗", e);
                return null;
            }
        }
        return null;
    }
}