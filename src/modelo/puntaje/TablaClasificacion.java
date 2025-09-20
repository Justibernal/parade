package modelo.puntaje;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Almacén persistente de la tabla de clasificación (Top N). */
public final class TablaClasificacion {
  // Cambié el nombre del archivo para mantener consistencia
  private static final Path FILE =
      Paths.get(System.getProperty("user.home"), ".parade_clasificacion.ser");

  private TablaClasificacion() {}

  @SuppressWarnings("unchecked")
  public static synchronized List<EntradaClasificacion> readAll() {
    if (!Files.exists(FILE)) return new ArrayList<>();
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(FILE))) {
      return (List<EntradaClasificacion>) in.readObject();
    } catch (Exception e) {
      return new ArrayList<>(); // ante corrupción, empezamos vacío
    }
  }

  public static synchronized void appendAll(Collection<EntradaClasificacion> toAdd) {
    List<EntradaClasificacion> all = readAll();
    all.addAll(toAdd);
    try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(FILE))) {
      out.writeObject(all);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static synchronized List<EntradaClasificacion> topN(int n) {
    List<EntradaClasificacion> all = readAll();
    Collections.sort(all); // RankingEntry implements Comparable (puntos, cartas, fecha)
    return (all.size() > n) ? new ArrayList<>(all.subList(0, n)) : all;
  }
}