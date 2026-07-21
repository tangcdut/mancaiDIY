package com.diy.controller.user;

import com.diy.dto.DesignRenderDTO;
import com.diy.result.Result;
import com.diy.utils.MinioUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DIY 设计图后端渲染接口。
 * 前端把每颗珠子的最终位置/尺寸/旋转算好发过来，这里用 Java Graphics2D 确定性地合成，
 * 避免浏览器 canvas 在不同设备上的缩放/偏移坑。对游客开放(/guest/**)。
 */
@RestController
@Api(tags = "DIY设计图渲染")
@Slf4j
public class DesignRenderController {

    @Autowired
    private MinioUtil minioUtil;

    private static final String IMG_PREFIX = "/admin/common/image/";

    @ApiOperation("后端合成 DIY 设计图")
    @PostMapping({"/user/design/render", "/guest/design/render"})
    public Result<Map<String, String>> render(@RequestBody DesignRenderDTO spec) {
        try {
            int size = spec.getSize() != null ? spec.getSize() : 1000;
            size = Math.max(200, Math.min(size, 2000)); // 限幅，防滥用

            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // 白底
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);

            // 1) 下层大珠子
            if (spec.getBeads() != null) {
                for (DesignRenderDTO.Bead b : spec.getBeads()) {
                    if (Boolean.TRUE.equals(b.getBelow())) drawBead(g, b);
                }
            }
            // 2) 绳子
            if (spec.getRope() != null) drawRope(g, spec.getRope());
            // 3) Logo
            if (spec.getLogo() != null) drawLogo(g, spec.getLogo());
            // 4) 上层普通珠子
            if (spec.getBeads() != null) {
                for (DesignRenderDTO.Bead b : spec.getBeads()) {
                    if (!Boolean.TRUE.equals(b.getBelow())) drawBead(g, b);
                }
            }

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            String objectName = "diy_designs/diy_design/" + UUID.randomUUID().toString().replace("-", "") + ".jpg";
            String path = minioUtil.upload(baos.toByteArray(), objectName);

            Map<String, String> result = new HashMap<>();
            result.put("path", path);
            result.put("url", path);
            return Result.success(result);
        } catch (Exception e) {
            log.error("设计图渲染失败", e);
            return Result.error("设计图生成失败");
        }
    }

    private void drawBead(Graphics2D g, DesignRenderDTO.Bead b) {
        if (b == null || b.getCx() == null || b.getCy() == null || b.getSize() == null) return;
        double cx = b.getCx(), cy = b.getCy(), sz = b.getSize();
        if (sz <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(cx, cy);
            if (b.getRot() != null) g2.rotate(b.getRot());
            if (Boolean.TRUE.equals(b.getMirror())) g2.scale(-1, 1);

            BufferedImage bead = loadImage(b.getUrl());
            if (bead != null) {
                // aspectFit：把图片完整装入 sz×sz 方形
                double iw = bead.getWidth(), ih = bead.getHeight();
                double dw = sz, dh = sz;
                if (iw > 0 && ih > 0) {
                    double ar = ih / iw;
                    if (ar >= 1) { dh = sz; dw = sz / ar; } else { dw = sz; dh = sz * ar; }
                }
                g2.drawImage(bead, (int) Math.round(-dw / 2), (int) Math.round(-dh / 2),
                        (int) Math.round(dw), (int) Math.round(dh), null);
            } else {
                g2.setColor(parseColor(b.getColor(), new Color(0xe8, 0xe8, 0xe8)));
                g2.fill(new Ellipse2D.Double(-sz / 2, -sz / 2, sz, sz));
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawRope(Graphics2D g, DesignRenderDTO.Rope r) {
        if (r == null || r.getCx() == null || r.getCy() == null || r.getR() == null) return;
        double cx = r.getCx(), cy = r.getCy(), rad = r.getR();
        if (rad <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(parseColor(r.getColor(), new Color(0xe0, 0xe0, 0xe0)));
            float w = r.getWidth() != null ? r.getWidth().floatValue() : 2f;
            g2.setStroke(new BasicStroke(Math.max(1f, w)));
            g2.draw(new Ellipse2D.Double(cx - rad, cy - rad, rad * 2, rad * 2));
        } finally {
            g2.dispose();
        }
    }

    private void drawLogo(Graphics2D g, DesignRenderDTO.Logo l) {
        if (l == null || l.getCx() == null || l.getCy() == null || l.getSize() == null) return;
        BufferedImage logo = loadImage(l.getUrl());
        if (logo == null) return; // Logo 缺失则留空(次要)
        double cx = l.getCx(), cy = l.getCy(), sz = l.getSize();
        g.drawImage(logo, (int) Math.round(cx - sz / 2), (int) Math.round(cy - sz / 2),
                (int) Math.round(sz), (int) Math.round(sz), null);
    }

    /** 加载图片：MinIO 优先(/admin/common/image/KEY)，否则 http 拉取。失败返回 null。 */
    private BufferedImage loadImage(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        try {
            String u = url.trim();
            byte[] data = null;
            int idx = u.indexOf(IMG_PREFIX);
            if (idx >= 0) {
                String key = u.substring(idx + IMG_PREFIX.length());
                try {
                    key = URLDecoder.decode(key, StandardCharsets.UTF_8.name());
                } catch (Exception ignore) {
                }
                data = minioUtil.download(key);
            } else if (u.startsWith("http://") || u.startsWith("https://")) {
                data = httpGet(u);
            } else {
                data = minioUtil.download(u.startsWith("/") ? u.substring(1) : u);
            }
            if (data == null || data.length == 0) return null;
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (Exception e) {
            log.warn("渲染加载图片失败: {}", url, e);
            return null;
        }
    }

    private byte[] httpGet(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            log.warn("HTTP 拉取图片失败: {}", url, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private Color parseColor(String s, Color def) {
        if (s == null) return def;
        String c = s.trim();
        try {
            if (c.startsWith("#")) {
                if (c.length() == 7) return Color.decode(c);
                if (c.length() == 4) {
                    int r = Integer.parseInt("" + c.charAt(1) + c.charAt(1), 16);
                    int gg = Integer.parseInt("" + c.charAt(2) + c.charAt(2), 16);
                    int bb = Integer.parseInt("" + c.charAt(3) + c.charAt(3), 16);
                    return new Color(r, gg, bb);
                }
            }
        } catch (Exception ignore) {
        }
        return def;
    }
}
