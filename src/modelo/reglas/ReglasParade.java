package modelo.reglas;

public class ReglasParade {
  private final java.util.Deque<modelo.core.Carta> mazo = new java.util.ArrayDeque<>();
  private boolean ultimaRonda = false;
  private int turnosRestantesUltima = 0;


  public void iniciar(modelo.core.EstadoJuego estado,
                      java.util.List<modelo.core.EstadoJuego.Jugador> jugadores) {
	  
	  ultimaRonda = false;
	  turnosRestantesUltima = 0;

    // mazo 66 cartas (6 colores, 0..10)
    java.util.List<modelo.core.Carta> todas = new java.util.ArrayList<>(66);
    for (modelo.core.Color c : modelo.core.Color.values())
      for (int v = 0; v <= 10; v++)
        todas.add(new modelo.core.Carta(c, v));

    java.util.Collections.shuffle(todas, new java.util.Random());
    mazo.clear();
    for (modelo.core.Carta c : todas) mazo.push(c);

    estado.desfile.clear();
    estado.juegoTerminado = false;

    // limpiar manos y pilas
    for (modelo.core.EstadoJuego.Jugador j : jugadores) {
      j.mano.clear();
      j.pilas = new java.util.EnumMap<modelo.core.Color, java.util.List<modelo.core.Carta>>(modelo.core.Color.class);
    }

    // repartir 5
    for (int r = 0; r < 5; r++)
      for (modelo.core.EstadoJuego.Jugador j : jugadores) j.mano.add(robar());

    // 6 al centro
    for (int i = 0; i < 6; i++) estado.desfile.add(robar());

    estado.turnoDe = jugadores.get(0).id;
    estado.cartasMazoRestantes = mazo.size();
  }

  public void jugarCarta(modelo.core.EstadoJuego estado, String jugadorId, int indiceMano) {
    var j = estado.jugadores.get(jugadorId);
    if (j == null || !java.util.Objects.equals(estado.turnoDe, jugadorId) || indiceMano < 0 || indiceMano >= j.mano.size()) return;

    // 1) jugar al final
    modelo.core.Carta jugada = j.mano.remove(indiceMano);
    estado.desfile.add(jugada);

 // 2) evaluar retiros (X cartas "seguras" desde el final; se evalúa el RESTO desde el FRENTE)
    int prev = estado.desfile.size() - 1;              // cartas antes de la jugada
    int seguras = Math.min(prev, jugada.getValor());   // X cartas inmediatamente anteriores a la jugada (inmunes)
    int aEvaluarDesdeFrente = prev - seguras;          // cantidad de cartas a evaluar empezando desde el frente

    if (aEvaluarDesdeFrente > 0) {
      java.util.List<modelo.core.Carta> tomadas = new java.util.ArrayList<>();
      for (int k = 0; k < aEvaluarDesdeFrente; k++) {
        modelo.core.Carta c = estado.desfile.get(k);
        if (c.getColor() == jugada.getColor() || c.getValor() <= jugada.getValor()) {
          tomadas.add(c);
        }
      }
      if (!tomadas.isEmpty()) {
        estado.desfile.removeAll(tomadas);
        for (modelo.core.Carta c : tomadas) {
          j.pilas.computeIfAbsent(c.getColor(), x -> new java.util.ArrayList<>()).add(c);
        }
      }
    }
  


 // 3) robar si corresponde (en última ronda NO se roba)
    boolean puedeRobar = !ultimaRonda && !mazo.isEmpty();
    if (puedeRobar) j.mano.add(robar());
    estado.cartasMazoRestantes = mazo.size();

    // ¿se dispara la última ronda?
    if (!ultimaRonda) {
        if (tieneSeisColores(j)) {          // caso “6 colores” (termina este turno y 1 extra c/u SIN robar)
            ultimaRonda = true;
            turnosRestantesUltima = estado.jugadores.size(); // 1 turno extra cada uno
        } else if (mazo.isEmpty()) {        // caso “se acabó el mazo”
            ultimaRonda = true;
            turnosRestantesUltima = estado.jugadores.size(); // 1 turno extra cada uno
        }
    }

    // avanzar turno
    var ids = new java.util.ArrayList<>(estado.jugadores.keySet());
    int idx = ids.indexOf(jugadorId);
    estado.turnoDe = ids.get((idx + 1) % ids.size());

    // si estamos en última ronda, contamos los turnos restantes y cerramos cuando toca
    if (ultimaRonda) {
        turnosRestantesUltima--;
        // En ambos casos (6 colores o mazo vacío) la ronda final es UNA vuelta y sin robar.
        if (turnosRestantesUltima == 0) {
            estado.juegoTerminado = true;
        }
    }
  }

  private modelo.core.Carta robar() { return mazo.pop(); }

  private boolean tieneSeisColores(modelo.core.EstadoJuego.Jugador j) {
    int distintos = 0;
    for (modelo.core.Color c : java.util.EnumSet.allOf(modelo.core.Color.class)) {
      java.util.List<modelo.core.Carta> l = j.pilas.get(c);
      if (l != null && !l.isEmpty()) distintos++;
    }
    return distintos == modelo.core.Color.values().length;
  }
}
