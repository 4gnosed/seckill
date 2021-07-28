package cn.gnosed.shopping.constroller;


import cn.gnosed.shopping.constant.Constant;
import cn.gnosed.shopping.entity.Good;
import cn.gnosed.shopping.entity.Order;
import cn.gnosed.shopping.result.Result;
import cn.gnosed.shopping.result.ResultFactory;
import cn.gnosed.shopping.service.IGoodService;
import cn.gnosed.shopping.service.IOrderService;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cn.gnosed.shopping.base.AbstractClass;

import java.text.DecimalFormat;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Gnosed Lu
 * @since 2020-07-22
 */
@RestController
@RequestMapping("/order")
public class OrderController extends AbstractClass {

    @Autowired
    IOrderService iOrderService;
    @Autowired
    IGoodService iGoodService;

    @GetMapping("")
    public Result queryOrder(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam String goodId) {
        QueryWrapper<Order> q = new QueryWrapper<>();
        q.eq(Constant.GOOD_ID, goodId).orderByDesc(Constant.CREATE_TIME);
        Page<Order> orderPage = iOrderService.page(new Page<>(page, size), q);
        JSONObject data = new JSONObject();
        data.put("total", orderPage.getTotal());
        data.put("dataList", orderPage.getRecords());

        Good good = iGoodService.getGood(goodId);
        if (good != null) {
            DecimalFormat df = new DecimalFormat("0.00");
            String stockPercentage = df.format((float) good.getStock() / good.getOldStock());
            data.put("stock", good.getStock());
            data.put("stockPercentage", stockPercentage);
        } else {
            data.put("stock", 0);
            data.put("stockPercentage", 0);
        }

        return ResultFactory.buildSuccessResult(data);
    }
    
    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat("0.00");
        String stockPercentage = df.format((float) 88 / 100);  
        System.out.println(stockPercentage);
    }
}
