package controlador;

import java.util.List;
import modelo.core.EstadoJuego;
import modelo.puntaje.EntradaClasificacion;

public interface JuegoController {

  

  /** Conecta (lookup) y se registra con el servidor, devolviendo el id de jugador. */
  String join(String nombre, Listener listener) throws Exception;

  void start() throws Exception;

  /** Jugar carta por índice en la mano del jugador con id `playerId`. */
  void playCard(String playerId, int handIndex) throws Exception;

  /** Persistencia en el servidor. */
  void saveGame() throws Exception;
  void loadGame() throws Exception;
  
  void sendChat(String playerId, String message) throws Exception;
  void chooseFinalCards(String playerId, int[] indices) throws Exception;

  interface Listener {
	    void onStateUpdate(EstadoJuego s);
	    void onEvent(String msg);
	  }
  
  /** Consultas varias. */
  EstadoJuego getSnapshot() throws Exception;
  List<EntradaClasificacion> getTopRanking(int n) throws Exception;
}
