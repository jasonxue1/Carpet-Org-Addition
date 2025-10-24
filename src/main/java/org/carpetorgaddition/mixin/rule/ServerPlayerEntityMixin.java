package org.carpetorgaddition.mixin.rule;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.BlockRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Shadow
    public abstract ServerWorld getEntityWorld();

    @Unique
    private final ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        // 强制补货
        if (CarpetOrgAdditionSettings.forceRestock.get()) {
            PlayerInventory inventory = thisPlayer.getInventory();
            restock(inventory);
        }
        // 自动同步玩家状态
        if (CarpetOrgAdditionSettings.autoSyncPlayerStatus.get() && FetcherUtils.getWorld(thisPlayer).getTime() % 30 == 0) {
            FetcherUtils.getServer(thisPlayer).getPlayerManager().sendPlayerStatus(thisPlayer);
            BlockPos blockPos = thisPlayer.getBlockPos();
            int range = (int) Math.min(thisPlayer.getBlockInteractionRange() + 1, 8);
            BlockRegion blockRegion = new BlockRegion(blockPos.add(-range, -range, -range), blockPos.add(range, range, range));
            for (BlockPos pos : blockRegion) {
                if (blockPos.toCenterPos().distanceTo(pos.toCenterPos()) > range) {
                    continue;
                }
                thisPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, this.getEntityWorld().getBlockState(pos)));
            }
        }
    }

    @Unique
    private void restock(PlayerInventory inventory) {
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            // 获取快捷栏物品
            ItemStack hotbarStack = inventory.getMainStacks().get(i);
            int count = hotbarStack.getMaxCount() - hotbarStack.getCount();
            // 检查该物品是否可堆叠或堆叠已满
            if (hotbarStack.isStackable() && count > 0) {
                for (int j = PlayerInventory.getHotbarSize(); j < inventory.getMainStacks().size(); j++) {
                    ItemStack inventoryStack = inventory.getStack(j);
                    // 如果可堆叠就自动补货
                    if (ItemStack.areItemsAndComponentsEqual(hotbarStack, inventoryStack)) {
                        int inventoryStackCount = inventoryStack.getCount();
                        if (inventoryStackCount >= count) {
                            hotbarStack.increment(count);
                            inventoryStack.decrement(count);
                        } else {
                            hotbarStack.increment(inventoryStackCount);
                            inventoryStack.decrement(inventoryStackCount);
                        }
                        // 物品是否已经补满
                        count = hotbarStack.getMaxCount() - hotbarStack.getCount();
                        if (count <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
