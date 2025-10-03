// src/controlador/Controladores.java
package controlador;

import controlador.rmi.RmiJuegoController;

public final class Controladores {
  private Controladores() {}

  // URL por defecto del registro RMI
  public static final String DEFAULT_RMI_URL = "rmi://127.0.0.1:1099/ParadeServer";

  public static JuegoController defaultRmi() {
    try {
      return new RmiJuegoController(DEFAULT_RMI_URL);
    } catch (Exception e) {
      throw new IllegalStateException("No pude inicializar el controlador RMI en "
          + DEFAULT_RMI_URL, e);
    }
  }

  public static JuegoController rmi(String url) {
    try {
      return new RmiJuegoController(url);
    } catch (Exception e) {
      throw new IllegalStateException("No pude inicializar el controlador RMI en "
          + url, e);
    }
  }
}
