package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import carpet.CarpetSettings;
import carpet.utils.WoolTool;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 以下代码来自<a href="https://github.com/TISUnion/Carpet-TIS-Addition">{@code Carpet TIS Addition}</a>模组：
 * <ul>
 * <li>漏斗计数器无限速度实现：{@link HopperBlockEntityMixin#hopperCountersUnlimitedSpeed(World, BlockPos, HopperBlockEntity, BooleanSupplier)}</li>
 * <li>漏斗不消耗物品实现：{@link HopperBlockEntityMixin#hopperNoItemCost(World, BlockPos, HopperBlockEntity, int, ItemStack, int)}</li>
 * </ul>
 * <br>
 * <p>
 *     该部分代码遵循LGPL-3.0协议，许可证全文如下：
 * </p>
 * <p>
 *                    GNU LESSER GENERAL PUBLIC LICENSE
 *                        Version 3, 29 June 2007
 * <p>
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 * <p>
 * <p>
 *   This version of the GNU Lesser General Public License incorporates
 * the terms and conditions of version 3 of the GNU General Public
 * License, supplemented by the additional permissions listed below.
 * <p>
 *   0. Additional Definitions.
 * <p>
 *   As used herein, "this License" refers to version 3 of the GNU Lesser
 * General Public License, and the "GNU GPL" refers to version 3 of the GNU
 * General Public License.
 * <p>
 *   "The Library" refers to a covered work governed by this License,
 * other than an Application or a Combined Work as defined below.
 * <p>
 *   An "Application" is any work that makes use of an interface provided
 * by the Library, but which is not otherwise based on the Library.
 * Defining a subclass of a class defined by the Library is deemed a mode
 * of using an interface provided by the Library.
 * <p>
 *   A "Combined Work" is a work produced by combining or linking an
 * Application with the Library.  The particular version of the Library
 * with which the Combined Work was made is also called the "Linked
 * Version".
 * <p>
 *   The "Minimal Corresponding Source" for a Combined Work means the
 * Corresponding Source for the Combined Work, excluding any source code
 * for portions of the Combined Work that, considered in isolation, are
 * based on the Application, and not on the Linked Version.
 * <p>
 *   The "Corresponding Application Code" for a Combined Work means the
 * object code and/or source code for the Application, including any data
 * and utility programs needed for reproducing the Combined Work from the
 * Application, but excluding the System Libraries of the Combined Work.
 * <p>
 *   1. Exception to Section 3 of the GNU GPL.
 * <p>
 *   You may convey a covered work under sections 3 and 4 of this License
 * without being bound by section 3 of the GNU GPL.
 * <p>
 *   2. Conveying Modified Versions.
 * <p>
 *   If you modify a copy of the Library, and, in your modifications, a
 * facility refers to a function or data to be supplied by an Application
 * that uses the facility (other than as an argument passed when the
 * facility is invoked), then you may convey a copy of the modified
 * version:
 * <p>
 *    a) under this License, provided that you make a good faith effort to
 *    ensure that, in the event an Application does not supply the
 *    function or data, the facility still operates, and performs
 *    whatever part of its purpose remains meaningful, or
 * <p>
 *    b) under the GNU GPL, with none of the additional permissions of
 *    this License applicable to that copy.
 * <p>
 *   3. Object Code Incorporating Material from Library Header Files.
 * <p>
 *   The object code form of an Application may incorporate material from
 * a header file that is part of the Library.  You may convey such object
 * code under terms of your choice, provided that, if the incorporated
 * material is not limited to numerical parameters, data structure
 * layouts and accessors, or small macros, inline functions and templates
 * (ten or fewer lines in length), you do both of the following:
 * <p>
 *    a) Give prominent notice with each copy of the object code that the
 *    Library is used in it and that the Library and its use are
 *    covered by this License.
 * <p>
 *    b) Accompany the object code with a copy of the GNU GPL and this license
 *    document.
 * <p>
 *   4. Combined Works.
 * <p>
 *   You may convey a Combined Work under terms of your choice that,
 * taken together, effectively do not restrict modification of the
 * portions of the Library contained in the Combined Work and reverse
 * engineering for debugging such modifications, if you also do each of
 * the following:
 * <p>
 *    a) Give prominent notice with each copy of the Combined Work that
 *    the Library is used in it and that the Library and its use are
 *    covered by this License.
 * <p>
 *    b) Accompany the Combined Work with a copy of the GNU GPL and this license
 *    document.
 * <p>
 *    c) For a Combined Work that displays copyright notices during
 *    execution, include the copyright notice for the Library among
 *    these notices, as well as a reference directing the user to the
 *    copies of the GNU GPL and this license document.
 * <p>
 *    d) Do one of the following:
 * <p>
 *        0) Convey the Minimal Corresponding Source under the terms of this
 *        License, and the Corresponding Application Code in a form
 *        suitable for, and under terms that permit, the user to
 *        recombine or relink the Application with a modified version of
 *        the Linked Version to produce a modified Combined Work, in the
 *        manner specified by section 6 of the GNU GPL for conveying
 *        Corresponding Source.
 * <p>
 *        1) Use a suitable shared library mechanism for linking with the
 *        Library.  A suitable mechanism is one that (a) uses at run time
 *        a copy of the Library already present on the user's computer
 *        system, and (b) will operate properly with a modified version
 *        of the Library that is interface-compatible with the Linked
 *        Version.
 * <p>
 *    e) Provide Installation Information, but only if you would otherwise
 *    be required to provide such information under section 6 of the
 *    GNU GPL, and only to the extent that such information is
 *    necessary to install and execute a modified version of the
 *    Combined Work produced by recombining or relinking the
 *    Application with a modified version of the Linked Version. (If
 *    you use option 4d0, the Installation Information must accompany
 *    the Minimal Corresponding Source and Corresponding Application
 *    Code. If you use option 4d1, you must provide the Installation
 *    Information in the manner specified by section 6 of the GNU GPL
 *    for conveying Corresponding Source.)
 * <p>
 *   5. Combined Libraries.
 * <p>
 *   You may place library facilities that are a work based on the
 * Library side by side in a single library together with other library
 * facilities that are not Applications and are not covered by this
 * License, and convey such a combined library under terms of your
 * choice, if you do both of the following:
 * <p>
 *    a) Accompany the combined library with a copy of the same work based
 *    on the Library, uncombined with any other library facilities,
 *    conveyed under the terms of this License.
 * <p>
 *    b) Give prominent notice with the combined library that part of it
 *    is a work based on the Library, and explaining where to find the
 *    accompanying uncombined form of the same work.
 * <p>
 *   6. Revised Versions of the GNU Lesser General Public License.
 * <p>
 *   The Free Software Foundation may publish revised and/or new versions
 * of the GNU Lesser General Public License from time to time. Such new
 * versions will be similar in spirit to the present version, but may
 * differ in detail to address new problems or concerns.
 * <p>
 *   Each version is given a distinguishing version number. If the
 * Library as you received it specifies that a certain numbered version
 * of the GNU Lesser General Public License "or any later version"
 * applies to it, you have the option of following the terms and
 * conditions either of that published version or of any later version
 * published by the Free Software Foundation. If the Library as you
 * received it does not specify a version number of the GNU Lesser
 * General Public License, you may choose any version of the GNU Lesser
 * General Public License ever published by the Free Software Foundation.
 * <p>
 *   If the Library as you received it specifies that a proxy can decide
 * whether future versions of the GNU Lesser General Public License shall
 * apply, that proxy's public statement of acceptance of any version is
 * permanent authorization for you to choose that version for the
 * Library.
 */
