package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Main extends JFrame implements Runnable, KeyListener, ActionListener {
    Thread gameThread;
    Image[] playerImages = new Image[4];
    Image[] bombImages = new Image[3];
    Image[] explosionImages = new Image[4];
    Image[] backgroundImages = new Image[2]; // Imagens de fundo
    int currentBackground = 0;
    long lastBackgroundChangeTime = 0;
    final int BACKGROUND_CHANGE_INTERVAL = 100; // Intervalo para mudar a imagem de fundo em milissegundos
    Random random = new Random();

    int playerX = 20, playerY = 400, dx = 5, dy = 5;
    int currentFrame = 0;
    int lives = 3;
    long startTime;
    final int GAME_TIME = 40000;
    boolean gameOver = false;
    boolean exploding = false;
    boolean isWin = false;
    int explosionFrame = 0;
    long explosionStartTime;
    int explosionX, explosionY;
    final ArrayList<Bomb> bombs = new ArrayList<>();
    long lastSpawnTime = 0;
    final int SPAWN_INTERVAL = 300; // 1 segundo
    int PLAYER_SIZE = 70; // Tamanho do jogador
    int BOMB_SIZE = 50; // Tamanho das bombas

    Image offscreenImage;
    Graphics offscreenGraphics;
    JButton playAgainButton;

    public Main() {
        for (int i = 0; i < 4; i++) {
            playerImages[i] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/" + i + ".png").getImage();
        }

        for (int i = 0; i < 3; i++) {
            bombImages[i] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/bomb" + i + ".jpg").getImage();
        }

        for (int i = 0; i < 4; i++) {
            explosionImages[i] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/explosion" + i + ".jpg").getImage();
        }

        // Carregar imagens de fundo
        backgroundImages[0] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/pista0.png").getImage();
        backgroundImages[1] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/pista1.png").getImage();

        setSize(1920, 1080);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null); // Permite posicionamento manual dos componentes

        addKeyListener(this);

        playAgainButton = new JButton("Play Again");
        playAgainButton.addActionListener(this);
        playAgainButton.setVisible(false);
        add(playAgainButton);

        gameThread = new Thread(this);
        startTime = System.currentTimeMillis();

        spawnBomb();
        lastSpawnTime = System.currentTimeMillis();

        gameThread.start();
    }

    @Override
    public void paint(Graphics g) {
        if (offscreenImage == null) {
            offscreenImage = createImage(getWidth(), getHeight());
            offscreenGraphics = offscreenImage.getGraphics();
        }

        // Alternar a imagem de fundo
        long now = System.currentTimeMillis();
        if (now - lastBackgroundChangeTime >= BACKGROUND_CHANGE_INTERVAL) {
            currentBackground = (currentBackground + 1) % backgroundImages.length;
            lastBackgroundChangeTime = now;
        }

        offscreenGraphics.drawImage(backgroundImages[currentBackground], 0, 0, getWidth(), getHeight(), this);

        if (gameOver) {
            offscreenGraphics.setFont(new Font("Arial", Font.BOLD, 32));
            offscreenGraphics.setColor(Color.RED);
            offscreenGraphics.drawString("Game Over", getWidth() / 2 - 100, getHeight() / 2);

            int buttonWidth = 150;
            int buttonHeight = 50;
            playAgainButton.setBounds(getWidth() / 2 - buttonWidth / 2, getHeight() / 2 + 50, buttonWidth, buttonHeight);
            playAgainButton.setVisible(true);
        } else if (isWin) {
            offscreenGraphics.setFont(new Font("Arial", Font.BOLD, 32));
            offscreenGraphics.setColor(Color.GREEN);
            offscreenGraphics.drawString("You Win", getWidth() / 2 - 60, getHeight() / 2);

            int buttonWidth = 150;
            int buttonHeight = 50;
            playAgainButton.setBounds(getWidth() / 2 - buttonWidth / 2, getHeight() / 2 + 50, buttonWidth, buttonHeight);
            playAgainButton.setVisible(true);
        } else {
            offscreenGraphics.drawImage(playerImages[currentFrame], playerX, playerY, PLAYER_SIZE - 20, PLAYER_SIZE, this);

            for (Bomb bomb : bombs) {
                offscreenGraphics.drawImage(bomb.image, bomb.x, bomb.y, BOMB_SIZE, BOMB_SIZE + 20, this);
            }

            offscreenGraphics.setColor(Color.BLACK);
            offscreenGraphics.setFont(new Font("Arial", Font.BOLD, 18));
            offscreenGraphics.drawString("Life: " + lives, 20, 50);

            long elapsed = System.currentTimeMillis() - startTime;
            offscreenGraphics.drawString("Time: " + (GAME_TIME - elapsed) / 1000, 20, 70);

            if (exploding) {
                offscreenGraphics.drawImage(explosionImages[explosionFrame], explosionX, explosionY, BOMB_SIZE, BOMB_SIZE, this);
                long explosionElapsed = System.currentTimeMillis() - explosionStartTime;
                if (explosionElapsed > 100) {
                    explosionFrame++;
                    explosionStartTime = System.currentTimeMillis();
                    if (explosionFrame >= explosionImages.length) {
                        exploding = false;
                        explosionFrame = 0;
                    }
                }
            }
        }

        // Copiar a imagem offscreen para a tela
        g.drawImage(offscreenImage, 0, 0, this);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && !gameOver && !isWin) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastSpawnTime >= SPAWN_INTERVAL) {
                spawnBomb();
                lastSpawnTime = now;
            }

            Iterator<Bomb> iterator = bombs.iterator();
            while (iterator.hasNext()) {
                Bomb bomb = iterator.next();
                bomb.move();
                if (!exploding && checkBombCollision(bomb)) {
                    lives--;
                    startExplosion(bomb);
                    iterator.remove();
                    if (lives <= 0) {
                        gameOver = true;
                        break;
                    }
                }
            }

            currentFrame = (currentFrame + 1) % playerImages.length;

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= GAME_TIME) {
                isWin = true;
            }

            repaint();
        }
    }

    private void spawnBomb() {
        int bombX = getWidth() - 50;
        int maxHeight = Math.max(getHeight() - 50, 1); // Garante que o valor seja no mínimo 1
        int bombY = random.nextInt(maxHeight);
        int speed = 50 + random.nextInt(10);
        Image image = bombImages[random.nextInt(bombImages.length)];
        bombs.add(new Bomb(bombX, bombY, speed, image));
    }

    private boolean checkBombCollision(Bomb bomb) {
        final int HITBOX_REDUCTION = 20; // Redução da hitbox das bombas

        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_SIZE - 20, PLAYER_SIZE + 20);
        Rectangle bombRect = new Rectangle(bomb.x + HITBOX_REDUCTION / 2, bomb.y + HITBOX_REDUCTION / 2,
                BOMB_SIZE - HITBOX_REDUCTION, BOMB_SIZE - HITBOX_REDUCTION);
        return playerRect.intersects(bombRect);
    }

    private void startExplosion(Bomb bomb) {
        exploding = true;
        explosionX = bomb.x;
        explosionY = bomb.y;
        explosionFrame = 0;
        explosionStartTime = System.currentTimeMillis();
    }

    private void resetGame() {
        playerX = 20;
        playerY = 400;
        currentFrame = 0;
        lives = 3;
        startTime = System.currentTimeMillis();
        gameOver = false;
        isWin = false;
        bombs.clear();
        exploding = false;
        spawnBomb();
        lastSpawnTime = System.currentTimeMillis();
        playAgainButton.setVisible(false);
        requestFocus();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playAgainButton) {
            resetGame();
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public static void main(String[] args) {
        new Main();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_UP) playerY -= 30;
        if (ke.getKeyCode() == KeyEvent.VK_DOWN) playerY += 30;
        if (ke.getKeyCode() == KeyEvent.VK_RIGHT) playerX += 30;
        if (ke.getKeyCode() == KeyEvent.VK_LEFT) playerX -= 30;
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    static class Bomb {
        int x, y, speed;
        Image image;

        public Bomb(int x, int y, int speed, Image image) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.image = image;
        }

        public void move() {
            x -= speed;
        }
    }
}