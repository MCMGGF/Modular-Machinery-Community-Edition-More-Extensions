package com.fushu.mmceguiext.common.network;

import com.fushu.mmceguiext.common.config.ControllerButtonPolicyManager;
import com.fushu.mmceguiext.common.integration.crafttweaker.MMCEGEEvents;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fushu.mmceguiext.MMCEGuiExt;

public class PktControllerButtonAction implements IMessage, IMessageHandler<PktControllerButtonAction, IMessage> {
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);
    private static final int MAX_KEY_LENGTH = 128;
    private static final int MAX_BUTTON_ID_LENGTH = 128;
    private static final int MAX_VALUE_LENGTH = 1024;
    private static final byte KIND_SMART_SET = 0;
    private static final byte KIND_SMART_ADD = 1;
    private static final byte KIND_EVENT = 2;

    private BlockPos controllerPos = BlockPos.ORIGIN;
    private String key = "";
    private String buttonId = "";
    private byte kind = KIND_SMART_ADD;
    private float value = 0.0F;
    private boolean stringValue = false;
    private String textValue = "";
    private boolean hasMin = false;
    private float min = 0.0F;
    private boolean hasMax = false;
    private float max = 0.0F;

    public PktControllerButtonAction() {
    }

    public static PktControllerButtonAction smart(BlockPos controllerPos, String key, boolean additive, float value, Float min, Float max) {
        PktControllerButtonAction pkt = new PktControllerButtonAction();
        pkt.controllerPos = controllerPos == null ? BlockPos.ORIGIN : controllerPos;
        pkt.key = key == null ? "" : key;
        pkt.kind = additive ? KIND_SMART_ADD : KIND_SMART_SET;
        pkt.value = value;
        pkt.stringValue = false;
        pkt.hasMin = min != null && Float.isFinite(min.floatValue());
        pkt.min = pkt.hasMin ? min.floatValue() : 0.0F;
        pkt.hasMax = max != null && Float.isFinite(max.floatValue());
        pkt.max = pkt.hasMax ? max.floatValue() : 0.0F;
        return pkt;
    }

    public static PktControllerButtonAction smart(BlockPos controllerPos, String key, String value) {
        PktControllerButtonAction pkt = new PktControllerButtonAction();
        pkt.controllerPos = controllerPos == null ? BlockPos.ORIGIN : controllerPos;
        pkt.key = key == null ? "" : key;
        pkt.kind = KIND_SMART_SET;
        pkt.stringValue = true;
        pkt.textValue = value == null ? "" : value;
        return pkt;
    }

    public static PktControllerButtonAction event(BlockPos controllerPos, String buttonId) {
        PktControllerButtonAction pkt = new PktControllerButtonAction();
        pkt.controllerPos = controllerPos == null ? BlockPos.ORIGIN : controllerPos;
        pkt.buttonId = buttonId == null ? "" : buttonId;
        pkt.kind = KIND_EVENT;
        return pkt;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NetworkBufferUtils.requireReadable(buf, 9);
        this.controllerPos = BlockPos.fromLong(buf.readLong());
        this.kind = buf.readByte();
        this.key = NetworkBufferUtils.readBoundedUtf8(buf, MAX_KEY_LENGTH);
        this.buttonId = NetworkBufferUtils.readBoundedUtf8(buf, MAX_BUTTON_ID_LENGTH);
        NetworkBufferUtils.requireReadable(buf, 2);
        this.stringValue = buf.readBoolean();
        if (this.stringValue) {
            this.textValue = NetworkBufferUtils.readBoundedUtf8(buf, MAX_VALUE_LENGTH);
        } else {
            NetworkBufferUtils.requireReadable(buf, 4);
            this.value = buf.readFloat();
        }
        this.hasMin = buf.readBoolean();
        if (this.hasMin) {
            NetworkBufferUtils.requireReadable(buf, 4);
            this.min = buf.readFloat();
        }
        NetworkBufferUtils.requireReadable(buf, 1);
        this.hasMax = buf.readBoolean();
        if (this.hasMax) {
            NetworkBufferUtils.requireReadable(buf, 4);
            this.max = buf.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.controllerPos.toLong());
        buf.writeByte(this.kind);
        NetworkBufferUtils.writeBoundedUtf8(buf, this.key, MAX_KEY_LENGTH);
        NetworkBufferUtils.writeBoundedUtf8(buf, this.buttonId, MAX_BUTTON_ID_LENGTH);
        buf.writeBoolean(this.stringValue);
        if (this.stringValue) {
            NetworkBufferUtils.writeBoundedUtf8(buf, this.textValue, MAX_VALUE_LENGTH);
        } else {
            buf.writeFloat(this.value);
        }
        buf.writeBoolean(this.hasMin);
        if (this.hasMin) {
            buf.writeFloat(this.min);
        }
        buf.writeBoolean(this.hasMax);
        if (this.hasMax) {
            buf.writeFloat(this.max);
        }
    }

    @Override
    public IMessage onMessage(PktControllerButtonAction message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null) {
            LOGGER.info("Rejected controller button action packet because the server player was null.");
            return null;
        }
        LOGGER.info(
            "Received controller button action packet for {} key={} kind={} on thread={}.",
            message == null ? null : message.controllerPos,
            message == null ? null : message.key,
            message == null ? null : message.kind,
            Thread.currentThread().getName()
        );
        player.getServerWorld().addScheduledTask(() -> {
            LOGGER.info(
                "Handling controller button action packet for {} on thread={}.",
                message == null ? null : message.controllerPos,
                Thread.currentThread().getName()
            );
            handle(message, player);
        });
        return null;
    }

    private static void handle(PktControllerButtonAction message, EntityPlayerMP player) {
        if (message == null) {
            LOGGER.info("Rejected controller button action because the decoded message was null.");
            return;
        }
        if (player == null || player.world == null || !player.world.isBlockLoaded(message.controllerPos)) {
            LOGGER.info(
                "Rejected controller button action at {} because player/world/chunk state was invalid. player={} world={} loaded={}.",
                message.controllerPos,
                player == null ? "null" : player.getName(),
                player == null || player.world == null ? "null" : player.world.provider.getDimension(),
                player != null && player.world != null && player.world.isBlockLoaded(message.controllerPos)
            );
            return;
        }
        if (!isPlayerEditingThisController(player, message.controllerPos)) {
            LOGGER.info(
                "Rejected controller button action at {} because the player is not editing that controller. openContainer={}",
                message.controllerPos,
                player.openContainer == null ? "null" : player.openContainer.getClass().getName()
            );
            return;
        }

        TileEntity tile = player.world.getTileEntity(message.controllerPos);
        if (!(tile instanceof TileMultiblockMachineController)) {
            LOGGER.info(
                "Rejected controller button action at {} because the server tile was {}.",
                message.controllerPos,
                tile == null ? "null" : tile.getClass().getName()
            );
            return;
        }
        TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;

        if (message.kind == KIND_EVENT) {
            String buttonId = normalizeBounded(message.buttonId, MAX_BUTTON_ID_LENGTH);
            if (buttonId == null || ControllerButtonPolicyManager.matchEvent(controller, buttonId) == null) {
                LOGGER.info("Rejected controller event at {} because no matching server policy exists for buttonId={}.",
                    message.controllerPos, buttonId);
                return;
            }
            MMCEGEEvents.postControllerButtonClick(controller, buttonId);
            PktControllerSmartInterfaceUpdate.syncCustomDataToPlayer(controller, player);
            return;
        }

        String key = normalizeBounded(message.key, MAX_KEY_LENGTH);
        if (key == null) {
            LOGGER.info("Rejected controller smart action at {} because the key was invalid.", message.controllerPos);
            return;
        }
        if (message.stringValue) {
            if (message.kind != KIND_SMART_SET || message.textValue == null || message.textValue.length() > MAX_VALUE_LENGTH) {
                return;
            }
            ControllerButtonPolicyManager.ButtonPolicy stringPolicy =
                ControllerButtonPolicyManager.matchSmart(controller, message.kind, key, message.textValue);
            if (stringPolicy == null) {
                LOGGER.info("Rejected string smart action at {} because no matching server policy exists for key={}.",
                    message.controllerPos, key);
                return;
            }
            if (PktControllerSmartInterfaceUpdate.applySmartInterfaceUpdate(controller, message.controllerPos, key, message.textValue)) {
                PktControllerSmartInterfaceUpdate.syncCustomDataToPlayer(controller, player);
            }
            return;
        }
        if (!Float.isFinite(message.value)) {
            return;
        }
        ControllerButtonPolicyManager.ButtonPolicy policy =
            ControllerButtonPolicyManager.matchSmart(controller, message.kind, key, message.value);
        if (policy == null) {
            LOGGER.info(
                "Rejected numeric smart action at {} because no matching server policy exists for key={} value={} foundMachine={} styleProvider={}.",
                message.controllerPos,
                key,
                message.value,
                controller.getFoundMachine() == null ? "null" : controller.getFoundMachine().getRegistryName(),
                describeStyleProvider(controller)
            );
            return;
        }

        float resolved = message.value;
        if (message.kind == KIND_SMART_ADD) {
            SmartInterfaceData current = controller.getSmartInterfaceData(key);
            Float customValue = PktControllerSmartInterfaceUpdate.readNumericControllerCustomData(controller, key);
            float base = current != null
                ? current.getValue()
                : customValue != null && Float.isFinite(customValue.floatValue()) ? customValue.floatValue() : 0.0F;
            resolved = base + message.value;
        } else if (controller.getSmartInterfaceData(key) == null
            && !ControllerButtonPolicyManager.isConfiguredSmartKey(controller, key)
            && !PktControllerSmartInterfaceUpdate.hasControllerCustomData(controller, key)) {
            LOGGER.info(
                "Rejected numeric smart action at {} because key={} had no Smart Interface data, configured editor, or custom data.",
                message.controllerPos,
                key
            );
            return;
        }
        if (!Float.isFinite(resolved)) {
            return;
        }
        Float min = policy.min;
        Float max = policy.max;
        if (min != null && max != null && min.floatValue() > max.floatValue()) {
            Float swap = min;
            min = max;
            max = swap;
        }
        if (min != null && Float.isFinite(min.floatValue())) {
            resolved = Math.max(min.floatValue(), resolved);
        }
        if (max != null && Float.isFinite(max.floatValue())) {
            resolved = Math.min(max.floatValue(), resolved);
        }
        if (!Float.isFinite(resolved)) {
            return;
        }
        if (PktControllerSmartInterfaceUpdate.applySmartInterfaceUpdate(controller, message.controllerPos, key, resolved)) {
            PktControllerSmartInterfaceUpdate.syncCustomDataToPlayer(controller, player);
        } else {
            LOGGER.info(
                "Server policy matched numeric smart action at {} but applying it failed. key={} value={} foundMachine={} smartData={}.",
                message.controllerPos,
                key,
                resolved,
                controller.getFoundMachine() == null ? "null" : controller.getFoundMachine().getRegistryName(),
                controller.getSmartInterfaceData(key)
            );
        }
    }

    private static String describeStyleProvider(TileMultiblockMachineController controller) {
        if (!(controller instanceof com.fushu.mmceguiext.api.gui.IMachineGuiStyleProvider)) {
            return "none";
        }
        try {
            net.minecraft.util.ResourceLocation style =
                ((com.fushu.mmceguiext.api.gui.IMachineGuiStyleProvider) controller).getMachineControllerGuiStyle();
            return style == null ? "null" : style.toString();
        } catch (RuntimeException ignored) {
            return "error";
        }
    }

    private static String normalizeBounded(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.length() > maxLength ? null : trimmed;
    }

    private static boolean isPlayerEditingThisController(EntityPlayerMP player, BlockPos controllerPos) {
        if (player.getDistanceSqToCenter(controllerPos) > 64D) {
            return false;
        }
        if (player.openContainer == null || !player.openContainer.canInteractWith(player)) {
            return false;
        }

        if (player.openContainer instanceof ContainerController) {
            TileMultiblockMachineController owner = ((ContainerController) player.openContainer).getOwner();
            return owner != null
                && owner.getPos().equals(controllerPos)
                && player.world.getTileEntity(controllerPos) == owner;
        }
        if (player.openContainer instanceof ContainerFactoryController) {
            TileMultiblockMachineController owner = ((ContainerFactoryController) player.openContainer).getOwner();
            return owner != null
                && owner.getPos().equals(controllerPos)
                && player.world.getTileEntity(controllerPos) == owner;
        }
        return false;
    }
}
