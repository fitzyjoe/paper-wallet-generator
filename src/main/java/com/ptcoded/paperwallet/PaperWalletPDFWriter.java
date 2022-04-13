package com.ptcoded.paperwallet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

public class PaperWalletPDFWriter
{
    // PDF standard... note points not pixels
    private static final float POINTS_PER_INCH = 72;

    // size of qr codes
    private static final int QR_CODE_WIDTH_PX = 80;
    private static final int QR_CODE_HEIGHT_PX = 80;

    // size of entire pdf
    private static final int WIDTH_INCHES = 9;
    private static final int HEIGHT_INCHES = 3;

    private static BufferedImage hexToQRImage(String hex) throws WriterException
    {
        final var hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.MARGIN, 1);

        final var privateKeyQrMatrix = new MultiFormatWriter().encode(hex, BarcodeFormat.QR_CODE, QR_CODE_WIDTH_PX, QR_CODE_HEIGHT_PX, hints);
        return MatrixToImageWriter.toBufferedImage(privateKeyQrMatrix);
    }

    public void generatePDF(final String privateKeyWIF, final String addressWIF) throws WriterException, IOException
    {
        // generate qr codes
        final var privateKeyQrImage = hexToQRImage(privateKeyWIF);
        final var addressQrImage = hexToQRImage(addressWIF);

        // create the pdf
        try (PDDocument document = new PDDocument())
        {
            final var width_points = POINTS_PER_INCH * WIDTH_INCHES;
            final var height_points = POINTS_PER_INCH * HEIGHT_INCHES;

            final var rect = new PDRectangle(width_points, height_points);
            final var page = new PDPage(rect);
            document.addPage(page);

            // draw the background
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false))
            {
                final var backgroundUrl = getClass().getClassLoader().getResource("background.png");
                if (backgroundUrl != null)
                {
                    final var backgroundImage = ImageIO.read(backgroundUrl);
                    final var backgroundPdImage = LosslessFactory.createFromImage(document, backgroundImage);
                    contentStream.drawImage(backgroundPdImage, 0, 0, width_points, height_points);
                }

                final var pdImageXObjectAddr = LosslessFactory.createFromImage(document, addressQrImage);
                contentStream.drawImage(pdImageXObjectAddr, 8, 36, QR_CODE_WIDTH_PX, QR_CODE_HEIGHT_PX);

                final var pdImageXObjectPriv = LosslessFactory.createFromImage(document, privateKeyQrImage);
                contentStream.drawImage(pdImageXObjectPriv, 410, 36, QR_CODE_WIDTH_PX, QR_CODE_HEIGHT_PX);

                contentStream.beginText();
                contentStream.newLineAtOffset(7, 5);
                contentStream.setFont(PDType1Font.COURIER, 6f);
                contentStream.showText(addressWIF);

                contentStream.newLineAtOffset(405, 0);
                contentStream.setFont(PDType1Font.COURIER, 6f);
                contentStream.showText(privateKeyWIF);

                contentStream.endText();
            }

            document.save("wallet.pdf");
        }
    }
}
