export interface WalletAccount {
  address: string;
}

export interface NativeEthWalletPlugin {
  /** Generate a new wallet and store in Android Keystore */
  create(): Promise<WalletAccount>;

  /** Get the current wallet address (null if none) */
  getAddress(): Promise<{ address: string | null }>;

  /** Sign a personal message (EIP-191) */
  signMessage(options: { message: string }): Promise<{ signature: string }>;

  /** Sign a transaction and return the raw signed tx  */
  signTransaction(options: {
    to: string;
    value: string; // hex wei
    data: string; // hex
    nonce: number;
    gasLimit: string;
    maxFeePerGas: string;
    maxPriorityFeePerGas: string;
    chainId: number;
  }): Promise<{ signedTx: string }>;

  /** Delete the wallet from this device */
  delete(): Promise<void>;

  /** Check if wallet exists */
  exists(): Promise<{ exists: boolean }>;
}
