
package config;

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

public class TextureStorage // TODO TextureStorage; Move to a separate package?
{
  private static TextureStorage textureStorage;
  private static BufferedImage missingTexture;
  
  private Map<String, BufferedImage> textures;
  
  private FilenameFilter textureFilter;
  
  
  public TextureStorage()
  {
    textures = new HashMap<String, BufferedImage>();
    
    loadTextures(new File("textures"));
    createFilenameFilter();
  }

  private void loadTextures(File textureFolder)
  {
    if (textureFolder.isDirectory())
    {
      for (File file : textureFolder.listFiles(textureFilter))
      {
        if (file.isDirectory())
        {
          loadTextures(file);
        }
        else
        {
          loadTexture(file);
        }
      }
    }
  }

  private void loadTexture(File file)
  {
    try
    {
      BufferedImage texture = ImageIO.read(file);
      String textureName = getFilenameWithoutExtension(file);
      textures.put(textureName, texture);
    } catch (IOException e)
    {
      System.err.println("TextureStorage: loadTextures(): Failed to load the texture: " + file.getPath());
    }
  }

  private String getFilenameWithoutExtension(File file)
  {
    String fileName = file.getName();
    fileName = fileName.substring(0, fileName.length() - 4);
    return fileName;
  }
  
  private void createFilenameFilter()
  {
    textureFilter = new FilenameFilter() {
      
      @Override
      public boolean accept(File dir, String name)
      {
        File file = new File(dir, name);
        return file.isDirectory() || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png");
      }
    };
  }
  
  
  public static TextureStorage instance()
  {
    if (textureStorage == null)
    {
      textureStorage = new TextureStorage();
      createMissingTexture();
    }
    return textureStorage;
  }

  private static void createMissingTexture()
  {
    missingTexture = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = missingTexture.createGraphics();
    g2d.setColor(Color.MAGENTA);
    g2d.fillRect(0, 0, 64, 64);
    g2d.setColor(Color.BLACK);
    g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 16f));
    g2d.drawString("Missing", 3, 36);
    g2d.dispose();
  }
  
  
  public BufferedImage getTexture(String name)
  {
    BufferedImage texture = textures.get(name);
    if (texture == null)
    {
      texture = missingTexture;
    }
    
    return texture;
  }
}
