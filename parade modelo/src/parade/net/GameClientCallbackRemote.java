package parade.net;

import parade.modelo.EstadoJuego;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameClientCallbackRemote extends Remote {
  void onStateUpdate(EstadoJuego snapshot) throws RemoteException;
  void onEvent(String message) throws RemoteException;
}

