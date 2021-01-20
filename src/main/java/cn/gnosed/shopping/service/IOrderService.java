package cn.gnosed.shopping.service;

import cn.gnosed.shopping.entity.Order;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author Gnosed Lu
 * @since 2020-07-22
 */
public interface IOrderService extends IService<Order> {

    void insert(Integer goodId, Integer quantity, Integer stock, boolean isPlace);

    void successPlace(Integer goodId, Integer quantity, Integer stock);

    void failPlace(Integer goodId, Integer quantity, Integer stock);
}
