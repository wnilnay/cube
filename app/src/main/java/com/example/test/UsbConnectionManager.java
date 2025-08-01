// UsbConnectionManager.java (Final Debug Version)
package com.example.test;

import android.content.Context;
import android.util.Log;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
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
    private boolean isConnecting = false;

    private static final int RASPBERRY_PI_PORT = 8080;
    private String foundIpAddress = null;

    // --- 內部類：用於回傳詳細的連接結果 ---
    public static class ConnectionResult {
        public final boolean success;
        public final String message;
        public final String scannedSubnet;

        public ConnectionResult(boolean success, String message, String scannedSubnet) {
            this.success = success;
            this.message = message;
            this.scannedSubnet = scannedSubnet;
        }

        public static ConnectionResult success(String ip) {
            return new ConnectionResult(true, "連接成功: " + ip, null);
        }
        public static ConnectionResult failure(String message, String subnet) {
            return new ConnectionResult(false, message, subnet);
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
            if (isConnected()) {
                Log.d(TAG, "connect: Socket 已連線，無需重複。");
                return ConnectionResult.success(foundIpAddress);
            }
            if (isConnecting) {
                Log.d(TAG, "connect: 已在掃描/連接中，請稍候...");
                return ConnectionResult.failure("已在連接中", null);
            }

            isConnecting = true;
            disconnectNetwork();

            String subnet = getTetheringSubnet();
            if (subnet == null) {
                isConnecting = false;
                return ConnectionResult.failure("找不到 USB 網路共享介面。\n請確認已在系統設定中啟用。", null);
            }

            Log.d(TAG, "connect: 成功找到網段，開始掃描: " + subnet + "x");
            progressCallback.accept("正在掃描樹莓派 (" + subnet + "x)...");

            for (int i = 1; i <= 254; i++) {
                String host = subnet + i;
                progressCallback.accept(String.format("正在掃描: %s (%d/254)", host, i));
                try (Socket tempSocket = new Socket()) {
                    tempSocket.connect(new InetSocketAddress(host, RASPBERRY_PI_PORT), 75);
                    Log.d(TAG, "connect: ✅ 找到伺服器! IP 位址是: " + host);
                    foundIpAddress = host;
                    if (establishPermanentConnection(foundIpAddress)) {
                        return ConnectionResult.success(foundIpAddress);
                    } else {
                        return ConnectionResult.failure("找到伺服器但建立連接失敗", subnet);
                    }
                } catch (Exception e) {
                    // 掃描中，失敗是正常的
                }
            }

            isConnecting = false;
            Log.e(TAG, "connect: 掃描網段 " + subnet + "x 結束，未找到伺服器。");
            return ConnectionResult.failure("已掃描網段 " + subnet + "x，\n但未找到伺服器。\n請確認樹莓派程式是否運行？", subnet);
        });
    }

    private String getTetheringSubnet() {
        Log.d(TAG, "--- 開始查找 USB 網路共享網段 ---");
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            if (interfaces.isEmpty()) {
                Log.w(TAG, "getTetheringSubnet: 警告：系統未回傳任何網路介面。");
                return null;
            }

            for (NetworkInterface intf : interfaces) {
                String interfaceName = intf.getName().toLowerCase();
                boolean isUp = intf.isUp();
                Log.d(TAG, "getTetheringSubnet: 正在檢查介面: '" + intf.getName() + "', 是否啟用(Up): " + isUp);

                if (isUp) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            String ip = addr.getHostAddress();
                            Log.d(TAG, "getTetheringSubnet:  -> 找到 IPv4 位址: " + ip);

                            // 關鍵判斷條件
                            if (interfaceName.contains("rndis") || interfaceName.contains("usb") || interfaceName.contains("tether")) {
                                Log.d(TAG, "getTetheringSubnet:  -> 介面 '" + intf.getName() + "' 符合條件！網段推導成功。");
                                int lastDot = ip.lastIndexOf('.');
                                if(lastDot > 0) {
                                    return ip.substring(0, lastDot + 1);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "getTetheringSubnet: 查詢網段時出錯", ex);
        }
        Log.e(TAG, "--- 查找結束，未找到任何符合條件的 USB 共享介面 ---");
        return null;
    }

    private boolean establishPermanentConnection(String ip) {
        try {
            Log.d(TAG, "establishPermanentConnection: 正在建立到 " + ip + " 的永久連接...");
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, RASPBERRY_PI_PORT), 7000);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            isConnecting = false;
            Log.d(TAG, "establishPermanentConnection: ✅ 永久連接建立成功！");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "establishPermanentConnection: ❌ 建立永久連接失敗", e);
            disconnectNetwork();
            return false;
        }
    }

    public boolean isConnected() { return socket != null && socket.isConnected(); }
    public String getConnectedDeviceName() { return isConnected() ? "Raspberry Pi (IP: " + foundIpAddress + ")" : "N/A"; }
    public String getConnectionStatus() { return isConnected() ? "已透過掃描連接" : "未連接"; }

    public void sendUsbCommand(String command) {
        if (!isConnected() || outputStream == null) {
            Log.e(TAG, "sendUsbCommand: 無法發送命令，Socket 未連接。");
            return;
        }
        new Thread(() -> {
            try {
                String commandToSend = command + "\n";
                outputStream.write(commandToSend.getBytes("UTF-8"));
                outputStream.flush();
                Log.d(TAG, "sendUsbCommand: 命令發送成功: " + command);
            } catch (Exception e) {
                Log.e(TAG, "sendUsbCommand: 命令發送失敗", e);
                disconnectNetwork();
            }
        }).start();
    }

    public String receiveUsbResponse() {
        if (!isConnected() || inputStream == null) {
            Log.e(TAG, "receiveUsbResponse: 無法接收回應，Socket 未連接。");
            return null;
        }
        try {
            byte[] buffer = new byte[4096];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead > 0) {
                String response = new String(buffer, 0, bytesRead, "UTF-8");
                Log.d(TAG, "receiveUsbResponse: 收到回應: " + response.trim());
                return response;
            }
            Log.w(TAG, "receiveUsbResponse: 接收時讀取到 EOF，連接可能已由對方關閉。");
            disconnectNetwork();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "receiveUsbResponse: 接收回應失敗", e);
            disconnectNetwork();
            return null;
        }
    }

    public void disconnectNetwork() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (Exception e) { /* Do nothing */ }
        finally {
            inputStream = null;
            outputStream = null;
            socket = null;
            isConnecting = false;
            foundIpAddress = null;
        }
    }
}
