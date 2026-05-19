/**
 * AI2Work Native Wallet Bridge (fallback)
 * Uses _ai2workWallet JavascriptInterface if Capacitor.Plugins not yet ready.
 */
(function () {
  if (window.ethereum && window.ethereum.isAI2Work) return; // already injected

  var w = window._ai2workWallet;
  if (!w) { console.log("[AI2Work] No native wallet interface"); return; }

  var addr = null, connected = false;
  try { addr = w.getAddress(); connected = !!addr; } catch(e) {}

  function getOrCreate() {
    if (addr) return Promise.resolve(addr);
    try {
      if (w.exists()) addr = w.getAddress();
      else addr = w.create();
      connected = !!addr;
      return Promise.resolve(addr);
    } catch(e) { return Promise.reject(e); }
  }

  window.ethereum = {
    isAI2Work: true, isMetaMask: false,
    isConnected: function() { return connected; },
    request: function(a) {
      var m = a.method, p = a.params || [];
      if (m === "eth_requestAccounts" || m === "eth_accounts") return getOrCreate().then(function(aa) { return [aa]; });
      if (m === "eth_chainId") return Promise.resolve("0x14a34");
      if (m === "personal_sign") {
        var msg = p[0];
        if (msg && msg.startsWith("0x")) {
          var s = "";
          for (var i = 2; i < msg.length; i += 2) s += String.fromCharCode(parseInt(msg.substr(i, 2), 16));
          msg = s;
        }
        return Promise.resolve(w.signMessage(msg));
      }
      if (m === "eth_signTransaction" || m === "eth_sendTransaction") {
        var t = p[0];
        return Promise.resolve(w.signTransaction(
          t.to || "0x", t.value || "0", t.data || "0x",
          String(t.nonce || 0), t.gas || "0",
          t.maxFeePerGas || t.gasPrice || "0",
          t.maxPriorityFeePerGas || "0", "42220"
        ));
      }
      if (m === "eth_getBalance") return Promise.resolve("0x0");
      if (m === "wallet_switchEthereumChain" || m === "wallet_addEthereumChain") return Promise.resolve(null);
      return Promise.reject(new Error("Unsupported: " + m));
    },
    on: function(e, cb) { if (e === "chainChanged") cb("0x14a34"); },
    removeListener: function() {},
    enable: function() { return getOrCreate().then(function(aa) { return [aa]; }); }
  };
  console.log("[AI2Work] Bridge fallback injected");
})();
