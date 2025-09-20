package servidor;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

import net.ServidorJuegoRemoto;

public class ServerMain {
  public static void main(String[] args) throws Exception {
    System.setProperty("java.rmi.server.hostname", "127.0.0.1");
    LocateRegistry.createRegistry(1099);
    ServidorJuegoRemoto server = new ServidorJuegoImpl();
    Naming.rebind("rmi://127.0.0.1:1099/ParadeServer", server);
    System.out.println("Servidor Parade RMI listo en 1099");
  }
}
