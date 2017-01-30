// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.LinkedHashMap;
import java.util.Map;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    public static final int MAX_BLOCK_IN_RAM = 100;

    private CandidateBlock maxHeightCandidateBlock = null;
    private LinkedHashMap<ByteArrayWrapper, CandidateBlock> blockChain = null;
    private TransactionPool txPool = null;

    private class CandidateBlock{
        public Block block = null;
        public UTXOPool utxoPool = null;
        public long height = 0;

        public CandidateBlock(Block block, UTXOPool parentUTXOPool, long parentHeight){
            this.block = block;
            this.height = parentHeight + 1;
            this.utxoPool = new UTXOPool(parentUTXOPool);
            for (Transaction tx: block.getTransactions()) {
                for (Transaction.Input tsInput : tx.getInputs()) {
                    UTXO utxo = new UTXO(tsInput.prevTxHash, tsInput.outputIndex);
                    this.utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    this.utxoPool.addUTXO(utxo, tx.getOutput(i));
                }
            }
            // coinbase
            Transaction coinbase = block.getCoinbase();
            UTXO utxo = new UTXO(coinbase.getHash(),0);
            for (int i = 0; i < coinbase.numOutputs(); i++)
                this.utxoPool.addUTXO(utxo, coinbase.getOutput(i));
        }
    }

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS

        txPool = new TransactionPool();
        blockChain = new LinkedHashMap<ByteArrayWrapper, CandidateBlock>(MAX_BLOCK_IN_RAM, 0.75f, false){
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_BLOCK_IN_RAM;
            }
        };
        CandidateBlock candidateBlock = new CandidateBlock(genesisBlock, new UTXOPool(), 0);
        blockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), candidateBlock);
        this.maxHeightCandidateBlock = candidateBlock;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return this.maxHeightCandidateBlock.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return this.maxHeightCandidateBlock.utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return new TransactionPool(txPool);
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        byte[] prevBlockHash = block.getPrevBlockHash();

        if (prevBlockHash == null)
            return false;

        if (!blockChain.containsKey(new ByteArrayWrapper(prevBlockHash)))
            return false;


        // check validation
        CandidateBlock prevBlock = blockChain.get(new ByteArrayWrapper(prevBlockHash));
        if (prevBlock.height < maxHeightCandidateBlock.height - CUT_OFF_AGE)
            return false;
        TxHandler txHandler = new TxHandler(prevBlock.utxoPool);
        Transaction[] proposedTXs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTXs = txHandler.handleTxs(proposedTXs);
        if (proposedTXs.length != validTXs.length)
            return false;
        CandidateBlock candidateBlock = new CandidateBlock(block, prevBlock.utxoPool, prevBlock.height);
        blockChain.put(new ByteArrayWrapper(block.getHash()), candidateBlock);
        if (candidateBlock.height > maxHeightCandidateBlock.height)
            maxHeightCandidateBlock = candidateBlock;
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        txPool.addTransaction(tx);
    }
}