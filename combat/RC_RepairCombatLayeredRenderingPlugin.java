package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MagicRenderPlugin;
import org.lazywizard.lazylib.LazyLib;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_Drone_borerAI;
import real_combat.util.MyMath;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.Map;

public class RC_RepairCombatLayeredRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_RepairCombatLayeredRenderingPlugin";
    public RC_RepairCombatLayeredRenderingPlugin() {}

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        //获取要绘制的直线
        Map<ShipAPI, RC_Drone_borerAI.NeedDrawLine> allDrawShip = (Map<ShipAPI, RC_Drone_borerAI.NeedDrawLine>) engine.getCustomData().get(ID);
        if (allDrawShip!=null){
            for(ShipAPI s : allDrawShip.keySet())
            {
                RC_Drone_borerAI.NeedDrawLine needDrawLine = allDrawShip.get(s);
                if(needDrawLine.getStartList().size()>0) {
                    float timer = needDrawLine.getTimer();
                    float targetJitterLevel = 0;
                    if (timer == 0) {
                        Global.getCombatEngine().addFloatingText(s.getLocation(), "修理中", 50f, Color.GREEN, s, 5f, 10f);
                    }
                    //开始维修
                    if (timer < 2.5) {
                        targetJitterLevel = timer / 2.5f;
                    } else {
                        targetJitterLevel = (5 - timer) / 2.5f;
                    }
                    s.setJitter(s, Color.GREEN, targetJitterLevel, 0, 0f, 0f);
                    timer += amount;
                    if (timer > 5) {
                        timer = 0;
                    }
                    needDrawLine.setTimer(timer);
                }
            }
        }
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
        CombatEngineAPI engine = Global.getCombatEngine();
        //获取要绘制的直线
        Map<ShipAPI, RC_Drone_borerAI.NeedDrawLine> allDrawShip = (Map<ShipAPI, RC_Drone_borerAI.NeedDrawLine>) engine.getCustomData().get(ID);
        SpriteAPI sprite = Global.getSettings().getSprite("beamfringec", "beamfringec");
        if (allDrawShip!=null){
            for(ShipAPI s : allDrawShip.keySet())
            {
                RC_Drone_borerAI.NeedDrawLine needDrawLine = allDrawShip.get(s);

                for (int i=0;i<needDrawLine.getStartList().size();i++){
                    Vector2f start = needDrawLine.getStartList().get(i);
                    Vector2f end = needDrawLine.getEndList().get(i);

                    float r = 1f;
                    float arg = 0;
                    if (start.getY() > end.getY()) {
                        arg = 90f - (float) Math.toDegrees(Math.atan((start.getX() - end.getX()) / (start.getY() - end.getY())));
                    } else {
                        arg = 270f - (float) Math.toDegrees(Math.atan((start.getX() - end.getX()) / (start.getY() - end.getY())));
                    }
                    Vector2f p1 = new Vector2f(end.getX() + r * (float) Math.cos(Math.toRadians(arg)), end.getY() + r * (float) Math.sin(Math.toRadians(arg)));
                    Vector2f p2 = new Vector2f(start.getX() - r * (float) Math.cos(Math.toRadians(arg)), start.getY() - r * (float) Math.sin(Math.toRadians(arg)));

                    renderLine(Color.GREEN, p1, sprite, p2, 0.1f, 1, 3f , 3f, needDrawLine.getAngle());
                }
                if (needDrawLine.getEndList().size()>0) {
                    SpriteAPI ball = Global.getSettings().getSprite("campaignEntities", "fusion_lamp_glow");
                    ball.setAngle(MyMath.getRandomAngle());
                    ball.setSize(ball.getWidth()/1.5f, ball.getHeight()/1.5f);
                    ball.setColor(Color.GREEN);
                    ball.renderAtCenter(needDrawLine.getEndList().get(0).getX(), needDrawLine.getEndList().get(0).getY());
                    ball.setAlphaMult(1 / needDrawLine.getStartList().size());
                }
                if(!engine.isPaused()) {
                    needDrawLine.getStartList().clear();
                    needDrawLine.getEndList().clear();
                }
            }
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    public void renderLine(Color paramColor, Vector2f anchor, SpriteAPI lineTex, Vector2f target, double t, float alphaMult, float textElapsed, float width, float arg) {
        if (target.getY() > anchor.getY()) {
            arg = 90f - (float) Math.toDegrees(Math.atan((target.getX() - anchor.getX()) / (target.getY() - anchor.getY())));
        } else {
            arg = 270f - (float) Math.toDegrees(Math.atan((target.getX() - anchor.getX()) / (target.getY() - anchor.getY())));
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(0f, 0f, 0f);
        GL11.glRotatef(0f, 0f, 0f, 1f);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        lineTex.bindTexture();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        GL11.glColor4ub((byte) paramColor.getRed(), (byte) paramColor.getGreen(), (byte) paramColor.getBlue(), (byte) (paramColor.getAlpha() * alphaMult));

        float base = 0F;
        float maxTex = lineTex.getTextureHeight();
        float maxWidth=lineTex.getWidth();
        GL11.glBegin(GL11.GL_QUAD_STRIP);

        double x = anchor.getX() +0.5 * width * Math.cos(Math.toRadians(arg));
        double y = anchor.getY() +0.5 * width * Math.sin(Math.toRadians(arg));
        double leftEdgeOfShowingTex = 0D;
        for (double k = 0; k <= 1.1d; k += t) {

            double nextX = anchor.getX() + (target.getX() - anchor.getX()) * (k + t) + 0.5f * width * Math.cos(Math.toRadians(arg + 90f));
            double nextY = anchor.getY() + (target.getY() - anchor.getY()) * (k + t) + 0.5f * width * Math.sin(Math.toRadians(arg + 90f));
            double distance = Math.hypot(nextX - x, nextY - y);

            GL11.glTexCoord2f((float) (leftEdgeOfShowingTex + textElapsed), base);
            GL11.glVertex2f((float) x, (float) y);

            x = anchor.getX() + (target.getX() - anchor.getX()) * k + 0.5f * width * Math.cos(Math.toRadians(arg - 90f));
            y = anchor.getY() + (target.getY() - anchor.getY()) * k + 0.5f * width * Math.sin(Math.toRadians(arg - 90f));
            GL11.glTexCoord2f((float) (leftEdgeOfShowingTex + textElapsed), base);
            GL11.glVertex2f((float) x, (float) y);

            x = nextX;
            y = nextY;
            leftEdgeOfShowingTex += distance;
        }

        GL11.glEnd();
        GL11.glPopMatrix();
    }
}