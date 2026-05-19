import { registerPlugin } from "@capacitor/core";
import type { NativeEthWalletPlugin } from "./definitions";

const NativeEthWallet = registerPlugin<NativeEthWalletPlugin>("NativeEthWallet", {
  web: () => import("./web").then((m) => new m.NativeEthWalletWeb()),
});

export * from "./definitions";
export { NativeEthWallet };
