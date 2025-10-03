package parade.modelo;

import java.io.Serializable;

public class Carta implements Serializable {
  private static final long serialVersionUID = 1L;
  private final Color color;
  private final int valor; // 0..10

  public Carta(Color color, int valor) {
    this.color = color;
    this.valor = valor;
  }
  public Color getColor() { return color; }
  public int getValor() { return valor; }

  @Override public String toString() { return color + "(" + valor + ")"; }
}


