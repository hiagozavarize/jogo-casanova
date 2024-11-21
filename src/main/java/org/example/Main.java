package org.example;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Main extends JFrame implements Runnable, KeyListener, ActionListener {
    // Thread do jogo
    private Thread gameThread;
    // Arrays para armazenar as imagens do jogador, bombas, explosões e fundos
    private final Image[] playerImages = new Image[3];
    private final Image[] bombImages = new Image[3];
    private final Image[] explosionImages = new Image[4];
    private final Image[] backgroundImages = new Image[2];

    //imagem do coração
    private Image heartImage;

    //variavel de controle do som da explosão da bomba
    private boolean isBombExploding = false;

    // Variáveis para controle do fundo
    private int currentBackground = 0;
    private long lastBackgroundChangeTime = 0;
    private final Random random = new Random();

    // Variáveis de posição e velocidade do jogador
    private int playerX = 20, playerY = 400;
    private int playerSpeedX = 0;
    private int playerSpeedY = 0;

    // Controle de frames e vidas
    private int currentFrame = 0;
    private int lives = 3;

    // Tempo de início do jogo e duração do jogo
    private long startTime;
    private final int GAME_TIME = 30000;

    // Controle de estado do jogo (game over, explosão, vitória)
    private boolean gameOver = false;
    private boolean exploding = false;
    private boolean isWin = false;

    // Variáveis para controle da explosão
    private int explosionFrame = 0;
    private long explosionStartTime;
    private int explosionX, explosionY;

    // Lista para armazenar as bombas e controlar o tempo de spawn
    private final ArrayList<Bomb> bombs = new ArrayList<>();
    private long lastSpawnTime = 0;

    // Constantes de tamanho do jogador e da bomba
    private final int PLAYER_SIZE = 70;
    private final int BOMB_SIZE = 50;

    // Imagem e gráficos para renderização offscreen
    private Image offscreenImage;
    private Graphics offscreenGraphics;

    // Botão para jogar novamente
    private final JButton playAgainButton;
    private final Color blueColor = new Color(0, 0, 255);
    private final Color yellowColor = new Color(255, 255, 0);

    private Clip backgroundClip;

    public Main() {
        loadImages();

        backgroundClip = playSound("backgroundSong1", true);

        // Configuração da janela
        setSize(1920, 1080);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        // Configuração do listener do teclado e botão "play again"
        addKeyListener(this);
        playAgainButton = new JButton("Play Again");
        playAgainButton.setBackground(blueColor);
        playAgainButton.setForeground(yellowColor);
        playAgainButton.setFont(new Font("Arial", Font.BOLD, 16));
        playAgainButton.setBorder(BorderFactory.createLineBorder(yellowColor, 2, true));
        playAgainButton.setFocusPainted(false);
        playAgainButton.addActionListener(this);
        playAgainButton.setVisible(false);
        add(playAgainButton);

        // Inicialização da thread do jogo
        gameThread = new Thread(this);
        startTime = System.currentTimeMillis();
        spawnBomb();
        lastSpawnTime = System.currentTimeMillis();
        gameThread.start();
    }

    private Clip playSound(String soundFile, boolean loop) {
        try {
            File file = new File("C:/jogo-casanova/src/main/java/org/example/songs/" + soundFile + ".wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            clip.start();
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Carregamento das imagens
    private void loadImages() {
        for (int i = 0; i < 3; i++) {
            playerImages[i] = new ImageIcon("C:/jogo-casanova/src/main/java/org/example/" + i + ".png").getImage();
        }
        for (int i = 0; i < 3; i++) {
            bombImages[i] = new ImageIcon("C:/jogo-casanova/src/main/java/org/example/bomb" + i + ".jpg").getImage();
        }
        for (int i = 0; i < 4; i++) {
            explosionImages[i] = new ImageIcon("C:/jogo-casanova/src/main/java/org/example/explosion" + i + ".jpg").getImage();
        }
        backgroundImages[0] = new ImageIcon("C:/jogo-casanova/src/main/java/org/example/pista0.jpg").getImage();
        backgroundImages[1] = new ImageIcon("C:/jogo-casanova/src/main/java/org/example/pista1.jpg").getImage();

        heartImage = new ImageIcon("C:/jogo-casanova/src/main/java/org/example/heart.png").getImage();
    }


    @Override
    // Paint método para desenhar objetos e o estado do jogo na tela
    public void paint(Graphics g) {
        if (offscreenImage == null) {
            offscreenImage = createImage(getWidth(), getHeight());
            offscreenGraphics = offscreenImage.getGraphics();
        }

        // Atualiza e desenha o fundo
        updateBackground();
        offscreenGraphics.drawImage(backgroundImages[currentBackground], 0, 0, getWidth(), getHeight(), this);

        if (gameOver || isWin) {
            // Mostra mensagem de fim de jogo
            showEndMessage();
        } else {
            // Desenha os objetos do jogo
            drawGameObjects();
        }

        g.drawImage(offscreenImage, 0, 0, this);
    }

    // Atualiza a imagem de fundo
    private void updateBackground() {
        long now = System.currentTimeMillis();
        // Intervalo para mudar a imagem de fundo em milissegundos
        int BACKGROUND_CHANGE_INTERVAL = 100;
        if (now - lastBackgroundChangeTime >= BACKGROUND_CHANGE_INTERVAL) {
            currentBackground = (currentBackground + 1) % backgroundImages.length;
            lastBackgroundChangeTime = now;
        }
    }

    // Mostra mensagem de fim de jogo
    private void showEndMessage() {
        offscreenGraphics.setFont(new Font("Arial", Font.BOLD, 32));
        offscreenGraphics.setColor(gameOver ? Color.RED : Color.GREEN);

        // Cálculo das posições para centralizar a mensagem e o botão
        int messageWidth = offscreenGraphics.getFontMetrics().stringWidth(gameOver ? "Game Over" : "You Win");
        int messageX = (getWidth() - messageWidth) / 2;
        int messageY = getHeight() / 2 - 50;

        offscreenGraphics.drawString(gameOver ? "Game Over" : "You Win", messageX, messageY);

        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
        }

        playAgainButton.setBounds(getWidth() / 2 - 75, getHeight() / 2, 150, 50);
        playAgainButton.setVisible(true);
    }

    // Desenha os objetos do jogo na tela
    private void drawGameObjects() {
        offscreenGraphics.drawImage(playerImages[currentFrame], playerX, playerY, PLAYER_SIZE - 20, PLAYER_SIZE, this);
        for (Bomb bomb : bombs) {
            offscreenGraphics.drawImage(bomb.image, bomb.x, bomb.y, BOMB_SIZE, BOMB_SIZE + 20, this);
        }

        // Desenha as vidas como corações
        int heartX = 60; // Posição inicial do primeiro coração no eixo X
        int heartY = 30; // Posição no eixo Y
        int heartSpacing = 30; // Espaçamento entre os corações
        for (int i = 0; i < lives; i++) {
            offscreenGraphics.drawImage(heartImage, heartX + i * heartSpacing, heartY, 30, 30, this);
        }

        offscreenGraphics.setColor(Color.BLACK);
        offscreenGraphics.setFont(new Font("Arial", Font.BOLD, 18));
        offscreenGraphics.drawString("Life: ", 20, 50);
        offscreenGraphics.drawString("Time: " + (GAME_TIME - (System.currentTimeMillis() - startTime)) / 1000, 20, 75);

        if (exploding) handleExplosion();
    }

    // Lida com a animação da explosão
    private void handleExplosion() {
        offscreenGraphics.drawImage(explosionImages[explosionFrame], explosionX, explosionY, BOMB_SIZE + 50, BOMB_SIZE + 50, this);
        if (System.currentTimeMillis() - explosionStartTime > 100) {
            explosionFrame++;
            explosionStartTime = System.currentTimeMillis();
            if (explosionFrame >= explosionImages.length) {
                exploding = false;
                explosionFrame = 0;
                isBombExploding = false;
            }
        }
        if (!isBombExploding) {
            playSound("explosionSoundEffect", false);
            isBombExploding = true; // Evita tocar novamente durante a mesma explosão
        }

    }
    // Método de execução da thread do jogo
    @Override
    public void run() {
        while (!gameOver && !isWin) {
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // Atualizar posição do jogador
            playerX += playerSpeedX;
            playerY += playerSpeedY;

            // Verificar limites para impedir que o jogador saia da tela
            if (playerX < 0) playerX = 0;
            if (playerY < 0) playerY = 0;
            if (playerX > getWidth() - PLAYER_SIZE) playerX = getWidth() - PLAYER_SIZE;
            if (playerY > getHeight() - PLAYER_SIZE) playerY = getHeight() - PLAYER_SIZE;

            // Controla o tempo de spawn das bombas
            int SPAWN_INTERVAL = 300;
            if (System.currentTimeMillis() - lastSpawnTime >= SPAWN_INTERVAL) {
                spawnBomb();
                lastSpawnTime = System.currentTimeMillis();
            }

            // Atualiza bombas e checa colisões
            bombs.removeIf(this::updateBomb);
            currentFrame = (currentFrame + 1) % playerImages.length;

            // Checa se o tempo do jogo acabou
            if (System.currentTimeMillis() - startTime >= GAME_TIME) isWin = true;
            repaint();
        }
    }

    // Atualiza a posição das bombas e checa colisões
    private boolean updateBomb(Bomb bomb) {
        bomb.move();
        if (!exploding && checkBombCollision(bomb)) {
            lives--;
            startExplosion(bomb);
            if (lives <= 0) gameOver = true;
            return true;
        }
        return false;
    }

    // Spawna uma nova bomba
    private void spawnBomb() {
        int bombX = getWidth() - 50;
        int bombY = random.nextInt(Math.max(getHeight() - 50, 1));
        int speed = 50 + random.nextInt(10);
        Image image = bombImages[random.nextInt(bombImages.length)];
        bombs.add(new Bomb(bombX, bombY, speed, image));
    }

    // Verifica se há colisão entre o jogador e uma bomba
    private boolean checkBombCollision(Bomb bomb) {
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_SIZE - 20, PLAYER_SIZE - 10);
        Rectangle bombRect = new Rectangle(bomb.x + 10, bomb.y + 10, BOMB_SIZE - 20, BOMB_SIZE - 20);
        return playerRect.intersects(bombRect);
    }

    // Inicia a animação de explosão
    private void startExplosion(Bomb bomb) {
        exploding = true;
        explosionX = bomb.x;
        explosionY = bomb.y;
        explosionFrame = 0;
        explosionStartTime = System.currentTimeMillis();
    }

    // Reseta o jogo para o estado inicial
    private void resetGame() {
        playerX = 20;
        playerY = 400;
        playerSpeedX = 0;
        playerSpeedY = 0;
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
        backgroundClip = playSound("backgroundSong1", true);
        requestFocus();
    }

    // Trata eventos de ação como o botão "play again"
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playAgainButton) {
            resetGame();
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    // Configuração inicial
    public static void main(String[] args) {
        new Main();
    }

    // Métodos do KeyListener
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent ke) {
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_W -> playerSpeedY = -20;
            case KeyEvent.VK_S -> playerSpeedY = 20;
            case KeyEvent.VK_D -> playerSpeedX = 20;
            case KeyEvent.VK_A -> playerSpeedX = -20;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_S -> playerSpeedY = 0;
            case KeyEvent.VK_D, KeyEvent.VK_A -> playerSpeedX = 0;
        }
    }

    // Classe interna para representar Bombas
    static class Bomb {
        int x, y, speed;
        Image image;

        public Bomb(int x, int y, int speed, Image image) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.image = image;
        }

        // Move a bomba para a esquerda
        public void move() {
            x -= speed;
        }
    }
}