package parade.reglas;

import parade.modelo.*;
import parade.modelo.EstadoJuego.Jugador;

import java.util.*;

public class ReglasParade {
  private final Deque<Carta> mazo = new ArrayDeque<>();
  private boolean ultimaRonda = false;

  public void iniciar(EstadoJuego estado, List<Jugador> jugadores) {
    // armar mazo 66
    List<Carta> todas = new ArrayList<>(66);
    for (Color c : Color.values())
      for (int v = 0; v <= 10; v++)
        todas.add(new Carta(c, v));
    Collections.shuffle(todas, new Random());

    mazo.clear();
    // usar como pila
    for (Carta c : todas) mazo.push(c);

    // limpiar estado
    estado.desfile.clear();
    estado.juegoTerminado = false;
    for (Jugador j : jugadores) {
      j.mano.clear();
      j.pilas = new EnumMap<>(Color.class);
    }

    // repartir 5
    for (int r = 0; r < 5; r++)
      for (Jugador j : jugadores) j.mano.add(robar());

    // 6 al centro
    for (int i = 0; i < 6; i++) estado.desfile.add(robar());

    // primer turno
    estado.turnoDe = jugadores.get(0).id;
    estado.cartasMazoRestantes = mazo.size();
  }

  public void jugarCarta(EstadoJuego estado, String jugadorId, int indiceMano) {
    Jugador j = estado.jugadores.get(jugadorId);
    if (j == null || !Objects.equals(estado.turnoDe, jugadorId)) return;
    if (indiceMano < 0 || indiceMano >= j.mano.size()) return;

    // 1) jugar al final
    Carta jugada = j.mano.remove(indiceMano);
    estado.desfile.add(jugada);

    // 2) evaluar retiros según reglamento
    int prev = estado.desfile.size() - 1; // sin contar la jugada
    if (prev > jugada.getValor()) {
      int aEvaluar = prev - jugada.getValor();
      List<Carta> tomadas = new ArrayList<>();
      for (int k = prev - 1; k >= prev - aEvaluar; k--) {
        Carta c = estado.desfile.get(k);
        if (c.getColor() == jugada.getColor() || c.getValor() <= jugada.getValor())
          tomadas.add(c);
      }
      if (!tomadas.isEmpty()) {
        estado.desfile.removeAll(tomadas);
        for (Carta c : tomadas)
          j.pilas.computeIfAbsent(c.getColor(), x -> new ArrayList<>()).add(c);
      }
    }

    // 3) robar (si hay)
    if (!mazo.isEmpty()) j.mano.add(robar());
    estado.cartasMazoRestantes = mazo.size();

    // marcar última ronda (borrador)
    if (tieneSeisColores(j) || mazo.isEmpty()) ultimaRonda = true;

    // avanzar turno
    var ids = new ArrayList<>(estado.jugadores.keySet());
    int idx = ids.indexOf(jugadorId);
    estado.turnoDe = ids.get((idx + 1) % ids.size());

    // fin placeholder
    if (ultimaRonda && mazo.isEmpty()) estado.juegoTerminado = true;
  }

  private Carta robar() { return mazo.pop(); }

  private boolean tieneSeisColores(Jugador j) {
    int distintos = 0;
    for (Color c : Color.values()) {
      List<Carta> lista = j.pilas.get(c);
      if (lista != null && !lista.isEmpty()) distintos++;
    }
    return distintos == Color.values().length;
  }
}
