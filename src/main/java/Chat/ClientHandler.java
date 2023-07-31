package Chat;

import Models.Membre;
import Repository.ServiceCommentaire;
import Repository.ServiceMembre;
import lombok.AllArgsConstructor;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable{
    public static List<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    ServiceMembre serviceMembre = new ServiceMembre() ;

    public ClientHandler(Socket socket) {
        try {
            // Vérifier si le nombre de clients connectés est inférieur à 4
            if (clientHandlers.size() < 4) {
                this.socket = socket;

                // Initialiser les flux de lecture et d'écriture pour communiquer avec le client
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Lire le nom d'utilisateur du client à partir du flux de lecture
                this.clientUsername = bufferedReader.readLine();

                System.out.println(this.clientUsername + " s'est connecté");

                // Rechercher si un membre avec le même nom d'utilisateur existe déjà
                Membre membre = serviceMembre.findMember(this.clientUsername);

                // Si le membre n'existe pas, l'enregistrer comme nouveau membre
                if (membre == null) {
                    serviceMembre.setUsername(this.clientUsername);
                    serviceMembre.saveMember();
                }

                // Ajouter ce ClientHandler à la liste des clients connectés
                clientHandlers.add(this);

                // Envoyer un message de diffusion pour indiquer que le client est entré dans le chat
                broadcastMessage("SERVER : " + clientUsername + " est entré dans le chat");
            } else {
                System.out.println("! Nombre de client Maximal atteint (4)");

                // Initialiser le flux de sortie pour communiquer avec le client
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Envoyer un message au client indiquant que le nombre maximal de clients est atteint
                this.bufferedWriter.write(" ! Nombre de client Maximal atteint (4)");
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();

                // Définir le socket sur null pour indiquer qu'aucun socket n'est associé à ce ClientHandler
                this.socket = null;
            }
        } catch (IOException e) {
            // En cas d'exception IO, fermer le socket et les flux de lecture/écriture
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        // Vérifier si le socket est différent de null
        if (this.socket != null) {
            // Rechercher le membre correspondant au nom d'utilisateur du client
            Membre m = serviceMembre.findMember(this.clientUsername);
            String messageFromClient;

            // Boucle tant que le socket est connecté
            while (socket.isConnected()) {
                try {
                    // Lire le message envoyé par le client à partir du flux de lecture
                    messageFromClient = bufferedReader.readLine();

                    // Vérifier si le message ne contient pas la commande "/quit"
                    if (!messageFromClient.contains("/quit")) {
                        // Définir le membre et le message dans le service de commentaires
                        serviceCommentaire.setMembre(m);
                        serviceCommentaire.setMessage(messageFromClient);

                        // Sauvegarder les commentaires
                        serviceCommentaire.saveComments();
                    }

                    // Vérifier si le message contient la commande "/quit"
                    if (messageFromClient.contains("/quit")) {
                        // Fermer le socket et les flux de lecture/écriture
                        closeEverything(socket, bufferedReader, bufferedWriter);
                        this.socket = null;

                        // Sortir de la boucle while
                        break;
                    } else {
                        // Diffuser le message à tous les clients connectés
                        broadcastMessage(messageFromClient);
                    }
                } catch (IOException e) {
                    // En cas d'exception IO, fermer le socket et les flux de lecture/écriture
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
            }
        }
    }


    public void broadcastMessage(String messageToSend) {
        // Parcourir tous les ClientHandler dans la liste clientHandlers
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                // Vérifier si le ClientHandler actuel n'est pas le même que l'expéditeur du message
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    // Écrire le message à envoyer dans le flux de sortie du ClientHandler actuel
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                // En cas d'exception IO, fermer le socket et les flux de lecture/écriture du ClientHandler actuel
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }


    public void removeClientHandler() {
        // Supprimer le ClientHandler actuel de la liste clientHandlers
        clientHandlers.remove(this);

        // Diffuser un message pour indiquer que le client est sorti du chat
        broadcastMessage("SERVER : " + clientUsername + " est sorti du chat");
    }


    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Appeler la méthode removeClientHandler pour supprimer le ClientHandler actuel de la liste clientHandlers
        removeClientHandler();

        try {
            // Fermer le flux de lecture bufferedReader s'il n'est pas null
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            // Fermer le flux d'écriture bufferedWriter s'il n'est pas null
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }

            // Fermer le socket s'il n'est pas null
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
