package parade.net;

import parade.modelo.EstadoJuego;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServerRemote extends Remote {
  String join(String nombre, GameClientCallbackRemote cb) throws RemoteException;
  void start() throws RemoteException;
  void playCard(String playerId, int handIndex) throws RemoteException;
  EstadoJuego getSnapshot() throws RemoteException;
}
