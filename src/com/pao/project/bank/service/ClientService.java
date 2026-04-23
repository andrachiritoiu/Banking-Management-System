package com.pao.project.bank.service;

import com.pao.project.bank.model.person.Client;
import com.pao.project.bank.model.person.CorporateClient;
import com.pao.project.bank.model.person.IndividualClient;

import java.util.ArrayList;
import java.util.List;

public class ClientService {
    private static final ClientService INSTANCE = new ClientService();

    private final List<Client> clients = new ArrayList<>();

    private ClientService(){}

    public static ClientService getInstance(){
        return INSTANCE;
    }

    //methods
    public void addClient(Client client){
        if(client == null){
            throw new IllegalArgumentException("Client cannot be null.");
        }

        if (findByClientCode(client.getClientCode()) != null) {
            throw new IllegalArgumentException("Client code already exists.");
        }

        clients.add(client);
    }

    public void removeClient(String clientCode){
        clients.removeIf(client -> clientCode != null && client.getClientCode().equals(clientCode));
    }

    public List<Client> getAllClients(){
        return new ArrayList<>(clients);
    }

    public Client findById(int clientId){
        for(Client client : clients){
            if(client.getId() == clientId)
                return client;
        }
        return null;
    }

    public Client findByClientCode(String clientCode){
        if (clientCode == null) {
            return null;
        }

        for(Client client : clients){
            if(client.getClientCode().equals(clientCode))
                return client;
        }
        return null;
    }

    public IndividualClient findIndividualByCnp(String cnp){
        if (cnp == null) {
            return null;
        }

        for(Client client : clients) {
            if (client instanceof IndividualClient individualClient) {
                if (individualClient.getCnp().equals(cnp))
                    return individualClient;
            }
        }

        return null;
    }

    public CorporateClient findCorporateByCui(String cui){
        if (cui == null) {
            return null;
        }

        for(Client client : clients) {
            if (client instanceof CorporateClient corporateClient) {
                if (corporateClient.getCui().equals(cui))
                    return corporateClient;
            }
        }

        return null;
    }


    public List<Client> getClientsSortedByName() {
        List<Client> result = new ArrayList<>(clients);

        result.sort((c1, c2) -> c1.getFullName().compareToIgnoreCase(c2.getFullName()));

        return result;
    }

}
