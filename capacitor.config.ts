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
};

export default config;
