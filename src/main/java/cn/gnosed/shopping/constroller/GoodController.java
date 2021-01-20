package cn.gnosed.shopping.constroller;


import cn.gnosed.shopping.result.Result;
import cn.gnosed.shopping.result.ResultFactory;
import cn.gnosed.shopping.service.IGoodService;
import cn.gnosed.shopping.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
@RequestMapping("/api/good")
public class GoodController extends AbstractClass {

    @Autowired
    IGoodService iGoodService;

    @RequestMapping("/buy")
    public Result buyGood(@RequestParam("goodId") Integer goodId, @RequestParam("quantity") Integer quantity) {
        if(iGoodService.buy(goodId,quantity)){
            return ResultFactory.buildSuccessResult("");
        }
        return ResultFactory.buildFailResult("购买失败");
    }
}
