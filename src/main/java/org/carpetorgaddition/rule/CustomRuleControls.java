package org.carpetorgaddition.rule;

import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.value.BlockDropsDirectlyEnterInventory;
import org.carpetorgaddition.util.FetcherUtils;

public class CustomRuleControls {
    public static final CustomRuleControl<Boolean> BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY = new CustomRuleControl<>() {
        @Override
        public Boolean getRuleValue(ServerPlayerEntity player) {
            if (player == null) {
                return false;
            }
            return switch (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.get()) {
                case TRUE -> true;
                case FALSE -> false;
                case CUSTOM -> {
                    RuleSelfManager ruleSelfManager = FetcherUtils.getRuleSelfManager(player);
                    yield ruleSelfManager.isEnabled(player, this);
                }
            };
        }

        @Override
        public boolean isServerDecision() {
            return CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.get() != BlockDropsDirectlyEnterInventory.CUSTOM;
        }
    };

    public static final CustomRuleControl<Integer> ITEM_PICKUP_RANGE_EXPAND = new CustomRuleControl<>() {
        @Override
        public Integer getRuleValue(ServerPlayerEntity player) {
            int range = CarpetOrgAdditionSettings.itemPickupRangeExpand.get();
            if (range == 0) {
                return 0;
            }
            if (CarpetOrgAdditionSettings.itemPickupRangeExpandPlayerControl.get()) {
                RuleSelfManager ruleSelfManager = FetcherUtils.getRuleSelfManager(player);
                return ruleSelfManager.isEnabled(player, this) ? range : 0;
            } else {
                return range;
            }
        }

        @Override
        public boolean isServerDecision() {
            return !CarpetOrgAdditionSettings.itemPickupRangeExpandPlayerControl.get();
        }
    };
}
