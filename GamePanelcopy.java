import java.lang.Math;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GamePanelcopy extends JPanel implements ActionListener {
    Timer timer;

    //int [][][] map = {new Map().pillarmap[1]};
    int [][][] map = new Map().pillarmap;

    final double DEG = Math.PI/180;

    final int WIDTH = 1024;
    final int HEIGHT = 512;
    final int DEPTH = 512;
    final int DELAY = 50;
    final int mapSize = 8;
    final int blockSize = (int)(Math.log(HEIGHT/mapSize)/Math.log(2)); // log2 of the pixel dimensions of a block

    // [Y][X], so row is Y and column is X, and [0][0] in array is (0, 0) in (Y, X)
    // angle 0 rad is [0][1], and is counterclockwise (just like cartesian plane)
    final int STARTING_X = 96;
    final int STARTING_Y = 96;
    final int STARTING_Z = 511;
    final double STARTING_ANGLE_THETA = Math.PI / 2; // X-Y plane angle
    final double STARTING_ANGLE_EPSILON = 0; // XY plane - Z axis angle
    final int PLAYER_MOUSE_X_MIN = 0;
    final int PLAYER_MOUSE_X_MAX = 1775;
    final int PLAYER_MOUSE_Y_MIN = 0;
    final int PLAYER_MOUSE_Y_MAX = 1119;

    final int COLLIDE_DIST = 20;
    final int HALF_ANGULAR_COVERAGE = 64;
    final int MAX_RAY_DEPTH = 8;
    final int VIEW_PIXELS = 64;
    final double ACCURACY_CONST = 0.0001;

    int ray_dim = 0;
    int map_layer = 0;

    boolean running = false;

    int player_x;
    int player_y;
    int player_z;
    double player_dx;
    double player_dy;
    double player_dz;
    double player_angle_theta;
    double player_dtheta;
    double player_angle_epsilon;
    double player_depsilon;
    int player_speed;
    double player_omega;
    int frame;

    GamePanelcopy() {
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
        player_z = STARTING_Z;
        player_angle_theta = STARTING_ANGLE_THETA;
        player_angle_epsilon = STARTING_ANGLE_EPSILON;
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

    public double dist(double x1, double y1, double z1, double x2, double y2, double z2) {
        return (Math.pow(x2-x1, 2)+Math.pow(y2-y1, 2)+Math.pow(z2-z1, 2));
    }

    public double round_block_base(double n) {
        return Math.round(n*Math.pow(2, blockSize)) / Math.pow(2, blockSize);
    }

    public double terminal(double angle) {
        if (angle > Math.PI * 2) return angle - Math.PI * 2;
        if (angle < 0) return angle + Math.PI * 2;
        return angle;
    }

    public double clamp(double var, double min, double max) {
        if (var < min) return min;
        if (var > max) return max;
        return var;
    }

    public void drawRay(Graphics g) {
        int map_x, map_y, map_z, depth; // map hit index, and depth of ray searching
        double x_offset, y_offset, z_offset; // increment for each ray in each direction in order to hit a block at the beginning
        double ray_angle_theta, ray_angle_epsilon; // ray angle of each planar-axis
        double ray_x, ray_y, ray_z; // ray current position
        double xDist, yDist, zDist, resultDist; // distance of each ray from player to collision
        double lx, ly, lz, wx, wy, wz, hx, hy, hz; // record position of ray hit
        double xfunc_x, yfunc_y, zfunc_y, xfunc_z, yfunc_z, zfunc_x;
        boolean no_draw, ray_pos_set;

        ray_angle_theta = terminal(player_angle_theta - HALF_ANGULAR_COVERAGE * DEG);
        ray_angle_epsilon = terminal(player_angle_epsilon - HALF_ANGULAR_COVERAGE * DEG);

        for (int row = 0; row < VIEW_PIXELS; row++) { // row is in angular measurement
            for (int col = 0; col < VIEW_PIXELS; col++) { // same angular coverage for row and column
                xDist = 1000000000;
                yDist = 1000000000;
                zDist = 1000000000;
                ray_x = 0;
                ray_y = 0;
                ray_z = 0;
                x_offset = 0;
                y_offset = 0;
                z_offset = 0;
                no_draw = false;

                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // Horizontal check
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                depth = 0;
                lx = 0;
                ly = 0;
                lz = 0;
                xfunc_x = - 1 / Math.tan (ray_angle_theta);
                xfunc_z = - 1 / Math.sin (ray_angle_theta) * Math.tan (ray_angle_epsilon);
                ray_pos_set = false;
                if (ray_angle_theta == 0 || ray_angle_theta == Math.PI) { // looking exactly east or west
                    ray_x = player_x;
                    ray_y = player_y;
                    ray_z = player_z;
                    x_offset = 0;
                    y_offset = 0;
                    z_offset = 0;
                    depth = MAX_RAY_DEPTH;
                }
                if (ray_angle_theta > Math.PI) { // looking south (general direction)
                    ray_y = round_block_base(player_y) - ACCURACY_CONST; // ray y is at player y, initially
                    ray_x = (player_y - ray_y) * xfunc_x + player_x; // a bit of starting xtan with the player x
                    ray_z = player_z;
                    y_offset = - HEIGHT / (float)mapSize; // base offset
                    x_offset = -y_offset*xfunc_x;
                    z_offset = -y_offset*xfunc_z;
                }
                if (ray_angle_theta < Math.PI){ // looking north (general direction)
                    ray_y = round_block_base(player_y) + HEIGHT/(float)mapSize;
                    ray_x = (player_y - ray_y) * xfunc_x + player_x;
                    ray_z = player_z;
                    y_offset = HEIGHT / (float)mapSize;
                    x_offset = -y_offset*xfunc_x;
                    z_offset = -y_offset*xfunc_z;
                }
                while (depth < MAX_RAY_DEPTH) {
                    map_x = (int)ray_x >> blockSize;
                    map_y = (int)ray_y >> blockSize;
                    map_z = (int)ray_z >> blockSize;
                    if (map_x < mapSize && map_x >= 0 && map_y < mapSize && map_y >= 0 &&
                            map_z < mapSize && map_z >= 0 && map[map_z][map_y][map_x] != 0) {
                        lx = ray_x;
                        ly = ray_y;
                        lz = ray_z;
                        xDist = dist(player_x, player_y, player_z, lx, ly, lz);
                        depth = MAX_RAY_DEPTH;
                        ray_pos_set = true;
                    } else {
                        ray_x += x_offset;
                        ray_y += y_offset;
                        ray_z += z_offset;
                        depth += 1;
                    }
                }

                if (!ray_pos_set) {
                    lx = ray_x;
                    ly = ray_y;
                    lz = ray_z;
                }

                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // Vertical check
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                depth = 0;
                wx = 0;
                wy = 0;
                wz = 0;
                yfunc_y = - Math.tan (ray_angle_theta);
                yfunc_z = - 1 / Math.cos (ray_angle_theta) * Math.tan (ray_angle_epsilon);
                ray_pos_set = false;
                if (ray_angle_theta == Math.PI/2 || ray_angle_theta == -Math.PI/2) { // looking exactly north or south
                    ray_x = player_x;
                    ray_y = player_y;
                    ray_z = player_z;
                    x_offset = 0;
                    y_offset = 0;
                    z_offset = 0;
                    depth = MAX_RAY_DEPTH;
                }
                if (ray_angle_theta > Math.PI/2 && ray_angle_theta < Math.PI * 3/2) { // looking west (general direction)
                    ray_x = round_block_base(player_x) - ACCURACY_CONST;
                    ray_y = (player_x - ray_x) * yfunc_y + player_y;
                    ray_z = player_z;
                    x_offset = - HEIGHT / (float)mapSize; // base offset
                    y_offset = -x_offset*yfunc_y;
                    z_offset = -x_offset*yfunc_z;
                }
                if (ray_angle_theta < Math.PI/2 || ray_angle_theta > Math.PI * 3/2) {// looking east (general direction)
                    ray_x = round_block_base(player_x) + HEIGHT/(float)mapSize;
                    ray_y = (player_x - ray_x) * yfunc_y + player_y;
                    ray_z = player_z;
                    x_offset = HEIGHT / (float)mapSize;
                    y_offset = -x_offset*yfunc_y;
                    z_offset = -x_offset*yfunc_z;
                }
                while (depth < MAX_RAY_DEPTH) {
                    map_x = (int)ray_x >> blockSize;
                    map_y = (int)ray_y >> blockSize;
                    map_z = (int)ray_z >> blockSize;
                    if (map_x < mapSize && map_x >= 0 && map_y < mapSize && map_y >= 0 &&
                            map_z < mapSize && map_z >= 0 && map[map_z][map_y][map_x] != 0) {
                        wx = ray_x;
                        wy = ray_y;
                        wz = ray_z;
                        yDist = dist(player_x, player_y, player_z, wx, wy, wz);
                        depth = MAX_RAY_DEPTH;
                        ray_pos_set = true;
                    } else {
                        ray_x += x_offset;
                        ray_y += y_offset;
                        ray_z += z_offset;
                        depth += 1;
                    }
                }

                if (!ray_pos_set) {
                    wx = ray_x;
                    wy = ray_y;
                    wz = ray_z;
                }

                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // 3D Plane check
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                depth = 0;
                hx = 0;
                hy = 0;
                hz = 0;
                zfunc_y = 1/Math.tan(ray_angle_epsilon)*Math.sin(ray_angle_theta);
                zfunc_x = 1/Math.tan(ray_angle_epsilon)*Math.cos(ray_angle_theta);
                ray_pos_set = false;
                if (ray_angle_epsilon == 0) { // looking parallel to z-planes
                    ray_x = -player_x;
                    ray_y = -player_y;
                    ray_z = -player_z;
                    x_offset = 0;
                    y_offset = 0;
                    z_offset = 0;
                    depth = MAX_RAY_DEPTH;
                }
                if (ray_angle_epsilon < 0) { // looking up (general direction)
                    ray_z = round_block_base(player_z)- ACCURACY_CONST;
                    ray_x = (player_z - ray_z) * zfunc_x + player_x;
                    ray_y = (player_z - ray_z) * zfunc_y + player_y;
                    z_offset = -HEIGHT / (float)mapSize; // base offset
                    y_offset = -z_offset*zfunc_y;
                    x_offset = z_offset*zfunc_x;
                }
                if (ray_angle_epsilon > 0) {// looking down (general direction)
                    ray_z = round_block_base(player_z) + HEIGHT/(float)mapSize;
                    ray_x = (player_z - ray_z) * zfunc_x + player_x;
                    ray_y = (player_z - ray_z) * zfunc_y + player_y;
                    z_offset = HEIGHT / (float)mapSize;
                    y_offset = z_offset*zfunc_y;
                    x_offset = z_offset*zfunc_x;
                }
                while (depth < MAX_RAY_DEPTH) {
                    map_x = (int)ray_x >> blockSize;
                    map_y = (int)ray_y >> blockSize;
                    map_z = (int)ray_z >> blockSize;
                    if (map_x < mapSize && map_x >= 0 && map_y < mapSize && map_y >= 0 &&
                            map_z < mapSize && map_z >= 0 && map[map_z][map_y][map_x] != 0) {
                        hx = ray_x;
                        hy = ray_y;
                        hz = ray_z;
                        zDist = dist(player_x, player_y, player_z, hx, hy, hz);
                        depth = MAX_RAY_DEPTH;
                        ray_pos_set = true;
                    } else {
                        int factor = -1;
                        ray_x += factor * x_offset;
                        ray_y += factor * y_offset;
                        ray_z += factor * z_offset;
                        depth += 1;
                    }
                }


                if (!ray_pos_set) {
                    hx = ray_x;
                    hy = ray_y;
                    hz = ray_z;
                }

                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // Display
                ////////////////////////////////////////////////////////////////////////////////////////////////////////

                // Find shortest ray
                resultDist = Math.min(Math.min(xDist, yDist), zDist);
                if (resultDist == xDist) {
                    ray_x = lx;
                    ray_y = ly;
                    ray_z = lz;
                    g.setColor(Color.red);
                } else if (resultDist == yDist) {
                    ray_x = wx;
                    ray_y = wy;
                    ray_z = wz;
                    g.setColor(Color.blue);
                } else {
                    ray_x = hx;
                    ray_y = hy;
                    ray_z = hz;
                    g.setColor(Color.green);
                }


                // Draw shorter ray (horizontal detect vs vertical detect) on 2d map
                if(ray_dim == 0) {
                    g.setColor(Color.red);
                    g.drawLine(player_x - 1, player_y - 1, (int) lx, (int) ly);

                    g.setColor(Color.green);
                    g.drawLine(player_x, player_y, (int) wx, (int) wy);

                    g.setColor(Color.blue);
                    g.drawLine(player_x + 2, player_y + 2, (int) hx, (int) hy);
                } else if (ray_dim == 1) {
                    g.setColor(Color.red);
                    g.drawLine(player_x-1, player_y-1, (int)lx, (int)ly);
                } else if (ray_dim == 2) {
                    g.setColor(Color.green);
                    g.drawLine(player_x, player_y, (int)wx, (int)wy);
                } else if (ray_dim == 3) {
                    g.setColor(Color.blue);
                    g.drawLine(player_x+2, player_y+2, (int)hx, (int)hy);
                }




                // Draw contact dot
                g.setColor(Color.yellow);
                g.fillRect((int)ray_x, (int)ray_y, 5, 5);

                // Draw pixel dot
                map_x = (int)ray_x >> blockSize;
                map_y = (int)ray_y >> blockSize;
                map_z = (int)ray_z >> blockSize;
                if (map_x >= mapSize || map_x < 0 ||
                        map_y >= mapSize || map_y < 0 ||
                        map_z >= mapSize || map_z < 0 ) {
                    no_draw = true;
                }


                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // Fix fisheye effect
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // Find the player sight direction vector (with magnitude 1)
                double player_sight_x = Math.cos (player_angle_theta) * Math.cos (player_angle_epsilon);
                double player_sight_y = Math.sin (player_angle_theta) * Math.cos (player_angle_epsilon);
                double player_sight_z = Math.sin (player_angle_epsilon);

                // Find dot product of player sight, and ray vector, which is magnitude of
                // player sight vector times the ray magnitude times the cosine of the angle
                // between the vectors
                double dot = player_sight_x * (ray_x-player_x) + player_sight_y * (ray_y-player_y) + player_sight_z * (ray_z-player_z);
                dot /= resultDist;
                //System.out.println(Math.acos(dot) * 180.0 / Math.PI);
                resultDist *= dot; // currently the cosine of the angle between ray and player sight


                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // Draw on 3d Screen
                ////////////////////////////////////////////////////////////////////////////////////////////////////////

                double tempDist = Math.min(Math.min(xDist, yDist), zDist);;
                /*System.out.println("\nF" + frame + ": Player - " + player_sight_x + " " + player_sight_y + " " + player_sight_z);
                System.out.println("F" + frame + ": Angles - " + player_angle_theta * 180.0 / Math.PI + " " + player_angle_epsilon * 180.0 / Math.PI);
                System.out.println("F" + frame + ": Ray    - " + (ray_x-player_x)/1 + " " +
                        (ray_y-player_y)/1 + " " + (ray_z-player_z)/1);
                System.out.println("F" + frame + ": AngleBe- " + Math.acos(dot) * 180.0 / Math.PI);
                System.out.println("F" + frame + ": Dist- " + resultDist);*/

                // Set distance to "block-worthy" units; divide by 8^3
                g.setColor(Color.magenta);
                g.drawString("X: " + player_x, 50, 50);
                g.drawString("Y: " + player_y, 50, 75);
                g.drawString("Z: " + player_z, 50, 100);
                g.drawString("Theta: " + player_angle_theta * 180 / Math.PI, 50, 125);
                g.drawString("Epsilon: " + player_angle_epsilon * 180 / Math.PI, 50, 150);
                g.drawString("Player_z: " + player_z, 50, 175);
                if (!no_draw) {
                    // Calculate angle between main line of sight and ray angle
                    double shade_multiplier = Math.min((100/resultDist), 1);
                    //shade_multiplier = ;
                    g.setColor(
                            new Color((float)shade_multiplier,
                                    (float)shade_multiplier,
                                    (float)shade_multiplier)
                    );
                    g.fillRect(
                            512+(int) (col * HEIGHT / (HALF_ANGULAR_COVERAGE)),
                            HEIGHT - (int) (row * HEIGHT / (HALF_ANGULAR_COVERAGE)),
                            (int) (HEIGHT / (HALF_ANGULAR_COVERAGE)),
                            (int) (HEIGHT / (HALF_ANGULAR_COVERAGE))
                    );
                }

                // Increment column (theta) angle
                ray_angle_theta += DEG;
                ray_angle_theta = terminal(ray_angle_theta);

            }

            // Increment row (epsilon) angle
            ray_angle_epsilon += DEG;
            ray_angle_epsilon = terminal(ray_angle_epsilon);

            // Reset column (theta) angle;
            ray_angle_theta = terminal(player_angle_theta - HALF_ANGULAR_COVERAGE * DEG);
        }

        frame++;
    }

    public void draw(Graphics g) {
        // Draw all walls on 2d map
        map_layer = (player_z / (DEPTH / mapSize));
        map_layer = (int)clamp(map_layer, 0, mapSize-1);
        g.setColor(Color.white);
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (map[map_layer][i][j] != 0) {
                    g.fillRect(HEIGHT / mapSize * j + 1, HEIGHT / mapSize * i + 1, HEIGHT / mapSize - 2, HEIGHT / mapSize - 2);
                } else if (map_layer > 0) {
                    if (map[map_layer-1][i][j] != 0) {
                        g.setColor(Color.gray);
                        g.fillRect(HEIGHT / mapSize * j + 1, HEIGHT / mapSize * i + 1, HEIGHT / mapSize - 2, HEIGHT / mapSize - 2);
                        g.setColor(Color.white);
                    }
                }
            }
        }

        // Draw direction indicator on 2d map
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(Color.blue);
        g2d.translate(player_x, player_y);
        g2d.rotate(Math.PI + player_angle_theta);
        g2d.translate(-player_x, -player_y);
        g2d.fillRect(player_x, player_y, 48, 10);

        // Draw player on 2d map
        g.setColor(Color.blue);
        g.fillRect(player_x, player_y, 24, 24);
    }

    public void updateMovement () {

        int x_offset = (int)(Math.abs(player_dx)/player_dx) * COLLIDE_DIST;
        int y_offset = (int)(Math.abs(player_dy)/player_dy) * COLLIDE_DIST;

        player_x += player_dx;
        player_y += player_dy;
        player_z += player_dz;

        double mouse_x = clamp(MouseInfo.getPointerInfo().getLocation().x, PLAYER_MOUSE_X_MIN, PLAYER_MOUSE_X_MAX);
        double mouse_y = clamp(MouseInfo.getPointerInfo().getLocation().y, PLAYER_MOUSE_Y_MIN, PLAYER_MOUSE_Y_MAX);
        mouse_x = (mouse_x-PLAYER_MOUSE_X_MIN)/PLAYER_MOUSE_X_MAX;
        mouse_y = (mouse_y-PLAYER_MOUSE_Y_MIN)/PLAYER_MOUSE_Y_MAX;
        double radian_scroll = Math.PI*2;
        player_angle_theta = terminal(mouse_x*radian_scroll-radian_scroll/2);
        player_angle_epsilon = 0; // terminal(-mouse_y*Math.PI+Math.PI/2)

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public class ProjectionKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            running = true;
            if (e.getKeyCode() == KeyEvent.VK_LEFT) { // turn left
                player_dtheta = - player_omega;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) { // turn right
                player_dtheta = player_omega;
            }
            if (e.getKeyCode() == KeyEvent.VK_S && player_y > 0) { // move in direction
                player_dx = -Math.cos(player_angle_theta-Math.PI/360*HALF_ANGULAR_COVERAGE) * player_speed;
                player_dy = -Math.sin(player_angle_theta-Math.PI/360*HALF_ANGULAR_COVERAGE) * player_speed;
            }
            if (e.getKeyCode() == KeyEvent.VK_W && player_y < HEIGHT) { // move opposite direction
                player_dx = Math.cos(player_angle_theta-Math.PI/360*HALF_ANGULAR_COVERAGE) * player_speed;
                player_dy = Math.sin(player_angle_theta-Math.PI/360*HALF_ANGULAR_COVERAGE) * player_speed;
            }
            if (e.getKeyCode() == KeyEvent.VK_A && player_y < HEIGHT) { // move left
                player_dx = -Math.cos(player_angle_theta+Math.PI/2-Math.PI/360*HALF_ANGULAR_COVERAGE) * player_speed;
                player_dy = -Math.sin(player_angle_theta+Math.PI/2-Math.PI/360*HALF_ANGULAR_COVERAGE) * player_speed;
            }
            if (e.getKeyCode() == KeyEvent.VK_D && player_y < HEIGHT) { // move right
                player_dx = Math.cos(player_angle_theta+Math.PI/2-Math.PI/360*HALF_ANGULAR_COVERAGE) * player_speed;
                player_dy = Math.sin(player_angle_theta+Math.PI/2-Math.PI/360*HALF_ANGULAR_COVERAGE) * player_speed;
            }
            if (e.getKeyCode() == KeyEvent.VK_SHIFT && player_z > 0) { // move down directly
                player_dz = -player_speed;
            }
            if (e.getKeyCode() == KeyEvent.VK_SPACE && player_z < DEPTH) { // move up directly
                player_dz = player_speed;
            }

            if (e.getKeyCode() == KeyEvent.VK_0) {
                ray_dim = 0;
            }
            if (e.getKeyCode() == KeyEvent.VK_X) {
                ray_dim = 1;
            }
            if (e.getKeyCode() == KeyEvent.VK_Y) {
                ray_dim = 2;
            }
            if (e.getKeyCode() == KeyEvent.VK_Z) {
                ray_dim = 3;
            }

        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT
                    /*e.getKeyCode() == KeyEvent.VK_E || e.getKeyCode() == KeyEvent.VK_D*/) {
                player_dtheta = 0;
                player_depsilon = 0;
            }
            if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_S
                || e.getKeyCode() == KeyEvent.VK_SHIFT || e.getKeyCode() == KeyEvent.VK_SPACE
                    || e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_D
            ) {
                player_dx = 0;
                player_dy = 0;
                player_dz = 0;
            }
        }
    }
}
