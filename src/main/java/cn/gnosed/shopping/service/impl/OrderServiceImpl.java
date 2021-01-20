package cn.gnosed.shopping.service.impl;

import cn.gnosed.shopping.entity.Order;
import cn.gnosed.shopping.mapper.OrderMapper;
import cn.gnosed.shopping.service.IOrderService;
import cn.gnosed.shopping.util.DateTimeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Gnosed Lu
 * @since 2020-07-22
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private static Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Override
    public void insert(Integer goodId, Integer quantity, Integer stock, boolean isPlace) {
        Order order = new Order();
        order.setGoodId(goodId);
        order.setAmount(quantity);
        order.setStockAmount(stock);
        order.setCreateTime(DateTimeUtil.getCurrentTime());
        order.setStatus(isPlace);
        save(order);
        logger.info(order.toString());
    }

    @Override
    public void successPlace(Integer goodId, Integer quantity, Integer stock) {
        insert(goodId, quantity, stock, true);
    }

    @Override
    public void failPlace(Integer goodId, Integer quantity, Integer stock) {
        insert(goodId, quantity, stock, false);
    }
}
