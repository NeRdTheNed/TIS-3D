package li.cil.tis3d.common.inventory;

import li.cil.tis3d.api.ModuleAPI;
import li.cil.tis3d.api.machine.Face;
import li.cil.tis3d.api.module.Module;
import li.cil.tis3d.api.module.ModuleProvider;
import li.cil.tis3d.common.Constants;
import li.cil.tis3d.common.network.Network;
import li.cil.tis3d.common.network.message.MessageCasingInventory;
import li.cil.tis3d.common.tileentity.TileEntityCasing;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * Inventory implementation for casings, having six slots for modules, one per face.
 */
public final class InventoryCasing extends Inventory implements ISidedInventory {
    private final TileEntityCasing tileEntity;

    public InventoryCasing(final TileEntityCasing tileEntity) {
        super(Constants.NAME_INVENTORY_CASING, Face.VALUES.length);
        this.tileEntity = tileEntity;
    }

    // --------------------------------------------------------------------- //
    // IInventory

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        tileEntity.markDirty();
        if (tileEntity.getWorldObj() != null) {
            tileEntity.getWorldObj().markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
        }
    }

    // --------------------------------------------------------------------- //
    // ISidedInventory

    @Override
    public int[] getAccessibleSlotsFromSide(final int side) {
        return new int[side];
    }

    @Override
    public boolean canInsertItem(final int index, final ItemStack stack, final int side) {
        return side == index &&
               getStackInSlot(index) == null &&
               tileEntity.getModule(Face.fromEnumFacing(EnumFacing.getFront(side))) == null && // Handles virtual modules.
               canInstall(stack, Face.fromEnumFacing(EnumFacing.getFront(side)));
    }

    @Override
    public boolean canExtractItem(final int index, final ItemStack stack, final int side) {
        return side == index && stack == getStackInSlot(index);
    }

    private boolean canInstall(final ItemStack stack, final Face face) {
        return ModuleAPI.getProviderFor(stack, tileEntity, face) != null;
    }

    // --------------------------------------------------------------------- //
    // Inventory

    @Override
    protected void onItemAdded(final int index) {
        final ItemStack stack = getStackInSlot(index);
        if (stack == null) {
            return;
        }

        final Face face = Face.VALUES[index];
        final ModuleProvider provider = ModuleAPI.getProviderFor(stack, tileEntity, face);
        if (provider == null) {
            return;
        }

        final Module module = provider.createModule(stack, tileEntity, face);
        if (!tileEntity.getCasingWorld().isRemote) {
            // Grab module data from newly created module, if any, don't rely on stack.
            // Rationale: module may initialize data from stack while contents of stack
            // are not synchronized to client, or do some fancy server-side only setup
            // based on the stack. The possibilities are endless. This is robust.
            final NBTTagCompound moduleData;
            if (module != null) {
                module.onInstalled(stack);
                module.writeToNBT(moduleData = new NBTTagCompound());
            } else {
                moduleData = null;
            }

            Network.INSTANCE.getWrapper().sendToAllAround(new MessageCasingInventory(tileEntity, index, stack, moduleData), Network.getTargetPoint(tileEntity, Network.RANGE_HIGH));
        }

        tileEntity.setModule(Face.VALUES[index], module);
    }

    @Override
    protected void onItemRemoved(final int index) {
        final Face face = Face.VALUES[index];
        final Module module = tileEntity.getModule(face);
        tileEntity.setModule(face, null);
        if (module != null && !tileEntity.getCasingWorld().isRemote) {
            module.onUninstalled(getStackInSlot(index));
            module.onDisposed();
        }

        Network.INSTANCE.getWrapper().sendToAllAround(new MessageCasingInventory(tileEntity, index, null, null), Network.getTargetPoint(tileEntity, Network.RANGE_HIGH));
    }
}
