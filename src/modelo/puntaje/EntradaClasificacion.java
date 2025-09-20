package modelo.puntaje;

import java.io.Serializable;

public class EntradaClasificacion implements Serializable, Comparable<EntradaClasificacion > {
  private static final long serialVersionUID = 1L;

  public final String nombre;
  public final int puntos;     // menor es mejor
  public final int cartas;     // criterio de desempate
  private final long epochMillis;

  public EntradaClasificacion (String nombre, int puntos, int cartas, long epochMillis) {
    this.nombre = nombre;
    this.puntos = puntos;
    this.cartas = cartas;
    this.epochMillis = epochMillis;
  }
  
  public String getNombre() { return nombre; }
  public int getPuntos() { return puntos; }
  public int getCartas() { return cartas; }
  public long getEpochMillis() { return epochMillis; }

  @Override public int compareTo(EntradaClasificacion  o) {
    int c = Integer.compare(this.puntos, o.puntos);
    if (c != 0) return c;
    c = Integer.compare(this.cartas, o.cartas);
    if (c != 0) return c;
    return Long.compare(this.epochMillis, o.epochMillis);
  }

  @Override public String toString() {
    return nombre + " – " + puntos + " pts (" + cartas + " cartas)";
  }
}