@SuppressWarnings("JavadocLinkAsPlainText")
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends BlockEntity {
    @Shadow
    private Direction facing;

    public HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    private static boolean isInventoryFull(Inventory inventory, Direction direction) {
        return false;
    }

    @Shadow
    @Nullable
    private static Inventory getOutputInventory(World world, BlockPos pos, HopperBlockEntity blockEntity) {
        return null;
    }

    @Shadow
    public static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side) {
        throw new AssertionError();
    }

    @Shadow
    @Nullable
    private static Inventory getInputInventory(World world, Hopper hopper, BlockPos pos, BlockState state) {
        return null;
    }

    @Shadow
    private static int[] getAvailableSlots(Inventory inventory, Direction side) {
        return new int[]{};
    }

    @Shadow
    private static boolean extract(Hopper hopper, Inventory inventory, int slot, Direction side) {
        return false;
    }

    @Shadow
    public static List<ItemEntity> getInputItemEntities(World world, Hopper hopper) {
        return List.of();
    }

    @Shadow
    public static boolean extract(Inventory inventory, ItemEntity itemEntity) {
        return false;
    }

    @Shadow
    private static boolean insert(World world, BlockPos pos, HopperBlockEntity blockEntity) {
        return false;
    }

    @Shadow
    protected abstract boolean needsCooldown();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Shadow
    protected abstract boolean isFull();

    @Shadow
    protected abstract void setTransferCooldown(int transferCooldown);

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;insertAndExtract(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z"))
    private static boolean insert(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier, Operation<Boolean> original) {
        if (CarpetOrgAddition.LITHIUM) {
            return original.call(world, pos, state, blockEntity, booleanSupplier);
        }
        return RuleUtils.shulkerBoxStackableWrap(() -> original.call(world, pos, state, blockEntity, booleanSupplier));
    }

    @WrapOperation(method = "onEntityCollided", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;insertAndExtract(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z"))
    private static boolean onEntityCollided(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier, Operation<Boolean> original) {
        if (CarpetOrgAddition.LITHIUM) {
            return original.call(world, pos, state, blockEntity, booleanSupplier);
        }
        return RuleUtils.shulkerBoxStackableWrap(() -> original.call(world, pos, state, blockEntity, booleanSupplier));
    }

    // 让漏斗一次从一堆掉落物中只吸取一个潜影盒
    @WrapOperation(method = "extract(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/entity/ItemEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;"))
    private static ItemStack extract(Inventory from, Inventory to, ItemStack stack, Direction side, Operation<ItemStack> original, @Local LocalBooleanRef bl) {
        if (CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.get()) {
            return original.call(from, to, stack, side);
        }
        if (CarpetOrgAdditionSettings.shulkerBoxStackable.get() && InventoryUtils.isShulkerBoxItem(stack)) {
            ItemStack split = stack.split(stack.getMaxCount());
            int count = split.getCount();
            ItemStack result = original.call(from, to, split.copy(), side);
            stack.increment(result.getCount());
            if (count != result.getCount()) {
                bl.set(true);
            }
            return stack;
        }
        return original.call(from, to, stack, side);
    }

    /**
     * 保持与锂的兼容
     */
    @Unique
    private static void compatible(Runnable runnable) {
        if (CarpetOrgAdditionSettings.shulkerBoxStackable.get() && CarpetOrgAddition.LITHIUM) {
            boolean changed = CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.get();
            try {
                CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(false);
                runnable.run();
            } finally {
                CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(changed);
            }
        }
    }

    @Inject(method = "insertAndExtract", at = @At("HEAD"), cancellable = true)
    private static void insertAndExtract(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier, CallbackInfoReturnable<Boolean> cir) {
        compatible(() -> cir.setReturnValue(tryInsertAndExtract(world, pos, state, blockEntity, booleanSupplier)));
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private static void insert(World world, BlockPos pos, HopperBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        compatible(() -> cir.setReturnValue(tryInsert(world, pos, blockEntity)));
    }

    @Inject(method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z", at = @At("HEAD"), cancellable = true)
    private static void extract(World world, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        compatible(() -> cir.setReturnValue(tryExtract(world, hopper)));
    }

    @Unique
    private static boolean tryInsertAndExtract(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
        if (world.isClient()) {
            return false;
        }
        HopperBlockEntityMixin mixin = (HopperBlockEntityMixin) (Object) blockEntity;
        //noinspection DataFlowIssue
        if (!mixin.needsCooldown() && state.get(HopperBlock.ENABLED)) {
            boolean bl = false;
            if (!blockEntity.isEmpty()) {
                bl = insert(world, pos, blockEntity);
            }
            if (!mixin.isFull()) {
                bl |= booleanSupplier.getAsBoolean();
            }
            if (bl) {
                mixin.setTransferCooldown(8);
                // 漏斗计数器无限速度相关逻辑
                hopperCountersUnlimitedSpeed(world, pos, blockEntity, booleanSupplier);
                markDirty(world, pos, state);
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean tryInsert(World world, BlockPos pos, HopperBlockEntity blockEntity) {
        Inventory inventory = getOutputInventory(world, pos, blockEntity);
        if (inventory == null) {
            return false;
        }
        Direction direction = ((HopperBlockEntityMixin) (Object) blockEntity).facing.getOpposite();
        if (isInventoryFull(inventory, direction)) {
            return false;
        }
        for (int i = 0; i < blockEntity.size(); ++i) {
            ItemStack itemStack = blockEntity.getStack(i);
            if (!itemStack.isEmpty()) {
                int prevCount = itemStack.getCount();
                ItemStack itemStack2 = transfer(blockEntity, inventory, blockEntity.removeStack(i, 1), direction);
                if (itemStack2.isEmpty()) {
                    inventory.markDirty();
                    // 漏斗不消耗物品相关逻辑
                    hopperNoItemCost(world, pos, blockEntity, i, itemStack, prevCount);
                    return true;
                }
                itemStack.setCount(prevCount);
                if (prevCount == 1) {
                    blockEntity.setStack(i, itemStack);
                }
            }
        }
        return false;
    }

    /**
     * 漏斗计数器无限速度相关逻辑
     *
     * @see <a href="https://github.com/TISUnion/Carpet-TIS-Addition/tree/master/src/main/java/carpettisaddition/mixins/rule/hopperCountersUnlimitedSpeed">漏斗计数器无限速度</a>
     */
    @Unique
    private static void hopperCountersUnlimitedSpeed(World world, BlockPos blockPos, HopperBlockEntity blockEntity, BooleanSupplier supplier) {
        if (CarpetSettings.hopperCounters && RuleUtils.hopperCountersUnlimitedSpeed.get()) {
            Direction direction = blockEntity.getCachedState().get(HopperBlock.FACING);
            DyeColor color = WoolTool.getWoolColorAtPosition(world, blockPos.offset(direction));
            if (color == null) {
                return;
            }
            HopperBlockEntityMixin mixin = (HopperBlockEntityMixin) (Object) blockEntity;
            for (int i = Short.MAX_VALUE - 1; i >= 0; i--) {
                boolean flag = false;
                if (!blockEntity.isEmpty()) {
                    flag = insert(world, blockPos, blockEntity);
                }
                if (!mixin.isFull()) {
                    flag |= supplier.getAsBoolean();
                }
                if (!flag) {
                    break;
                }
                if (i == 0) {
                    CarpetOrgAddition.LOGGER.warn("Hopper at {} exceeded hopperCountersUnlimitedSpeed operation limit {}", blockEntity, Short.MAX_VALUE);
                }
            }
            mixin.setTransferCooldown(0);
        }
    }

    /**
     * 漏斗不消耗物品相关逻辑
     *
     * @see <a href="https://github.com/TISUnion/Carpet-TIS-Addition/tree/master/src/main/java/carpettisaddition/mixins/rule/hopperNoItemCost">漏斗不消耗物品</a>
     */
    @Unique
    private static void hopperNoItemCost(World world, BlockPos blockPos, HopperBlockEntity blockEntity, int index, ItemStack itemStack, int prevCount) {
        if (RuleUtils.hopperNoItemCost.get()) {
            DyeColor color = WoolTool.getWoolColorAtPosition(world, blockPos.offset(Direction.UP));
            if (color == null) {
                return;
            }
            int currentCount = itemStack.getCount();
            itemStack.setCount(prevCount);
            ItemStack prevStack = itemStack.copy();
            itemStack.setCount(currentCount);
            blockEntity.setStack(index, prevStack);
        }
    }

    @Unique
    private static boolean tryExtract(World world, Hopper hopper) {
        BlockPos blockPos = BlockPos.ofFloored(hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
        BlockState blockState = world.getBlockState(blockPos);
        Inventory inventory = getInputInventory(world, hopper, blockPos, blockState);
        if (inventory != null) {
            Direction direction = Direction.DOWN;
            int[] var11 = getAvailableSlots(inventory, direction);
            for (int i : var11) {
                if (extract(hopper, inventory, i, direction)) {
                    return true;
                }
            }
        } else {
            boolean bl = hopper.canBlockFromAbove() && blockState.isFullCube(world, blockPos) && !blockState.isIn(BlockTags.DOES_NOT_BLOCK_HOPPERS);
            if (!bl) {
                for (ItemEntity itemEntity : getInputItemEntities(world, hopper)) {
                    if (extract(hopper, itemEntity)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
