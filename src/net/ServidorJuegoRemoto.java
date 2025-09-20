package net;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import modelo.core.EstadoJuego;
import modelo.puntaje.EntradaClasificacion;

public interface ServidorJuegoRemoto extends Remote {
  String join(String nombre, CallbackClienteRemoto cb) throws RemoteException;
  void start() throws RemoteException;
  void playCard(String playerId, int handIndex) throws RemoteException;
  EstadoJuego getSnapshot() throws RemoteException;
  void saveGame() throws RemoteException;
  void loadGame() throws RemoteException;
  void sendChat(String playerId, String message) throws java.rmi.RemoteException;
  void chooseFinalCards(String playerId, int[] indices) throws RemoteException;



  List<EntradaClasificacion> getTopRanking(int n) throws RemoteException;

}
