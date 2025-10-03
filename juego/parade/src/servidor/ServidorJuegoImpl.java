package servidor;

import persistencia.Persistencia;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import modelo.core.*;
import modelo.puntaje.EntradaClasificacion;
import modelo.puntaje.TablaClasificacion;
import modelo.reglas.ReglasParade;
import net.*;

/** Implementación del servidor Parade vía RMI. */
public class ServidorJuegoImpl extends UnicastRemoteObject implements ServidorJuegoRemoto {
    private static final long serialVersionUID = 1L;

    private final Map<String, CallbackClienteRemoto> clientes  = new LinkedHashMap<>();
    private final Map<String, EstadoJuego.Jugador>   jugadores = new LinkedHashMap<>();
    private final EstadoJuego estado = new EstadoJuego();
    private final ReglasParade reglas = new ReglasParade();

    public ServidorJuegoImpl() throws RemoteException { super(); }

    // ========== RMI API ==========

    @Override
    public synchronized String join(String nombre, CallbackClienteRemoto cb) throws RemoteException {
        verificarClientesActivos();

        // Si no queda nadie, reiniciamos el estado completamente
        if (clientes.isEmpty()) resetearPartidaCompleta();

        // ¿El jugador ya existía con ese nombre? → reconectar
        for (EstadoJuego.Jugador j : estado.jugadores.values()) {
            if (Objects.equals(j.nombre, nombre)) {
                clientes.put(j.id, cb);
                broadcast("[info] " + nombre + " se reconectó.");
                push();
                return j.id;
            }
        }

        // Jugador nuevo
        String id = UUID.randomUUID().toString();
        var j = new EstadoJuego.Jugador();
        j.id = id;
        j.nombre = nombre;

        jugadores.put(id, j);
        estado.jugadores.put(id, j);
        clientes.put(id, cb);

        broadcast("Se unió " + nombre);
        push();
        return id;
    }

    @Override
    public synchronized void start() throws RemoteException {
        var lista = new ArrayList<>(jugadores.values());
        if (lista.size() < 2) {
            broadcast("[info] Se necesitan al menos 2 jugadores.");
            return;
        }

        // reset flags de fin si había
        estado.ultimaRonda = false;
        estado.causaFin = null;
        estado.pendientesEleccion.clear();
        estado.juegoTerminado = false;

        reglas.iniciar(estado, lista);
        broadcast("Partida iniciada");
        push();
    }

    @Override
    public synchronized void playCard(String playerId, int handIndex) throws RemoteException {
        var jugador = estado.jugadores.get(playerId);
        if (jugador == null) return;

        if (!Objects.equals(estado.turnoDe, playerId)) {
            var cb = clientes.get(playerId);
            if (cb != null) cb.onEvent("No es tu turno.");
            return;
        }
        if (handIndex < 0 || handIndex >= jugador.mano.size()) {
            var cb = clientes.get(playerId);
            if (cb != null) cb.onEvent("Índice inválido: " + handIndex);
            return;
        }

        reglas.jugarCarta(estado, playerId, handIndex);

        // ¿disparó última ronda?
        if (!estado.ultimaRonda) {
            boolean sinMazo = (estado.cartasMazoRestantes <= 0);
            boolean seisColores = estado.jugadores.values().stream()
                    .anyMatch(jg -> jg.pilas.keySet().size() >= Color.values().length);

            if (sinMazo || seisColores) {
                estado.ultimaRonda = true;
                estado.causaFin = sinMazo ? "sin_mazo" : "seis_colores";
                estado.turnoDe = null; // sin turnos: cada uno elige 2
                estado.pendientesEleccion.clear();
                estado.pendientesEleccion.addAll(estado.jugadores.keySet());

                broadcast("¡Última ronda! Motivo: " +
                        (sinMazo ? "se agotó el mazo" : "un jugador completó los 6 colores") +
                        ". Cada jugador debe elegir 2 cartas de su mano para mover a sus pilas.");
                push();
                return;
            }
        }
        push();
    }

