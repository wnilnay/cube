// UsbConnectionManager.java (最終穩定版 - 僅包含掃描模式)
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

/**
 * 管理與樹莓派透過 USB 網路共享的連接。
 * 此類別實現為單例模式，並只提供一種可靠的連接方式：完整掃描。
 */
public class UsbConnectionManager {
    private static final String TAG = "UsbConnectionManager";
    private static volatile UsbConnectionManager instance;
    private final Context appContext;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private volatile boolean isConnecting = false;
    private String foundIpAddress = null;

    private static final int RASPBERRY_PI_PORT = 8080;
    // 用於在記憶體中快取 IP 位址，應用程式關閉後即消失
    private String lastSuccessfullyConnectedIp = null;

    /**
     * 連接結果的資料類別。
     */
    public static class ConnectionResult {
        public final boolean success;
        public final String message;

        public ConnectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ConnectionResult success(String ip) {
            return new ConnectionResult(true, "連接成功: " + ip);
        }

        public static ConnectionResult failure(String message) {
            return new ConnectionResult(false, message);
        }
    }

    /**
     * 儲存 USB 網路介面資訊的內部輔助類別。
     */
    private static class UsbInterfaceInfo {
        final NetworkInterface networkInterface;
        final InetAddress localIpAddress;

        UsbInterfaceInfo(NetworkInterface networkInterface, InetAddress localIpAddress) {
            this.networkInterface = networkInterface;
            this.localIpAddress = localIpAddress;
        }
    }

    /**
     * 私有建構函式，防止外部實例化。
     * @param context 應用程式上下文。
     */
    private UsbConnectionManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * 獲取 UsbConnectionManager 的單例實例。
     * @param context 應用程式上下文。
     * @return UsbConnectionManager 的單例。
     */
    public static UsbConnectionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (UsbConnectionManager.class) {
                if (instance == null) {
                    instance = new UsbConnectionManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * 唯一的公開連接方法。
     * 透過完整掃描 USB 網路共享介面下的子網路，來尋找並連接到樹莓派伺服器。
     *
     * @param progressCallback 用於向 UI 回報進度字串的回呼。
     * @return 一個 CompletableFuture，其結果為 ConnectionResult 物件。
     */
    /**
     * 統一的連接方法，實現了「嘗試快速連接，失敗則自動降級為完整掃描」的邏輯。
     * @param progressCallback 用於向 UI 回報進度。
     * @return CompletableFuture 包含連接結果。
     */
    public CompletableFuture<ConnectionResult> connect(Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            if (isConnected()) {
                Log.w(TAG, "connect: 連接請求被中止，因為已有一個活動的連接。");
                return ConnectionResult.success(foundIpAddress);
            }
            if (isConnecting) {
                return ConnectionResult.failure("已在連接中");
            }

            isConnecting = true;
            disconnectNetwork(); // 確保開始前是乾淨狀態

            progressCallback.accept("正在查找 USB 網路介面...");
            UsbInterfaceInfo usbInfo = findUsbInterfaceAndLocalIp();
            if (usbInfo == null) {
                isConnecting = false;
                return ConnectionResult.failure("找不到 USB 網路共享介面。\n請確認已在系統設定中啟用。");
            }

            // 步驟 1: 嘗試使用記憶體中的 IP 進行快速連接
            if (lastSuccessfullyConnectedIp != null) {
                progressCallback.accept("正在嘗試快速連接到: " + lastSuccessfullyConnectedIp + "...");
                if (establishPermanentConnection(lastSuccessfullyConnectedIp, usbInfo.localIpAddress)) {
                    Log.i(TAG, "✅ 快速連接成功！");
                    return ConnectionResult.success(lastSuccessfullyConnectedIp);
                } else {
                    progressCallback.accept("快速連接失敗，自動降級為完整掃描...");
                    Log.w(TAG, "快速連接到 " + lastSuccessfullyConnectedIp + " 失敗，將自動開始掃描。");
                }
            }

            // 步驟 2: 如果沒有快取 IP 或快速連接失敗，則執行完整掃描
            String localIpStr = usbInfo.localIpAddress.getHostAddress();
            String subnet = localIpStr.substring(0, localIpStr.lastIndexOf('.') + 1);
            progressCallback.accept("正在掃描樹莓派 (" + subnet + "x)...");

            for (int i = 1; i <= 254; i++) {
                if (localIpStr.endsWith("." + i)) continue;

                String host = subnet + i;
//                if (i % 20 == 0 || i == 1 || i == 254) {
//                    progressCallback.accept(String.format("正在掃描: %s (%d/254)", host, i));
//                }
                progressCallback.accept(String.format("正在掃描: %s (%d/254)", host, i));

                // 使用 try-with-resources 確保臨時 Socket 被關閉
                try (Socket tempSocket = new Socket()) {
                    tempSocket.bind(new InetSocketAddress(usbInfo.localIpAddress, 0));
                    tempSocket.connect(new InetSocketAddress(host, RASPBERRY_PI_PORT), 75);

                    // 掃描找到後，直接建立永久連接
                    if (establishPermanentConnection(host, usbInfo.localIpAddress)) {
                        Log.i(TAG, "✅ 掃描連接成功！");
                        return ConnectionResult.success(host);
                    } else {
                        // 如果永久連接建立失敗，是嚴重錯誤，停止掃描
                        break;
                    }
                } catch (Exception e) {
                    // Normal during scan
                }
            }

            isConnecting = false;
            return ConnectionResult.failure("已掃描網段，但未找到伺服器。\n請確認樹莓派程式是否運行？");
        });
    }

