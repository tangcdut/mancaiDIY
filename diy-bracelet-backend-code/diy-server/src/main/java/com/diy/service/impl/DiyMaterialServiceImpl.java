package com.diy.service.impl;

import com.diy.entity.DiyMaterial;
import com.diy.mapper.DiyMaterialMapper;
import com.diy.result.PageResult;
import com.diy.service.DiyMaterialService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class DiyMaterialServiceImpl implements DiyMaterialService {

    @Autowired
    private DiyMaterialMapper diyMaterialMapper;

    @Override
    public List<DiyMaterial> list(String categoryKey, String colorSeriesKey) {
        List<String> categories = categoryKey != null ? Arrays.asList(categoryKey) : null;
        List<String> colorSeries = colorSeriesKey != null ? Arrays.asList(colorSeriesKey) : null;
        return diyMaterialMapper.list(categories, colorSeries);
    }

    @Override
    public PageResult page(Integer page, Integer pageSize, String categoryKey, String colorSeriesKey) {
        // 开启分页
        PageHelper.startPage(page, pageSize);
        
        List<String> categories = categoryKey != null ? Arrays.asList(categoryKey) : null;
        List<String> colorSeriesList = colorSeriesKey != null ? Arrays.asList(colorSeriesKey) : null;
        
        // 执行查询
        Page<DiyMaterial> pageResult = (Page<DiyMaterial>) diyMaterialMapper.list(categories, colorSeriesList);
        
        // 封装分页结果
        return new PageResult(pageResult.getTotal(), pageResult.getResult());
    }

    @Override
    public DiyMaterial getById(Long id) {
        return diyMaterialMapper.getById(id);
    }

    @Override
    public void add(DiyMaterial material) {
        diyMaterialMapper.insert(material);
    }

    @Override
    public void update(DiyMaterial material) {
        diyMaterialMapper.update(material);
    }

    @Override
    public void delete(Long id) {
        diyMaterialMapper.delete(id);
    }

    @Override
    public void updateStatus(Integer status, List<Long> ids) {
        for (Long id : ids) {
            diyMaterialMapper.updateStatus(id, status);
        }
    }
}
