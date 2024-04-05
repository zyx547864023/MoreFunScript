package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BreachOnHitEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_BaseShipAI;
import real_combat.ai.RC_CounterWeaponFighterAI;
import real_combat.ai.RC_FighterAI;
import real_combat.ai.RC_FrigateAI;
import real_combat.weapons.RC_CamouflageNetOnHitEffect;
import real_combat.weapons.RC_OvumOnHitEffect;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 命中之后持续生成电弧 给一个力拉向目标
 * 导弹命中之后展开蛛网
 * 先*
 * 后端点连线向外延伸
 * 可以设定层数
 *
 * 导弹网是平的
 */

public class RC_OvumRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_OvumRenderingPlugin";
    private final static String OVUM_LIST="OVUM_LIST";
    private final static String OVUM_SHIP="OVUM_SHIP";
    private final static String VARIANT="RC_spy";
    private final static int MASS=30;
    private final static float SYSTEM_AMMO_FONT_SIZE = 20;

    protected IntervalUtil tracker = new IntervalUtil(1f, 1f);
    private CombatEngineAPI engine = Global.getCombatEngine();
    private LazyFont font;

    public RC_OvumRenderingPlugin() {}
    @Override
    public void advance(float amount) {

        if (!engine.isPaused()) {
            tracker.advance(amount);
            if (tracker.intervalElapsed()) {
                if (engine.getCustomData().get(OVUM_LIST) != null) {
                    List<RC_OvumOnHitEffect.RC_Ovum> ovumList = (List<RC_OvumOnHitEffect.RC_Ovum>) engine.getCustomData().get(OVUM_LIST);
                    List<ShipAPI> ovumShip = (List<ShipAPI>) engine.getCustomData().get(OVUM_SHIP);
                    List<RC_OvumOnHitEffect.RC_Ovum> removeList = new ArrayList<>();
                    for (RC_OvumOnHitEffect.RC_Ovum o:ovumList) {
                        if (o.time>0) {
                            o.time--;
                            if (o.ship.getHitpoints()/o.ship.getMaxHitpoints()<0.1f) {
                                o.time++;
                            }
                            if (!o.ship.isAlive()) {
                                //召唤
                                float count = o.ship.getMass()*o.time/10000;
                                if (count>24) {
                                    count = 24;
                                }
                                for (int num=0;num<count;num++) {
                                    ShipAPI spy = engine.createFXDrone(Global.getSettings().getVariant("RC_copy_variant"));
                                    spy.getLocation().set(MathUtils.getRandomPointInCircle(o.ship.getLocation(),o.ship.getCollisionRadius()));
                                    spy.setFacing(MathUtils.getRandomNumberInRange(0,360f));
                                    spy.getVelocity().set(MathUtils.getPoint(new Vector2f(0,0),spy.getMaxSpeed(),spy.getFacing()));
                                    spy.setShipAI(new RC_CounterWeaponFighterAI(spy));
                                    if (o.ship.getOwner()==0) {
                                        spy.setOwner(1);
                                    }
                                    else {
                                        spy.setOwner(0);
                                    }
                                    engine.addEntity(spy);
                                }
                                ovumShip.remove(o.ship);
                                removeList.add(o);
                            }
                        }
                        else {
                            ovumShip.remove(o.ship);
                            removeList.add(o);
                        }
                    }
                    ovumList.removeAll(removeList);
                    engine.getCustomData().put(OVUM_SHIP, ovumShip);
                    engine.getCustomData().put(OVUM_LIST, ovumList);
                }
            }
        }
    }

    public void getOut(CombatEntityAPI s){

    }
    private final static String FONT_FILE = "graphics/fonts/orbitron20aabold.fnt";
    public void init(CombatEngineAPI engine) {
        try {
            font = LazyFont.loadFont(FONT_FILE);
        } catch (FontException ex) {
            throw new RuntimeException("Failed to load font");
        }
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
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.getCustomData().get(OVUM_LIST) != null) {
            List<RC_OvumOnHitEffect.RC_Ovum> ovumList = (List<RC_OvumOnHitEffect.RC_Ovum>) engine.getCustomData().get(OVUM_LIST);
            for (RC_OvumOnHitEffect.RC_Ovum o : ovumList) {
                if (o.ship.isAlive()) {
                    drawFont(o.time+"", Color.WHITE, 0, o.ship.getCollisionRadius(), 0, SYSTEM_AMMO_FONT_SIZE * viewport.getViewMult() * 1.1f, o.ship);
                    drawFont(o.time+"", Color.RED, 0, o.ship.getCollisionRadius(), 0, SYSTEM_AMMO_FONT_SIZE * viewport.getViewMult(), o.ship);
                }
            }
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    private void drawFont(String text, Color color, float aimAngle, float right, float up, float size, ShipAPI ship){
        color = new Color(color.getRed(), color.getGreen(), color.getBlue());
        if (font == null) {
            try {
                font = LazyFont.loadFont(FONT_FILE);
            } catch (FontException ex) {
                throw new RuntimeException("Failed to load font");
            }
        }
        LazyFont.DrawableString toDraw = font.createText(text, color, size);
        Vector2f point = MathUtils.getPoint(ship.getLocation(),right,ship.getFacing()+aimAngle);
        //point = MathUtils.getPoint(point,size/2,135F);
        //point = MathUtils.getPoint(point,size/8,90F);
        toDraw.draw(
                point.getX(),
                point.getY()

        );
    }
}