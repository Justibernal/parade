package parade.cliente.consola;

import parade.net.GameClientCallbackRemote;
import parade.modelo.EstadoJuego;
import parade.observer.EventBus;

import java.rmi.server.UnicastRemoteObject;

public class ClientCallBack extends UnicastRemoteObject implements GameClientCallbackRemote {
  private static final long serialVersionUID = 1L;
  private final EventBus bus;
  public ClientCallBack(EventBus bus) throws java.rmi.RemoteException { super(); this.bus = bus; }

  @Override public void onStateUpdate(EstadoJuego s){ bus.fire("STATE", null, s); }
  @Override public void onEvent(String m){ bus.fire("EVENT", null, m); }
}
