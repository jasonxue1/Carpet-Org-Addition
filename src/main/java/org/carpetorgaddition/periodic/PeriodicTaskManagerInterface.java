package org.carpetorgaddition.periodic;

public interface PeriodicTaskManagerInterface {
    default ServerComponentCoordinator carpet_Org_Addition$getServerPeriodicTaskManager() {
        throw new UnsupportedOperationException();
    }

    default PlayerComponentCoordinator carpet_Org_Addition$getPlayerPeriodicTaskManager() {
        throw new UnsupportedOperationException();
    }
}
