package com.pao.project.bank;

import com.pao.project.bank.model.Cheque;
import com.pao.project.bank.model.IBAN;
import com.pao.project.bank.model.account.Account;
import com.pao.project.bank.model.account.CurrentAccount;
import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.person.IndividualClient;
import com.pao.project.bank.service.AccountService;
import com.pao.project.bank.service.ChequeService;
import com.pao.project.bank.service.ClientService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        ClientService clientService = ClientService.getInstance();
        AccountService accountService = AccountService.getInstance();
        ChequeService chequeService = ChequeService.getInstance();

        Client client1 = new IndividualClient(
                1,
                "ion.popescu@mail.com",
                "0711111111",
                "C001",
                true,
                "Ion",
                "Popescu",
                "1980101010017"
        );

        Client client2 = new IndividualClient(
                2,
                "maria.ionescu@mail.com",
                "0722222222",
                "C002",
                true,
                "Maria",
                "Ionescu",
                "2960730156784"
        );

        clientService.addClient(client1);
        clientService.addClient(client2);

        System.out.println("-SORTARE CLIENTI");
        List<Client> sortedClients = new ArrayList<>(clientService.getAllClients());
        Collections.sort(sortedClients);

        for (Client client : sortedClients) {
            System.out.println(client.getFullName() + " | code=" + client.getClientCode());
        }

        Account account1 = new CurrentAccount(
                1,
                new IBAN("RO49AAAA1B31007593840000"),
                1000.0,
                "RON",
                client1,
                10.0
        );

        Account account2 = new CurrentAccount(
                2,
                new IBAN("RO49AAAA1B31007593840001"),
                500.0,
                "RON",
                client2,
                8.0
        );

        accountService.openAccount(account1);
        accountService.openAccount(account2);

        System.out.println("\n-CONTURI INITIALE");
        System.out.println(account1.getOwner().getFullName() + " -> " + account1.getBalance());
        System.out.println(account2.getOwner().getFullName() + " -> " + account2.getBalance());

        accountService.deposit(account1.getIban().getCode(), 200.0);
        accountService.withdraw(account2.getIban().getCode(), 100.0);
        accountService.transfer(
                account1.getIban().getCode(),
                account2.getIban().getCode(),
                150.0
        );

        System.out.println("\n-DUPA OPERATII");
        System.out.println(account1.getOwner().getFullName() + " -> " + account1.getBalance());
        System.out.println(account2.getOwner().getFullName() + " -> " + account2.getBalance());

        Cheque cheque = new Cheque(
                account1,
                client2,
                200.0,
                LocalDate.now().plusDays(10)
        );

        chequeService.issueCheque(cheque);

        System.out.println("\n-CEC EMIS");
        System.out.println(cheque);

        chequeService.cashCheque(cheque.getSeries(), account2);

        System.out.println("\n-CEC DUPA INCASARE");
        System.out.println(cheque);

        System.out.println("\n-SOLDURI FINALE");
        System.out.println(account1.getOwner().getFullName() + " -> " + account1.getBalance());
        System.out.println(account2.getOwner().getFullName() + " -> " + account2.getBalance());
    }
}