package parade.observer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class EventBus {
  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  public void addListener(PropertyChangeListener l){ pcs.addPropertyChangeListener(l); }
  public void removeListener(PropertyChangeListener l){ pcs.removePropertyChangeListener(l); }
  public void fire(String evt, Object oldV, Object newV){ pcs.firePropertyChange(evt, oldV, newV); }
}
