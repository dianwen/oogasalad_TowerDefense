package main.java.player;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;


public class Sound {
	private Clip clip;
	public Sound(String fileName) throws LineUnavailableException, IOException, UnsupportedAudioFileException {        
		File file = new File(fileName);
		if (file.exists()) {
			AudioInputStream sound = AudioSystem.getAudioInputStream(file);
			clip = AudioSystem.getClip();
			clip.open(sound);
		}     
	}

	public void loop(){
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}
	public void stop(){
		clip.stop();
	}
}