package org.example;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class SoundManager {

    public static void playSound(String filename) {
        try {
            URL url = SoundManager.class.getResource("/sounds/" + filename);
            if (url == null) {
                System.err.println("Sound file not found: " + filename);
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    // Looping background music
    public static Clip playLoop(String filename) {
        try {
            URL url = SoundManager.class.getResource("/sounds/" + filename);
            if (url == null) {
                System.err.println("Sound file not found: " + filename);
                return null;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

