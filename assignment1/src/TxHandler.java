import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

    private UTXOPool pool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code pool}. This should make a copy of pool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        HashSet<UTXO> seenUTXOs = new HashSet<>();
        double inputValue = 0;
        double outputValue = 0;
        int index = 0;
        for (Transaction.Input tsInput : txInputs){
            UTXO utxo = new UTXO(tsInput.prevTxHash, tsInput.outputIndex);
            if (seenUTXOs.contains(utxo)) return false; //no UTXO is claimed multiple times
            seenUTXOs.add(utxo);
            if (!pool.contains(utxo)) return false;
            Transaction.Output tsInputFrom = pool.getTxOutput(utxo);
            // the signatures on each input of tx are valid
            if (!Crypto.verifySignature(tsInputFrom.address, tx.getRawDataToSign(index), tsInput.signature))
                return false;
            inputValue += tsInputFrom.value;
            index += 1;
        }
        for (Transaction.Output txOutputs : tx.getOutputs()){
            if (txOutputs.value < 0)
                return false;
            outputValue += txOutputs.value;
        }
        if (inputValue < outputValue)
            return false;
        return true;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTransactions  = new ArrayList<>();
        int initValidSize = 0;
        while (true){
            for (Transaction tx : possibleTxs) {
                if (isValidTx(tx)) {
                    validTransactions.add(tx);
                    for (Transaction.Input tsInput : tx.getInputs()) {
                        UTXO utxo = new UTXO(tsInput.prevTxHash, tsInput.outputIndex);
                        pool.removeUTXO(utxo);
                    }
                    for (int i = 0; i < tx.numOutputs(); i++) {
                        UTXO utxo = new UTXO(tx.getHash(), i);
                        pool.addUTXO(utxo, tx.getOutput(i));
                    }
                }
            }
            if (validTransactions.size() == initValidSize) break;
            initValidSize = validTransactions.size();
        }
        Transaction[] returnValue = new Transaction[validTransactions.size()];
        return validTransactions.toArray(returnValue);
    }

}