    @Override
    public synchronized void chooseFinalCards(String playerId, int[] indices) throws RemoteException {
        var j = estado.jugadores.get(playerId);
        if (j == null) return;

        if (!estado.ultimaRonda) {
            var cb = clientes.get(playerId);
            if (cb != null) cb.onEvent("Aún no es la fase final.");
            return;
        }
        if (!estado.pendientesEleccion.contains(playerId)) {
            var cb = clientes.get(playerId);
            if (cb != null) cb.onEvent("Ya registraste tu elección final.");
            return;
        }
        if (indices == null || indices.length != 2) {
            var cb = clientes.get(playerId);
            if (cb != null) cb.onEvent("Debés elegir exactamente 2 cartas.");
            return;
        }

        Set<Integer> set = new HashSet<>();
        for (int ix : indices) {
            if (ix < 0 || ix >= j.mano.size()) {
                var cb = clientes.get(playerId);
                if (cb != null) cb.onEvent("Índice inválido: " + ix);
                return;
            }
            if (!set.add(ix)) {
                var cb = clientes.get(playerId);
                if (cb != null) cb.onEvent("Los índices deben ser distintos.");
                return;
            }
        }

        Arrays.sort(indices);
        for (int k = indices.length - 1; k >= 0; k--) {
            Carta c = j.mano.remove(indices[k]);
            j.pilas.computeIfAbsent(c.getColor(), cc -> new ArrayList<>()).add(c);
        }

        estado.pendientesEleccion.remove(playerId);
        broadcast("[info] " + j.nombre + " eligió sus 2 cartas finales.");

        // ¿cerramos la partida?
        if (estado.pendientesEleccion.isEmpty()) {
            estado.turnoDe = null;
            estado.juegoTerminado = true;

            List<EntradaClasificacion> clasif = buildClasificacionFinal(estado);
            TablaClasificacion.appendAll(clasif);

            String resumen = computeScores(estado);
            broadcast("Fin de juego");
            broadcast(resumen);
        }
        push();
    }

    @Override
    public synchronized void sendChat(String playerId, String message) throws RemoteException {
        String nombre = (estado.jugadores.get(playerId) != null)
                ? estado.jugadores.get(playerId).nombre
                : playerId;
        broadcast("[CHAT] " + nombre + ": " + message);
    }

    @Override
    public synchronized EstadoJuego getSnapshot() throws RemoteException {
        // Vista neutra: oculta manos de todos
        return sanitizeAllHideHands();
    }

    @Override
    public synchronized void saveGame() throws RemoteException {
        try {
            Persistencia.save(estado);
            broadcast("[info] Partida guardada correctamente.");
        } catch (Exception e) {
            broadcast("[error] Error al guardar: " + e.getMessage());
        }
    }

    @Override
    public synchronized void loadGame() throws RemoteException {
        try {
            if (!clientes.isEmpty()) {
                broadcast("[error] No se puede cargar partida con jugadores conectados.");
                return;
            }
            EstadoJuego saved = Persistencia.load();
            if (saved != null) {
                copiarEstadoCompleto(saved, this.estado);
                jugadores.clear();
                jugadores.putAll(estado.jugadores);
                broadcast("[info] Partida cargada ✓. Únanse los mismos jugadores.");
                push();
            } else {
                broadcast("[info] No hay partida guardada.");
            }
        } catch (Exception e) {
            broadcast("[error] Error al cargar: " + e.getMessage());
        }
    }

    @Override
    public synchronized List<EntradaClasificacion> getTopRanking(int n) throws RemoteException {
        return TablaClasificacion.topN(n);
    }

    // ========== Gestión de clientes / estado ==========

    /** Pinga callbacks y limpia desconectados. Si no queda nadie, resetea. */
    public synchronized void verificarClientesActivos() {
        List<String> desconectados = new ArrayList<>();
        for (var e : clientes.entrySet()) {
            try { e.getValue().onEvent("[ping]"); }
            catch (RemoteException ex) { desconectados.add(e.getKey()); }
        }
        for (String id : desconectados) {
            clientes.remove(id);
            System.out.println("Cliente desconectado: " + id);
        }
        if (clientes.isEmpty() && !estado.jugadores.isEmpty()) resetearPartidaCompleta();
    }

