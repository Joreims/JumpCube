import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main extends JPanel implements ActionListener, KeyListener {

    // Window
    static final int WIDTH = 800;
    static final int HEIGHT = 600;

    // Player
    int playerX = 100;
    int playerY = 500;
    int playerW = 40;
    int playerH = 50;
    int playerSpeed = 5;
    int velocityY = 0;
    boolean onGround = false;

    // Physics
    int gravity = 1;
    int jumpStrength = -15;

    // Movement keys
    boolean leftPressed = false;
    boolean rightPressed = false;

    // Platforms
    Rectangle[] platforms = {
            new Rectangle(0, 550, 800, 50),
            new Rectangle(200, 450, 200, 20),
            new Rectangle(450, 350, 200, 20),
            new Rectangle(150, 250, 150, 20)
    };

    // Enemy
    Rectangle enemy = new Rectangle(600, 520, 40, 40);
    int enemyDir = -2;

    // Win flag
    Rectangle flag = new Rectangle(700, 180, 20, 100);

    // Timer
    Timer timer = new Timer(16, this);

    public Main() {
        JFrame frame = new JFrame("Simple Platformer");
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.setVisible(true);

        frame.addKeyListener(this);
        timer.start();
    }

    // ============================== GAME LOOP ==============================
    @Override
    public void actionPerformed(ActionEvent e) {
        updatePlayer();
        updateEnemy();
        repaint();
    }

    // ============================== PLAYER UPDATE ==============================
    public void updatePlayer() {
        // Horizontal movement
        if (leftPressed) playerX -= playerSpeed;
        if (rightPressed) playerX += playerSpeed;

        // Gravity
        velocityY += gravity;
        playerY += velocityY;
        onGround = false;

        Rectangle playerRect = new Rectangle(playerX, playerY, playerW, playerH);

        // Platform collision
        for (Rectangle p : platforms) {
            if (playerRect.intersects(p)) {
                if (velocityY > 0) {
                    playerY = p.y - playerH;
                    velocityY = 0;
                    onGround = true;
                }
            }
        }

        // Enemy collision = lose
        if (playerRect.intersects(enemy)) {
            JOptionPane.showMessageDialog(this, "You lost!");
            resetGame();
        }

        // Win collision
        if (playerRect.intersects(flag)) {
            JOptionPane.showMessageDialog(this, "You Win!");
            resetGame();
        }

        // Floor boundaries
        if (playerY > HEIGHT - playerH) {
            playerY = HEIGHT - playerH;
            velocityY = 0;
            onGround = true;
        }
    }

    // ============================== ENEMY UPDATE ==============================
    public void updateEnemy() {
        enemy.x += enemyDir;

        if (enemy.x < 400) enemyDir = 2;
        if (enemy.x > 700) enemyDir = -2;
    }

    public void resetGame() {
        playerX = 100;
        playerY = 500;
        velocityY = 0;
    }

    // ============================== DRAWING ==============================
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.BLACK);

        Graphics2D g2 = (Graphics2D) g;

        // Player
        g2.setColor(Color.CYAN);
        g2.fillRect(playerX, playerY, playerW, playerH);

        // Platforms
        g2.setColor(Color.WHITE);
        for (Rectangle p : platforms) g2.fill(p);

        // Enemy
        g2.setColor(Color.RED);
        g2.fill(enemy);

        // Win Flag
        g2.setColor(Color.YELLOW);
        g2.fill(flag);
    }

    // ============================== KEY INPUT ==============================
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;

        if (e.getKeyCode() == KeyEvent.VK_SPACE && onGround) {
            velocityY = jumpStrength;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // ============================== MAIN ==============================
    public static void main(String[] args) {
        new Main();
    }
}
