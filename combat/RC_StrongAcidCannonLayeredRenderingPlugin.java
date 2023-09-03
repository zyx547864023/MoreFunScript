package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.util.MyMath;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class RC_StrongAcidCannonLayeredRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_StrongAcidCannonEffect";
    private final static String PROJ_ID = "strong_acid_cannon";
    private CombatEngineAPI engine = Global.getCombatEngine();

    public RC_StrongAcidCannonLayeredRenderingPlugin() {}

    @Override
    public void advance(float amount) {
        if (engine==null) {return;}
        if (engine.isPaused()) {return;}
        List<DamagingProjectileAPI> projList = new ArrayList<>();
        if (engine.getCustomData().get(ID)!=null) {
            List<DamagingProjectileAPI> newProjList = (List<DamagingProjectileAPI>)engine.getCustomData().get(ID);
            List<DamagingProjectileAPI> removeList = new ArrayList<>();
            for (DamagingProjectileAPI n:newProjList) {
                if(n.isFading()||n.isExpired()) {
                    removeList.add(n);
                }
                else {
                    projList.add(n);
                }
            }
            newProjList.removeAll(removeList);
            engine.getCustomData().put(ID,newProjList);
        }
        for (DamagingProjectileAPI p:projList) {
            for (int i = 0; i < 7; i++) {
                Vector2f partstartloc = MathUtils.getPointOnCircumference(p.getLocation(), 20f, MyMath.RANDOM.nextFloat() * 360.0F);
                Vector2f partvec = Vector2f.sub(partstartloc, p.getLocation(), (Vector2f) null);
                partvec.scale(1F);
                float size = MathUtils.getRandomNumberInRange(40F, 60F);
                engine.addNegativeNebulaParticle(partstartloc, partvec, size, 1.5f, 0.05f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), RiftLanceEffect.getColorForDarkening(Color.GREEN));
                engine.addSwirlyNebulaParticle(partstartloc, partvec, size, 2f, 0.05f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.GREEN, false);
            }
        }
        /*
        for (DamagingProjectileAPI p:engine.getProjectiles()) {
            if (p.getWeapon()==null) {
                continue;
            }
            if (!DamageType.HIGH_EXPLOSIVE.equals(p.getDamageType()) && !WeaponAPI.WeaponType.BALLISTIC.equals(p.getWeapon().getType())) {
                continue;
            }
            if ("strong_acid_cannon_shot".equals(p.getProjectileSpecId()))
                for (int i = 0; i < 7; i++) {
                    Vector2f partstartloc = MathUtils.getPointOnCircumference(p.getLocation(), 20f, MyMath.RANDOM.nextFloat() * 360.0F);
                    Vector2f partvec = Vector2f.sub(partstartloc, p.getLocation(), (Vector2f) null);
                    partvec.scale(1.5F);
                    float size = MathUtils.getRandomNumberInRange(40F, 60F);
                    engine.addNegativeNebulaParticle(partstartloc, partvec, size, 1.5f, 0.05f, 0f, 1f+MathUtils.getRandomNumberInRange(0.1f, 1f), RiftLanceEffect.getColorForDarkening(Color.GREEN));
                    engine.addSwirlyNebulaParticle(partstartloc, partvec, size, 2f, 0.05f, 0f, 1f+MathUtils.getRandomNumberInRange(0.1f, 1f), Color.GREEN, false);
                }
        }
         */
    }

    public void getOut(CombatEntityAPI s){

    }

    public void init(CombatEngineAPI engine) {
    }

    @Override
    public float getRenderRadius() {
        return 1000000f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {

    }

    @Override
    public boolean isExpired() {
        return false;
    }
}