package edu.emory.cellbio.svg;

import ij.IJ;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import java.awt.image.BufferedImage;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.codec.binary.Base64OutputStream;

/**
 * Inkscape extension: Embed and Crop Images
 * 
 * <p> This java-based extension for Inkscape facilitates 
 * image embedding by: 
 * <ul>
 * <li> Automatically identifying all linked images.
 * <li> Cropping image data that lies outside the images'
 * clipping frame.
 * <li> Optionally applying jpeg compression.
 * <li> Optionally resampling images.
 * <li> Writing the cropped and possibly compressed image
 * data directly in the SVG file.
 * </ul>
 * <p> By cropping image data that lies outside the clipping frame, 
 * applying jpeg compression, or resampling to lower resolution,
 * the resulting file size can be reduced significantly.
 * If preserving image quality is a priority 
 * jpeg compression and resampling can be explicitly avoided.
 * 
 * @author Benjamin Nanes
 */
public class EmbedAndCrop 
{
     
     // -- Fields --
     
     private String imgFileType = "png";
     private float compQual = 0.8f;
     private boolean doResampling = false;
     private double maxRes = 11.811; // px/mm (default is ~300dpi)
     
     // -- Methods --
     
     /**
      * Run the extension
      * @param args <b>Command line arguments</b>
      *   <br>
      *   <code> [&lt;<em>input</em>&gt;] 
      *          [-o &lt;<em>output</em>&gt; | -s] 
      *          [-t &lt;<em>type</em>&gt; [-q &lt;<em>quality</em>&gt;]]
      *          [-r [&lt;<em>resolution</em>&gt;]]
      *   </code>
      *   <ul>
      *   <li>  <code>&lt;<em>input</em>&gt; </code>
      *         Path to the input SVG file.
      *         If missing, the user will be presented with
      *         a file open dialog box.
      *   <li>  <code>-o &lt;<em>output</em>&gt; </code>
      *         Path to save the output
      *         SVG file with embedded images
      *   <li>  <code>-s </code>
      *         Present the user with a file save dialog
      *         to specify the output file
      *   <br>  <em>Note:</em> If neither <code>-o</code> nor <code>-s</code>
      *         is specified, the output is sent to the
      *         standard output stream
      *   <li>  <code>-t &lt;<em>type</em>&gt; </code>
      *         Specify the type of image for encoding.
      *         Supported options are <code>png</code> or <code>jpeg</code>.
      *         If this is not specified, the user will
      *         be presented with a selection dialog.
      *   <li>  <code>-q &lt;<em>quality</em>&gt; </code>
      *         Quality parameter for jpeg compression.
      *         Default value is <code>0.85</code>.
      *   <li>  <code>-r &lt;<em>resolution</em>&gt; </code>
      *         Maximum image resolution, in pixels per mm.
      *         Higher resolution images will be downsampled.
      *         If this flag is not given, no resampling will be done.
      *         If this flag is given, but no resolution is provided,
      *         the default value is <code>11.811</code>, approximately equal to 300dpi.
      *   </ul>
      *   Examples:
      *   <br> <code> input.svg -s -t jpeg -q 0.95 </code>
      *   <br> <code> input.svg -o output.svg </code>
      */
     public void runInkscapeExtension(String[] args) {
          File input = null;
          File output = null;
          boolean saveAs = false;
          try {
               parseArgs(args);
          } catch(Throwable t) {
               t.printStackTrace();
               if(t.getMessage().contains("Canceled by user")) {
                   System.exit(0);
               }
               JOptionPane.showMessageDialog(null, t.getMessage(), "Error: " + t.getMessage(), JOptionPane.ERROR_MESSAGE);
               System.exit(1);
          }
          System.exit(0);
     }
     
     /**
      * Run the extension, but do not exit the JVM when finished.
      * This method allows an alternate entry point to allow
      * use of the extension programmatically, rather than from
      * Inkscape or the command line.
      * 
      * @see #runInkscapeExtension(java.lang.String[]) 
      */
     public boolean runWithoutExit(String[] args) {
          File input = null;
          File output = null;
          boolean saveAs = false;
          try {
               parseArgs(args);
          } catch(Throwable t) {
               t.printStackTrace();
               JOptionPane.showMessageDialog(null, t.getMessage(), "Error: " + t.getMessage(), JOptionPane.ERROR_MESSAGE);
               return false;
          }
          return true;
     }
     
