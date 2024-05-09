package real_combat.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.apache.log4j.Level;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Vector2f;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class RC_Util {

    public static int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
    }

    public static Vector2f getCollisionRayCircle(Vector2f start, Vector2f end, Vector2f circle, float radius, boolean getNear) {
        float x1 = start.x - circle.x;
        float x2 = end.x - circle.x;
        float y1 = start.y - circle.y;
        float y2 = end.y - circle.y;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dr_sqrd = (dx * dx) + (dy * dy);
        float D = (x1 * y2) - (x2 * y1);
        float delta = (radius * radius * dr_sqrd) - (D * D);
        if (delta < 0f) {
            return null;
        } else if (delta > 0f) {
            float x_sub = Math.signum(dy) * dx * (float) Math.sqrt(delta);
            float x_a = ((D * dy) + x_sub) / dr_sqrd;
            float x_b = ((D * dy) - x_sub) / dr_sqrd;
            float y_sub = Math.abs(dy) * (float) Math.sqrt(delta);
            float y_a = ((-D * dx) + y_sub) / dr_sqrd;
            float y_b = ((-D * dx) - y_sub) / dr_sqrd;
            float dax = x_a - x1;
            float dbx = x_b - x1;
            float day = y_a - y1;
            float dby = y_b - y1;
            float dist_a_sqrt = (dax * dax) + (day * day);
            float dist_b_sqrt = (dbx * dbx) + (dby * dby);
            if ((dist_a_sqrt < dist_b_sqrt) ^ !getNear) {
                return new Vector2f(x_a + circle.x, y_a + circle.y);
            } else {
                return new Vector2f(x_b + circle.x, y_b + circle.y);
            }
        } else {
            float x = (D * dy) / dr_sqrd;
            float y = (-D * dx) / dr_sqrd;
            return new Vector2f(x + circle.x, y + circle.y);
        }
    }

    /* LazyLib 2.4b revert */
    public static List<ShipAPI> getShipsWithinRange(Vector2f location, float range) {
        List<ShipAPI> ships = new ArrayList<>();

        for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
            if (tmp.isShuttlePod()) {
                continue;
            }

            if (MathUtils.isWithinRange(tmp, location, range)) {
                ships.add(tmp);
            }
        }

        return ships;
    }

    /* LazyLib 2.4b revert */
    public static List<MissileAPI> getMissilesWithinRange(Vector2f location, float range) {
        List<MissileAPI> missiles = new ArrayList<>();

        for (MissileAPI tmp : Global.getCombatEngine().getMissiles()) {
            if (MathUtils.isWithinRange(tmp.getLocation(), location, range)) {
                missiles.add(tmp);
            }
        }

        return missiles;
    }

    public static boolean hide(CombatEngineAPI engine){
        if (engine == null || engine.getCombatUI() == null) {return true;}
        if (engine.getCombatUI().isShowingCommandUI() || engine.isUIShowingDialog()) {return true;}
        return !engine.isUIShowingHUD();
    }

    /**
     * 获取两条直线的交点
     * @param line1
     * @param line2
     * @return
     */
    public static Point2D getIntersectPointBy2Line(Line2D line1, Line2D line2) {
        return getIntersectPointBy2Line(line1.getP1(),line1.getP2(),line2.getP1(),line2.getP2());

    }


    /**
     * 获取两条直线的交点
     * @param p1 line1 第一个点
     * @param p2 line1 第二个点
     * @param p3 line2 第一个点
     * @param p4 line2 第二个点
     * @return
     */
    public static Point2D getIntersectPointBy2Line(Point2D p1, Point2D p2, Point2D p3, Point2D p4){


        double A1=p1.getY()-p2.getY();
        double B1=p2.getX()-p1.getX();
        double C1=A1*p1.getX()+B1*p1.getY();

        double A2=p3.getY()-p4.getY();
        double B2=p4.getX()-p3.getX();
        double C2=A2*p3.getX()+B2*p3.getY();

        double det_k=A1*B2-A2*B1;

        if(Math.abs(det_k)<0.00001){
            return null;
        }

        double a=B2/det_k;
        double b=-1*B1/det_k;
        double c=-1*A2/det_k;
        double d=A1/det_k;

        double x=a*C1+b*C2;
        double y=c*C1+d*C2;

        return  new Point2D.Double(x,y);
    }

    public static int createShader(String soruce, int shaderType) {
        // 你不可能在硬件不支持 OpenGL2.0 的情况下调用相关方法，原因是游戏本体的需求只做到了固定管线，所以返回；此处填0意味着输出了一个空的着色器
        if (!GLContext.getCapabilities().OpenGL20) {
            Global.getLogger(Global.class).log(Level.ERROR, "'Your hardware is not supported OpenGL2.0.");
            return 0;
        }
        int shaderID = GL20.glCreateShader(shaderType);
        GL20.glShaderSource(shaderID, soruce);
        GL20.glCompileShader(shaderID);
        // 该分支用于检测着色器是否通过编译而可用
        if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            // 输出相关错误信息至log供修改
            Global.getLogger(Global.class).log(Level.ERROR, "Shader ID: '" + shaderID + "' compilation failed:\n" + GL20.glGetShaderInfoLog(shaderID, GL20.glGetShaderi(shaderID, GL20.GL_INFO_LOG_LENGTH)));
            // 既然不可用，那么将其删除释放相关资源
            GL20.glDeleteShader(shaderID);
            // 在出错的情况下，可以检测这个返回值
            return 0;
        } else {
            // 编译成功，输出一行成功的信息
            Global.getLogger(Global.class).info("Shader compiled with ID: '" + shaderID + "'");
            return shaderID;
        }
    }

    public static int createShaderProgram(String vertSource, String fragSource) {
        // 同创建着色器
        if (!GLContext.getCapabilities().OpenGL20) {
            Global.getLogger(Global.class).log(Level.ERROR, "'Your hardware is not supported OpenGL2.0.");
            return 0;
        }
        int programID = GL20.glCreateProgram();
        int[] shaders = new int[]{createShader(vertSource, GL20.GL_VERTEX_SHADER), createShader(fragSource, GL20.GL_FRAGMENT_SHADER)};
        if (shaders[0] == 0 || shaders[1] == 0) return 0; // 只要有任意一个着色器出问题，必然不可能让这个无效程序就这么运行；此处返回0是因为OpenGL在启用ID为0的着色器程序时意味着关闭
        // 将有效的着色器附着于着色器程序，并链接程序
        GL20.glAttachShader(programID, shaders[0]);
        GL20.glAttachShader(programID, shaders[1]);
        GL20.glLinkProgram(programID);

        // 同着色器创建的操作
        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            Global.getLogger(Global.class).log(Level.ERROR, "'Shader program ID: '" + programID + "' linking failed:\n" + GL20.glGetProgramInfoLog(programID, GL20.glGetProgrami(programID, GL20.GL_INFO_LOG_LENGTH)));
            GL20.glDeleteProgram(programID);
            GL20.glDetachShader(programID, shaders[0]);
            GL20.glDeleteShader(shaders[0]);
            GL20.glDetachShader(programID, shaders[1]);
            GL20.glDeleteShader(shaders[1]);
            return 0;
        } else {
            Global.getLogger(Global.class).info("Shader program created with ID: '" + programID + "'");
            return programID;
        }
    }

}
