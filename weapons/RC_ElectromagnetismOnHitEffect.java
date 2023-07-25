package real_combat.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_AnchoredEntity;

import java.util.ArrayList;
import java.util.List;



public class RC_ElectromagnetismOnHitEffect implements OnHitEffectPlugin {
    private final static String ID="ELECTROMAGNETISMONHITEFFECT";
    private final static String ELECTROMAGNETISM_LIST="ELECTROMAGNETISM_LIST";
    private final static float TIME_FOR_EXPLOSION = 1.0F;
    private List<ElectromagnetismProj> electromagnetismProjList = new ArrayList<>();
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        //相对位置
        float targetFace = target.getFacing();
        float landingFace = projectile.getFacing();
        float relativeAngle = MathUtils.getShortestRotation(targetFace,landingFace);
        RC_AnchoredEntity a = new RC_AnchoredEntity(target,point,relativeAngle);
        //存放剩余时间
        //存放命中的物体
        //存放是否命中盾
        //存放发射的武器
        if(engine.getCustomData().get(ELECTROMAGNETISM_LIST)!=null)
        {
            electromagnetismProjList = (List<ElectromagnetismProj>) engine.getCustomData().get(ELECTROMAGNETISM_LIST);
        }
        electromagnetismProjList.add(new ElectromagnetismProj(a,target,projectile.getWeapon(),projectile,shieldHit));
        engine.getCustomData().put(ELECTROMAGNETISM_LIST,electromagnetismProjList);

    }

    public class ElectromagnetismProj{
        public RC_AnchoredEntity anchoredEntity;
        public CombatEntityAPI target;
        public WeaponAPI weapon;
        public DamagingProjectileAPI projectile;
        public Boolean shieldHit;
        public float distance = 1.5F;
        public float timeForExplosion = TIME_FOR_EXPLOSION;
        public ElectromagnetismProj(RC_AnchoredEntity anchoredEntity,CombatEntityAPI target,WeaponAPI weapon,DamagingProjectileAPI projectile,Boolean shieldHit)
        {
            this.anchoredEntity = anchoredEntity;
            this.target = target;
            this.weapon = weapon;
            this.projectile = projectile;
            this.shieldHit = shieldHit;
        }
    }
}
