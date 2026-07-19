package com.diy.service.impl;

import com.diy.dto.ProductPageQueryDTO;
import com.diy.entity.Product;
import com.diy.entity.ProductImage;
import com.diy.mapper.ProductImageMapper;
import com.diy.mapper.ProductMapper;
import com.diy.result.PageResult;
import com.diy.service.ProductService;
import com.diy.utils.MinioUtil;
import com.diy.vo.ProductListVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;
    
    @Autowired
    private MinioUtil minioUtil;

    /**
     * 根据分类ID查询商品列表
     * @param categoryId 分类ID
     * @return 商品列表
     */
    @Override
    public List<Product> listByCategoryId(Long categoryId) {
        log.info("根据分类ID查询商品列表: categoryId={}", categoryId);
        return productMapper.listByCategoryId(categoryId);
    }
    
    /**
     * 根据ID查询商品详情
     * @param id 商品ID
     * @return 商品详情
     */
    @Override
    public Product getById(Long id) {
        log.info("根据ID查询商品详情: id={}", id);
        Product product = productMapper.getById(id);
        if (product == null) {
            return null;
        }

        List<String> all = productImageMapper.listUrlsByProductId(id);
        if (all == null) {
            all = new ArrayList<>();
        }

        // 详情图：排除封面（封面单独通过 coverImage 返回）
        String cover = product.getCoverImage();
        List<String> detailImages = new ArrayList<>();
        for (String url : all) {
            if (url == null || url.isEmpty()) {
                continue;
            }
            if (cover != null && !cover.isEmpty() && cover.equals(url)) {
                continue;
            }
            detailImages.add(url);
        }
        product.setImages(detailImages);
        return product;
    }
    
    /**
     * 商品分页查询
     * @param productPageQueryDTO 分页查询条件
     * @return 分页结果
     */
    @Override
    public PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO) {
        log.info("商品分页查询: {}", productPageQueryDTO);
        PageHelper.startPage(productPageQueryDTO.getPage(), productPageQueryDTO.getPageSize());
        Page<Product> page = productMapper.pageQuery(productPageQueryDTO);
        
        // 转换为VO对象，包含分类名称
        List<ProductListVO.ProductItem> productItems = page.getResult().stream()
                .map(product -> ProductListVO.ProductItem.builder()
                        .id(product.getId())
                        .categoryId(product.getCategoryId())
                        .categoryName(product.getCategoryName())
                        .title(product.getTitle())
                        .coverImage(product.getCoverImage())
                        .price(product.getPrice())
                        .priceOverseas(product.getPriceOverseas())
                        .stock(product.getStock())
                        .status(product.getStatus())
                        .build())
                .collect(Collectors.toList());
        
        return new PageResult(page.getTotal(), productItems);
    }
    
    /**
     * 新增商品
     * @param product 商品信息
     */
    @Override
    public void add(Product product) {
        log.info("新增商品: {}", product);
        product.setCreateTime(LocalDateTime.now());

        // 兼容：若传了 images 但没传 coverImage，默认第一张为封面
        if ((product.getCoverImage() == null || product.getCoverImage().isEmpty())
                && product.getImages() != null && !product.getImages().isEmpty()) {
            product.setCoverImage(product.getImages().get(0));
        }

        productMapper.insert(product);

        // 落库多图（即使只传封面，也会补一条明细）
        saveOrPatchProductImagesAfterInsert(product);
    }
    
    /**
     * 修改商品
     * @param product 商品信息
     */
    @Override
    public void update(Product product) {
        log.info("修改商品: {}", product);

        // 兼容：若传了 images 但没传 coverImage，默认第一张为封面
        if ((product.getCoverImage() == null || product.getCoverImage().isEmpty())
                && product.getImages() != null && !product.getImages().isEmpty()) {
            product.setCoverImage(product.getImages().get(0));
        }

        // 删除旧封面图（如果本次更新传了新封面图，且与旧图不同）
        if (product.getId() != null) {
            Product old = productMapper.getById(product.getId());
            if (old != null) {
                String oldCover = old.getCoverImage();
                String newCover = product.getCoverImage();
                if (oldCover != null && !oldCover.isEmpty()
                        && newCover != null && !newCover.isEmpty()
                        && !oldCover.equals(newCover)) {
                    log.info("删除旧商品封面图: {}", oldCover);
                    minioUtil.delete(oldCover);
                }
            }
        }
        productMapper.update(product);

        // images != null 表示本次明确要更新图片列表（可为空表示清空并只保留封面）
        if (product.getId() != null && product.getImages() != null) {
            replaceProductImages(product.getId(), product.getCoverImage(), product.getImages());
        } else if (product.getId() != null
                && product.getCoverImage() != null && !product.getCoverImage().isEmpty()) {
            // 只更新封面，不影响原有详情图
            int updated = productImageMapper.updateCoverUrl(product.getId(), product.getCoverImage());
            if (updated == 0) {
                ProductImage cover = ProductImage.builder()
                        .productId(product.getId())
                        .imageUrl(product.getCoverImage())
                        .sort(0)
                        .isCover(1)
                        .createTime(LocalDateTime.now())
                        .build();
                productImageMapper.insert(cover);
            }
        }
    }
    
    /**
     * 修改商品状态（上架/下架）
     * @param id 商品ID
     * @param status 状态：1上架 0下架
     */
    @Override
    public void changeStatus(Long id, Integer status) {
        log.info("修改商品状态: id={}, status={}", id, status);
        Product product = Product.builder()
                .id(id)
                .status(status)
                .build();
        productMapper.update(product);
    }
    
    /**
     * 删除商品
     * @param id 商品ID
     */
    @Override
    public void delete(Long id) {
        log.info("删除商品: id={}", id);
        // 删除商品前先删除封面图文件
        Product old = productMapper.getById(id);
        if (old != null && old.getCoverImage() != null && !old.getCoverImage().isEmpty()) {
            log.info("删除商品封面图文件: {}", old.getCoverImage());
            minioUtil.delete(old.getCoverImage());
        }
        productMapper.deleteById(id);
    }

    private void saveOrPatchProductImagesAfterInsert(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        // insert 后，若 images/cover 都为空则不写明细
        if ((product.getCoverImage() == null || product.getCoverImage().isEmpty())
                && (product.getImages() == null || product.getImages().isEmpty())) {
            return;
        }
        replaceProductImages(product.getId(), product.getCoverImage(), product.getImages());
    }

    private void replaceProductImages(Long productId, String coverImage, List<String> images) {
        // 计算“最终图片序列”：封面在首位，其余去重保序
        Set<String> dedup = new LinkedHashSet<>();
        if (coverImage != null && !coverImage.isEmpty()) {
            dedup.add(coverImage);
        }
        if (images != null) {
            for (String url : images) {
                if (url != null && !url.isEmpty()) {
                    dedup.add(url);
                }
            }
        }

        // 若最终仍为空，则直接清空明细
        productImageMapper.deleteByProductId(productId);
        if (dedup.isEmpty()) {
            return;
        }

        List<ProductImage> rows = new ArrayList<>();
        int sort = 0;
        LocalDateTime now = LocalDateTime.now();
        for (String url : dedup) {
            rows.add(ProductImage.builder()
                    .productId(productId)
                    .imageUrl(url)
                    .sort(sort)
                    .isCover(sort == 0 ? 1 : 0)
                    .createTime(now)
                    .build());
            sort++;
        }
        productImageMapper.insertBatch(rows);
    }
}