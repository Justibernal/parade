package controlador.rmi;

import controlador.JuegoController;
import modelo.core.EstadoJuego;
import modelo.puntaje.EntradaClasificacion;
import net.ServidorJuegoRemoto;
import net.CallbackClienteRemoto;

import javax.swing.SwingUtilities;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RmiJuegoController implements JuegoController {
  private final ServidorJuegoRemoto server;
  private JuegoController.Listener listener;

  public RmiJuegoController(String rmiUrl) throws Exception {
    System.setProperty("java.rmi.server.hostname", "127.0.0.1");
    this.server = (ServidorJuegoRemoto) Naming.lookup(rmiUrl);
  }

  @Override public String unirseJuego(String nombre, Listener l) throws Exception {
    this.listener = l;
    return server.join(nombre, new Cb());
  }

  @Override public void iniciarJuego() throws Exception { server.start(); }
  @Override public void jugarCarta(String jugadorId, int index) throws Exception { server.playCard(jugadorId, index); }
  @Override public EstadoJuego obtenerEstadoActual() throws Exception { return server.getSnapshot(); }
  @Override public List<EntradaClasificacion> getTopRanking(int n) throws Exception { return server.getTopRanking(n); }
  @Override public void guardarPartida() throws Exception { server.saveGame(); }
  @Override public void cargarPartida() throws Exception { server.loadGame(); }
  
  @Override
  public void sendChat(String playerId, String message) throws Exception {
    server.sendChat(playerId, message);
  }
  

  @Override
  public void elegirCartasFinales(String playerId, int[] indices) throws Exception {
    server.chooseFinalCards(playerId, indices);
  }


  private class Cb extends UnicastRemoteObject implements CallbackClienteRemoto {
    private static final long serialVersionUID = 1L;
	Cb() throws RemoteException { super(); }
    @Override public void onStateUpdate(EstadoJuego s) throws RemoteException {
      if (listener != null) SwingUtilities.invokeLater(() -> listener.onStateUpdate(s));
    }
    @Override public void onEvent(String m) throws RemoteException {
      if (listener != null) SwingUtilities.invokeLater(() -> listener.onEvent(m));
    }
  }
}