    private void resetearPartidaCompleta() {
        estado.jugadores.clear();
        estado.desfile.clear();
        estado.turnoDe = null;
        estado.cartasMazoRestantes = 0;
        estado.juegoTerminado = false;
        estado.ultimaRonda = false;
        estado.causaFin = null;
        estado.pendientesEleccion.clear();
        jugadores.clear();
        System.out.println("=== NUEVA PARTIDA - Estado reiniciado ===");
    }

    private void copiarEstadoCompleto(EstadoJuego src, EstadoJuego dst) {
        dst.jugadores.clear();
        dst.desfile.clear();
        dst.pendientesEleccion.clear();

        for (var entry : src.jugadores.entrySet()) {
            EstadoJuego.Jugador jOrig = entry.getValue();
            EstadoJuego.Jugador jCopia = new EstadoJuego.Jugador();
            jCopia.id = jOrig.id;
            jCopia.nombre = jOrig.nombre;
            jCopia.mano.addAll(jOrig.mano);
            jCopia.pilas.putAll(jOrig.pilas);
            dst.jugadores.put(entry.getKey(), jCopia);
        }
        dst.desfile.addAll(src.desfile);
        dst.turnoDe = src.turnoDe;
        dst.cartasMazoRestantes = src.cartasMazoRestantes;
        dst.juegoTerminado = src.juegoTerminado;
        dst.ultimaRonda = src.ultimaRonda;
        dst.causaFin = src.causaFin;
        dst.pendientesEleccion.addAll(src.pendientesEleccion);
    }

    /** Estado “para jugador”: solo su mano, pilas de todos. */
    private EstadoJuego sanitizeFor(String playerId) {
        var dst = new EstadoJuego();
        dst.turnoDe = estado.turnoDe;
        dst.cartasMazoRestantes = estado.cartasMazoRestantes;
        dst.juegoTerminado = estado.juegoTerminado;
        dst.ultimaRonda = estado.ultimaRonda;
        dst.causaFin = estado.causaFin;
        dst.pendientesEleccion.addAll(estado.pendientesEleccion);
        dst.desfile.addAll(estado.desfile);

        for (var e : estado.jugadores.entrySet()) {
            var j = e.getValue();
            var jj = new EstadoJuego.Jugador();
            jj.id = j.id;
            jj.nombre = j.nombre;
            if (Objects.equals(j.id, playerId)) jj.mano.addAll(j.mano);
            for (var pe : j.pilas.entrySet()) {
                jj.pilas.put(pe.getKey(), new ArrayList<>(pe.getValue()));
            }
            dst.jugadores.put(jj.id, jj);
        }
        return dst;
    }

    /** Estado neutral: oculta todas las manos. */
    private EstadoJuego sanitizeAllHideHands() {
        var dst = new EstadoJuego();
        dst.turnoDe = estado.turnoDe;
        dst.cartasMazoRestantes = estado.cartasMazoRestantes;
        dst.juegoTerminado = estado.juegoTerminado;
        dst.ultimaRonda = estado.ultimaRonda;
        dst.causaFin = estado.causaFin;
        dst.pendientesEleccion.addAll(estado.pendientesEleccion);
        dst.desfile.addAll(estado.desfile);

        for (var e : estado.jugadores.entrySet()) {
            var j = e.getValue();
            var jj = new EstadoJuego.Jugador();
            jj.id = j.id;
            jj.nombre = j.nombre;
            for (var pe : j.pilas.entrySet()) {
                jj.pilas.put(pe.getKey(), new ArrayList<>(pe.getValue()));
            }
            dst.jugadores.put(jj.id, jj);
        }
        return dst;
    }

    // ========== Puntajes / ranking ==========

