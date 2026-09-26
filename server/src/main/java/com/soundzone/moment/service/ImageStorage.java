package com.soundzone.moment.service;

import com.soundzone.common.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

import javax.imageio.ImageIO;

/** 本地持久文件存储适配，可替换为宿主对象存储；无公共静态目录。 */
@Service
public class ImageStorage {
    @Value("${soundzone.image-directory:./data/images}")
    private String directory;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new BizException(ResultCode.PARAM_INVALID, "请选择 5MB 内的 JPEG 或 PNG 图片");
        String key = UUID.randomUUID().toString() + ".jpg";
        Path path = resolve(key);
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BizException(ResultCode.PARAM_INVALID, "图片格式无法识别");
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName().toLowerCase();
                if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg"))
                    throw new BizException(ResultCode.PARAM_INVALID, "仅支持 JPEG 和 PNG");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if ((long) width * height > 16000000 || width <= 0 || height <= 0)
                    throw new BizException(ResultCode.PARAM_INVALID, "图片尺寸过大");
                var original = reader.read(0);
                var clean =
                        new java.awt.image.BufferedImage(
                                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics = clean.createGraphics();
                try {
                    graphics.setColor(java.awt.Color.WHITE);
                    graphics.fillRect(0, 0, width, height);
                    graphics.drawImage(original, 0, 0, null);
                } finally {
                    graphics.dispose();
                }
                Files.createDirectories(path.getParent());
                if (!ImageIO.write(clean, "jpg", path.toFile()))
                    throw new IOException("图片编码失败"); // 重新编码，去除 EXIF 等元数据
            } finally {
                reader.dispose();
            }
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                /* 保留原读取错误，由存储清理重试处理残留文件。 */
            }
            throw new BizException(ResultCode.PARAM_INVALID, "图片读取失败");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED)
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    /* 定期文件清理可重试 */
                                }
                        }
                    });
        return key;
    }

    public Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-f-]{36}\\.jpg"))
            throw new BizException(ResultCode.MOMENT_NOT_FOUND);
        return Path.of(directory).toAbsolutePath().normalize().resolve(key);
    }
}
