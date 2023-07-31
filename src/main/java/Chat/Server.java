package Chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer(){
        System.out.println("Serveur démarré ! ");
        try {
            while (!serverSocket.isClosed()){
                // Attente de nouvelles connexions des clients
                Socket socket = serverSocket.accept();

                // Création d'une instance du gestionnaire de client pour gérer les connexions individuelles
                ClientHandler clientHandler = new ClientHandler(socket);

                // Création d'un thread pour chaque client connecté
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            // Gestion des erreurs lors de l'acceptation des connexions clients
            e.printStackTrace();
        }
    }

    public void closeServer(){
        try {
            if(serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
