import java.util.List;
import java.util.Queue;
import java.util.LinkedList;

public class GameController {
    private int currentTurn;
    private int totalRevenueCents;
    
    // Domain Entities (Passed in / Managed)
    private final Queue<Customer> customerQueue;
    private final List<Table> tables;
    private final List<Order> activeOrders;
    
    // Staff Entities
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;
    private Host host; // Stage 3
    
    // Constructor Injection: Decoupled setup
    public GameController(List<Table> tables, Waiter waiter, Chef chef, Cashier cashier) {
        this.currentTurn = 0;
        this.totalRevenueCents = 0;
        this.customerQueue = new LinkedList<>();
        this.tables = tables;
        this.activeOrders = new LinkedList<>();
        this.waiter = waiter;
        this.chef = chef;
        this.cashier = cashier;
    }

    /**
     * Executes 1 complete turn (1 through 18)
     */
    public void executeTurn(Waiter.Action waiterAction, Integer waiterTargetTable, 
                            Chef.Action chefAction, Integer chefOrderIndex) 
                            throws InvalidGameActionException {
        
        currentTurn++;
        
        // STEP 1: Process Scheduled Customer Arrivals (Turns 1, 4, 7, 10, 13, 16)
        processArrivals();
        
        // STEP 2: Process Player Actions (Waiter & Chef)
        processWaiterAction(waiterAction, waiterTargetTable);
        processChefAction(chefAction, chefOrderIndex);
        
        // STEP 3: Advance Eating Customers & Automated Cashier (Step 4)
        processEatingAndPayment();
        
        // STEP 4: Process Satisfaction Decay for waiting customers
        processSatisfactionDecay();
        
        // Reset staff turn availability for next turn
        resetStaffCapacity();
    }

    private void processArrivals() {
        if (isArrivalTurn(currentTurn)) {
            // Factory creates customer based on stage/turn
            Customer newCustomer = CustomerFactory.createCustomerForTurn(currentTurn);
            customerQueue.add(newCustomer);
        }
    }

    private boolean isArrivalTurn(int turn) {
        return turn == 1 || turn == 4 || turn == 7 || turn == 10 || turn == 13 || turn == 16;
    }

    private void processWaiterAction(Waiter.Action action, Integer targetTableIndex) throws InvalidGameActionException {
        if (action == Waiter.Action.WAIT) return;
        
        if (!waiter.isAvailable()) {
            throw new InvalidGameActionException("Waiter has already performed an action this turn!");
        }

        Table targetTable = (targetTableIndex != null && targetTableIndex < tables.size()) 
                            ? tables.get(targetTableIndex) : null;

        switch (action) {
            case SEAT:
                if (customerQueue.isEmpty()) {
                    throw new InvalidGameActionException("No customers in queue to seat!");
                }
                if (targetTable == null || targetTable.isOccupied()) {
                    throw new InvalidGameActionException("Selected table is occupied or invalid!");
                }
                Customer customerToSeat = customerQueue.poll();
                targetTable.assignCustomer(customerToSeat);
                customerToSeat.setStatus(Customer.Status.SEATED);
                waiter.setAvailable(false);
                break;

            case TAKE_ORDER:
                if (targetTable == null || !targetTable.isOccupied()) {
                    throw new InvalidGameActionException("Cannot take order from empty table!");
                }
                Customer seatedCustomer = targetTable.getCustomer();
                if (seatedCustomer.getStatus() != Customer.Status.SEATED) {
                    throw new InvalidGameActionException("Customer is not ready to order!");
                }
                // Take order logic...
                seatedCustomer.setStatus(Customer.Status.SERVED); // or ORDERED
                waiter.setAvailable(false);
                break;

            case SERVE:
                // Serve logic...
                waiter.setAvailable(false);
                break;
        }
    }

    private void processChefAction(Chef.Action action, Integer orderIndex) throws InvalidGameActionException {
        if (action == Chef.Action.WAIT) return;
        
        if (orderIndex == null || orderIndex >= activeOrders.size()) {
            throw new InvalidGameActionException("Invalid order selected for Chef!");
        }
        Order order = activeOrders.get(orderIndex);
        order.advancePrep();
        chef.setAvailable(false);
    }

    private void processEatingAndPayment() {
        // Automated Cashier payment logic (oldest READY_TO_PAY customer)
        for (Table t : tables) {
            if (t.isOccupied()) {
                Customer c = t.getCustomer();
                if (c.getStatus() == Customer.Status.READY_TO_PAY && cashier.isAvailable()) {
                    // Collect payment, add revenue, free table
                    int bill = BillCalculator.calculateBill(c);
                    totalRevenueCents += bill;
                    c.setStatus(Customer.Status.PAID);
                    t.clear();
                    cashier.setAvailable(false);
                    break; // Max 1 payment per turn
                }
            }
        }
    }

    private void processSatisfactionDecay() {
        for (Customer c : customerQueue) {
            c.decaySatisfaction();
        }
        for (Table t : tables) {
            if (t.isOccupied()) {
                Customer c = t.getCustomer();
                // Decay ONLY if not yet SERVED
                if (c.getStatus() == Customer.Status.WAITING || c.getStatus() == Customer.Status.SEATED) {
                    c.decaySatisfaction();
                }
            }
        }
    }

    private void resetStaffCapacity() {
        waiter.resetTurn();
        chef.resetTurn();
        cashier.resetTurn();
        if (host != null) host.resetTurn();
    }
}