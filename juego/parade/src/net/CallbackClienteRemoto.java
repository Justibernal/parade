package net;

import java.rmi.Remote;
import java.rmi.RemoteException;

import modelo.core.EstadoJuego;

public interface CallbackClienteRemoto extends Remote {
  void onStateUpdate(EstadoJuego snapshot) throws RemoteException;
  void onEvent(String message) throws RemoteException;
}
