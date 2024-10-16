package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import real_combat.weapons.RC_MirrorOnHitEffect;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class RC_MirrorLayeredRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_MirrorOnHitEffect";
    private SpriteAPI sprite  = Global.getSettings().getSprite("graphics/missiles/mirror_shot.png");
    private CombatEngineAPI engine = Global.getCombatEngine();
    List<MirrorSprite> spriteList = new ArrayList<>();
    public RC_MirrorLayeredRenderingPlugin() {}

    @Override
    public void advance(float amount) {
        if (engine==null) {return;}
        //if (engine.isPaused()) {return;}
        if (engine.getCustomData().get(ID)==null) {return;}
        List<RC_MirrorOnHitEffect.MirrorProj> projList = (List<RC_MirrorOnHitEffect.MirrorProj>) engine.getCustomData().get(ID);
        for (RC_MirrorOnHitEffect.MirrorProj p:projList) {
            if(!p.projectile.isFading()&&!p.projectile.isExpired()) {
                SpriteAPI sprite = Global.getSettings().getSprite("graphics/missiles/mirror_shot.png");
                sprite.setAlphaMult(p.alphaMult);
                sprite.setAngle(p.angle);
                //sprite.renderAtCenter(new Vector2f(p.projectile.getLocation()).x,new Vector2f(p.projectile.getLocation()).y);
                MirrorSprite mirrorSprite = new MirrorSprite(new Vector2f(p.projectile.getLocation()), sprite);
                spriteList.add(mirrorSprite);
            }
        }
        List<MirrorSprite> removeList = new ArrayList<>();
        for (MirrorSprite s:spriteList) {
            s.sprite.setAlphaMult(s.sprite.getAlphaMult()-amount*6);
            if (s.sprite.getAlphaMult()<0) {
                s.sprite.setAlphaMult(0);
                removeList.add(s);
            }
        }
        spriteList.removeAll(removeList);
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
        for (MirrorSprite s:spriteList) {
            s.sprite.renderAtCenter(s.point.x, s.point.y);
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    public class MirrorSprite{
        public SpriteAPI sprite;
        public Vector2f point;
        public MirrorSprite(Vector2f point,SpriteAPI sprite)
        {
            this.point = point;
            this.sprite = sprite;
        }
    }
}