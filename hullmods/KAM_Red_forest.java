package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SpriteId;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WingRole;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.P;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.plugins.MagicRenderPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class KAM_Red_forest extends BaseHullMod {
    private static String ID = "KAM_Red_forest";
    private static final float RANGE = 800f;
    public boolean init = false;
    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        List<ShipAPI> redList = new ArrayList<>();
        for (ShipAPI a : AIUtils.getNearbyAllies(ship,RANGE)) {
           if (a.getVariant().hasHullMod(ID)) {
               redList.add(a);
           }
        }
        int count = redList.size();
        if (count>5) {
            count = 5;
        }
        ship.getMutableStats().getShieldDamageTakenMult().modifyPercent(ID,-5*count);
        ship.getMutableStats().getWeaponRangeThreshold().modifyPercent(ID,2*count);
        ship.getMutableStats().getMaxSpeed().modifyPercent(ID,2*count);
        ship.getMutableStats().getMaxTurnRate().modifyPercent(ID,2*count);
        ship.getMutableStats().getAcceleration().modifyPercent(ID,2*count);
        ship.getMutableStats().getTurnAcceleration().modifyPercent(ID,2*count);
        ship.getMutableStats().getBallisticRoFMult().modifyPercent(ID,-2*count);
        ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyPercent(ID,-3*count);
        ship.setCustomData(ID,redList);
        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine.getCustomData().get(ID)==null) {
            engine.addLayeredRenderingPlugin(new KAM_Red_forestPlugin());
            engine.getCustomData().put(ID,true);
        }
    }

    public static class KAM_Red_forestPlugin extends BaseCombatLayeredRenderingPlugin {
        protected float timer = 0f;
        private CombatEngineAPI engine = Global.getCombatEngine();
        private SpriteAPI arrowSprite = Global.getSettings().getSprite("fx","arrow");

        public KAM_Red_forestPlugin() {
        }

        @Override
        public void advance(float amount) {
            if (engine == null) return;
            if (engine.isPaused()) return;
            timer+=amount;
            for (ShipAPI s : engine.getShips()) {
                if (s.getVariant().hasHullMod(ID) && s.isAlive()) {
                    if (s.getCustomData().get(ID) != null) {
                        float schedule = 0f;
                        List<ShipAPI> redList = (List<ShipAPI>) s.getCustomData().get(ID);
                        for (ShipAPI r : redList) {
                            List<Arrow> arrowList = new ArrayList<>();
                            List<Arrow> removeList = new ArrayList<>();
                            if (r.getCustomData().get(ID + s.getId()) != null) {
                                arrowList = (List<Arrow>) r.getCustomData().get(ID + s.getId());
                                //移动
                                for (Arrow a:arrowList) {
                                    a.location = MathUtils.getPoint(r.getLocation(),MathUtils.getDistance(r,s) * a.schedule + r.getCollisionRadius(),VectorUtils.getAngle(r.getLocation(),s.getLocation()));
                                    if (MathUtils.getDistance(a.location,s.getLocation())<=s.getCollisionRadius()) {
                                        removeList.add(a);
                                    }
                                    a.alphaMult-=amount;
                                    if (a.alphaMult<=0) {
                                        a.alphaMult = 0;
                                        removeList.add(a);
                                    }
                                }
                            }
                            if (s.getCustomData().get(ID+"schedule")!=null) {
                                schedule = (float) s.getCustomData().get(ID + "schedule");
                            }
                            if (timer>=0.1f) {
                                schedule+=0.025f;
                                if (schedule>1) {
                                    schedule = 0;
                                }
                                Arrow arrow = new Arrow(r, s, 1f, MathUtils.getPoint(r.getLocation(), MathUtils.getDistance(r,s) * schedule + r.getCollisionRadius(), VectorUtils.getAngle(r.getLocation(), s.getLocation())),schedule);
                                arrowList.add(arrow);
                            }
                            arrowList.removeAll(removeList);
                            r.setCustomData(ID + s.getId(), arrowList);
                        }
                        s.setCustomData(ID + "schedule", schedule);
                    }
                }
            }
            if (timer>=0.1f) {
                timer=0f;
            }
        }

        public void getOut(CombatEntityAPI s) {

        }

        public void init(CombatEngineAPI engine) {

        }

        @Override
        public float getRenderRadius() {
            return 1000000f;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.UNDER_SHIPS_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (engine == null) return;
            //if (engine.isPaused()) return;
            for (ShipAPI s : engine.getShips()) {
                if (s.getVariant().hasHullMod(ID) && s.isAlive()) {
                    if (s.getCustomData().get(ID) != null) {
                        List<ShipAPI> redList = (List<ShipAPI>) s.getCustomData().get(ID);
                        for (ShipAPI r : redList) {
                            List<Arrow> arrowList = new ArrayList<>();
                            if (r.getCustomData().get(ID + s.getId()) != null) {
                                arrowList = (List<Arrow>) r.getCustomData().get(ID + s.getId());
                            }
                            for (Arrow a:arrowList) {
                                /*
                                SpriteAPI sprite,
                                Vector2f loc,
                                Vector2f size,
                                float angle,
                                Color color,
                                boolean additive
                                 */
                                SpriteAPI sprite = Global.getSettings().getSprite("fx","arrow");
                                sprite.setAngle(VectorUtils.getAngle(r.getLocation(),s.getLocation())-90f);
                                sprite.setAlphaMult(a.alphaMult);
                                sprite.setColor(Color.GREEN);
                                sprite.render(a.location.getX(),a.location.getY());
                                //MagicRenderPlugin.addSingleframe(sprite, a.location, CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        /**
         * 每0.5s创造一个箭头
         * 在上一个箭头前面创建
         * 每帧移动所有箭头
         * 总进度需要记录
         */
        public class Arrow{
            ShipAPI from;
            ShipAPI to;
            float alphaMult;
            Vector2f location;
            float schedule;
            public Arrow(ShipAPI from,ShipAPI to,float alphaMult,Vector2f location,float schedule){
                this.from = from;
                this.location = location;
                this.to = to;
                this.alphaMult = alphaMult;
                this.schedule = schedule;
            }
        }
    }
}