     // -- Helper methods --
     
     private void parseArgs(String[] args) {
          File input = null;
          File output = null;
          boolean saveAs = false;
          try {
               boolean typeLoaded = false;
               if(args != null) {
                    for(int i=0; i<args.length; i++) {
                         final String token = args[i].trim();
                         final String next = args.length > i+1 ? args[i+1] : null;
                         if(token == null || token.isEmpty())
                              continue;
                         if(token.equals("-o") && next != null) {
                              output = new File(next);
                              i++;
                         }
                         else if(token.equals("-s"))
                              saveAs = true;
                         else if(token.equals("-t") && next != null) {
                              imgFileType = next;
                              typeLoaded = true;
                              i++;
                         }
                         else if(token.equals("-q") && next != null) {
                              if(!typeLoaded)
                                   throw new EmbedAndCropException
                                        ("Can't set quality without image type");
                              compQual = Float.parseFloat(next);
                              i++;
                         } 
                         else if(token.equals("-r")) {
                             if(next != null)
                                maxRes = Double.parseDouble(next);
                             doResampling = true;
                             i++;
                         }
                         else if(i == 0)
                              input = new File(token);
                    }
               }
               if(input == null)
                    input = openDialog();
               if(!typeLoaded)
                    getOutputParams();
               if(input == null || !input.canRead())
                    throw new EmbedAndCropException("Can't read temporary input file "
                         + input != null ? input.getPath() : "<null>");
               Document dom = readSVG(input);
               process(dom, input.getParent());
               if(saveAs)
                    saveAs(dom);
               else if(output != null)
                    save(dom, output);
               else
                    SVGToStream(dom, System.out);
          } catch(Exception e) {
              throw new RuntimeException(e);
          }
     }
     
     /** Process an SVG DOM */
     private void process(Document dom, String basePath) throws EmbedAndCropException {
          NodeList images = dom.getElementsByTagName("image");
          for(int i=0; i<images.getLength(); i++) {
               Node img = images.item(i);
               if(img.getNodeType() == Node.ELEMENT_NODE) {
                    Element clip = getClipPath((Element)img, dom);
                    if(clip != null) {
                         processImg((Element)img, clip, basePath);
                    }
                    else
                         processImg((Element)img, null, basePath);
               }
          }
     }
     
     /** Push XML(SVG) to a stream */
     private void SVGToStream(Document dom, OutputStream os) throws EmbedAndCropException {
          try{
               Transformer xmlt = TransformerFactory.newInstance().newTransformer();
               xmlt.setOutputProperty(OutputKeys.METHOD, "xml");
               xmlt.transform(new DOMSource(dom), new StreamResult(os));
               os.close();
          } catch(Throwable t) { throw new EmbedAndCropException("XML write error: " + t); }
     }
     
     /** Harvest output parameters from dialog */
     private void getOutputParams() throws EmbedAndCropException {
          OutputParamDialog opd = new OutputParamDialog();
          opd.showAndWait();
          if(!opd.wasOKd())
               throw new EmbedAndCropException("Canceled by user");
          imgFileType = opd.getImgFileMode();
          compQual = opd.getCompressionQuality();
          doResampling = opd.getDoResample();
          maxRes = opd.getResampleLevel();
     }
     
