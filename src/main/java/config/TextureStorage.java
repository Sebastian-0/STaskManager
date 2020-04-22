/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class TextureStorage { // TODO TextureStorage; Move to a separate package?
	private static final Logger LOGGER = LoggerFactory.getLogger(TextureStorage.class);

	private static TextureStorage textureStorage;
	private static BufferedImage missingTexture;
	
	private final Map<String, BufferedImage> textures;

	
	public TextureStorage() {
		textures = new HashMap<>();
		loadTextures(new File("textures"));
	}

	private void loadTextures(File textureFolder) {
		if (textureFolder.isDirectory()) {
			for (File file : textureFolder.listFiles(textureFilter())) {
				if (file.isDirectory()) {
					loadTextures(file);
				} else {
					loadTexture(file);
				}
			}
		}
	}

	private FilenameFilter textureFilter() {
		return (dir, name) -> {
			File file = new File(dir, name);
			return file.isDirectory() || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png");
		};
	}

	private void loadTexture(File file) {
		try {
			BufferedImage texture = ImageIO.read(file);
			String textureName = getFilenameWithoutExtension(file);
			textures.put(textureName, texture);
		} catch (IOException e) {
			LOGGER.error("Failed to load the texture: " + file.getPath(), e);
		}
	}

	private String getFilenameWithoutExtension(File file) {
		return file.getName().substring(0, file.getName().length() - 4);
	}

	public static TextureStorage instance() {
		if (textureStorage == null) {
			textureStorage = new TextureStorage();
			missingTexture = createMissingTexture();
		}
		return textureStorage;
	}

	private static BufferedImage createMissingTexture() {
		BufferedImage missingTexture = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = missingTexture.createGraphics();
		g2d.setColor(Color.MAGENTA);
		g2d.fillRect(0, 0, 64, 64);
		g2d.setColor(Color.BLACK);
		g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 16f));
		g2d.drawString("Missing", 3, 36);
		g2d.dispose();
		return missingTexture;
	}

	public BufferedImage getTexture(String name) {
		BufferedImage texture = textures.get(name);
		if (texture == null) {
			texture = missingTexture;
		}
		
		return texture;
	}
}
