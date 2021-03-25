/*
 * Sincere-Loyalty
 * Copyright (C) 2020 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.sincereloyalty.mixin;

import ladysnake.sincereloyalty.LoyalTrident;
import ladysnake.sincereloyalty.TridentRecaller;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow
    public abstract ItemStack getStack(int slot);

    @Shadow
    @Final
    public PlayerEntity player;

    @Shadow public abstract boolean insertStack(int slot, ItemStack stack);

    @Shadow @Final public DefaultedList<ItemStack> offHand;

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(ILnet/minecraft/item/ItemStack;)Z"), cancellable = true)
    private void insertToPreferredSlot(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag tag = stack.getSubTag(LoyalTrident.MOD_NBT_KEY);
        if (tag != null) {
            if (tag.contains(LoyalTrident.RETURN_SLOT_NBT_KEY)) {
                int preferredSlot = tag.getInt(LoyalTrident.RETURN_SLOT_NBT_KEY);
                tag.remove(LoyalTrident.RETURN_SLOT_NBT_KEY);
                if (preferredSlot == -1) {
                    if (this.player.getOffHandStack().isEmpty()) {
                        this.player.equipStack(EquipmentSlot.OFFHAND, stack.copy());
                        stack.setCount(0);
                        cir.setReturnValue(true);
                    }
                } else if (this.getStack(preferredSlot).isEmpty()) {
                    this.insertStack(preferredSlot, stack);
                    cir.setReturnValue(true);
                }
            }

            TridentRecaller caller = (TridentRecaller) this.player;
            if (caller.getCurrentRecallStatus() == TridentRecaller.RecallStatus.RECALLING) {
                player.world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_TRIDENT_RETURN, player.getSoundCategory(), 0.7f, 0.5f);
            }
            caller.updateRecallStatus(TridentRecaller.RecallStatus.NONE);
        }
    }
}
