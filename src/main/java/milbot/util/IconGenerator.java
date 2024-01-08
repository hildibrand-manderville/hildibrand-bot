package milbot.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class IconGenerator {

    private static final int FONT_SIZE = 25;
    private static final String FONT = "Arial Black";
    private static final int MARGIN = 12;
    private static final int ADDITIONAL_X_MARGIN = 5;
    private static final int ARC = MARGIN;
    private static final Font f = new Font(FONT, Font.BOLD, FONT_SIZE);

    public static BufferedImage writeRaidNumberOnImage(File input, int raids) throws IOException {
        BufferedImage bf = ImageIO.read(input);
        Graphics2D img = bf.createGraphics();
        if (raids > 0) {
            img.setColor(Color.GREEN);
            img.fillRoundRect(MARGIN / 2 + ADDITIONAL_X_MARGIN, bf.getHeight() - MARGIN * 2 - FONT_SIZE, MARGIN * 3, MARGIN + FONT_SIZE, ARC, ARC);
            img.setColor(Color.BLACK);
            img.drawRoundRect(MARGIN / 2 + ADDITIONAL_X_MARGIN, bf.getHeight() - MARGIN * 2 - FONT_SIZE, MARGIN * 3, MARGIN + FONT_SIZE, ARC, ARC);
            img.setFont(f);
            String str;
            if (raids > 9) {
                str = "9+";
            } else {
                str = String.valueOf(raids);
            }
            Rectangle2D stringBounds = img.getFont().getStringBounds(str, img.getFontRenderContext());
            img.drawString(str, (int) (MARGIN * 3 / 2 - stringBounds.getCenterX() / 2) + ADDITIONAL_X_MARGIN, (int) (bf.getHeight() - FONT_SIZE - stringBounds.getCenterY() / 2));
        }

        return bf;
    }

}
