// Customer.java
public abstract class Customer {
    public enum Status { WAITING, SEATED, SERVED, EATING, READY_TO_PAY, PAID, LEFT }
    
    public abstract Status getStatus();
    public abstract void setStatus(Status status);
    public abstract int getSatisfaction();
    public abstract void decaySatisfaction(); // Regular: -8, VIP: -5, Critic: -12
    public abstract double getDiscountRate();  // Regular: 0.0, VIP: 0.10
    public abstract MenuItem chooseFood(Menu menu);
}

// Table.java
public class Table {
    public boolean isOccupied();
    public void assignCustomer(Customer c);
    public Customer getCustomer();
    public void clear();
}

// Order.java
public class Order {
    public enum Status { PREPARING, READY, SERVED, PAID }
    public MenuItem getItem();
    public Status getStatus();
    public void setStatus(Status status);
    public void advancePrep(); // Called by Chef
    public boolean isReady();
}