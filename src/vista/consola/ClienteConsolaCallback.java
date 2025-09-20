package vista.consola;

import util.BusEventos;

import java.rmi.server.UnicastRemoteObject;

import modelo.core.EstadoJuego;
import net.CallbackClienteRemoto;

public class ClienteConsolaCallback extends UnicastRemoteObject implements CallbackClienteRemoto {
  private static final long serialVersionUID = 1L;
  private final BusEventos bus;
  public ClienteConsolaCallback(BusEventos bus) throws java.rmi.RemoteException { super(); this.bus = bus; }
  @Override public void onStateUpdate(EstadoJuego s){ bus.fire("STATE", null, s); }
  @Override public void onEvent(String m){ bus.fire("EVENT", null, m); }
}
