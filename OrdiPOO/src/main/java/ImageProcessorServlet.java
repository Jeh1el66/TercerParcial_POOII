import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.awt.Color;
import java.awt.Font;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.List;
import java.util.ArrayList;

@WebServlet("/ImageProcessorServlet")
@MultipartConfig
public class ImageProcessorServlet extends HttpServlet {
    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Part filePart = request.getPart("imagen");

            if (filePart == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se subió ninguna imagen");
                return;
            }

            try (InputStream fileInputStream = filePart.getInputStream()) {
                BufferedImage originalImage = ImageIO.read(fileInputStream);

                if (originalImage == null) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se pudo leer la imagen");
                    return;
                }

                Callable<BufferedImage> negativeTask = () -> applyNegative(cloneImage(originalImage));
                Callable<BufferedImage> blurTask = () -> applyBlur(cloneImage(originalImage));
                Callable<BufferedImage> sharpenTask = () -> applySharpen(cloneImage(originalImage));

                List<Future<BufferedImage>> futures = new ArrayList<>();
                futures.add(executorService.submit(negativeTask));
                futures.add(executorService.submit(blurTask));
                futures.add(executorService.submit(sharpenTask));

                BufferedImage negativeImage = futures.get(0).get();
                BufferedImage blurredImage = futures.get(1).get();
                BufferedImage sharpenedImage = futures.get(2).get();

                BufferedImage combinedImage = combineImagesWithLabels(originalImage, negativeImage, blurredImage, sharpenedImage);

                response.setContentType("image/jpeg");

                try (OutputStream out = response.getOutputStream()) {
                    ImageIO.write(combinedImage, "jpeg", out);
                    out.flush();
                }
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al procesar la imagen");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ocurrió un error al procesar la imagen");
        }
    }

    private BufferedImage cloneImage(BufferedImage image) {
        BufferedImage clonedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g2d = clonedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return clonedImage;
    }

    private BufferedImage applyNegative(BufferedImage image) {
        RescaleOp rescaleOp = new RescaleOp(-1.0f, 255f, null);
        BufferedImage negativeImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        rescaleOp.filter(image, negativeImage);
        return negativeImage;
    }

    private BufferedImage applyBlur(BufferedImage image) {
        float[] matrix = {
                1/16f, 2/16f, 1/16f,
                2/16f, 4/16f, 2/16f,
                1/16f, 2/16f, 1/16f
        };
        Kernel kernel = new Kernel(3, 3, matrix);
        ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage blurredImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        convolveOp.filter(image, blurredImage);
        return blurredImage;
    }

    private BufferedImage applySharpen(BufferedImage image) {
        float[] matrix = {
                -1.0f, -1.0f, -1.0f,
                -1.0f,  9.0f, -1.0f,
                -1.0f, -1.0f, -1.0f
        };
        Kernel kernel = new Kernel(3, 3, matrix);
        ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage sharpenedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        convolveOp.filter(image, sharpenedImage);
        return sharpenedImage;
    }

    private BufferedImage combineImagesWithLabels(BufferedImage original, BufferedImage img1, BufferedImage img2, BufferedImage img3) {
        int width = Math.max(original.getWidth(), img1.getWidth() + img2.getWidth() + img3.getWidth());
        int height = original.getHeight() + Math.max(img1.getHeight(), Math.max(img2.getHeight(), img3.getHeight())) + 100;
        BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = combinedImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        int margin = 50;
        int labelMargin = 20;

        g2d.drawImage(original, (width - original.getWidth()) / 2, margin, null);
        g2d.drawImage(img1, 0, original.getHeight() + margin + labelMargin, null);
        g2d.drawImage(img2, img1.getWidth(), original.getHeight() + margin + labelMargin, null);
        g2d.drawImage(img3, img1.getWidth() + img2.getWidth(), original.getHeight() + margin + labelMargin, null);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Imagen Original", (width - original.getWidth()) / 2 + original.getWidth() / 2 - g2d.getFontMetrics().stringWidth("Imagen Original") / 2, margin - 10);
        g2d.drawString("Negativo", img1.getWidth() / 2 - g2d.getFontMetrics().stringWidth("Negativo") / 2, original.getHeight() + margin);
        g2d.drawString("Desenfoque", img1.getWidth() + img2.getWidth() / 2 - g2d.getFontMetrics().stringWidth("Desenfoque") / 2, original.getHeight() + margin);
        g2d.drawString("Nitidez", img1.getWidth() + img2.getWidth() + img3.getWidth() / 2 - g2d.getFontMetrics().stringWidth("Nitidez") / 2, original.getHeight() + margin);

        g2d.dispose();

        return combinedImage;
    }

    @Override
    public void destroy() {
        executorService.shutdown();
        super.destroy();
    }
}