package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class Main extends JPanel implements ActionListener, KeyListener {

    // Window
    static final int WIDTH = 900;
    static final int HEIGHT = 640;

    // Game loop
    private final Timer timer;
    private final int FPS = 60;

    // Player
    private Player player;

    // Level system
    private LevelBase currentLevel;
    private int currentLevelIndex = 1; // 1..10
    private final int MAX_LEVEL = 10;

    // Lives & scoring
    private int lives = 5;
    private final int MAX_LIVES = 5;
    private int totalScore = 0;
    private int levelsPassed = 0; // used for life recovery every 3 levels

    // Floating texts for "+X" feedback
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    // Game state
    private enum State {PLAYING, LEVEL_TRANSITION, GAME_OVER, FINISHED}

    private State state = State.PLAYING;

    // Input
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean jumpPressed = false;
    private boolean downPressed = false;

    // Sounds
    private Clip bgMusic;

    // Constructor
    public Main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235));
        setFocusable(true);
        addKeyListener(this);

        // Initialize game state before creating timer
        initGame();

        // Initialize Swing timer
        timer = new Timer(1000 / FPS, this);
        timer.start();
    }

    // Initialize or restart whole game
    private void initGame() {
        lives = MAX_LIVES;
        totalScore = 0;
        currentLevelIndex = 1;
        levelsPassed = 0;
        player = new Player(60, HEIGHT - 150, 36, 48);
        loadLevel(currentLevelIndex);
        state = State.PLAYING;
        floatingTexts.clear();

        // Start main theme
        if (bgMusic != null && bgMusic.isRunning()) bgMusic.stop();
        bgMusic = SoundManager.playLoop("mainTheme.wav", -15.0f);
    }

    // Load a level based on index
    private void loadLevel(int idx) {
        switch (idx) {
            case 1:
                currentLevel = new Level1();
                break;
            case 2:
                currentLevel = new Level2();
                break;
            case 3:
                currentLevel = new Level3();
                break;
            case 4:
                currentLevel = new Level4();
                break;
            case 5:
                currentLevel = new Level5();
                break;
            case 6:
                currentLevel = new Level6();
                break;
            case 7:
                currentLevel = new Level7();
                break;
            case 8:
                currentLevel = new Level8();
                break;
            case 9:
                currentLevel = new Level9();
                break;
            case 10:
                currentLevel = new Level10();
                break;
            default:
                currentLevel = new Level1();
                break;
        }
        player.setX(currentLevel.playerStartX);
        player.setY(currentLevel.playerStartY);
        player.resetVelocity();
        state = State.PLAYING;
    }

    // Main loop
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.PLAYING) {
            gameUpdate();
        } else if (state == State.LEVEL_TRANSITION) {
            if (currentLevelIndex > MAX_LEVEL) {
                finishGame();
            } else {
                loadLevel(currentLevelIndex);
            }
        }

        Iterator<FloatingText> it = floatingTexts.iterator();
        while (it.hasNext()) {
            FloatingText ft = it.next();
            ft.update();
            if (ft.isFinished()) it.remove();
        }

        repaint();
    }

    private void gameUpdate() {
        if (leftPressed && !rightPressed) player.moveLeft();
        else if (rightPressed && !leftPressed) player.moveRight();
        else player.stopX();

        if (jumpPressed) {
            player.jump();
            jumpPressed = false;
        }

        player.applyGravity();
        player.updatePosition();

        handlePlatformCollisions();

        if (currentLevel != null) currentLevel.updateEnemy();

        if (currentLevel != null && currentLevel.enemy != null
                && player.getBounds().intersects(currentLevel.enemy.getBounds())) {
            loseLifeAndRespawn("Hit by enemy!");
        }

        if (player.getY() > HEIGHT + 200) {
            loseLifeAndRespawn("You fell!");
        }

        if (currentLevel != null && currentLevel.flag != null
                && player.getBounds().intersects(currentLevel.flag)) {
            levelCompleted();
        }
    }

    private void handlePlatformCollisions() {
        Rectangle pRect = player.getBounds();
        boolean onAnyPlatform = false;

        if (currentLevel == null) return;

        for (Platform plat : currentLevel.platforms) {
            Rectangle r = plat.getBounds();
            if (pRect.intersects(r)) {
                Rectangle inter = pRect.intersection(r);
                if (inter.height < inter.width) {
                    if (player.getVy() > 0) {
                        player.setY(r.y - player.getH());
                        player.setVy(0);
                        onAnyPlatform = true;
                        player.setCanJump(true);
                    } else if (player.getVy() < 0) {
                        player.setY(r.y + r.height);
                        player.setVy(0);
                    }
                } else {
                    if (player.getVx() > 0) player.setX(r.x - player.getW());
                    else if (player.getVx() < 0) player.setX(r.x + r.width);
                    player.setVx(0);
                }
                pRect = player.getBounds();
            }
        }
        if (!onAnyPlatform) player.setCanJump(false);

        if (player.getX() < 0) player.setX(0);
        if (player.getX() + player.getW() > WIDTH) player.setX(WIDTH - player.getW());
    }

    private void loseLifeAndRespawn(String reason) {
        SoundManager.playSound("death.wav");
        lives--;
        if (lives <= 0) {
            state = State.GAME_OVER;
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(this,
                        reason + " You have no lives left. Game Over.\nRestart game?",
                        "Game Over",
                        JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) initGame();
                else System.exit(0);
            });
        } else {
            floatingTexts.add(new FloatingText(" -1 Life", player.getX(), player.getY() - 20, 2000));
            player.setX(currentLevel.playerStartX);
            player.setY(currentLevel.playerStartY);
            player.resetVelocity();
        }
    }

    private void levelCompleted() {
        SoundManager.playSound("flagPole.wav");
        int levelPoints = currentLevelIndex * 5;
        boolean hadFullLives = (lives == MAX_LIVES);
        totalScore += levelPoints;

        if (currentLevel.flag != null) {
            floatingTexts.add(new FloatingText("+" + levelPoints, currentLevel.flag.x, currentLevel.flag.y - 20, 1800));
        }

        if (hadFullLives) {
            totalScore += 50;
            floatingTexts.add(new FloatingText("+50 (Full lives bonus)", WIDTH / 2 - 40, HEIGHT / 2 - 40, 2200));
        }

        levelsPassed++;
        if (levelsPassed % 3 == 0 && lives < MAX_LIVES) {
            lives++;
            floatingTexts.add(new FloatingText("+1 Life", WIDTH - 120, 50, 2000));
        }

        floatingTexts.add(new FloatingText("Total: " + totalScore, 20, 40, 1500));
        currentLevelIndex++;
        state = State.LEVEL_TRANSITION;
    }

    private void finishGame() {
        if (bgMusic != null && bgMusic.isRunning()) bgMusic.stop();
        state = State.FINISHED;
        SwingUtilities.invokeLater(() -> {
            String name = JOptionPane.showInputDialog(this,
                    "You finished all levels!\nYour score: " + totalScore + "\nEnter your name:",
                    "All Levels Completed",
                    JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) name = "Anonymous";
            JOptionPane.showMessageDialog(this,
                    "Thanks for playing, " + name + "!\nFinal score: " + totalScore);
            int choice = JOptionPane.showConfirmDialog(this,
                    "Play again?",
                    "Play again?",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) initGame();
            else System.exit(0);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScene((Graphics2D) g);
    }

    private void drawScene(Graphics2D g2) {
        g2.setColor(new Color(135, 206, 235));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        if (currentLevel != null) {
            g2.setColor(new Color(120, 72, 18));
            for (Platform p : currentLevel.platforms) g2.fillRect(p.x, p.y, p.width, p.height);

            if (currentLevel.flag != null) {
                g2.setColor(new Color(255, 215, 0));
                g2.fillRect(currentLevel.flag.x, currentLevel.flag.y, currentLevel.flag.width, currentLevel.flag.height);
            }

            if (currentLevel.enemy != null) {
                g2.setColor(new Color(200, 0, 0));
                Rectangle er = currentLevel.enemy.getBounds();
                g2.fillRect(er.x, er.y, er.width, er.height);
            }
        }

        g2.setColor(Color.BLUE);
        Rectangle pr = player.getBounds();
        g2.fillRect(pr.x, pr.y, pr.width, pr.height);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Score: " + totalScore, 18, 26);
        g2.drawString("Lives: " + lives, WIDTH - 110, 26);
        g2.drawString("Level: " + Math.min(currentLevelIndex, MAX_LEVEL) + " / " + MAX_LEVEL, WIDTH / 2 - 60, 26);

        for (FloatingText ft : floatingTexts) ft.draw(g2);

        if (state == State.FINISHED) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            g2.setColor(new Color(0, 120, 0));
            drawCenteredString(g2, "ALL LEVELS COMPLETED!", WIDTH, HEIGHT);
        } else if (state == State.GAME_OVER) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            g2.setColor(new Color(160, 0, 0));
            drawCenteredString(g2, "GAME OVER", WIDTH, HEIGHT);
        }
    }

    private void drawCenteredString(Graphics2D g2, String text, int w, int h) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(text)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int kc = e.getKeyCode();
        if (kc == KeyEvent.VK_A) leftPressed = true;
        if (kc == KeyEvent.VK_D) rightPressed = true;
        if (kc == KeyEvent.VK_W) jumpPressed = true;
        if (kc == KeyEvent.VK_S) downPressed = true;
        if (kc == KeyEvent.VK_R) initGame();
        if (kc == KeyEvent.VK_ESCAPE) System.exit(0);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int kc = e.getKeyCode();
        if (kc == KeyEvent.VK_A) leftPressed = false;
        if (kc == KeyEvent.VK_D) rightPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Platformer - 10 Levels");
            Main gamePanel = new Main();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(gamePanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // -------------------------
    // Helper classes
    // -------------------------

    static class Player {
        private double x, y, vx = 0, vy = 0;
        private final int w, h;
        private final double speed = 4.2, jumpStrength = -13.2, gravity = 0.6;
        private boolean canJump = false;

        public Player(double x, double y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public void moveLeft() {
            vx = -speed;
        }

        public void moveRight() {
            vx = speed;
        }

        public void stopX() {
            vx = 0;
        }

        public void jump() {
            if (canJump) {
                vy = jumpStrength;
                canJump = false;
            }
        }

        public void applyGravity() {
            vy += gravity;
            if (vy > 16) vy = 16;
        }

        public void updatePosition() {
            x += vx;
            y += vy;
        }

        public Rectangle getBounds() {
            return new Rectangle((int) x, (int) y, w, h);
        }

        public int getX() {
            return (int) x;
        }

        public int getY() {
            return (int) y;
        }

        public int getW() {
            return w;
        }

        public int getH() {
            return h;
        }

        public double getVx() {
            return vx;
        }

        public double getVy() {
            return vy;
        }

        public void setX(double nx) {
            x = nx;
        }

        public void setY(double ny) {
            y = ny;
        }

        public void setVx(double nvx) {
            vx = nvx;
        }

        public void setVy(double nvy) {
            vy = nvy;
        }

        public void setCanJump(boolean b) {
            canJump = b;
        }

        public void resetVelocity() {
            vx = 0;
            vy = 0;
        }
    }

    static class Platform {
        public int x, y, width, height;

        public Platform(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    static class Enemy {
        double x, y;
        int w, h;
        double leftBound, rightBound, speed;
        int dir = 1;

        public Enemy(double x, double y, int w, int h, double left, double right, double speed) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.leftBound = left;
            this.rightBound = right;
            this.speed = speed;
        }

        public void update() {
            x += dir * speed;
            if (x < leftBound) {
                x = leftBound;
                dir = 1;
            } else if (x + w > rightBound) {
                x = rightBound - w;
                dir = -1;
            }
        }

        public Rectangle getBounds() {
            return new Rectangle((int) x, (int) y, w, h);
        }
    }

    static class FloatingText {
        String text;
        double x, y;
        long durationMs, startTime;

        public FloatingText(String text, double x, double y, long durationMs) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.durationMs = durationMs;
            this.startTime = System.currentTimeMillis();
        }

        public void update() {
            y -= 0.3;
        }

        public boolean isFinished() {
            return System.currentTimeMillis() - startTime > durationMs;
        }

        public void draw(Graphics2D g2) {
            float alpha = Math.max(0, 1 - (System.currentTimeMillis() - startTime) / (float) durationMs);
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.setColor(Color.BLACK);
            g2.drawString(text, (int) x, (int) y);
            g2.setComposite(old);
        }
    }

    // -------------------------
    // Levels
    // -------------------------

    static abstract class LevelBase {
        List<Platform> platforms = new ArrayList<>();
        Enemy enemy;
        Rectangle flag;
        int playerStartX = 60;
        int playerStartY = HEIGHT - 150;

        public void updateEnemy() {
            if (enemy != null) enemy.update();
        }
    }

    static class Level1 extends LevelBase {
        public Level1() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(150, HEIGHT - 180, 160, 18));
            platforms.add(new Platform(380, HEIGHT - 260, 140, 18));
            platforms.add(new Platform(600, HEIGHT - 320, 120, 18));
            enemy = new Enemy(200, HEIGHT - 80 - 32, 32, 32, 180, 320, 1.6);
            flag = new Rectangle(600, HEIGHT - 320 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level2 extends LevelBase {
        public Level2() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(100, HEIGHT - 220, 130, 18));
            platforms.add(new Platform(300, HEIGHT - 300, 120, 18));
            platforms.add(new Platform(480, HEIGHT - 200, 140, 18));
            platforms.add(new Platform(700, HEIGHT - 320, 120, 18));
            enemy = new Enemy(350, HEIGHT - 300 - 32, 32, 32, 300, 420, 1.8);
            flag = new Rectangle(700, HEIGHT - 320 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level3 extends LevelBase {
        public Level3() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(120, HEIGHT - 190, 130, 18));
            platforms.add(new Platform(280, HEIGHT - 320, 120, 18));
            platforms.add(new Platform(440, HEIGHT - 250, 150, 18));
            platforms.add(new Platform(650, HEIGHT - 350, 120, 18));
            platforms.add(new Platform(800, HEIGHT - 250, 100, 18));
            enemy = new Enemy(300, HEIGHT - 320 - 32, 32, 32, 280, 400, 2.0);
            flag = new Rectangle(800, HEIGHT - 250 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level4 extends LevelBase {
        public Level4() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(100, HEIGHT - 180, 140, 18));
            platforms.add(new Platform(300, HEIGHT - 260, 130, 18));
            platforms.add(new Platform(500, HEIGHT - 340, 120, 18));
            platforms.add(new Platform(700, HEIGHT - 260, 140, 18));
            enemy = new Enemy(500, HEIGHT - 340 - 32, 32, 32, 500, 620, 1.8);
            flag = new Rectangle(700, HEIGHT - 260 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level5 extends LevelBase {
        public Level5() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(150, HEIGHT - 200, 120, 18));
            platforms.add(new Platform(300, HEIGHT - 280, 140, 18));
            platforms.add(new Platform(480, HEIGHT - 220, 130, 18));
            platforms.add(new Platform(650, HEIGHT - 300, 120, 18));
            platforms.add(new Platform(800, HEIGHT - 220, 100, 18));
            enemy = new Enemy(480, HEIGHT - 220 - 32, 32, 32, 480, 610, 2.0);
            flag = new Rectangle(800, HEIGHT - 220 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level6 extends LevelBase {
        public Level6() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(120, HEIGHT - 190, 130, 18));
            platforms.add(new Platform(300, HEIGHT - 300, 140, 18));
            platforms.add(new Platform(500, HEIGHT - 240, 120, 18));
            platforms.add(new Platform(650, HEIGHT - 320, 140, 18));
            platforms.add(new Platform(800, HEIGHT - 240, 100, 18));
            enemy = new Enemy(300, HEIGHT - 300 - 32, 32, 32, 300, 440, 1.6);
            flag = new Rectangle(800, HEIGHT - 240 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level7 extends LevelBase {
        public Level7() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(100, HEIGHT - 200, 120, 18));
            platforms.add(new Platform(260, HEIGHT - 280, 130, 18));
            platforms.add(new Platform(430, HEIGHT - 360, 140, 18));
            platforms.add(new Platform(620, HEIGHT - 280, 130, 18));
            platforms.add(new Platform(800, HEIGHT - 360, 100, 18));
            enemy = new Enemy(430, HEIGHT - 360 - 32, 32, 32, 430, 570, 1.8);
            flag = new Rectangle(800, HEIGHT - 360 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level8 extends LevelBase {
        public Level8() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(120, HEIGHT - 200, 130, 18));
            platforms.add(new Platform(300, HEIGHT - 310, 140, 18));
            platforms.add(new Platform(500, HEIGHT - 250, 130, 18));
            platforms.add(new Platform(680, HEIGHT - 330, 120, 18));
            platforms.add(new Platform(820, HEIGHT - 380, 70, 18));
            enemy = new Enemy(300, HEIGHT - 310 - 32, 32, 32, 300, 440, 1.9);
            flag = new Rectangle(820, HEIGHT - 380 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level9 extends LevelBase {
        public Level9() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(100, HEIGHT - 210, 120, 18));
            platforms.add(new Platform(280, HEIGHT - 290, 130, 18));
            platforms.add(new Platform(450, HEIGHT - 370, 140, 18));
            platforms.add(new Platform(630, HEIGHT - 290, 130, 18));
            platforms.add(new Platform(800, HEIGHT - 370, 100, 18));
            enemy = new Enemy(450, HEIGHT - 370 - 32, 32, 32, 450, 590, 2.0);
            flag = new Rectangle(800, HEIGHT - 370 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 170;
        }
    }

    static class Level10 extends LevelBase {
        public Level10() {
            platforms.add(new Platform(0, HEIGHT - 80, WIDTH, 80));
            platforms.add(new Platform(120, HEIGHT - 200, 130, 18));
            platforms.add(new Platform(300, HEIGHT - 320, 140, 18));
            platforms.add(new Platform(500, HEIGHT - 400, 130, 18));
            platforms.add(new Platform(680, HEIGHT - 450, 120, 18));
            platforms.add(new Platform(820, HEIGHT - 500, 70, 18));
            enemy = new Enemy(300, HEIGHT - 320 - 32, 32, 32, 300, 440, 2.1);
            flag = new Rectangle(820, HEIGHT - 500 - 36, 24, 36);
            playerStartX = 40;
            playerStartY = HEIGHT - 200;
        }
    }
    static class SoundManager {
            public static void playSound(String filename) {
                try {
                    URL url = SoundManager.class.getResource("/" + filename); // <- aquí
                    if (url == null) {
                        System.err.println("Sound not found: " + filename);
                        return;
                    }
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public static Clip playLoop(String filename, float volumeDb) {
                try {
                    URL url = SoundManager.class.getResource("/" + filename); // <- aquí también
                    if (url == null) {
                        System.err.println("Sound not found: " + filename);
                        return null;
                    }
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(volumeDb);
                    clip.start();
                    return clip;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }



}

