package vista.consola;

import util.BusEventos;

import java.util.Map;

import modelo.core.Carta;
import modelo.core.Color;
import modelo.core.EstadoJuego;
import net.ServidorJuegoRemoto;

import java.util.List;
import java.beans.PropertyChangeEvent;
import java.rmi.Naming;

public class ClienteConsolaMain {
	public static void main(String[] args) throws Exception {
		  System.setProperty("java.rmi.server.hostname", "127.0.0.1");
		  var server = (ServidorJuegoRemoto) Naming.lookup("rmi://127.0.0.1:1099/ParadeServer");

		  String nombre = (args.length > 0) ? args[0] : "Jugador";
		  var bus = new BusEventos();
		  var cb = new ClienteConsolaCallback(bus);

		  // Me uno y ya tengo mi id
		  String myId = server.join(nombre, cb);
		  final String MY_ID = myId;
		  final String MY_NAME = nombre;

		  // Listener DESPUÉS de join, así conozco mi id
		  bus.addListener((PropertyChangeEvent e) -> {
		    switch (e.getPropertyName()) {
		      case "EVENT":
		        System.out.println("[" + MY_NAME + "] " + e.getNewValue());
		        break;
		      case "STATE":
		        var s = (EstadoJuego) e.getNewValue();
		        var turno = s.jugadores.get(s.turnoDe);
		        if (s.juegoTerminado) {
		        	  System.out.println("[" + MY_NAME + "] PARTIDA FINALIZADA");
		        	  // si querés, mostrar tus pilas por color acá y salir temprano
		        	  return;
		        	}

		        boolean esMiTurno = s.turnoDe != null && s.turnoDe.equals(MY_ID);
		        System.out.println("[" + MY_NAME + "] " + (esMiTurno ? "TU TURNO" :
		                           "Turno de " + (turno != null ? turno.nombre : "?")));
		        System.out.println("[" + MY_NAME + "] Desfile: " + s.desfile);
		        
		        var miMano = s.jugadores.get(MY_ID).mano;
		        var conIndices = new StringBuilder("[");
		        for (int i = 0; i < miMano.size(); i++) {
		          conIndices.append(i).append(": ").append(miMano.get(i));
		          if (i < miMano.size()-1) conIndices.append(", ");
		        }
		        conIndices.append("]");
		        System.out.println("[" + MY_NAME + "] Tu mano: " + conIndices);
		        
		        Map<Color, List<Carta>> misPilas = s.jugadores.get(MY_ID).pilas;
		        StringBuilder sb = new StringBuilder("Tus pilas: ");
		        for (Map.Entry<Color, List<Carta>> entry : misPilas.entrySet()) {
		            sb.append(entry.getKey()).append("=")
		              .append(entry.getValue().size()).append(" ");
		        }
		        System.out.println("[" + MY_NAME + "] " + sb);


		        System.out.println("[" + MY_NAME + "] Mazo: " + s.cartasMazoRestantes);
		        break;
		    }
		  });

		  System.out.println("Conectado id=" + myId + ". Comandos: start | play <i>");

		  try (var sc = new java.util.Scanner(System.in)) {
		    while (sc.hasNextLine()) {
		      var line = sc.nextLine().trim();
		      if (line.equals("start")) server.start();
		      else if (line.startsWith("play")) {
		        var p = line.split("\\s+");
		        if (p.length >= 2) server.playCard(myId, Integer.parseInt(p[1]));
		      }
		    }
		  }
		}

}