     /** Save an XML(SVG) file */
     private void saveAs(Document dom) throws EmbedAndCropException {
          JFileChooser fd = new JFileChooser();
          if(fd.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
               return;
          File f = fd.getSelectedFile();
          if(f.exists() &&
               JOptionPane.showConfirmDialog(null,
               "File exists. OK to overwite?", "",
               JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
               return;
          save(dom, f);
     }
     
     /** Save an XML(SVG) file */
     private void save(Document dom, File f) throws EmbedAndCropException {
          try{
               f.getParentFile().mkdirs();
               Transformer xmlt = TransformerFactory.newInstance().newTransformer();
               xmlt.setOutputProperty(OutputKeys.METHOD, "xml");
               xmlt.transform(new DOMSource(dom), new StreamResult(f));
          } catch(Throwable t) { throw new EmbedAndCropException("XML write error: " + t); }
     }
     
     /** Process an image element */
     private void processImg(Element img, Element clip, String basePath) throws EmbedAndCropException {
          double[] cf = {0,0,0,0};
          if(clip != null)
               cf = getCropFraction(img, clip);
          putImgData(img, cf, basePath);
     }
     
     /**
      * Load image data to embed
      * @param img Image element
      * @param crop Fraction of image to crop from each edge, {@code {top, bottom, left, right}}
      */
     private void putImgData(Element img, double[] crop, String basePath) throws EmbedAndCropException {
          String path = img.getAttribute("xlink:href");
          if(path == null || path.equals(""))
               throw new EmbedAndCropException("No image file listed!");
          if(path.startsWith("data:image")) {
              return; // Skip already embeded images
          }
          if(path.startsWith("file:///")) {
               if(System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH).indexOf("win") > 0)
                   path = path.substring(8);
               else
                   path = path.substring(7);
          }
          path = path.replace("%20", " ");
          path = path.replace("%5C", "\\");
          File imf = new File(path);
          if(!imf.isAbsolute())
              imf = new File(basePath, path);
          if(!imf.canRead())
               throw new EmbedAndCropException("Can't read file link: " + path);
          BufferedImage origImg;
          try{
            origImg = ImageIO.read(imf);
            if(origImg == null) {
                System.err.println("Unable to open " + imf.getName() + " with ImageIO, falling back to ImageJ.");
                origImg = IJ.openImage(imf.getAbsolutePath()).getBufferedImage();
            }
          }
          catch(Throwable t) { throw new EmbedAndCropException("Problem reading image file; " + t); }
          if(origImg == null)
               throw new EmbedAndCropException("Couldn't load appropriate plugin for image");
          int w = origImg.getWidth();
          int h = origImg.getHeight();
          int[] icrop = { (int)Math.floor(crop[0]*h), (int)Math.floor(crop[1]*h),
                          (int)Math.floor(crop[2]*w), (int)Math.floor(crop[3]*w) };
          for(int i=0; i<4; i++)
               icrop[i] = Math.max(icrop[i], 0); // Don't crop on outside
          BufferedImage cropImg = origImg.getSubimage(
               icrop[2], icrop[0],
               w - icrop[2] - icrop[3], h - icrop[0] - icrop[1]);
          double[] acrop = { ((double)icrop[0])/h, ((double)icrop[1])/h,
                             ((double)icrop[2])/w, ((double)icrop[3])/w };
          adjustImgPlacement(img, acrop);
          if(doResampling) {
                double croppedWidth = Double.parseDouble(img.getAttribute("width"));
                cropImg = limitResolution(cropImg, croppedWidth, maxRes);
          }
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          Base64OutputStream out64 = new Base64OutputStream(baos);
          try{
               Iterator<ImageWriter> iws = ImageIO.getImageWritersByFormatName(imgFileType);
               if(!iws.hasNext())
                    throw new EmbedAndCropException
                         ("Can't find image writer for " + imgFileType);
               ImageWriter iw = iws.next();
               ImageWriteParam iwp = iw.getDefaultWriteParam();
               if(iwp.canWriteCompressed()) {
                    iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    iwp.setCompressionQuality(compQual);
               }
               ImageOutputStream ios = ImageIO.createImageOutputStream(out64);
               iw.setOutput(ios);
               iw.write(null, new IIOImage(cropImg, null, null), iwp);
               ios.close();
               out64.close();
          }
          catch(Throwable t) { throw new EmbedAndCropException("Problem writing/encoding image data; " + t); }
          
          String result = "data:image/" + imgFileType + ";base64," + baos.toString();
          img.setAttribute("xlink:href", result);
     }
     
     /**
      * Adjust placement of the image element to account for cropping
      * 
      * @param img The image element
      * @param crop Fraction of image <em>actually</em> cropped from
      *             each edge, {@code {top, bottom, left, right}};
      *             note that this must account for rounding to pixels
      */
     private void adjustImgPlacement(Element img, double[] crop) {
          double w = new Double(img.getAttribute("width"));
          double h = new Double(img.getAttribute("height"));
          double nx = new Double(img.getAttribute("x")) + crop[2] * w;
          double ny = new Double(img.getAttribute("y")) + crop[0] * h;
          double nw = w * (1 - crop[2] - crop[3]);
          double nh = h * (1 - crop[0] - crop[1]);
          img.setAttribute("x", String.valueOf(nx));
          img.setAttribute("y", String.valueOf(ny));
          img.setAttribute("width", String.valueOf(nw));
          img.setAttribute("height", String.valueOf(nh));
     }
     
     /**
      * Down-sample an image if above a maximum resolution
      * 
      * @param I Source image
      * @param w Image width (physical units)
      * @param r Limiting resolution (pixels per physical unit)
      * @return A resampled image with the lowest possible resolution 
      *     not less than r, or the source image unchanged if the
      *     source image resolution is less than or equal to r.
      */
     private BufferedImage limitResolution(BufferedImage I, double w, double r) {
         if(I.getWidth() / w > r) {
             double s = Math.ceil(r * w) / I.getWidth();
             AffineTransformOp ato = new AffineTransformOp(
                     AffineTransform.getScaleInstance(s, s), AffineTransformOp.TYPE_BICUBIC);
             BufferedImage J = ato.createCompatibleDestImage(I, I.getColorModel());
             ato.filter(I, J);
             return J;
         } else {
             return I;
         }
     }
     
     /**
      * Get the fraction of image that should be cropped off each side
      * @param img The image element
      * @param clip The clip-path element
      * @return Fraction of image to crop from each edge, {@code {top, bottom, left, right}}
      */
     private double[] getCropFraction(Element img, Element clip) throws EmbedAndCropException {
          double[] imgBounds = getRectBounds(img);
          System.err.println("Image bounds: (" + imgBounds[0] + "," + imgBounds[2] + "); (" + imgBounds[1] + "," + imgBounds[3] + ")");
          double[][] clipPoints = getClipPoints(clip);
          double[] cf = {1,1,1,1};
          if(clipPoints == null || clipPoints.length == 0)
               for(int i=0; i<4; i++)
                    cf[i] = 0;
          for(int i=0; i<clipPoints.length; i++) {
               double[] pf = scaleToRectFraction(
                    distanceFromRect(clipPoints[i], imgBounds), imgBounds);
               for(int j=0; j<4; j++)
                         cf[j] = Math.min(cf[j], pf[j]);
          }
          System.err.println("Top clip, " + cf[0] + "; Bottom clip, " + cf[1] + "; Left clip, " + cf[2] + "; Right clip, " + cf[3]);
          return cf;
     }
     
     /**
      * Is a point within a rectangle (edges excluded)?
      * 
      * @param p {@code {x,y}}
      * @param r Bounding points of the rectangle, {@code {x0, x1, y0, y1}}
      */
     private boolean isPointInRect(double[] p, double[] r) {
          return ((p[0] < r[0] && p[0] > r[1]) || (p[0] < r[1] && p[0] > r[0]))
               && ((p[1] < r[2] && p[1] > r[3]) || (p[1] < r[3] && p[1] > r[2]));
     }
     
     /**
      * How far is a point from each edge of a rectangle?
      * 
      * @param p {@code {x,y}}
      * @param r Bounding points of the rectangle, {@code {x0, x1, y0, y1}}
      * @return Distance from each edge (negative if point is on exterior side of the edge),
      *         {@code {top, bottom, left, right}}
      */
     private double[] distanceFromRect(double[] p, double[] r) {
          double top = Math.min(r[2], r[3]);
          double bot = Math.max(r[2], r[3]);
          double rit = Math.max(r[0], r[1]);
          double lef = Math.min(r[0], r[1]);
          double[] result =
               { p[1] - top, bot - p[1],
                 p[0] - lef, rit - p[0] };
          return result;
     }
     
     /**
      * Scale a set of distances from rectangle edges by the width and height of the rectangle.
      * 
      * @param d Absolute distances, {@code {top, bottom, left, right}}
      * @param r Bounding points of the rectangle, {@code {x0, x1, y0, y1}}
      * @return Relative distances, {@code {top/height, bottom/height, left/width, right/width}}
      */
     private double[] scaleToRectFraction(double[] d, double[] r) {
          d[0] = d[0] / Math.abs(r[2] - r[3]);
          d[1] = d[1] / Math.abs(r[2] - r[3]);
          d[2] = d[2] / Math.abs(r[0] - r[1]);
          d[3] = d[3] / Math.abs(r[0] - r[1]);
          return d;
     }
     
     /**
      * Get the minimum distance between a pont and a line defined by two other points
      * 
      * @param lp0 1st point on the line, {@code {x,y}}
      * @param lp1 2nd point on the line, {@code {x,y}}
      * @param x How far is <em>this</em> point from the line, {@code {x,y}}
      * @return Distance of point {@code x} from the line
      */
     private double distanceFromLine(double[] lp0, double[] lp1, double[] x) {
          double[] lpd = { lp1[0] - lp0[0], lp1[1] - lp0[1] };
          double tMin = ( x[0]*lpd[0] + x[1]*lpd[1] - lp0[0]*lpd[0] - lp0[1]*lpd[1])
               / (Math.pow(lpd[0], 2) + Math.pow(lpd[1], 2));
          System.err.println("Min t: " + tMin);
          double d2 = Math.pow(lp0[0] + tMin*(lp1[0] - lp0[0]) - x[0], 2)
               + Math.pow(lp0[1] + tMin*(lp1[1] - lp0[1]) - x[1], 2);
          System.err.println("r^2 = " + d2);
          return Math.sqrt(d2);
     }
     
     /**
      * Extract the coordinates of a clip path
      * @return {@code double[][] coordinates[point]{x,y}}
      */
     private double[][] getClipPoints(Element clip) throws EmbedAndCropException {
          ArrayList<double[][]> pA = new ArrayList<double[][]>();
          NodeList children = clip.getChildNodes();
          for(int i=0; i<children.getLength(); i++) {
               if(children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element)children.item(i);
                    String baseTransform = child.getAttribute("transform");
                    double[][] p = null;
                    if(child.getNodeName().equals("rect"))
                         p = rectBoundsToPointList(getRectBounds(child));
                    else
                         throw new EmbedAndCropException(
                              "Can't get points from element type " + child.getNodeName()); /////
                    if(p != null) {
                         if(baseTransform != null && !baseTransform.equals("")) {
                              for(int j=0; j<4; j++)
                                   p[j] = parseTransform(p[j], baseTransform);
                         }
                         pA.add(p);
                    }
               }
          }
          
          int n = 0;
          for(int i=0; i<pA.size(); i++)
               n += pA.get(i).length;
          double[][] points = new double[n][2];
          n = 0;
          for(int i=0; i<pA.size(); i++) {
               double[][] p = pA.get(i);
               System.arraycopy(p, 0, points, 0, p.length);
               n += p.length;
          }
          return points;
     }
     
     /**
      * Get the points along a path  ////Not working
      * @return {@code double[point][{x,y}]}
      */
     private double[][] getPathPointList(Element path) throws EmbedAndCropException {
          if(path == null)
               throw new EmbedAndCropException("Null path");
          String d = path.getAttribute("d");
          if(d == null || d.equals(""))
               throw new EmbedAndCropException("Empty path");
          String[] dd = d.split("[^\\d\\.]");
          int n = 0;
          for(int i=0; i<dd.length; i++)
               if(!dd[i].equals("")) n++;
          
          double[][] p = new double[n/2][2];
          
          return p;
     }
     
     /**
      * Get the boundaries of a rect element (or similar) without transformation
      * 
      * @return An array: {@code {x0, x1, y0, y1}}
      */
     private double[] getRectBounds(Element e) {
          double[] p0 = {new Double(e.getAttribute("x")), new Double(e.getAttribute("y"))};
          double[] p1 = {p0[0] + new Double(e.getAttribute("width")),
               p0[1] + new Double(e.getAttribute("height"))};
          double[] r = {p0[0], p1[0], p0[1], p1[1]};
          return r;
     }
     
     /**
      * Convert rect boundary points to a list of corner points
      * @param b {@code {x0, x1, y0, y1}}, as returned by {@link #getRectBounds}
      * @return {@code double[point][{x,y}]}
      */
     private double[][] rectBoundsToPointList(double[] b) {
          double[][] p = new double[4][2];
          p[0][0] = b[0]; p[0][1] = b[2];
          p[1][0] = b[0]; p[1][1] = b[3];
          p[2][0] = b[1]; p[2][1] = b[2];
          p[3][0] = b[1]; p[3][1] = b[3];
          return p;
     }
     
     /**
      * Transform a point ({@code {x,y}}) to document space
      * by recursively looking for transform attributes in
      * the given nodes and all parent nodes.
      * @param point
      * @param n
      * @return {@code {x,y}}
      */
     private double[] transformToDocumentSpace(double[] point, Node n) {
          if(n.getNodeType() == Node.ELEMENT_NODE) {
               Element e = (Element)n;
               String transform = e.getAttribute("transform");
               if(transform != null && !transform.equals(""))
                    point = parseTransform(point, transform);
          }
          Node parent = n.getParentNode();
               if(parent != null)
                    point = transformToDocumentSpace(point, parent);
          return point;
     }
     
     /**
      * Transform a coordinate pair using a transform attribute
      * @param point {@code {x,y}}
      * @param transform The transform atribute value
      * @return {@code {x,y}}
      */
     private double[] parseTransform(double[] point, String transform) {
          System.err.println("Transforming (" + point[0] + "," + point[1] + ") using attribute: " + transform);
          if(transform == null || transform.equals(""))
               return point;
          transform = transform.trim();
          String[] tList = transform.split("\\s");
          for(int i=0; i<tList.length; i++) {
               String t = tList[i].trim();
               
               if(t.startsWith("matrix(")) {
                    t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
                    String[] u = t.split(",");
                    double[] m = new double[6];
                    m[0] = new Double(u[0]);
                    m[1] = new Double(u[2]);
                    m[2] = new Double(u[4]);
                    m[3] = new Double(u[1]);
                    m[4] = new Double(u[3]);
                    m[5] = new Double(u[5]);
                    point = transformMatrix(point, m);
               }
               
               else if(t.startsWith("translate(")) {
                    t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
                    String[] u = t.split(",");
                    double tx = new Double(u[0]);
                    double ty;
                    if(u.length > 1)
                         ty = new Double(u[1]);
                    else
                         ty = 0;
                    point = transformTranslate(point, tx, ty);
               }
               
               else if(t.startsWith("scale(")) {
                    t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
                    String[] u = t.split(",");
                    double sx = new Double(u[0]);
                    double sy;
                    if(u.length > 1)
                         sy = new Double(u[1]);
                    else
                         sy = new Double(u[0]);
                    point = transformScale(point, sx, sy);
               }
               
               else if(t.startsWith("rotate(")) {
                    t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
                    String[] u = t.split(",");
                    double a = new Double(u[0]);
                    if(u.length == 1)
                         point = transformRotate(point, a);
                    else if(u.length == 3) {
                         double cx = new Double(u[1]);
                         double cy = new Double(u[2]);
                         point = transformRotate(point, a, cx, cy);
                    }
               }
               
               else if(t.startsWith("skewX(")) {
                    t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
                    point = transformSkewX(point, new Double(t));
               }
               
               else if(t.startsWith("skewY(")) {
                    t = t.substring(t.indexOf("(") + 1, t.lastIndexOf(")"));
                    point = transformSkewY(point, new Double(t));
               }
          }
          System.err.println("Result: (" + point[0] + "," + point[1] + ")");
          return point;
     }

     /**
      * Apply a matrix transformation.
      * <p><b>WARNING</b> - <em>Matrix specification (by row)
      * differs from standard SVG order (by column)</em>
      * @param point {@code {x,y}}
      * @param matrix {@code {a,c,e,b,d,f}}
      * @return {@code {x,y}}
      */
     private double[] transformMatrix(double[] point, double[] matrix) {
          if(point == null || matrix == null || point.length != 2 || matrix.length != 6)
               throw new IllegalArgumentException("Malformed matrices - point: " + point +"; matrix: " + matrix);
          double[] r = new double[2];
          r[0] = point[0]*matrix[0] + point[1]*matrix[1] + 1*matrix[2];
          r[1] = point[0]*matrix[3] + point[1]*matrix[4] + 1*matrix[5];
          return r;
     }
     
     /**
      * Apply a translation
      * @param point {@code {x,y}}
      * @param tx Delta x
      * @param ty Delta y
      * @return {@code {x,y}}
      */
     private double[] transformTranslate(double[] point, double tx, double ty) {
          double[] matrix = {1,0,tx,0,1,ty};
          return transformMatrix(point, matrix);
     }
     
     /**
      * Apply a scale
      * @param point {@code {x,y}}
      * @param sx x scale factor
      * @param sy y scale factor
      * @return {@code {x,y}}
      */
     private double[] transformScale(double[] point, double sx, double sy) {
          double[] matrix = {sx,0,0,0,sy,0};
          return transformMatrix(point, matrix);
     }
     
     /**
      * Apply a rotation about the origin
      * @param point {@code {x,y}}
      * @param a Angle, in degrees
      * @return {@code {x,y}}
      */
     private double[] transformRotate(double[] point, double a) {
          a = a * Math.PI / 180;
          double[] matrix = {Math.cos(a), -Math.sin(a), 0, Math.sin(a), Math.cos(a), 0};
          return transformMatrix(point, matrix);
     }
     
     /**
      * Apply a rotation about center point {@code {cx,cy}}
      * @param point {@code {x,y}}
      * @param a Angle, in degrees
      * @return {@code {x,y}}
      */
     private double[] transformRotate(double[] point, double a, double cx, double cy) {
          return transformTranslate(transformRotate(transformTranslate(point, cx, cy), a), -cx, -cy);
     }
     
     /**
      * Skew along the x-axis
      * @param point {@code {x,y}}
      * @param a Angle, in degrees
      * @return {@code {x,y}}
      */
     private double[] transformSkewX(double[] point, double a) {
          a = a * Math.PI / 180;
          double[] matrix = {1, Math.tan(a), 0, 0,1,0};
          return transformMatrix(point, matrix);
     }
     
     /**
      * Skew along the y-axis
      * @param point {@code {x,y}}
      * @param a Angle, in degrees
      * @return {@code {x,y}}
      */
     private double[] transformSkewY(double[] point, double a) {
          a = a * Math.PI / 180;
          double[] matrix = {1,0,0, Math.tan(a),1,0};
          return transformMatrix(point, matrix);
     }
     
     /** Get the clipping path of an image */
     private Element getClipPath(Element img, Document dom) {
          String clip = img.getAttribute("clip-path");
          if(clip == null || clip.equals(""))
               return null;
          int a = clip.indexOf("#");
          int b = clip.indexOf(")");
          if(a >= 0 && b >= 0)
               clip = clip.substring(a+1, b);
          System.err.println("Image " + img.getAttribute("xlink:href") + " has clip-path " + clip);
          NodeList clips = dom.getElementsByTagName("clipPath");
          Element clipElement = null;
          for(int i=0; i<clips.getLength(); i++)
               if(((Element)clips.item(i)).getAttribute("id").equals(clip))
                    { clipElement = (Element)clips.item(i); break; }
          return clipElement;
     }
     
     /** Get a file using a file open dialog */
     private File openDialog() {
          JFileChooser fc = new JFileChooser();
          if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
               return null;
          return fc.getSelectedFile();
     }
     
     /** Read an XML file and return a DOM */
     private Document readSVG(File f) throws EmbedAndCropException {
          Document svg;
          DocumentBuilder db;
          try {
               db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
          } catch(ParserConfigurationException e)
          { throw new EmbedAndCropException("Can't deal with XML: " + e.getMessage()); }
          try{ 
               svg = db.parse(f);
          } catch(Throwable t)
          { throw new EmbedAndCropException("Can't read file: " + t.getMessage()); }
          return svg;
     }
     
     // -- Tests --
     
     public void test() throws EmbedAndCropException {
          Document dom = readSVG(openDialog());
          getOutputParams();
          process(dom, "");
          saveAs(dom);
     }
     
     public static void main( String[] args ) {
        EmbedAndCrop ec = new EmbedAndCrop();
        try{ ec.runInkscapeExtension(args); }
        catch(Throwable t) {
             t.printStackTrace();
             JOptionPane.showMessageDialog(null, t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
             System.exit(1);
        }
        System.exit(0);
     }
}
