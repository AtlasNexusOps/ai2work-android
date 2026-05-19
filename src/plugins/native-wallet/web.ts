import type { NativeEthWalletPlugin, WalletAccount } from "./definitions";

export class NativeEthWalletWeb implements NativeEthWalletPlugin {
  async create(): Promise<WalletAccount> {
    throw new Error("NativeEthWallet is only available on Android");
  }
  async getAddress(): Promise<{ address: string | null }> {
    return { address: null };
  }
  async signMessage(_: { message: string }): Promise<{ signature: string }> {
    throw new Error("NativeEthWallet is only available on Android");
  }
  async signTransaction(_: any): Promise<{ signedTx: string }> {
    throw new Error("NativeEthWallet is only available on Android");
  }
  async delete(): Promise<void> {
    throw new Error("NativeEthWallet is only available on Android");
  }
  async exists(): Promise<{ exists: boolean }> {
    return { exists: false };
  }
}
