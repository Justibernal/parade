package vista.consola;

import controlador.Controladores;
import controlador.JuegoController;

import modelo.core.Carta;
import modelo.core.Color;
import modelo.core.EstadoJuego;

import java.util.List;
import java.util.Scanner;

public class ClienteConsolaMain {

    private static String MY_ID;
    private static String MY_NAME;
    private static String ultimoEstado = "";
    private static boolean primeraVez = true;

    private static JuegoController ctrl;

    public static void main(String[] args) {
        try {
            // Obtiene el controller que ya encapsula RMI (igual que en Swing)
            ctrl = Controladores.defaultRmi();

            String nombre = (args.length > 0) ? args[0] : "Jugador";
            MY_NAME = nombre;

            // Listener local (NO RMI): el controller se encarga de recibir callbacks RMI
            MY_ID = ctrl.unirseJuego(nombre, new JuegoController.Listener() {
                @Override
                public void onStateUpdate(EstadoJuego estado) {
                    if (estado == null) return;
                    if (primeraVez) { primeraVez = false; return; }
                    mostrarEstado(estado);
                }

                @Override
                public void onEvent(String mensaje) {
                    if (!mensaje.contains("[ping]")) {
                        System.out.println("> " + mensaje);
                        if (mensaje.contains("capturó") || mensaje.contains("cartas")) {
                            System.out.println(">>> " + mensaje);
                        }
                    }
                }
            });

            System.out.println("Conectado como: " + MY_NAME);
            System.out.println("Comandos: start | play N | final N M | chat texto | estado | exit");

            try (Scanner sc = new Scanner(System.in)) {
                while (sc.hasNextLine()) {
                    String linea = sc.nextLine().trim();

                    if (linea.equals("exit")) {
                        break;

                    } else if (linea.equals("start")) {
                        ctrl.iniciarJuego();

                    } else if (linea.startsWith("play")) {
                        String[] partes = linea.split(" ");
                        if (partes.length == 2) {
                            try {
                                int n = Integer.parseInt(partes[1]);
                                ctrl.jugarCarta(MY_ID, n);
                            } catch (NumberFormatException e) {
                                System.out.println("Número inválido");
                            }
                        } else {
                            System.out.println("Uso: play <índice>");
                        }

                    } else if (linea.startsWith("final")) {
                        String[] partes = linea.split(" ");
                        if (partes.length == 3) {
                            try {
                                int n1 = Integer.parseInt(partes[1]);
                                int n2 = Integer.parseInt(partes[2]);
                                ctrl.elegirCartasFinales(MY_ID, new int[]{n1, n2});
                            } catch (NumberFormatException e) {
                                System.out.println("Números inválidos");
                            }
                        } else {
                            System.out.println("Uso: final <índice1> <índice2>");
                        }

                    } else if (linea.startsWith("chat")) {
                        if (linea.length() > 4) {
                            ctrl.sendChat(MY_ID, linea.substring(4).trim());
                        } else {
                            System.out.println("Uso: chat <mensaje>");
                        }

                    } else if (linea.equals("estado")) {
                        // Pull explícito (opcional): útil si querés forzar impresión
                        EstadoJuego snap = ctrl.obtenerEstadoActual();
                        mostrarEstado(snap);

                    } else if (!linea.isEmpty()) {
                        System.out.println("Comandos: start | play N | final N M | chat texto | estado | exit");
                    }
                }
            }

            System.out.println("Desconectado");

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error iniciando cliente de consola: " + ex.getMessage());
        }
    }

    private static void mostrarEstado(EstadoJuego estado) {
        StringBuilder sb = new StringBuilder();

        // Turno actual
        String turnoNombre = "Esperando...";
        boolean miTurno = false;
        boolean faseFinal = false;

        if (estado.turnoDe != null) {
            var turnoJugador = estado.jugadores.get(estado.turnoDe);
            if (turnoJugador != null) {
                turnoNombre = turnoJugador.nombre;
                miTurno = MY_ID.equals(estado.turnoDe);
            }
        }

        // Verificar fase final
        if (estado.ultimaRonda &&
            estado.pendientesEleccion != null &&
            estado.pendientesEleccion.contains(MY_ID)) {
            faseFinal = true;
            turnoNombre = "FASE FINAL - Elegir 2 cartas";
        }

        sb.append("\n=== PARADE ===\n");
        sb.append("Turno: ").append(turnoNombre);
        if (miTurno) sb.append(" (TÚ)");
        if (faseFinal) sb.append(" [FASE FINAL]");
        sb.append("\n");

        // Desfile
        sb.append("Desfile: ").append(estado.desfile).append("\n");

        // Mazo
        sb.append("Mazo: ").append(estado.cartasMazoRestantes).append(" cartas\n");

        // Mi mano
        var yo = estado.jugadores.get(MY_ID);
        if (yo != null) {
            sb.append("\n--- TU MANO ---\n");
            for (int i = 0; i < yo.mano.size(); i++) {
                sb.append(i).append(": ").append(yo.mano.get(i)).append("\n");
            }

            // Mis pilas capturadas (con valores y suma)
            sb.append("\n--- TUS CARTAS CAPTURADAS ---\n");
            if (yo.pilas.isEmpty()) {
                sb.append("(ninguna)\n");
            } else {
                for (Color color : Color.values()) {
                    List<Carta> cartas = yo.pilas.get(color);
                    if (cartas != null && !cartas.isEmpty()) {
                        int suma = 0;
                        StringBuilder vals = new StringBuilder();
                        for (int i = 0; i < cartas.size(); i++) {
                            int v = cartas.get(i).getValor();
                            suma += v;
                            if (i > 0) vals.append(",");
                            vals.append(v);
                        }
                        sb.append(color).append(": ").append(cartas.size())
                          .append(" cartas (valores: ").append(vals)
                          .append(" | suma=").append(suma).append(")\n");
                    }
                }
            }
        }

        // Rivales (solo cantidad total)
        sb.append("\n--- OTROS JUGADORES ---\n");
        for (var jugador : estado.jugadores.values()) {
            if (!jugador.id.equals(MY_ID)) {
                int totalCartas = 0;
                for (List<Carta> pilas : jugador.pilas.values()) totalCartas += pilas.size();
                sb.append(jugador.nombre).append(": ")
                  .append(totalCartas).append(" cartas capturadas\n");
            }
        }

        // Instrucciones
        sb.append("\n>>> ");
        if (faseFinal) {
            sb.append("FASE FINAL - Elige 2 cartas: final <índice1> <índice2>");
        } else if (miTurno) {
            sb.append("TU TURNO - Juega: play <índice>");
        } else {
            sb.append("Esperando turno...");
        }

        String nuevoEstado = sb.toString();
        if (!nuevoEstado.equals(ultimoEstado)) {
            System.out.println(nuevoEstado);
            ultimoEstado = nuevoEstado;
        }
    }
}


