package edu.emory.cellbio.svg;

/**
 * Inkscape entry point for embed and crop images tool
 * @author Benjamin Nanes
 */
public class EmbedAndCropInkscapeEntry {

     /**
      * @param args The command line arguments
      */
     public static void main(String[] args) {
          EmbedAndCrop eac = new EmbedAndCrop();
          eac.runInkscapeExtension(args);
     }
}
