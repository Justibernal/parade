package modelo.core;

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

  // Parade: frente a la izquierda, final a la derecha
  public List<Carta> desfile = new ArrayList<>();
  public Map<String, Jugador> jugadores = new LinkedHashMap<>();

  /** id del jugador al que le toca (null si no hay turno activo). */
  public String turnoDe;

  /** cartas que quedan en el mazo. */
  public int cartasMazoRestantes;

  /** true si la partida ya terminó y se mostraron resultados. */
  public boolean juegoTerminado;

  // === Fase final (última ronda + elección de 2 cartas) ===

  /** true cuando se dispara la fase final (mazo agotado o alguien completa 6 colores). */
  public boolean ultimaRonda = false;

  /** motivo del fin, solo informativo/UI: "sin_mazo" | "seis_colores". */
  public String causaFin;

  /**
   * Jugadores que aún deben elegir sus 2 cartas finales.
   * Durante esta fase no hay turnos; cada jugador llama chooseFinalCards(...) una vez.
   */
  public Set<String> pendientesEleccion = new LinkedHashSet<>();
}

