import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "tech.atlasnexus.ai2work",
  appName: "AI2Work",
  webDir: "www",
  server: {
    url: "https://ai2work.onrender.com",
    cleartext: false,
  },
  android: {
    allowMixedContent: true,
    webContentsDebuggingEnabled: false,
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 0,
      launchAutoHide: true,
      backgroundColor: "#0B1120",
      showSpinner: false,
    },
  },
  includePlugins: ["NativeEthWallet"],
};

export default config;
