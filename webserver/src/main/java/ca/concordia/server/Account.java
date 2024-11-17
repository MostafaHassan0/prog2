package ca.concordia.server;

import java.util.concurrent.Semaphore;

public class Account {
    //represent a bank account with a balance and withdraw and deposit methods
    private int balance;
    private final int id;
    private final Semaphore semaphore = new Semaphore(1);



    public Account(int balance, int id){

        this.balance = balance;
        this.id = id;
    }

    public int getBalance(){
        try{
            semaphore.acquire();
            return balance;
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return -1;
        }finally {
            semaphore.release();
        }
    }

    public void withdraw(int amount) {
        try {
            semaphore.acquire();
            balance -= amount;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release();
        }
    }

    public void deposit(int amount) {
        try {
            semaphore.acquire();
            balance += amount;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release();
        }
    }
}