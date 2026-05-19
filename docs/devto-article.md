---
title: "Embedded Ethereum Wallet in Capacitor Android — No MetaMask Required"
published: true
tags: [android, ethereum, web3, kotlin]
series: ""
canonical_url: "https://atlasnexus.tech"
---

![Architecture diagram](https://atlasnexus.tech/wallet-architecture.png)

**Your Android app can sign Ethereum transactions without MetaMask, without WalletConnect, and without any external dependency.** Here's how we built a native, hardware-backed Ethereum wallet inside a Capacitor-wrapped Android app — and made it fully compatible with wagmi and connectkit out of the box.

---

## The Problem

You've built a Next.js web3 app. It works beautifully on desktop with MetaMask. Now you wrap it in Capacitor for Android — and nothing works.

The WebView has no `window.ethereum`. MetaMask's mobile browser won't inject into your Capacitor app. WalletConnect requires users to switch apps. Every solution adds friction.

What if the wallet just… existed inside the app?

---

## Architecture

We built a three-layer native wallet that lives entirely inside the APK:

```
┌─────────────────────────────────────┐
│  WebView (ai2work.onrender.com)     │
│  React + wagmi + connectkit         │
│  window.ethereum ✓                  │
└──────────┬──────────────────────────┘
           │ @JavascriptInterface
┌──────────▼──────────────────────────┐
│  _ai2workWallet (Bridge)            │
│  Injects window.ethereum shim       │
│  Compatible with wagmi/connectkit   │
└──────────┬──────────────────────────┘
           │ Capacitor Plugin API
┌──────────▼──────────────────────────┐
│  NativeEthWalletPlugin              │
│  @CapacitorPlugin bridge            │
└──────────┬──────────────────────────┘
           │
┌──────────▼──────────────────────────┐
│  EthWalletHelper (Kotlin)           │
│  secp256k1 ECDSA                    │
│  EIP-191 personal_sign              │
│  EIP-1559 transaction signing       │
│  Keccak256 + RLP (zero-dependency)  │
└──────────┬──────────────────────────┘
           │ AES-256/GCM
┌──────────▼──────────────────────────┐
│  Android Keystore                   │
│  Hardware-backed encryption         │
└─────────────────────────────────────┘
```

The key insight: **`@JavascriptInterface` runs before Capacitor's JavaScript bridge initializes.** This means `window.ethereum` is available the moment the remote web app starts loading — zero race conditions.

---

## The Bridge: Making wagmi See a Native Wallet

Here's the JavaScript injected into the WebView by the native plugin:

```javascript
window.ethereum = {
  isAI2Work: true,
  isMetaMask: false,

  request({ method, params }) {
    // wagmi calls this for everything
    if (method === "eth_requestAccounts") {
      return getOrCreateWallet().then(addr => [addr])
    }
    if (method === "personal_sign") {
      return _ai2workWallet.signMessage(hexToUtf8(params[0]))
    }
    if (method === "eth_signTransaction") {
      return _ai2workWallet.signTransaction(/* EIP-1559 params */)
    }
    if (method === "eth_chainId") return "0x14a34" // Celo
    // ...
  }
}
```

**That's it.** Wagmi detects `window.ethereum`, calls `eth_requestAccounts`, and gets an address back. The rest is automatic. No fork of wagmi. No custom connector. No modified provider.

---

## Pure Kotlin Crypto: Keccak256 in 200 Lines

We refused to pull Web3j (8+ MB) into the APK. Instead, we implemented Keccak256 directly — the sponge construction with all 24 rounds of theta/rho/pi/chi/iota:

```kotlin
object Keccak256 {
    private val RC = longArrayOf(
        0x0000000000000001L, 0x0000000000008082L,
        0x800000000000808AL, 0x8000000080008000L,
        // ... all 24 round constants
    )

    fun digest(input: ByteArray): ByteArray {
        val state = LongArray(25)
        val buf = ByteArray(136) // r = 1088 bits
        // absorb → pad → keccakF × 24 → squeeze
    }
}
```

Combined with a minimal RLP encoder and BouncyCastle's secp256k1 (already in Android), the entire crypto layer adds **under 50 KB** to the APK.

---

## EIP-1559 Transaction Signing

Signing an EIP-1559 transaction from native Kotlin:

```kotlin
fun signTransaction(
    to: String, value: BigInteger, data: ByteArray,
    nonce: Long, gasLimit: BigInteger,
    maxFeePerGas: BigInteger, maxPriorityFeePerGas: BigInteger,
    chainId: Long
): String {
    val rlp = RLP.list(
        RLP.long(chainId), RLP.long(nonce),
        RLP.bigint(maxPriorityFeePerGas), RLP.bigint(maxFeePerGas),
        RLP.bigint(gasLimit), RLP.address(to),
        RLP.bigint(value), RLP.bytes(data),
        RLP.list() // empty access list
    )
    val unsigned = byteArrayOf(0x02) + rlp
    val hash = Keccak256.digest(unsigned)
    val (r, s, v) = signForRecovery(hash, privateKey)

    return "0x02" + RLP.list(
        /* ... full tx with signature ... */
    )
}
```

The signed transaction can be broadcast directly to any RPC endpoint — no Ethereum library required on the client side.

---

## Key Storage: Hardware-Backed AES-256/GCM

The private key never exists in plaintext outside the Android Keystore:

```kotlin
private fun encryptWithKeystore(data: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAesKey())
    return cipher.iv + cipher.doFinal(data)
}
```

**Zero BIP39 mnemonics.** The key is generated on-device, encrypted immediately, and the Keystore entry is non-exportable. Delete the app = delete the wallet = generate a new one.

---

## Integration: 3 Lines in Your Web App

If your web app already uses wagmi:

```tsx
// No changes needed. wagmi auto-detects window.ethereum.
<ConnectButton />  // ← This just works now
```

For the Capacitor config:

```ts
const config: CapacitorConfig = {
  appId: "com.yourapp",
  server: { url: "https://your-dapp.com" },
  plugins: { SplashScreen: { /* ... */ } },
}
```

---

## Results

| Metric | Before | After |
|--------|--------|-------|
| Wallet connection steps | 4 (switch to MetaMask, approve, switch back) | 1 (tap Connect) |
| APK size impact | N/A | +50 KB crypto + 1.5 MB BouncyCastle |
| External dependencies | MetaMask or WalletConnect required | **Zero** |
| Key security | Depends on external app | Hardware Keystore |
| Web3 library on-device | ethers.js (800 KB) | None |

---

## Open Source

All code is MIT licensed:

- 📱 **Android app**: [AtlasNexusTech/ai2work-android](https://github.com/AtlasNexusTech/ai2work-android)
- 🌐 **Web app**: [AtlasNexusTech/ai2work](https://github.com/AtlasNexusTech/ai2work)
- 🏗️ **Live diagram**: [atlasnexus.tech/wallet-architecture.html](https://atlasnexus.tech/wallet-architecture.html)

---

*Built with Capacitor 7, Kotlin, and the conviction that mobile web3 shouldn't require users to install a second app just to sign a transaction.*
