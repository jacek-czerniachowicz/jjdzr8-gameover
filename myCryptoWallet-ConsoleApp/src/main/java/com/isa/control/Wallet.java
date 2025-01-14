package com.isa.control;

import com.isa.control.transactions.ActiveTransaction;
import com.isa.control.transactions.ClosedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Wallet {
    private static Logger LOGGER = LoggerFactory.getLogger(Wallet.class.getName());
    private String walletId;
    private double walletSum;
    private double profitLoss;
    private double historicalProfitLoss;
    private double transactionsCosts;
    private double walletBalance;
    private double paymentCalc;
    private Set<ClosedTransaction> transactionsHistory = new TreeSet<>();
    private Set<ActiveTransaction> activeTransactions = new TreeSet<>();
    public Wallet(){}

    public Wallet(String walletId){
        this.walletId = walletId;
        this.walletBalance = 0;
        this.paymentCalc = 0;
    }

    public void loadWalletBalance(double funds){
        if (funds > 0){
            updateWallet();
            this.paymentCalc += funds;
            LOGGER.info("{} USD added to wallet balance", funds);
        }
    }

    public void withdrawalFunds(double funds){
        updateWallet();
        if (funds > 0 && funds <= walletBalance){
            this.paymentCalc -= funds;
            LOGGER.info("{} USD withdrawal from wallet balance", funds);
        }
    }

    public void buyNewToken(Coin coin, double volume){
        ActiveTransaction activeTransaction = new ActiveTransaction(coin, volume);
        if(activeTransaction.countTransactionCost() < walletBalance) {
            activeTransactions.add(activeTransaction);
            LOGGER.info("Transaction completed successfully.");
            System.out.println("transakcja zawarta pomyślnie");
        }else{
            LOGGER.info("The transaction value exceeds the amount of funds available in the wallet.");
            System.out.println("wartość transakcji przekracza ilość środków dostępnych w portfelu");
            throw new RuntimeException("The transaction value exceeds the amount of funds available in the wallet.");
        }
    }
    public void closeActiveTransaction(ActiveTransaction transaction, double volume){
        long idTransaction = transaction.getIdTransaction();

        if(transaction.getVolume()<=volume){
            ClosedTransaction closed = new ClosedTransaction(transaction);
            transactionsHistory.add(closed);
            activeTransactions.removeIf(n->n.getIdTransaction() == idTransaction);
            LOGGER.info("Transaction {} closed successfully.", idTransaction);

        } else if (transaction.getVolume()>volume && volume>0) {
            ClosedTransaction closed = new ClosedTransaction(transaction, volume);
            transactionsHistory.add(closed);
            activeTransactions.removeIf(n->n.getIdTransaction() == idTransaction);
            LOGGER.info("Transaction {} closed successfully.", idTransaction);
            ActiveTransaction newActiveTransaction = closed.getActivePartOfClosedTransaction();
            activeTransactions.add(newActiveTransaction);
            LOGGER.info("New transaction opened.");
        }
        else {
            LOGGER.info("Volume must be greater than zero");
            System.out.println("volumen musi być liczbą dodatnią");
        }

    }
    public void updateWallet(){
        if (!activeTransactions.isEmpty()){
            activeTransactions.forEach(ActiveTransaction::refreshPrice);

            List<ActiveTransaction> slList = activeTransactions.stream().filter(n -> shouldStopLossExecute()).toList();
            if(!slList.isEmpty()) slList.forEach(this::executeStopLossAlarm);

            List<ActiveTransaction> tpList = activeTransactions.stream().filter(n -> shouldTakeProfitExecute()).toList();
            if(!tpList.isEmpty()) tpList.forEach(this::executeTakeProfitAlarm);
        }
        historyProfitCount();
        currentProfitCount();
        countActiveTransactionsCosts();
        countWalletBalance();
        countWalletSum();
        LOGGER.info("Wallet updated successfully.");
    }
    public void currentProfitCount(){
        if(!activeTransactions.isEmpty()) {
            this.profitLoss = activeTransactions.stream().mapToDouble(ActiveTransaction::countProfit).sum();
        }else this.profitLoss = 0;
        LOGGER.trace("Open transactions profit updated.");
    }

    public void historyProfitCount(){
        if(!transactionsHistory.isEmpty()){
            this.historicalProfitLoss =  transactionsHistory.stream().mapToDouble(ClosedTransaction::countProfit).sum();
        }else this.historicalProfitLoss = 0;
        LOGGER.trace("Closed transactions profit updated.");
    }


    public void countWalletBalance(){
        this.walletBalance = paymentCalc - transactionsCosts + historicalProfitLoss + profitLoss;
        LOGGER.trace("Wallet available founds updated to {}", this.walletBalance);
    }

    public void countWalletSum(){
        this.walletSum = paymentCalc + historicalProfitLoss + profitLoss;
        LOGGER.trace("Wallet worth updated to {}.", this.walletSum);
    }
    public void countActiveTransactionsCosts() {
        if (!activeTransactions.isEmpty()) {
            this.transactionsCosts = activeTransactions.stream().mapToDouble(ActiveTransaction::countTransactionCost).sum();
        }else this.transactionsCosts = 0;
        LOGGER.trace("Active transactions costs updated to: {}", this.transactionsCosts);
    }
    public void executeStopLossAlarm(ActiveTransaction activeTransaction){
        if(activeTransaction.isSLOn() && activeTransaction.getCurrentPrice() <= activeTransaction.getStopLoss()){
            closeActiveTransaction(activeTransaction, activeTransaction.getVolume());
            LOGGER.trace("Stop Loss executed for id transaction: {}", activeTransaction.getIdTransaction());
        }
    }

    public boolean shouldStopLossExecute(){
       return activeTransactions.stream().anyMatch(n-> n.isSLOn() && n.getCurrentPrice() <= n.getStopLoss());
    }

    public void executeTakeProfitAlarm(ActiveTransaction activeTransaction){
        if(activeTransaction.isTPOn() && activeTransaction.getCurrentPrice() >= activeTransaction.getTakeProfit()){
            closeActiveTransaction(activeTransaction, activeTransaction.getVolume());
            LOGGER.trace("Take profit executed for id transaction: {}", activeTransaction.getIdTransaction());
        }
    }

    public boolean shouldTakeProfitExecute(){
        return activeTransactions.stream().anyMatch(n-> n.isTPOn() && n.getCurrentPrice() >= n.getTakeProfit());
    }

    public static Wallet createNewWalletFromKeyboard(Scanner scanner){
//        System.out.println("podaj unikatową nazwę portfela");
//        String idForNewWallet = scanner.nextLine();
//        System.out.println("wybierz początkową wartość portfela:");
//        Balance.printBalance();
//        double walletBalance = scanner.nextDouble();
//        Balance balance = Balance.getBalance(walletBalance);
//        return new Wallet(idForNewWallet,balance);
        return new Wallet();
    }

    public static Coin searchCoinForBuying(){
        System.out.println("wybierz token który chcesz kupić");
        CoinSearch coinSearch = new CoinSearch();
        List<Coin> yourToken = new ArrayList<>();
        while (yourToken.isEmpty()) {
            yourToken = coinSearch.findYourToken();
        }
        return yourToken.get(0);
    }
    public ActiveTransaction searchActiveTransaction(Scanner scanner){
        activeTransactions.forEach(ActiveTransaction::printDetails);
        List<Long> idTransactionList = new ArrayList<>();
        activeTransactions.forEach(n -> idTransactionList.add(n.getIdTransaction()));
        System.out.println("wpisz ID aby wybrać pozycję");
        long idActiveTransaction = 0;
        while (!idTransactionList.contains(idActiveTransaction)) {
            idActiveTransaction = scanner.nextLong();
        }
        long finalIdActiveTransaction = idActiveTransaction;
        List<ActiveTransaction> activeTransactionList = activeTransactions.stream()
                .filter(n -> n.getIdTransaction() == finalIdActiveTransaction).collect(Collectors.toList());
        return activeTransactionList.get(0);
    }

    public ActiveTransaction searchActiveTransaction(long id){
        return activeTransactions.stream()
                .filter(n -> n.getIdTransaction() == id)
                .findFirst().orElse(new ActiveTransaction());
    }

    public boolean isActiveTransactionsContainsId(long idTransaction){
        Set<Long> idTransactionsSet = new HashSet<>();
        activeTransactions.forEach(n->idTransactionsSet.add(n.getIdTransaction()));
        return idTransactionsSet.contains(idTransaction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return Double.compare(wallet.walletSum, walletSum) == 0 && Double.compare(wallet.profitLoss, profitLoss) == 0 && Double.compare(wallet.historicalProfitLoss, historicalProfitLoss) == 0 && Double.compare(wallet.transactionsCosts, transactionsCosts) == 0 && Double.compare(wallet.walletBalance, walletBalance) == 0 && Double.compare(wallet.paymentCalc, paymentCalc) == 0 && Objects.equals(walletId, wallet.walletId) && Objects.equals(transactionsHistory, wallet.transactionsHistory) && Objects.equals(activeTransactions, wallet.activeTransactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletId, walletSum, profitLoss, historicalProfitLoss, transactionsCosts, walletBalance, paymentCalc, transactionsHistory, activeTransactions);
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public double getWalletSum() {
        return walletSum;
    }

    public void setWalletSum(double walletSum) {
        this.walletSum = walletSum;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(double profitLoss) {
        this.profitLoss = profitLoss;
    }

    public double getHistoricalProfitLoss() {
        return historicalProfitLoss;
    }

    public void setHistoricalProfitLoss(double historicalProfitLoss) {
        this.historicalProfitLoss = historicalProfitLoss;
    }

    public double getTransactionsCosts() {
        return transactionsCosts;
    }

    public void setTransactionsCosts(double transactionsCosts) {
        this.transactionsCosts = transactionsCosts;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    public double getPaymentCalc() {
        return paymentCalc;
    }

    public void setPaymentCalc(double paymentCalc) {
        this.paymentCalc = paymentCalc;
    }

    public Set<ClosedTransaction> getTransactionsHistory() {
        return transactionsHistory;
    }

    public void setTransactionsHistory(Set<ClosedTransaction> transactionsHistory) {
        this.transactionsHistory = transactionsHistory;
    }

    public Set<ActiveTransaction> getActiveTransactions() {
        return activeTransactions;
    }

    public void setActiveTransactions(Set<ActiveTransaction> activeTransactions) {
        this.activeTransactions = activeTransactions;
    }
}
