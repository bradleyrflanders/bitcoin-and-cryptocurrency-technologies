import java.util.ArrayList;
import java.util.HashMap;

public class TxHandler {

    /** Unspent transaction output pool (src.UTXOPool) for referencing unspent transactions */
    private UTXOPool uPool;
    /**
     * Creates a public ledger whose current src.UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the src.UTXOPool(src.UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        uPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current src.UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no src.UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        HashMap<UTXO, Integer> claimedUTXOPool = new HashMap<UTXO, Integer>(); // record claims for duplicate check
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
        double sumIn = 0.0;
        double sumOut = 0.0;

        // Transaction.Input index i needed, traditional for loop
        for (int i = 0; i < txInputs.size(); i++) {
            Transaction.Input in = txInputs.get(i);
            UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output txOutput = uPool.getTxOutput(ut);

            /** (1) */
            if (txOutput == null) { return false; }
            sumIn += txOutput.value;

            /** (2) */
            if (Crypto.verifySignature(txOutput.address, tx.getRawDataToSign(i), in.signature) == false) { return false; }

            /** (3) */
            if (claimedUTXOPool.containsKey(ut)) { return false; }
            claimedUTXOPool.put(ut, 0);
        }

        /** (4) */
        for (Transaction.Output out : txOutputs) {
            if (out.value < 0) { return false; }
            sumOut += out.value;
        }
        /** (5) */
        if (sumOut > sumIn) { return false; }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current src.UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx) == true) {
                ArrayList<Transaction.Input> txInputs = tx.getInputs();
                ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
                byte[] txHash = tx.getHash();

                // add valid transaction to accepted array
                validTxs.add(tx);

                // remove UTXOs claimed for transaction inputs
                for (Transaction.Input in : txInputs) {
                    UTXO removedUtxo = new UTXO(in.prevTxHash, in.outputIndex);
                    uPool.removeUTXO(removedUtxo);
                }

                // add new UTXOs to pool, need index
                for (int j = 0; j < txOutputs.size(); j++) {
                    UTXO addedUtxo = new UTXO(txHash, j);
                    uPool.addUTXO(addedUtxo, txOutputs.get(j));
                }
            }
        }

        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
