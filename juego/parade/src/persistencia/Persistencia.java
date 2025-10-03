package persistencia;

import java.io.*;
import java.nio.file.*;

import modelo.core.EstadoJuego;

public final class Persistencia {
  private static final Path FILE =
      Paths.get(System.getProperty("user.home"), ".parade_save.ser");

  private Persistencia(){}

  public static synchronized void save(EstadoJuego s) {
    try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(FILE))) {
      out.writeObject(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static synchronized EstadoJuego load() {
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(FILE))) {
      return (EstadoJuego) in.readObject();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
