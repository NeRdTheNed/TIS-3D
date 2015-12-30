package li.cil.tis3d.common.network.handler;

import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import li.cil.tis3d.api.machine.Face;
import li.cil.tis3d.api.module.Module;
import li.cil.tis3d.common.network.message.MessageCasingData;
import li.cil.tis3d.common.tile.TileEntityCasing;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.io.IOException;

public final class MessageHandlerCasingData extends AbstractMessageHandlerWithLocation<MessageCasingData> {
    @Override
    protected void process(final MessageCasingData message, final MessageContext context) {
        final TileEntity tileEntity = getTileEntity(message, context);
        if (!(tileEntity instanceof TileEntityCasing)) {
            return;
        }

        final TileEntityCasing casing = (TileEntityCasing) tileEntity;
        final ByteBuf data = message.getData();
        while (data.readableBytes() > 0) {
            final Module module = casing.getModule(Face.VALUES[data.readByte()]);
            final ByteBuf moduleData = data.readBytes(data.readInt());
            while (moduleData.readableBytes() > 0) {
                final boolean isNbt = moduleData.readBoolean();
                final ByteBuf packet = moduleData.readBytes(moduleData.readInt());
                if (module != null) {
                    if (isNbt) {
                        try {
                            final ByteBufInputStream bis = new ByteBufInputStream(packet);
                            final NBTTagCompound nbt = CompressedStreamTools.readCompressed(bis);
                            module.onData(nbt);
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        module.onData(packet);
                    }
                }
            }
        }
    }
}