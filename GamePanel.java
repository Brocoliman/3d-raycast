import java.lang.Math;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GamePanel extends JPanel implements ActionListener {

    Timer timer;

    int[][] map = {
            /*       0, 1, 2, 3, 4, 5, 6, 7
            /* 0 */ {1, 1, 1, 1, 1, 1, 1, 1},
            /* 1 */ {1, 0, 0, 2, 0, 0, 0, 1},
            /* 2 */ {1, 0, 0, 1, 0, 0, 1, 1},
            /* 3 */ {1, 1, 1, 1, 0, 0, 0, 1},
            /* 4 */ {1, 0, 0, 1, 0, 0, 0, 1},
            /* 5 */ {1, 0, 0, 0, 0, 0, 0, 1},
            /* 6 */ {1, 0, 0, 0, 0, 1, 0, 0},
            /* 7 */ {1, 0, 0, 0, 0, 0, 0, 0},
    };


    final double DEG = Math.PI/540;

    final int WIDTH = 1024;
    final int HEIGHT = 512;
    final int DELAY = 50;
    final int mapSize = 8;
    final int integerBase = (int)(Math.log(HEIGHT/mapSize)/Math.log(2));

    // [Y][X], so row is Y and column is X, and [0][0] in array is (0, 0) in (Y, X)
    // angle 0 rad is [0][1], and is counterclockwise (just like cartesian plane)

    final int STARTING_X = 96;
    final int STARTING_Y = 96;
    final double STARTING_ANGLE = 3 * Math.PI / 2;

    final int HALF_ANGULAR_COVERAGE = 64;
    final int COLLIDE_DIST = 20;
    final int DOOR_DIST = 100;

    boolean running = false;

    int player_x;
    int player_y;
    double player_angle;
    double player_dx;
    double player_dy;
    double player_dtheta;
    int player_speed;
    double player_omega;
    int frame;

    GamePanel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT)); // minimum size for the component to display correctly
        this.setBackground(Color.black);
        this.setFocusable(true);
        this.addKeyListener(new ProjectionKeyAdapter());

        startGame();
    }

    public void startGame() {
        timer = new Timer(DELAY, this);
        timer.start();

        player_x = STARTING_X;
        player_y = STARTING_Y;
        player_angle = STARTING_ANGLE;
        player_speed = 15;
        player_omega = Math.PI / 36;
        player_dx = 0;
        player_dy = 0;
        frame = 0;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (running) updateMovement();
        draw(g);
        drawRay(g);
    }

    public double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2-x1, 2)+Math.pow(y2-y1, 2));
    }

    public double terminal(double angle) {
        if (angle > Math.PI * 2) return angle - Math.PI * 2;
        if (angle < 0) return angle + Math.PI * 2;
        return angle;
    }

    public void drawRay(Graphics g) {
        int map_x, map_y, depth;
        double ray_x, ray_y, ray_angle, x_offset, y_offset, atan, ntan, hx, hy, vx, vy, hDist, vDist, resultDist, hitIDx, hitIDy;

        ray_angle = player_angle - HALF_ANGULAR_COVERAGE * DEG;
        ray_angle = terminal(ray_angle);
        for (int i = 0; i < 2 * HALF_ANGULAR_COVERAGE; i++) {
            int vertical_texture = 0, horizontal_texture = 0, result_texture = 0;

            // Horizontal Line Check
            depth = 0;
            atan = - 1 / Math.tan (ray_angle);
            hDist = 1000000000;
            vDist = 1000000000;
            ray_x = 0;
            ray_y = 0;
            x_offset = 0;
            y_offset = 0;
            hx = 0;
            hy = 0;
            if (ray_angle == 0 || ray_angle == Math.PI) { // looking exactly left or right
                ray_x = player_x;
                ray_y = player_y;
                x_offset = 0;
                y_offset = 0;
                depth = 8;
            }
            if (ray_angle > Math.PI) { // looking down (general direction)
                ray_y = ((player_y >> integerBase) << integerBase) - 0.0001;
                ray_x = (player_y - ray_y) * atan + player_x;
                y_offset = - HEIGHT / (float)mapSize;
                x_offset = -y_offset*atan;
            }
            if (ray_angle < Math.PI){ // looking up (general direction)
                ray_y = ((player_y >> integerBase) << integerBase) + HEIGHT/8.0;
                ray_x = (player_y - ray_y) * atan + player_x;
                y_offset = HEIGHT / (float)mapSize;
                x_offset = -y_offset*atan;
            }
            while (depth < 8) {
                map_x = (int)ray_x >> integerBase;
                map_y = (int)ray_y >> integerBase;
                if (map_x < mapSize && map_x >= 0 && map_y < mapSize && map_y >= 0 && map[map_y][map_x] != 0) {
                    horizontal_texture = map[map_y][map_x];
                    hx = ray_x;
                    hy = ray_y;
                    hDist = dist(player_x, player_y, hx, hy);
                    depth = 8;
                } else {
                    ray_x += x_offset;
                    ray_y += y_offset;
                    depth += 1;
                }
            }


            // Vertical Line Check
            depth = 0;
            ntan = - Math.tan (ray_angle);
            vx = 0;
            vy = 0;
            if (ray_angle == Math.PI/2 || ray_angle == -Math.PI/2) { // looking exactly up or down
                ray_x = player_x;
                ray_y = player_y;
                x_offset = 0;
                y_offset = 0;
                depth = 8;
            }
            if (ray_angle > Math.PI/2 && ray_angle < Math.PI * 3/2) { // looking left (general direction)
                ray_x = ((player_x >> integerBase) << integerBase) - 0.0001;
                ray_y = (player_x - ray_x) * ntan + player_y;
                x_offset = - HEIGHT / (float)mapSize;
                y_offset = -x_offset*ntan;
            }
            if (ray_angle < Math.PI/2 || ray_angle > Math.PI * 3/2) {// looking right (general direction)
                ray_x = ((player_x >> integerBase) << integerBase) + HEIGHT/(float)mapSize;
                ray_y = (player_x - ray_x) * ntan + player_y;
                x_offset = HEIGHT / (float)mapSize;
                y_offset = -x_offset*ntan;
            }
            while (depth < 8) {
                map_x = (int)ray_x >> integerBase;
                map_y = (int)ray_y >> integerBase;
                if (map_x < mapSize && map_x >= 0 && map_y < mapSize && map_y >= 0 && map[map_y][map_x] != 0) {
                    vertical_texture = map[map_y][map_x];
                    vx = ray_x;
                    vy = ray_y;
                    vDist = dist(player_x, player_y, vx, vy);
                    depth = 8;
                } else {
                    ray_x += x_offset;
                    ray_y += y_offset;
                    depth += 1;
                }
            }

            // Find the shorter one
            resultDist = 0;
            if (vDist > hDist) {
                ray_x = hx;
                ray_y = hy;
                resultDist = hDist;
                result_texture = horizontal_texture;
            }
            if (vDist < hDist) {
                ray_x = vx;
                ray_y = vy;
                resultDist = vDist;
                result_texture = vertical_texture;
            }


            // Fix fisheye effect of the distance
            resultDist *= Math.cos(terminal(ray_angle - player_angle));

            // Draw ray cast
            int barHeight = (int)((64*320) / resultDist);
            float colorLightMultiplier = (float) (ray_x == hx ? (ray_angle < Math.PI ? 255/255.0: 150/255.0):190/255.0);
            boolean no_draw = false;
            map_x = (int)ray_x >> integerBase;
            map_y = (int)ray_y >> integerBase;
            if (map_x >= mapSize || map_x < 0 || map_y >= mapSize || map_y < 0) {
                no_draw = true;
            }

            // Draw shorter ray (horizontal detect vs vertical detect) on 2d map
            g.setColor(Color.yellow);
            g.drawLine(player_x, player_y, (int)ray_x, (int)ray_y);

            // Draw 3d wall projection
            double texture_y = (result_texture-1) * 32;
            double texture_y_step = 32.0 / (float) barHeight;
            double texture_x;
            Texture texture = new Texture(new Color[] {new Color(255, 255, 255), new Color(120, 65, 0)});
            if (!no_draw) {
                if (colorLightMultiplier == (float)(190/255.0)) {
                    texture_x = ray_angle > Math.PI ? (31-(int)(ray_y/2.0) % 32): ((int)(ray_y/2.0) % 32);
                } else {
                    texture_x = ray_angle > Math.PI/2 && ray_angle < Math.PI * 3/2 ? (31-(int)(ray_x/2.0) % 32): ((int)(ray_x/2.0) % 32);
                }
                for (int pixel_y = 0; pixel_y < barHeight; pixel_y++) { // draw each row pixel in a bar
                    g.setColor(texture.getColorMap(texture.getTextureMap()[(int)texture_y*32+(int)texture_x], colorLightMultiplier));
                    g.fillRect((int) (WIDTH / 2 * (1 + i / (2.0 * HALF_ANGULAR_COVERAGE))),
                        HEIGHT / 2 - barHeight / 2 + pixel_y,
                            WIDTH / (4 * HALF_ANGULAR_COVERAGE),
                            2);
                    texture_y += texture_y_step;
                }
            }

            // Draw 3d floor projection
            /*
            double x = 320;
            for (int j = (int)x/2 - (barHeight>>2) + barHeight; j < x * 2; j++) {
                double dy = j-(x/2.0);
                double ray_angle_fixed = Math.cos(terminal(player_angle - ray_angle));
                texture_x = (int)(player_x / 2.0 + Math.cos(ray_angle) * (x/2.0-2) * 32/dy/ray_angle_fixed) & 31;
                texture_y = (int)(player_y / 2.0 - Math.sin(ray_angle) * (x/2.0-2) * 32/dy/ray_angle_fixed) & 31;
                g.setColor(texture.getColorMap(texture.getTextureMap()[(int)texture_y*32+(int)texture_x], 0.7f));
                g.fillRect((int) (WIDTH / 2 * (1 + i / (2.0 * HALF_ANGULAR_COVERAGE))),
                        96 - barHeight / 4 + j,
                        WIDTH / (4 * HALF_ANGULAR_COVERAGE),
                        2);

            }
            */

            // Increment ray angle
            ray_angle += DEG;
            ray_angle = terminal(ray_angle);
        }


        g.setColor(Color.black);
        g.drawString("Frame: " + frame, 50, 50);
        frame ++;

    }

    public void draw(Graphics g) {
        // Draw all walls on 2d map
        g.setColor(Color.white);
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (map[i][j] != 0)
                    g.fillRect(HEIGHT / mapSize * j + 1, HEIGHT / mapSize * i + 1, HEIGHT / mapSize - 2, HEIGHT / mapSize - 2);
            }
        }

        // Draw direction indicator on 2d map
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(Color.blue);
        g2d.translate(player_x, player_y);
        g2d.rotate(Math.PI + player_angle);
        g2d.translate(-player_x, -player_y);
        g2d.fillRect(player_x, player_y, 48, 10);

        // Draw player on 2d map
        g.setColor(Color.blue);
        g.fillRect(player_x, player_y, 24, 24);
    }

    public void updateMovement () {
        player_angle = terminal(player_angle);
        player_angle += player_dtheta;

        int x_offset = (int)(Math.abs(player_dx)/player_dx) * COLLIDE_DIST;
        int y_offset = (int)(Math.abs(player_dy)/player_dy) * COLLIDE_DIST;

        int ipx = player_x / (HEIGHT / mapSize);
        int ipx_with_offset = (player_x+x_offset) / (HEIGHT / mapSize);
        int ipy = player_y / (HEIGHT / mapSize);
        int ipy_with_offset = (player_y+y_offset) / (HEIGHT / mapSize);

        if(map[ipy][ipx_with_offset] == 0) {
            player_x += player_dx;
        }

        if(map[ipy_with_offset][ipx] == 0) {
            player_y += player_dy;
        }


    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public class ProjectionKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            running = true;
            if (e.getKeyCode() == KeyEvent.VK_LEFT && player_x > 0) {
                player_dtheta = - player_omega;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT && player_x < WIDTH) {
                player_dtheta = player_omega;
            }
            if (e.getKeyCode() == KeyEvent.VK_DOWN && player_y > 0) {
                player_dx = -Math.cos(player_angle) * player_speed;
                player_dy = -Math.sin(player_angle) * player_speed;
            }
            if (e.getKeyCode() == KeyEvent.VK_UP && player_y < HEIGHT) {
                player_dx = Math.cos(player_angle) * player_speed;
                player_dy = Math.sin(player_angle) * player_speed;
            }
            if (e.getKeyCode() == KeyEvent.VK_E) {
                int x_offset = (int)(Math.abs(player_dx)/player_dx) * DOOR_DIST;
                int y_offset = (int)(Math.abs(player_dy)/player_dy) * DOOR_DIST;

                int ipx_with_offset = Math.round((player_x+x_offset) / (float)(HEIGHT / mapSize));
                int ipy_with_offset = Math.round((player_y+y_offset) / (float)(HEIGHT / mapSize));
                System.out.println(Math.round((player_x+x_offset) / (float)(HEIGHT / mapSize)) + " "+Math.round((player_y+y_offset) / (float)(HEIGHT / mapSize)));
                if (map[ipy_with_offset][ipx_with_offset] == 2) map[ipy_with_offset][ipx_with_offset] = 0;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                player_dtheta = 0;
            }
            if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) {
                player_dx = 0;
                player_dy = 0;
            }
        }
    }


}
