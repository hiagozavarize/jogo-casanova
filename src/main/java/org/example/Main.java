package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

public class Main extends JFrame implements Runnable, KeyListener, ActionListener {
    // Thread do jogo
    private Thread gameThread;
    // Arrays para armazenar as imagens do jogador, bombas, explosões e fundos
    private final Image[] playerImages = new Image[4];
    private final Image[] bombImages = new Image[3];
    private final Image[] explosionImages = new Image[4];
    private final Image[] backgroundImages = new Image[2];

    // Variáveis para controle do fundo
    private int currentBackground = 0;
    private long lastBackgroundChangeTime = 0;
    private final int BACKGROUND_CHANGE_INTERVAL = 100; // Intervalo para mudar a imagem de fundo em milissegundos
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
    private final int GAME_TIME = 40000;

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
    private final int SPAWN_INTERVAL = 300;

    // Constantes de tamanho do jogador e da bomba
    private final int PLAYER_SIZE = 70;
    private final int BOMB_SIZE = 50;

    // Imagem e gráficos para renderização offscreen
    private Image offscreenImage;
    private Graphics offscreenGraphics;

    // Botão para jogar novamente
    private JButton playAgainButton;

    public Main() {
        loadImages();

        // Configuração da janela
        setSize(1920, 1080);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        // Configuração do listener do teclado e botão "play again"
        addKeyListener(this);
        playAgainButton = new JButton("Play Again");
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

    // Carregamento das imagens
    private void loadImages() {
        for (int i = 0; i < 4; i++) {
            playerImages[i] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/" + i + ".png").getImage();
        }
        for (int i = 0; i < 3; i++) {
            bombImages[i] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/bomb" + i + ".jpg").getImage();
        }
        for (int i = 0; i < 4; i++) {
            explosionImages[i] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/explosion" + i + ".jpg").getImage();
        }
        backgroundImages[0] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/pista0.jpg").getImage();
        backgroundImages[1] = new ImageIcon("C:/Users/noobs/IdeaProjects/aulaProgramacaoMovel/src/main/java/org/example/pista1.jpg").getImage();
    }

    // Paint método para desenhar objetos e o estado do jogo na tela
    @Override
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
        if (now - lastBackgroundChangeTime >= BACKGROUND_CHANGE_INTERVAL) {
            currentBackground = (currentBackground + 1) % backgroundImages.length;
            lastBackgroundChangeTime = now;
        }
    }

    // Mostra mensagem de fim de jogo
    private void showEndMessage() {
        offscreenGraphics.setFont(new Font("Arial", Font.BOLD, 32));
        offscreenGraphics.setColor(gameOver ? Color.RED : Color.GREEN);
        offscreenGraphics.drawString(gameOver ? "Game Over" : "You Win", getWidth() / 2 - 100, getHeight() / 2);

        playAgainButton.setBounds(getWidth() / 2 - 75, getHeight() / 2 + 50, 150, 50);
        playAgainButton.setVisible(true);
    }

    // Desenha os objetos do jogo na tela
    private void drawGameObjects() {
        offscreenGraphics.drawImage(playerImages[currentFrame], playerX, playerY, PLAYER_SIZE - 20, PLAYER_SIZE, this);
        for (Bomb bomb : bombs) {
            offscreenGraphics.drawImage(bomb.image, bomb.x, bomb.y, BOMB_SIZE, BOMB_SIZE + 20, this);
        }
        offscreenGraphics.setColor(Color.BLACK);
        offscreenGraphics.setFont(new Font("Arial", Font.BOLD, 18));
        offscreenGraphics.drawString("Life: " + lives, 20, 50);
        offscreenGraphics.drawString("Time: " + (GAME_TIME - (System.currentTimeMillis() - startTime)) / 1000, 20, 70);

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
            }
        }
    }

    // Método de execução da thread do jogo
    @Override
    public void run() {
        while (!gameOver && !isWin) {
            try {
                Thread.sleep(50);
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
            if (System.currentTimeMillis() - lastSpawnTime >= SPAWN_INTERVAL) {
                spawnBomb();
                lastSpawnTime = System.currentTimeMillis();
            }

            // Atualiza bombas e checa colisões
            bombs.removeIf(bomb -> updateBomb(bomb));
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
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_SIZE - 20, PLAYER_SIZE + 20);
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