package edu.emory.cellbio.svg;

/**
 * @author Benjamin Nanes
 */
public class EmbedAndCropException extends Exception {

     public EmbedAndCropException() {
          super("Plugin error");
     }

     public EmbedAndCropException(String msg) {
          super("Plugin error: " + msg);
     }
}