    /**
     * 查找當前活躍的、符合 USB 網路共享特徵的網路介面及其 IPv4 位址。
     *
     * @return 包含介面和位址資訊的 UsbInterfaceInfo 物件，如果找不到則返回 null。
     */
    private UsbInterfaceInfo findUsbInterfaceAndLocalIp() {
        Log.d(TAG, "====== 開始查找 USB 網路介面 ======");
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                String interfaceName = intf.getName().toLowerCase();
                boolean isUp = intf.isUp();
                // 關鍵字匹配，涵蓋了大多數 Android 手機的 USB 共享網路名稱
                boolean isUsbKeywordMatch = interfaceName.contains("rndis") || interfaceName.contains("usb") || interfaceName.contains("tether");

                if (isUp && isUsbKeywordMatch) {
                    Log.i(TAG, "-> 找到候選介面: '" + intf.getName() + "'。正在查找其 IPv4 位址...");
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            Log.i(TAG, "✅ 成功鎖定 USB 網路介面！名稱: " + intf.getName() + ", 本機 IP: " + addr.getHostAddress());
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

    /**
     * 建立一個永久的 Socket 連接。
     * @return 如果連接成功，返回 true。
     */
    private boolean establishPermanentConnection(String serverIp, InetAddress localIp) {
        try {
            Log.d(TAG, "establishPermanentConnection: 正在建立到 " + serverIp + " 的永久連接...");
            socket = new Socket();
            socket.bind(new InetSocketAddress(localIp, 0));
            socket.connect(new InetSocketAddress(serverIp, RASPBERRY_PI_PORT), 7000);

            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            foundIpAddress = serverIp;
            lastSuccessfullyConnectedIp = serverIp; // 在記憶體中快取 IP
            isConnecting = false;
            Log.i(TAG, "establishPermanentConnection: ✅ 連接建立成功，IP '" + serverIp + "' 已在記憶體中快取。");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "establishPermanentConnection: 建立連接失敗", e);
            disconnectNetwork();
            return false;
        }
    }

    /**
     * 檢查當前是否處於已連接狀態。
     *
     * @return 如果 socket 存在且已連接，返回 true。
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * 發送一個命令字串到樹莓派。此操作在背景執行緒中執行。
     *
     * @param command 要發送的命令字串。
     */
    public void sendUsbCommand(String command) {
        if (!isConnected() || outputStream == null) {
            Log.e(TAG, "sendUsbCommand: 無法發送命令，Socket 未連接。");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                // 確保命令以換行符結尾
                String commandToSend = command.endsWith("\n") ? command : command + "\n";
                outputStream.write(commandToSend.getBytes("UTF-8"));
                outputStream.flush();
                Log.d(TAG, "sendUsbCommand: 命令發送成功: " + command.trim());
            } catch (Exception e) {
                Log.e(TAG, "sendUsbCommand: 命令發送失敗，將斷開連接", e);
                // 發送失敗通常意味著連接已死，觸發斷開
                disconnectNetwork();
            }
        });
    }

    /**
     * 獲取 Socket 的輸入流，供外部（如 UsbTerminalActivity）持續監聽。
     *
     * @return Socket 的 InputStream，如果未連接則返回 null。
     */
    public InputStream getSocketInputStream() {
        if (isConnected()) {
            return inputStream;
        }
        return null;
    }

    /**
     * 斷開連接並清理所有相關資源。
     * 這是一個安全的方法，可以被多次呼叫。
     */
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
}