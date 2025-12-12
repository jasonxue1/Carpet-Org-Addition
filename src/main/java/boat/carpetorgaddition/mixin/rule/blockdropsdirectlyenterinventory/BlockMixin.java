package boat.carpetorgaddition.mixin.rule.blockdropsdirectlyenterinventory;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.CustomRuleControls;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(Block.class)
public abstract class BlockMixin {
    // 方块掉落物直接进入物品栏
    @WrapMethod(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;")
    private static List<ItemStack> getDroppedStacks(BlockState state, ServerLevel world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack stack, Operation<List<ItemStack>> original) {
        // 获取本来要掉落的物品
        List<ItemStack> list = original.call(state, world, pos, blockEntity, entity, stack);
        return collect(list);
    }

    @WrapMethod(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;")
    private static List<ItemStack> getDroppedStacks(BlockState state, ServerLevel world, BlockPos pos, @Nullable BlockEntity blockEntity, Operation<List<ItemStack>> original) {
        List<ItemStack> list = original.call(state, world, pos, blockEntity);
        return collect(list);
    }

    /**
     * @param list 原本要掉落的物品
     * @return 将物品插入物品栏后剩余的物品
     */
    @Unique
    private static List<ItemStack> collect(List<ItemStack> list) {
        ServerPlayer player = CarpetOrgAdditionSettings.blockBreaking.get();
        if (CustomRuleControls.BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY.getRuleValue(player)) {
            // 将物品直接插入玩家物品栏
            for (ItemStack itemStack : list) {
                player.getInventory().add(itemStack);
            }
            // 如果物品完全插入玩家物品栏，返回空集合，否则将剩余物品返回，然后掉落
            return list.isEmpty() ? List.of() : list.stream().filter(itemStack -> !itemStack.isEmpty()).toList();
        }
        return list;
    }
}