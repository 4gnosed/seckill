package cn.gnosed.shopping.constroller;


import cn.gnosed.shopping.entity.Good;
import cn.gnosed.shopping.result.Result;
import cn.gnosed.shopping.result.ResultFactory;
import cn.gnosed.shopping.service.IGoodService;
import cn.gnosed.shopping.service.IOrderService;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.gnosed.shopping.base.AbstractClass;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Gnosed Lu
 * @since 2020-07-22
 */
@RestController
@RequestMapping("/good")
public class GoodController extends AbstractClass {

    @Autowired
    IGoodService iGoodService;

    @GetMapping("/buy")
    public Result buyGood(@RequestParam("goodId") Integer goodId,
                          @RequestParam("quantity") Integer quantity,
                          @RequestParam("userId") Integer userId) {
        if (iGoodService.buy(goodId, quantity, userId)) {
            return ResultFactory.buildSuccessResult("");
        }
        return ResultFactory.buildFailResult("购买失败");
    }

    @PostMapping("")
    public Result saveGood(@RequestBody Good good) {
        logger.info("保存商品入参：{}", JSON.toJSONString(good));
        good.setOldStock(good.getStock());
        boolean save = iGoodService.save(good);
        if (save) {
            return ResultFactory.buildSuccessResult();
        } else {
            return ResultFactory.buildFailResult("保存失败");
        }

    }
}
