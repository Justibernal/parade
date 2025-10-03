package parade.modelo;

import java.io.Serializable;
import java.util.*;

public class EstadoJuego implements Serializable {
  private static final long serialVersionUID = 1L;

  public static class Jugador implements Serializable {
    private static final long serialVersionUID = 1L;
    public String id, nombre;
    public List<Carta> mano = new ArrayList<>();
    public Map<Color, List<Carta>> pilas = new EnumMap<>(Color.class);
  }

  // frente a la IZQUIERDA, final a la DERECHA
  public List<Carta> desfile = new ArrayList<>();

  // id -> jugador
  public Map<String, Jugador> jugadores = new LinkedHashMap<>();

  public String turnoDe;                 // id actual
  public int cartasMazoRestantes;        // info útil a la UI
  public boolean juegoTerminado;         // flag fin
}
