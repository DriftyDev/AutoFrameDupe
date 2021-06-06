package es.drifty.haxe.module.exploit;

import java.util.Comparator;

import es.drifty.haxe.SalHackMod;
import es.drifty.haxe.events.client.EventClientTick;
import net.minecraft.util.EnumHand;
import es.drifty.haxe.module.Module;
import es.drifty.haxe.module.Value;
import es.drifty.haxe.util.TickedTimer;
import me.zero.alpine.fork.listener.EventHandler;
import me.zero.alpine.fork.listener.Listener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.CPacketUseEntity;

public class AutoFrameDupeModule extends Module
{
    public final Value<Double> Ticks = new Value<Double>("Ticks", new String[] {"Ticks"}, "Tick delay", 10d, 1d, 20d, 1d);

    private TickedTimer tickedTimer;

    public AutoFrameDupeModule()
    {
        super("AutoFrameDupe", new String[] {"FrameDupe"}, "Dupe on colisseum.net, gracias pk2 por arreglar el error", "NONE", 0xFF0000, ModuleType.DUPE);
        tickedTimer = new TickedTimer();
        tickedTimer.stop();
    }

    private boolean Sending = false;
    private Entity entity;

    @Override
    public void onEnable()
    {
        super.onEnable();
        Sending = false;
        tickedTimer.start();
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        tickedTimer.stop();
    }

    @Override
    public void toggleNoSave()
    {

    }

    private boolean isValidTileEntity(Entity entity) { return (entity instanceof EntityItemFrame) && mc.player.getDistance(entity)<4f; }

    @EventHandler
    private Listener<EventClientTick> OnTick = new Listener<>(p_Event ->
    {
        if(!tickedTimer.passed(Ticks.getValue().intValue()))
            return;

        entity = mc.world.loadedEntityList.stream()
                .filter(loadedEntity -> isValidTileEntity(loadedEntity))
                .min(Comparator.comparing(loadedEntity -> mc.player.getDistance(loadedEntity.getPosition().getX(), loadedEntity.getPosition().getY(), loadedEntity.getPosition().getZ())))
                .orElse(null);
        EntityItemFrame itemFrame = (EntityItemFrame)entity;

        if(entity == null) {
            SalHackMod.log.info("No entity found");
            toggle();
            return;
        }

        if(mc.player.getHeldItemMainhand() == null || mc.player.getHeldItemMainhand().getItem() == Items.AIR)
            return;


        if(Sending && (itemFrame.getDisplayedItem() == null || itemFrame.getDisplayedItem().getItem() == Items.AIR))
            Sending = false;
        mc.player.connection.sendPacket(Sending?new CPacketUseEntity(entity):new CPacketUseEntity(entity, EnumHand.MAIN_HAND));
        Sending = !Sending;
        tickedTimer.reset();
    });
}
