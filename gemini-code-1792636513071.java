// Staff.java (Abstract)
public abstract class Staff {
    public boolean isAvailable();
    public void setAvailable(boolean available);
    public void resetTurn();
}

// Waiter.java
public class Waiter extends Staff {
    public enum Action { SEAT, TAKE_ORDER, SERVE, WAIT }
    // Clean: Waiter does NOT import Customer!
}

// Chef.java
public class Chef extends Staff {
    public enum Action { COOK, WAIT }
}

// Host.java (Stage 3)
public class Host extends Staff {
    public enum Action { SEAT, WAIT }
}