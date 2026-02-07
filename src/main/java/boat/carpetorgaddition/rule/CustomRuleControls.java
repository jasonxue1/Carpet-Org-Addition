package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.rule.value.BlockDropsDirectlyEnterInventory;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class CustomRuleControls {
    public static final CustomRuleControl<Boolean> BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY = new CustomRuleControl<>() {
        @Override
        public Boolean getRuleValue(ServerPlayer player) {
            if (player == null) {
                return false;
            }
            return switch (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.value()) {
                case TRUE -> true;
                case FALSE -> false;
                case CUSTOM -> {
                    MinecraftServer server = ServerUtils.getServer(player);
                    ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
                    RuleSelfManager ruleSelfManager = coordinator.getRuleSelfManager();
                    yield ruleSelfManager.isEnabled(player, this);
                }
            };
        }

        @Override
        public boolean isServerDecision() {
            return CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.value() != BlockDropsDirectlyEnterInventory.CUSTOM;
        }
    };

    public static final CustomRuleControl<Integer> ITEM_PICKUP_RANGE_EXPAND = new CustomRuleControl<>() {
        @Override
        public Integer getRuleValue(ServerPlayer player) {
            int range = CarpetOrgAdditionSettings.itemPickupRangeExpand.value();
            if (range == 0) {
                return 0;
            }
            if (CarpetOrgAdditionSettings.itemPickupRangeExpandPlayerControl.value()) {
                MinecraftServer server = ServerUtils.getServer(player);
                RuleSelfManager ruleSelfManager = ServerComponentCoordinator.getCoordinator(server).getRuleSelfManager();
                return ruleSelfManager.isEnabled(player, this) ? range : 0;
            } else {
                return range;
            }
        }

        @Override
        public boolean isServerDecision() {
            return !CarpetOrgAdditionSettings.itemPickupRangeExpandPlayerControl.value();
        }
    };
}
