package es.drifty.haxe.module.combat;

import java.util.Comparator;

import javax.annotation.Nullable;

import es.drifty.haxe.events.client.EventClientTick;
import es.drifty.haxe.managers.ModuleManager;
import es.drifty.haxe.managers.TickRateManager;
import es.drifty.haxe.module.Module;
import es.drifty.haxe.module.Value;
import es.drifty.haxe.MathUtil;
import es.drifty.haxe.RotationSpoof;
import es.drifty.haxe.util.Timer;
import es.drifty.haxe.util.entity.EntityUtil;
import es.drifty.haxe.util.entity.ItemUtil;
import me.zero.alpine.fork.listener.EventHandler;
import me.zero.alpine.fork.listener.Listener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.init.Items;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumHand;

public class AutoFrameDupeModule extends Module
{
    public final Value<Float> Distance = new Value<Float>("Distance", new String[] {"Range"}, "Range for interacting", 5.0f, 0.0f, 10.0f, 1.0f);
    public final Value<Boolean> TPSSync = new Value<Boolean>("TPSSync", new String[] {"TPSSync"}, "Use TPS Sync for hit delay", false);
    public final Value<Boolean> PauseIfEating = new Value<Boolean>("PauseIfEating", new String[] {"PauseIfEating"}, "Pauses if your eating", true);
    public final Value<Integer> Iterations = new Value<Integer>("Iterations", new String[] {""}, "Allows you to do more iteratons per tick", 1, 1, 2, 1);

    public AutoFrameDupeModule()
    {
        super("KillAura", new String[] {"Aura"}, "Automatically faces and hits entities around you", "NONE", 0xFF0000, ModuleType.COMBAT);
    }

    private Entity CurrentTarget;
    private AimbotModule Aimbot;
    private Timer AimbotResetTimer = new Timer();
    private int RemainingTicks = 0;
    
    @Override
    public void onEnable()
    {
        super.onEnable();
        RemainingTicks = 0;
        
        if (Aimbot == null)
        {
            Aimbot = (AimbotModule) ModuleManager.Get().GetMod(AimbotModule.class);
            
            if (!Aimbot.isEnabled())
                Aimbot.toggle();
        }
    }
    
    @Override
    public void onDisable()
    {
        super.onDisable();
        
        if (Aimbot != null)
            Aimbot.m_RotationSpoof = null;
    }
    
    @Override
    public String getMetaData()
    {
        return Mode.getValue().toString();
    }
    
    @Override
    public void toggleNoSave()
    {
        
    }
    
    private boolean IsValidTarget(Entity p_Entity, @Nullable Entity p_ToIgnore)
    {
        if (!(p_Entity instanceof EntityLivingBase))
        {
            boolean l_IsValid = (p_Entity instanceof ItemFrame);
        }
        
        if (p_ToIgnore != null && p_Entity == p_ToIgnore)
            return false;
        
        if (p_Entity instanceof EntityPlayer)
        {
            /// Ignore if it's us
            if (p_Entity == mc.player)
                return false;
            
            if (Players.getValue())
                return false;
        }
        
        
        if (EntityUtil.isPassive(p_Entity))
        {
            if (p_Entity instanceof AbstractChestHorse)
            {
                AbstractChestHorse l_Horse = (AbstractChestHorse)p_Entity;
                
                if (l_Horse.isTame() && !Tamed.getValue())
                    return false;
            }
            
            if (!Animals.getValue())
                return false;
        }
        
        if (EntityUtil.isHostileMob(p_Entity) && !Monsters.getValue())
            return false;
        
        if (EntityUtil.isNeutralMob(p_Entity) && !Neutrals.getValue())
            return false;
        
        
        if (p_Entity instanceof EntityLivingBase)
        {
            EntityLivingBase l_Base = (EntityLivingBase)p_Entity;
            
            l_HealthCheck = !l_Base.isDead && l_Base.getHealth() > 0.0f;
        }
        
        return l_HealthCheck && p_Entity.getDistance(p_Entity) <= Distance.getValue();
    }

    @EventHandler
    private Listener<EventClientTick> OnTick = new Listener<>(p_Event ->
    {
        
        
        if (AimbotResetTimer.passed(5000))
        {
            AimbotResetTimer.reset();
            Aimbot.m_RotationSpoof = null;
        }
        
        if (RemainingTicks > 0)
        {
            --RemainingTicks;
        }
        
        /// Chose target based on current mode
        Entity l_TargetToHit = CurrentTarget;
        
        switch (Closest)
        {
            case Closest:
                l_TargetToHit = mc.world.loadedEntityList.stream()
                        .filter(p_Entity -> IsValidTarget(p_Entity, null))
                        .min(Comparator.comparing(p_Entity -> mc.player.getDistance(p_Entity)))
                        .orElse(null);
                break;
            default:
                break;
            
        }
        
        if (l_TargetToHit == null || l_TargetToHit.getDistance(mc.player) > Distance.getValue())
        {
            CurrentTarget = null;
            return;
        }
        
        float[] l_Rotation = MathUtil.calcAngle(mc.player.getPositionEyes(mc.getRenderPartialTicks()), l_TargetToHit.getPositionEyes(mc.getRenderPartialTicks()));
        Aimbot.m_RotationSpoof = new RotationSpoof(l_Rotation[0], l_Rotation[1]);

        final float l_Ticks = 20.0f - TickRateManager.Get().getTickRate();

        final boolean l_IsAttackReady = this.HitDelay.getValue() ? (mc.player.getCooledAttackStrength(TPSSync.getValue() ? -l_Ticks : 0.0f) >= 1) : true;
        
        if (!l_IsAttackReady)
            return;

        if (!HitDelay.getValue() && RemainingTicks > 0)
            return;
        
        RemainingTicks = Ticks.getValue();
        
      //  mc.playerController.attackEntity(mc.player, l_TargetToHit);
        for (int l_I = 0; l_I < Iterations.getValue(); ++l_I)
        {
            mc.player.connection.sendPacket(new CPacketUseEntity(l_TargetToHit));
            mc.player.swingArm(EnumHand.MAIN_HAND);
            mc.player.resetCooldown();
        }  
    });
}
