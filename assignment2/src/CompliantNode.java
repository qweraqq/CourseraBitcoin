import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    private Set<Transaction> validTransactions = null;
    private double p_grapg = 0;
    private double p_malicious = 0;
    private double p_txDistribution = 0;
    private HashMap<Integer, Set<Transaction>> seenProposals = null;
    private int round = 0;
    private int numRounds = 0;
    private int numFollowees = 0;
    private Set<Integer> validNodes;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        validTransactions = new HashSet<>();
        seenProposals = new HashMap<>();
        this.round = 0;
        this.numRounds = numRounds;
        this.p_grapg = p_graph;
        this.p_malicious =  p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numFollowees = 0;
        this.validNodes = new HashSet<>();
    }

    public void setFollowees(boolean[] followees) {
        for (int i = 0; i < followees.length; i++){
            if (followees[i]){
                Set<Transaction> transactions = new HashSet<>();
                seenProposals.put(i, transactions);
                this.numFollowees += 1;
            }
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        for (Transaction tx: pendingTransactions) {
            validTransactions.add(tx);
        }
    }

    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS
        round += 1;
        if (round > numRounds) {
            Set<Transaction> ret = new HashSet<>();
            HashMap<Transaction, Integer> txNodeConfirmCount = new HashMap<>();
            for (int sender : seenProposals.keySet()){
                for (Transaction tx : seenProposals.get(sender)){
                    if (!txNodeConfirmCount.containsKey(tx))
                        txNodeConfirmCount.put(tx, 1);
                    else
                        txNodeConfirmCount.put(tx, txNodeConfirmCount.get(tx)+1);
                }
            }
            for (Transaction tx: txNodeConfirmCount.keySet())
                if (txNodeConfirmCount.get(tx) > (1 - this.p_malicious)*this.numFollowees-1)
                    ret.add(tx);
            return ret;
        }
        else {
            return validTransactions;
        }
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS

        for (Candidate candidate : candidates){

            seenProposals.get(candidate.sender).add(candidate.tx);

            if (!validTransactions.contains(candidate.tx))
                validTransactions.add(candidate.tx);
        }
    }
}