    /** Construye entradas de clasificación finales según el reglamento. */
    private List<EntradaClasificacion> buildClasificacionFinal(EstadoJuego est) {
        List<EntradaClasificacion> out = new ArrayList<>();
        List<EstadoJuego.Jugador> jugList = new ArrayList<>(est.jugadores.values());
        long ahora = System.currentTimeMillis();

        Map<String,Integer> totalCartas = new HashMap<>();
        Map<String,Integer> puntos      = new HashMap<>();
        for (var j : jugList) { totalCartas.put(j.id, 0); puntos.put(j.id, 0); }

        for (Color color : Color.values()) {
            Map<String,Integer> cnt = new HashMap<>();
            Map<String,Integer> sum = new HashMap<>();

            for (var j : jugList) {
                var pil = j.pilas.getOrDefault(color, List.of());
                cnt.put(j.id, pil.size());
                int s = 0; for (Carta c : pil) s += c.getValor();
                sum.put(j.id, s);
                totalCartas.put(j.id, totalCartas.get(j.id) + pil.size());
            }

            // Mayorías: 2 jugadores = diferencia >= 2; 3+ comparten máximo
            Set<String> conMayoria = new HashSet<>();
            if (jugList.size() == 2) {
                var a = jugList.get(0); var b = jugList.get(1);
                int ca = cnt.get(a.id), cb = cnt.get(b.id);
                if (ca >= cb + 2) conMayoria.add(a.id);
                if (cb >= ca + 2) conMayoria.add(b.id);
            } else {
                int max = jugList.stream().mapToInt(j -> cnt.get(j.id)).max().orElse(0);
                if (max > 0) for (var j : jugList) if (cnt.get(j.id) == max) conMayoria.add(j.id);
            }

            for (var j : jugList) {
                int acc = puntos.get(j.id);
                acc += conMayoria.contains(j.id) ? cnt.get(j.id) : sum.get(j.id);
                puntos.put(j.id, acc);
            }
        }

        for (var j : jugList) {
            out.add(new EntradaClasificacion(j.nombre, puntos.get(j.id), totalCartas.get(j.id), ahora));
        }
        return out;
    }

    /** Calcula puntajes y arma el resumen broadcast para clientes. */
    private String computeScores(EstadoJuego est) {
        var jugadoresList = new ArrayList<>(est.jugadores.values());
        Map<String,Integer> total = new LinkedHashMap<>();
        Map<String,Integer> totalCartas = new LinkedHashMap<>();
        for (var j : jugadoresList) { total.put(j.id, 0); totalCartas.put(j.id, 0); }

        for (Color color : Color.values()) {
            Map<String,Integer> count = new HashMap<>();
            for (var j : jugadoresList)
                count.put(j.id, j.pilas.getOrDefault(color, List.of()).size());

            Set<String> mayorias = new HashSet<>();
            if (jugadoresList.size() == 2) {
                var a = jugadoresList.get(0); var b = jugadoresList.get(1);
                if (count.get(a.id) >= count.get(b.id) + 2) mayorias.add(a.id);
                if (count.get(b.id) >= count.get(a.id) + 2) mayorias.add(b.id);
            } else {
                int max = jugadoresList.stream().mapToInt(j -> count.get(j.id)).max().orElse(0);
                if (max > 0) for (var j : jugadoresList) if (count.get(j.id) == max) mayorias.add(j.id);
            }

            for (var j : jugadoresList) {
                var cartas = j.pilas.getOrDefault(color, List.of());
                totalCartas.put(j.id, totalCartas.get(j.id) + cartas.size());
                int suma = mayorias.contains(j.id)
                        ? cartas.size()
                        : cartas.stream().mapToInt(Carta::getValor).sum();
                total.put(j.id, total.get(j.id) + suma);
            }
        }

        var sb = new StringBuilder();
        sb.append("PUNTAJES FINALES (gana menor puntaje)\n");
        for (var j : jugadoresList) {
            sb.append(j.nombre).append(": ").append(total.get(j.id))
              .append(" pts (").append(totalCartas.get(j.id)).append(" cartas)\n");
        }
        var ganador = jugadoresList.stream()
                .min(Comparator.comparingInt(total::get).thenComparingInt(totalCartas::get))
                .orElse(jugadoresList.get(0));
        sb.append("Ganador: ").append(ganador.nombre);
        return sb.toString();
    }

    // ========== Notificación ==========
    private void broadcast(String msg) {
        for (var cb : clientes.values()) {
            try { cb.onEvent(msg); } catch (Exception ignored) {}
        }
    }

    private void push() {
        for (var e : clientes.entrySet()) {
            try { e.getValue().onStateUpdate(sanitizeFor(e.getKey())); }
            catch (Exception ignored) {}
        }
    }
}
