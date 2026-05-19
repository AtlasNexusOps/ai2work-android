package tech.atlasnexus.ai2work.wallet;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.math.BigInteger;

@CapacitorPlugin(name = "NativeEthWallet")
public class NativeEthWalletPlugin extends Plugin {

    private static final String TAG = "NativeEthWallet";

    // ─── @JavascriptInterface bridge (always available, even before Capacitor init) ───
    public class WalletJsBridge {
        @JavascriptInterface
        public String getAddress() { return EthWalletHelper.INSTANCE.getAddress(); }

        @JavascriptInterface
        public boolean exists() { return EthWalletHelper.INSTANCE.exists(); }

        @JavascriptInterface
        public String create() { return EthWalletHelper.INSTANCE.createWallet(); }

        @JavascriptInterface
        public void delete() { EthWalletHelper.INSTANCE.delete(); }

        @JavascriptInterface
        public String signMessage(String message) {
            try { return EthWalletHelper.INSTANCE.signMessage(message); }
            catch (Exception e) { Log.e(TAG, "signMessage err", e); return "ERROR:" + e.getMessage(); }
        }

        @JavascriptInterface
        public String signTransaction(String to, String value, String data,
                                       String nonce, String gasLimit,
                                       String maxFeePerGas, String maxPriorityFeePerGas,
                                       String chainId) {
            try {
                return EthWalletHelper.INSTANCE.signTransaction(
                    to,
                    parseHexOrDecimal(value),
                    parseHexBytes(data),
                    Long.parseLong(nonce),
                    parseHexOrDecimal(gasLimit),
                    parseHexOrDecimal(maxFeePerGas),
                    parseHexOrDecimal(maxPriorityFeePerGas),
                    Long.parseLong(chainId)
                );
            } catch (Exception e) { Log.e(TAG, "signTx err", e); return "ERROR:" + e.getMessage(); }
        }
    }

    @Override
    public void load() {
        AppContextHolder.init(getContext());
        Log.i(TAG, "NativeEthWallet loaded, injecting bridge");
        injectBridge();
    }

    @Override
    protected void handleOnPageStarted() {
        injectBridge();
    }

    private void injectBridge() {
        try {
            WebView wv = getBridge().getWebView();
            // Add direct JS interface (works before Capacitor JS initializes)
            wv.addJavascriptInterface(new WalletJsBridge(), "_ai2workWallet");

            // Inject window.ethereum shim backed by _ai2workWallet
            String js =
                "(function(){" +
                "var w=window._ai2workWallet;if(!w){setTimeout(arguments.callee,30);return;}" +
                "var addr=null;" +
                "try{addr=w.getAddress();}catch(e){}" +
                "var connected=!!addr;" +
                "function getOrCreate(){" +
                "if(addr)return Promise.resolve(addr);" +
                "try{" +
                "var ex=w.exists();" +
                "if(ex){addr=w.getAddress();}else{addr=w.create();}" +
                "connected=!!addr;" +
                "return Promise.resolve(addr);" +
                "}catch(e){return Promise.reject(e);}" +
                "}" +
                "window.ethereum={isAI2Work:true,isMetaMask:false,isConnected:function(){return connected;}," +
                "request:function(a){var m=a.method,p=a.params||[];" +
                "if(m==='eth_requestAccounts'||m==='eth_accounts')return getOrCreate().then(function(aa){return[aa];});" +
                "if(m==='eth_chainId')return Promise.resolve('0x14a34');" +
                "if(m==='personal_sign'){var msg=p[0];if(msg&&msg.startsWith('0x')){var s='';for(var i=2;i<msg.length;i+=2)s+=String.fromCharCode(parseInt(msg.substr(i,2),16));msg=s;}return Promise.resolve(w.signMessage(msg));}" +
                "if(m==='eth_signTransaction'||m==='eth_sendTransaction'){var t=p[0];return Promise.resolve(w.signTransaction(t.to||'0x',t.value||'0',t.data||'0x',String(t.nonce||0),t.gas||'0',t.maxFeePerGas||t.gasPrice||'0',t.maxPriorityFeePerGas||'0',String(42220)));}" +
                "if(m==='eth_getBalance')return Promise.resolve('0x0');" +
                "if(m==='wallet_switchEthereumChain'||m==='wallet_addEthereumChain')return Promise.resolve(null);" +
                "return Promise.reject(new Error('Unsupported: '+m));}," +
                "on:function(e,cb){if(e==='chainChanged')cb('0x14a34');}," +
                "removeListener:function(){}," +
                "enable:function(){return getOrCreate().then(function(aa){return[aa];});}};" +
                "console.log('[AI2Work] Native wallet injected');" +
                "})()";

            wv.postDelayed(() -> wv.evaluateJavascript(js, r -> Log.i(TAG, "bridge result: " + r)), 50);
        } catch (Exception e) {
            Log.e(TAG, "injectBridge failed", e);
        }
    }

    // ─── Capacitor Plugin methods (for direct Capacitor.Plugins access) ───

    @PluginMethod
    public void create(PluginCall call) {
        try {
            JSObject ret = new JSObject();
            ret.put("address", EthWalletHelper.INSTANCE.createWallet());
            call.resolve(ret);
        } catch (Exception e) { call.reject("CREATE_FAILED", e.getMessage()); }
    }

    @PluginMethod
    public void getAddress(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("address", EthWalletHelper.INSTANCE.getAddress());
        call.resolve(ret);
    }

    @PluginMethod
    public void exists(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("exists", EthWalletHelper.INSTANCE.exists());
        call.resolve(ret);
    }

    @PluginMethod
    public void delete(PluginCall call) {
        EthWalletHelper.INSTANCE.delete();
        call.resolve();
    }

    @PluginMethod
    public void signMessage(PluginCall call) {
        String msg = call.getString("message");
        if (msg == null) { call.reject("MISSING_MESSAGE"); return; }
        try {
            JSObject ret = new JSObject();
            ret.put("signature", EthWalletHelper.INSTANCE.signMessage(msg));
            call.resolve(ret);
        } catch (Exception e) { call.reject("SIGN_FAILED", e.getMessage()); }
    }

    @PluginMethod
    public void signTransaction(PluginCall call) {
        try {
            String signed = EthWalletHelper.INSTANCE.signTransaction(
                call.getString("to"),
                parseHexOrDecimal(call.getString("value", "0x0")),
                parseHexBytes(call.getString("data", "0x")),
                call.getLong("nonce"),
                parseHexOrDecimal(call.getString("gasLimit")),
                parseHexOrDecimal(call.getString("maxFeePerGas")),
                parseHexOrDecimal(call.getString("maxPriorityFeePerGas")),
                call.getLong("chainId")
            );
            JSObject ret = new JSObject();
            ret.put("signedTx", signed);
            call.resolve(ret);
        } catch (Exception e) { call.reject("SIGN_TX_FAILED", e.getMessage()); }
    }

    private BigInteger parseHexOrDecimal(String s) {
        if (s == null || s.isEmpty()) return BigInteger.ZERO;
        if (s.startsWith("0x")) return new BigInteger(s.substring(2), 16);
        return new BigInteger(s);
    }

    private byte[] parseHexBytes(String s) {
        if (s == null || s.isEmpty() || s.equals("0x")) return new byte[0];
        String hex = s.startsWith("0x") ? s.substring(2) : s;
        if (hex.length() % 2 != 0) hex = "0" + hex;
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return out;
    }
}
